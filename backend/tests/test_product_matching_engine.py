from backend.core.matching.domain import (
    get_fuzzy_hydration,
    MatchingProfile,
    MatchingRule,
    ProductCandidate,
    ProductFunctionSignal,
    normalize_inci_key,
    parse_inci_ingredients,
)
from backend.core.matching.scoring import match_product


def test_normalize_inci_key_is_case_and_space_insensitive():
    assert normalize_inci_key("  NIACINAMIDE  ") == "niacinamide"
    assert normalize_inci_key("Sodium   Hyaluronate") == "sodium hyaluronate"


def test_parse_inci_ingredients_preserves_raw_names_and_positions():
    parsed = parse_inci_ingredients("Aqua, NIACINAMIDE,  Glycerin")

    assert parsed == [
        {"raw_name": "Aqua", "normalized_key": "aqua", "position": 1},
        {"raw_name": "NIACINAMIDE", "normalized_key": "niacinamide", "position": 2},
        {"raw_name": "Glycerin", "normalized_key": "glycerin", "position": 3},
    ]


def test_parse_inci_ingredients_accepts_semicolon_separated_lists():
    parsed = parse_inci_ingredients(
        "Deionized Water (Aqua); Isopropyl Myristate; Glycerin; Fragrance;"
        "Tocopheryl Acetate(Vitamin E); Azulene."
    )

    assert parsed == [
        {"raw_name": "Deionized Water (Aqua)", "normalized_key": "deionized water (aqua)", "position": 1},
        {"raw_name": "Isopropyl Myristate", "normalized_key": "isopropyl myristate", "position": 2},
        {"raw_name": "Glycerin", "normalized_key": "glycerin", "position": 3},
        {"raw_name": "Fragrance", "normalized_key": "fragrance", "position": 4},
        {"raw_name": "Tocopheryl Acetate(Vitamin E)", "normalized_key": "tocopheryl acetate(vitamin e)", "position": 5},
        {"raw_name": "Azulene", "normalized_key": "azulene", "position": 6},
    ]


def test_get_fuzzy_hydration_returns_membership_degrees():
    assert get_fuzzy_hydration(20) == {"dry": 1.0, "normal": 0.0, "hydrated": 0.0}
    assert get_fuzzy_hydration(40) == {"dry": 0.5, "normal": round(5 / 15, 4), "hydrated": 0.0}
    assert get_fuzzy_hydration(85) == {"dry": 0.0, "normal": 0.0, "hydrated": 1.0}


def test_fuzzy_hydration_rewards_moisturizing_products_proportionally():
    product = ProductCandidate(id=12, name="Hydration Serum", composition="Aqua, Glycerin", purpose="hydration")
    profile = MatchingProfile(answers={}, skin_state={"hydration": 40})

    result = match_product(product, profile, [])

    assert result.score_breakdown["skin_state_fit"] == 10
    assert any("нечетк" in item.lower() or "fuzzy" in item.lower() for item in result.explanations)


def test_russian_dryness_concern_rewards_moisturizing_cream():
    product = ProductCandidate(
        id=13,
        name="Увлажняющий крем",
        composition="Aqua, Glycerin, Panthenol",
        product_type="Крем",
        purpose="Увлажнение",
        skin_type="Сухая",
    )
    profile = MatchingProfile(answers={"concerns": ["Сухость"], "skin_type": ["Сухая"]})

    result = match_product(product, profile, [])

    assert result.score_breakdown["ingredient_function_fit"] > 0
    assert result.score_breakdown["skin_state_fit"] > 0
    assert result.decision == "recommend"
    assert result.compatibility_percent >= 50


def test_draft_rules_do_not_affect_matching():
    product = ProductCandidate(id=1, name="Retinol Cream", composition="Retinol")
    profile = MatchingProfile(answers={"pregnancy": ["true"]}, accepted_insights=[])
    rules = [MatchingRule(
        id=10,
        status="draft",
        target_type="ingredient",
        target_key="retinol",
        condition_type="pregnancy",
        condition_value="true",
        effect="block",
        weight_delta=0,
        severity="contraindication",
        source_id=5,
        evidence_quote="Retinoids are contraindicated during pregnancy.",
    )]

    result = match_product(product, profile, rules)

    assert result.decision == "caution"
    assert result.contraindications == []


def test_confirmed_block_rule_excludes_product_regardless_of_score():
    product = ProductCandidate(id=1, name="Retinol Cream", composition="Retinol, Niacinamide")
    profile = MatchingProfile(answers={"pregnancy": ["true"], "concerns": ["oiliness"]}, accepted_insights=[])
    rules = [
        MatchingRule(1, "confirmed", "ingredient", "retinol", "pregnancy", "true", "block", 0, "contraindication", 11, "Retinol contraindication."),
        MatchingRule(2, "confirmed", "ingredient", "niacinamide", "concern", "oiliness", "boost", 10, "info", 12, "Niacinamide helps oiliness."),
    ]

    result = match_product(product, profile, rules)

    assert result.decision == "exclude"
    assert result.final_score == 0
    assert result.compatibility_percent == 0
    assert result.contraindications == ["Retinol contraindication."]


def test_warning_rule_marks_product_as_caution_and_penalizes_score():
    product = ProductCandidate(id=2, name="Acid Toner", composition="Salicylic Acid")
    profile = MatchingProfile(answers={"concerns": ["acne"]}, accepted_insights=["damaged_barrier"])
    rules = [
        MatchingRule(3, "confirmed", "ingredient", "salicylic acid", "concern", "acne", "boost", 8, "info", 13, "Salicylic acid helps acne."),
        MatchingRule(4, "confirmed", "ingredient", "salicylic acid", "accepted_insight", "damaged_barrier", "warning", -4, "caution", 14, "Acids can irritate damaged barrier."),
    ]

    result = match_product(product, profile, rules)

    assert result.decision == "caution"
    assert result.score_breakdown["ingredient_function_fit"] > 0
    assert result.score_breakdown["safety_fit"] < 15
    assert result.compatibility_percent <= 69
    assert result.warnings == ["Acids can irritate damaged barrier."]


def test_non_ingredient_rules_do_not_apply_to_every_product():
    product = ProductCandidate(id=3, name="Basic Cream", composition="Glycerin")
    profile = MatchingProfile(answers={"concerns": ["acne"]}, accepted_insights=[])
    rules = [MatchingRule(5, "confirmed", "product_category", "toner", "concern", "acne", "block", 0, "contraindication", 15, "Do not apply globally.")]

    result = match_product(product, profile, rules)

    assert result.decision == "caution"
    assert result.contraindications == []


def test_rule_target_key_is_normalized_before_ingredient_matching():
    product = ProductCandidate(id=4, name="Retinol Cream", composition="retinol")
    profile = MatchingProfile(answers={"pregnancy": ["true"]}, accepted_insights=[])
    rules = [MatchingRule(6, "confirmed", "ingredient", "  Retinol ", "pregnancy", "true", "block", 0, "contraindication", 16, "Retinol contraindication.")]

    result = match_product(product, profile, rules)

    assert result.decision == "exclude"
    assert result.evidence_sources == [16]
    assert result.reason_codes == ["rule:6:block"]


def test_warning_rule_never_increases_score():
    product = ProductCandidate(id=5, name="Acid Toner", composition="Salicylic Acid")
    profile = MatchingProfile(answers={}, accepted_insights=["damaged_barrier"])
    rules = [MatchingRule(7, "confirmed", "ingredient", "salicylic acid", "accepted_insight", "damaged_barrier", "warning", 5, "caution", 17, "Warning should not boost.")]

    result = match_product(product, profile, rules)

    assert result.decision == "caution"
    assert result.score_breakdown["ingredient_function_fit"] == 0
    assert result.score_breakdown["safety_fit"] == 15
    assert result.compatibility_percent <= 69
    assert result.warnings == ["Warning should not boost."]


def test_low_boost_product_exposes_compatibility_percent_and_caution_decision():
    product = ProductCandidate(id=6, name="Niacinamide Serum", composition="Niacinamide")
    profile = MatchingProfile(answers={"concerns": ["oiliness"]}, accepted_insights=[])
    rules = [MatchingRule(8, "confirmed", "ingredient", "niacinamide", "concern", "oiliness", "boost", 8, "info", 18, "Niacinamide helps oiliness.")]

    result = match_product(product, profile, rules)

    assert result.decision == "recommend"
    assert result.score_breakdown["ingredient_function_fit"] > 0
    assert result.compatibility_percent >= 50


def test_low_score_product_is_caution_not_recommend():
    product = ProductCandidate(id=7, name="Basic Cream", composition="Aqua")
    profile = MatchingProfile(answers={"skin_type": ["dry"]}, accepted_insights=[])

    result = match_product(product, profile, [])

    assert result.compatibility_percent < 40
    assert result.decision == "caution"
    assert any("низкая совместимость" in item.lower() for item in result.explanations)


def test_match_result_exposes_score_breakdown_and_explanations():
    product = ProductCandidate(
        id=10,
        name="Niacinamide Gel",
        composition="Aqua, Niacinamide, Glycerin",
        product_type="Гель",
        purpose="oiliness",
        skin_type="combination",
    )
    profile = MatchingProfile(
        answers={"concerns": ["oiliness"], "skin_type": ["combination"]},
        accepted_insights=[],
        skin_state={"state_tags": ["oily_t_zone"], "zone_concerns": {"chin": ["oiliness"]}},
    )
    rules = [MatchingRule(20, "confirmed", "ingredient", "niacinamide", "concern", "oiliness", "boost", 15, "info", 101, "Niacinamide helps oiliness.")]

    result = match_product(product, profile, rules, semantic_score=0.5)

    assert result.decision == "recommend"
    assert result.compatibility_percent > 0
    assert set(result.score_breakdown) == {
        "ingredient_function_fit",
        "skin_state_fit",
        "safety_fit",
        "evidence_quality",
        "metadata_confirmation",
    }
    assert result.score_breakdown["safety_fit"] == 15
    assert result.score_breakdown["ingredient_function_fit"] > 0
    assert result.score_breakdown["skin_state_fit"] > 0
    assert result.score_breakdown["evidence_quality"] > 0
    assert any("ниацинамид" in item.lower() or "niacinamide" in item.lower() for item in result.explanations)


def test_block_rule_zeroes_breakdown_even_with_semantic_score():
    product = ProductCandidate(id=11, name="Retinol Cream", composition="Retinol, Glycerin", skin_type="dry")
    profile = MatchingProfile(answers={"pregnancy": ["true"], "skin_type": ["dry"]}, accepted_insights=[])
    rules = [MatchingRule(21, "confirmed", "ingredient", "retinol", "pregnancy", "true", "block", 0, "contraindication", 102, "Retinol contraindication.")]

    result = match_product(product, profile, rules, semantic_score=1.0)

    assert result.decision == "exclude"
    assert result.compatibility_percent == 0
    assert result.final_score == 0
    assert all(value == 0 for value in result.score_breakdown.values())


def test_dry_skin_product_with_hydration_functions_no_longer_scores_five_percent():
    product = ProductCandidate(
        id=1,
        name="Hydration Serum",
        composition="Glycerin, Panthenol",
        product_type="serum",
        purpose="hydration",
        skin_type="",
        function_signals=[
            ProductFunctionSignal(function_key="hydration", score=0.9, evidence_status="confirmed", evidence_count=2, source_ids=[7]),
            ProductFunctionSignal(function_key="barrier_support", score=0.6, evidence_status="auto_only", evidence_count=1, source_ids=[7]),
        ],
    )
    profile = MatchingProfile(answers={"concerns": ["dryness"]}, skin_state={"state_tags": ["dehydrated_areas"]})

    result = match_product(product, profile, rules=[])

    assert result.compatibility_percent >= 60
    assert result.score_breakdown["ingredient_function_fit"] > 0
    assert "hydration" in result.matched_functions
    assert any("увлаж" in text.lower() or "hydration" in text.lower() for text in result.explanations)


def test_metadata_without_composition_function_support_cannot_create_high_score():
    product = ProductCandidate(
        id=2,
        name="Marketing Only Cream",
        composition="",
        product_type="cream",
        purpose="hydration",
        skin_type="dryness",
        function_signals=[],
    )
    profile = MatchingProfile(answers={"concerns": ["dryness"], "skin_type": ["dryness"]})

    result = match_product(product, profile, rules=[])

    assert result.compatibility_percent < 40
    assert result.score_breakdown["metadata_confirmation"] == 0


def test_sensitive_skin_with_irritation_risk_gets_caution():
    product = ProductCandidate(
        id=3,
        name="Acid Peel",
        composition="Salicylic Acid",
        product_type="peel",
        function_signals=[
            ProductFunctionSignal(function_key="exfoliation", score=0.8, evidence_status="confirmed", evidence_count=1, source_ids=[2]),
            ProductFunctionSignal(function_key="irritation_risk", score=0.7, evidence_status="auto_only", evidence_count=1, source_ids=[2]),
        ],
    )
    profile = MatchingProfile(answers={"concerns": ["sensitivity"]}, skin_state={"state_tags": ["sensitive"]})

    result = match_product(product, profile, rules=[])

    assert result.decision == "caution"
    assert result.score_breakdown["safety_fit"] < 15
    assert result.warnings


def test_auto_warning_ingredient_evidence_creates_safety_caution():
    module = __import__("backend.core.ingredient_knowledge_admin", fromlist=["compute_product_function_profile_entries"])
    entries = module.compute_product_function_profile_entries([
        {
            "effect_key": "warning",
            "matching_effect": "warning",
            "matching_weight_delta": -8,
            "confidence": 0.9,
            "evidence_status": "auto_high_confidence",
            "source_id": 11,
        }
    ])

    product = ProductCandidate(
        id=5,
        name="Potentially Irritating Active",
        composition="Extracted Active",
        function_signals=[ProductFunctionSignal(**entries[0])],
    )
    profile = MatchingProfile(answers={"concerns": ["sensitivity"]}, skin_state={"state_tags": ["sensitive"]})

    result = match_product(product, profile, rules=[])

    assert entries[0]["function_key"] == "irritation_risk"
    assert result.decision == "caution"
    assert result.score_breakdown["safety_fit"] < 15
    assert result.warnings


def test_sensitive_skin_with_glycolic_acid_gets_caution_without_explicit_risk_signal():
    product = ProductCandidate(
        id=4,
        name="AHA Toner",
        composition="Aqua, Glycolic Acid",
        product_type="toner",
        function_signals=[
            ProductFunctionSignal(function_key="exfoliation", score=0.8, evidence_status="confirmed", evidence_count=1, source_ids=[9]),
        ],
    )
    profile = MatchingProfile(answers={"concerns": ["sensitivity"]}, skin_state={"state_tags": ["sensitive"]})

    result = match_product(product, profile, rules=[])

    assert result.decision == "caution"
    assert result.score_breakdown["safety_fit"] < 15
    assert result.warnings
