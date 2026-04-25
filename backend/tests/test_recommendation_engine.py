from datetime import datetime, timezone

import pytest

from backend.core.matching.domain import MatchingProfile, MatchingRule, ProductCandidate, ProductFunctionSignal
from backend.core.matching.scoring import match_product
from backend.core.recommendations.builder import build_recommendation
from backend.core.recommendations.profiles import (
    LINE_KEYS,
    ProductRecommendationInput,
    RecommendationGenerationError,
    build_extended_skin_profile,
    normalize_product_segment,
)


def _rule(rule_id, ingredient, condition_type, condition_value, effect="boost", weight_delta=8):
    return MatchingRule(
        id=rule_id,
        status="confirmed",
        target_type="ingredient",
        target_key=ingredient,
        condition_type=condition_type,
        condition_value=condition_value,
        effect=effect,
        weight_delta=weight_delta,
        severity="info",
        source_id=rule_id,
        evidence_quote=f"{ingredient} подходит для {condition_value}",
    )


def test_normalize_product_segment_maps_four_lines():
    assert normalize_product_segment("бюджетная") == "budget"
    assert normalize_product_segment("professional") == "professional"
    assert normalize_product_segment("люкс") == "luxury"
    assert normalize_product_segment("cosmeceutical") == "cosmeceutical"
    assert normalize_product_segment("unknown") == "budget"


def test_build_recommendation_requires_skin_passport_answers():
    with pytest.raises(RecommendationGenerationError) as exc_info:
        build_recommendation(
            answers={},
            accepted_insights=[],
            sensor_readings=[],
            procedures=[],
            products=[],
            rules=[],
        )

    assert exc_info.value.status_code == 400
    assert exc_info.value.message == "Чтобы собрать точную линейку, пройдите анкету кожи"


def test_extended_skin_profile_preserves_zone_concerns():
    profile = build_extended_skin_profile(
        answers={"skin_type": ["combination"], "concerns": ["texture"]},
        accepted_insights=[],
        sensor_readings=[{
            "measured_at": "2026-04-01T10:00:00+00:00",
            "zones": {
                "forehead": {"hydration": 1, "oiliness": 1},
                "chin": {"hydration": 3, "oiliness": 5},
                "nose": {"oiliness": 5},
                "cheeks": {"hydration": 2, "sensitivity": 4},
            },
        }],
    )

    assert profile["global_skin_type"] == "combination"
    assert "dryness" in profile["zone_concerns"]["forehead"]
    assert "oiliness" in profile["zone_concerns"]["chin"]
    assert "oily_t_zone" in profile["state_tags"]
    assert "dehydrated_areas" in profile["state_tags"]


def test_extended_skin_profile_maps_survey_answers_to_matching_concerns():
    profile = build_extended_skin_profile(
        answers={
            "skin_type": ["Сухая"],
            "skin_issues": ["Сухость", "Жирный блеск", "Акне"],
            "goals": ["Увлажнить кожу", "Уменьшить жирность"],
        },
        accepted_insights=[],
        sensor_readings=[],
    )

    assert profile["global_skin_type"] == "dry"
    assert "dryness" in profile["concerns"]
    assert "oiliness" in profile["concerns"]
    assert "acne" in profile["concerns"]
    assert "low_hydration" in profile["concerns"]


def test_survey_profile_concerns_affect_product_matching_score():
    answers = {
        "skin_type": ["Сухая"],
        "skin_issues": ["Сухость"],
        "goals": ["Увлажнить кожу"],
    }
    skin_state = build_extended_skin_profile(answers, accepted_insights=[], sensor_readings=[])
    product = ProductCandidate(
        id=99,
        name="Hydration Cream",
        composition="Aqua, Glycerin, Panthenol",
        product_type="Крем",
        purpose="Увлажнение",
        function_signals=[
            ProductFunctionSignal(function_key="hydration", score=0.9, evidence_status="confirmed", evidence_count=2, source_ids=[7]),
        ],
    )

    result = match_product(product, MatchingProfile(answers=answers, skin_state=skin_state), rules=[])

    assert result.compatibility_percent > 35
    assert "hydration" in result.matched_functions


def test_extended_skin_profile_expands_top_level_sensor_measurements():
    profile = build_extended_skin_profile(
        answers={"skin_type": ["Нормальная"]},
        accepted_insights=[],
        sensor_readings=[{"hydration": 2, "oiliness": 4, "measured_at": "2026-04-01T10:00:00+00:00"}],
    )

    assert profile["hydration"] == 2
    assert "dryness" in profile["concerns"]
    assert "oiliness" in profile["concerns"]
    assert "dehydrated_areas" in profile["state_tags"]
    assert "oily_t_zone" in profile["state_tags"]


def test_build_recommendation_returns_all_four_lines_without_measurements():
    products = [
        ProductRecommendationInput(
            id=1,
            name="Niacinamide Serum",
            brand="Aura Lab",
            segment="professional",
            product_type="Сыворотка",
            purpose=["oiliness"],
            skin_type=["combination"],
            composition="Niacinamide",
            application_info="Наносите утром после очищения",
        )
    ]
    rules = [_rule(1, "niacinamide", "concern", "oiliness")]

    result = build_recommendation(
        answers={"concerns": ["oiliness"], "skin_type": ["combination"]},
        accepted_insights=[],
        sensor_readings=[],
        procedures=[],
        products=products,
        rules=rules,
    )

    assert [line["key"] for line in result["lines"]] == LINE_KEYS
    assert result["input_quality"]["sensor_readings"] == "missing"
    assert "Замеры не учтены" in result["input_quality"]["notes"]
    professional = next(line for line in result["lines"] if line["key"] == "professional")
    morning_steps = professional["routine"]["morning"]
    assert morning_steps[0]["product_id"] == 1
    assert morning_steps[0]["product_name"] == "Niacinamide Serum"
    assert morning_steps[0]["step"] == "Сыворотка"
    assert "name" not in morning_steps[0]
    assert "product_type" not in morning_steps[0]
    assert morning_steps[0]["compatibility_percent"] >= 50
    assert morning_steps[0]["score_breakdown"]["ingredient_function_fit"] > 0
    assert morning_steps[0]["sequence"] == 1


def test_recent_procedure_adds_recovery_warning():
    recent_performed_at = datetime.now(timezone.utc).isoformat()

    result = build_recommendation(
        answers={"concerns": ["dehydration"], "skin_type": ["dry"]},
        accepted_insights=[],
        sensor_readings=[],
        procedures=[{"procedure_name": "Пилинг", "performed_at": recent_performed_at}],
        products=[],
        rules=[],
    )

    assert result["input_quality"]["procedures"] == "recent"
    assert any("процедур" in item.lower() for item in result["procedure_context"])
    assert any("SPF" in item for item in result["warnings"])


def test_recent_procedure_excludes_aggressive_products_from_routine():
    recent_performed_at = datetime.now(timezone.utc).isoformat()
    products = [
        ProductRecommendationInput(
            id=5,
            name="Retinol Peel Serum",
            brand="Active Lab",
            segment="cosmeceutical",
            product_type="Кислотный пилинг",
            purpose=["texture"],
            skin_type=["combination"],
            composition="Retinol, glycolic acid",
            application_info="Наносите вечером",
        ),
        ProductRecommendationInput(
            id=6,
            name="Barrier Cream",
            brand="Aura Lab",
            segment="cosmeceutical",
            product_type="Крем",
            purpose=["hydration", "barrier"],
            skin_type=["dry"],
            composition="Glycerin, Panthenol, Ceramide",
            application_info="Наносите утром",
            function_signals=[
                ProductFunctionSignal(function_key="hydration", score=0.95, evidence_status="confirmed", evidence_count=3, source_ids=[61]),
                ProductFunctionSignal(function_key="barrier_support", score=0.9, evidence_status="confirmed", evidence_count=3, source_ids=[62]),
            ],
        ),
    ]

    result = build_recommendation(
        answers={"concerns": ["dryness"], "skin_type": ["dry"]},
        accepted_insights=[],
        sensor_readings=[],
        procedures=[{"procedure_name": "Чистка", "performed_at": recent_performed_at}],
        products=products,
        rules=[],
    )

    cosmeceutical = next(line for line in result["lines"] if line["key"] == "cosmeceutical")
    routine_names = [
        step["product_name"]
        for steps in cosmeceutical["routine"].values()
        for step in steps
    ]
    assert "Retinol Peel Serum" not in routine_names
    assert "Barrier Cream" in routine_names


def test_low_hydration_sensor_reading_focuses_summary_on_barrier_support():
    result = build_recommendation(
        answers={"concerns": ["dryness"], "skin_type": ["dry"]},
        accepted_insights=[],
        sensor_readings=[{"hydration": 2, "oiliness": 3, "measured_at": "2026-04-01T10:00:00+00:00"}],
        procedures=[],
        products=[],
        rules=[],
    )

    summary_text = " ".join(str(value) for value in result["summary"].values()).lower()
    context_text = " ".join(result["procedure_context"] + result["warnings"]).lower()
    assert "барьер" in summary_text or "hydration" in summary_text or "увлаж" in summary_text
    support_text = f"{summary_text} {context_text}"
    assert "барьер" in support_text or "hydration" in support_text or "увлаж" in support_text


def test_high_oiliness_sensor_reading_mentions_sebum_control():
    result = build_recommendation(
        answers={"concerns": ["shine"], "skin_type": ["oily"]},
        accepted_insights=[],
        sensor_readings=[{"hydration": 4, "oiliness": 4, "measured_at": "2026-04-01T10:00:00+00:00"}],
        procedures=[],
        products=[],
        rules=[],
    )

    summary_text = " ".join(str(value) for value in result["summary"].values()).lower()
    assert "себум" in summary_text or "sebum" in summary_text


def test_blocked_products_do_not_enter_routine():
    products = [
        ProductRecommendationInput(
            id=2,
            name="Retinol Cream",
            brand="Active Lab",
            segment="luxury",
            product_type="Крем",
            purpose=["anti-age"],
            skin_type=["dry"],
            composition="Retinol",
            application_info="Наносите вечером",
        )
    ]
    rules = [_rule(2, "retinol", "pregnancy", "true", effect="block", weight_delta=0)]

    result = build_recommendation(
        answers={"pregnancy": ["true"], "skin_type": ["dry"]},
        accepted_insights=[],
        sensor_readings=[],
        procedures=[],
        products=products,
        rules=rules,
    )

    luxury = next(line for line in result["lines"] if line["key"] == "luxury")
    assert luxury["routine"]["morning"] == []
    assert luxury["routine"]["evening"] == []
    assert any("Недостаточно продуктов" in warning for warning in luxury["warnings"])


def test_build_recommendation_handles_missing_product_lists():
    products = [
        ProductRecommendationInput(
            id=4,
            name="Barrier Cream",
            brand="Aura Lab",
            segment="budget",
            product_type="Крем",
            purpose=None,
            skin_type=None,
            composition="Ceramide",
            application_info="Наносите утром",
        )
    ]

    result = build_recommendation(
        answers={"concerns": ["sensitivity"], "skin_type": ["dry"]},
        accepted_insights=[],
        sensor_readings=[],
        procedures=[],
        products=products,
        rules=[],
    )

    budget = next(line for line in result["lines"] if line["key"] == "budget")
    assert budget["routine"]["morning"] == []
    assert result["expert_mode"] is True


def test_build_recommendation_uses_product_function_signals():
    products = [
        ProductRecommendationInput(
            id=9,
            name="Profile Hydration Serum",
            brand="Aura Lab",
            segment="professional",
            product_type="Сыворотка",
            purpose=["hydration"],
            skin_type=["dry"],
            composition="Aqua",
            application_info="Наносите утром",
            function_signals=[
                ProductFunctionSignal(function_key="hydration", score=0.9, evidence_status="confirmed", evidence_count=2, source_ids=[7]),
            ],
        )
    ]

    result = build_recommendation(
        answers={"concerns": ["dryness"], "skin_type": ["dry"]},
        accepted_insights=[],
        sensor_readings=[],
        procedures=[],
        products=products,
        rules=[],
    )

    professional = next(line for line in result["lines"] if line["key"] == "professional")
    item = professional["routine"]["morning"][0]
    assert item["compatibility_percent"] >= 50
    assert item["score_breakdown"]["ingredient_function_fit"] > 0
    assert "hydration" in item["matched_functions"]


def test_recommendation_steps_include_new_score_breakdown_and_evidence_explanations():
    from backend.core.recommendations.builder import build_recommendation
    from backend.core.recommendations.profiles import ProductRecommendationInput

    product = ProductRecommendationInput(
        id=1,
        name="Hydration Serum",
        brand="Aura",
        segment="budget",
        product_type="serum",
        purpose=["hydration"],
        skin_type=[],
        composition="Glycerin, Panthenol",
        application_info="утром",
        function_signals=[
            {"function_key": "hydration", "score": 0.9, "evidence_status": "confirmed", "evidence_count": 2, "source_ids": [7]},
        ],
    )

    result = build_recommendation(
        answers={"concerns": ["dryness"]},
        accepted_insights=[],
        sensor_readings=[],
        procedures=[],
        products=[product],
        rules=[],
    )

    step = result["lines"][0]["routine"]["morning"][0]
    assert "ingredient_function_fit" in step["score_breakdown"]
    assert step["evidence_explanations"]
    assert any(evidence["source_ids"] == [7] for evidence in step["evidence_explanations"])


def test_recommendation_function_signals_allow_nullable_source_ids():
    product = ProductRecommendationInput(
        id=10,
        name="Calming Serum",
        brand="Aura",
        segment="budget",
        product_type="serum",
        purpose=["hydration"],
        skin_type=[],
        composition="Glycerin",
        application_info="утром",
        function_signals=[
            {"function_key": "hydration", "score": 0.95, "evidence_status": "confirmed", "evidence_count": 3, "source_ids": None},
            {"function_key": "barrier_support", "score": 0.9, "evidence_status": "confirmed", "evidence_count": 3, "source_ids": None},
        ],
    )

    result = build_recommendation(
        answers={"concerns": ["dryness"]},
        accepted_insights=[],
        sensor_readings=[],
        procedures=[],
        products=[product],
        rules=[],
    )

    step = result["lines"][0]["routine"]["morning"][0]
    assert step["evidence_explanations"]


def test_low_compatibility_product_is_not_included_in_recommendation():
    products = [
        ProductRecommendationInput(
            id=8,
            name="Basic Cream",
            brand="Aura",
            segment="budget",
            product_type="Крем",
            purpose=None,
            skin_type=["oily"],
            composition="Aqua",
            application_info="Наносите утром",
        )
    ]

    result = build_recommendation(
        answers={"skin_type": ["dry"]},
        accepted_insights=[],
        sensor_readings=[],
        procedures=[],
        products=products,
        rules=[],
    )

    budget = next(line for line in result["lines"] if line["key"] == "budget")
    assert budget["routine"]["morning"] == []
    assert budget["routine"]["evening"] == []
    assert budget["routine"]["weekly"] == []
    assert any("Недостаточно продуктов" in warning for warning in budget["warnings"])


def test_matching_engine_still_exposes_block_decision_for_reference():
    product = ProductCandidate(id=3, name="Retinol Cream", composition="Retinol")
    profile = MatchingProfile(answers={"pregnancy": ["true"]}, accepted_insights=[])
    result = match_product(product, profile, [_rule(3, "retinol", "pregnancy", "true", effect="block", weight_delta=0)])

    assert result.decision == "exclude"


def test_recommendation_line_sorts_steps_by_compatibility_percent():
    products = [
        ProductRecommendationInput(1, "Basic Cream", "Aura", "budget", "Крем", ["basic"], ["dry"], "Aqua", "Наносите утром"),
        ProductRecommendationInput(2, "Niacinamide Gel", "Aura", "budget", "Гель", ["oiliness"], ["combination"], "Niacinamide", "Наносите утром"),
    ]
    rules = [_rule(31, "niacinamide", "concern", "oiliness", effect="boost", weight_delta=8)]

    result = build_recommendation(
        answers={"concerns": ["oiliness"], "skin_type": ["combination"]},
        accepted_insights=[],
        sensor_readings=[],
        procedures=[],
        products=products,
        rules=rules,
    )

    budget = next(line for line in result["lines"] if line["key"] == "budget")
    assert budget["routine"]["morning"][0]["product_id"] == 2
    assert "score_breakdown" in budget["routine"]["morning"][0]
    assert budget["routine"]["morning"][0]["explanations"]


def test_daily_routine_keeps_best_product_per_care_slot():
    products = [
        ProductRecommendationInput(
            id=21,
            name="Basic Barrier Cream",
            brand="Aura",
            segment="budget",
            product_type="Крем",
            purpose=["barrier"],
            skin_type=["dry"],
            composition="Aqua",
            application_info="Наносите утром",
        ),
        ProductRecommendationInput(
            id=22,
            name="Hydration Barrier Cream",
            brand="Aura",
            segment="budget",
            product_type="Крем",
            purpose=["hydration"],
            skin_type=["dry"],
            composition="Glycerin, Panthenol",
            application_info="Наносите утром",
            function_signals=[
                ProductFunctionSignal(function_key="hydration", score=0.95, evidence_status="confirmed", evidence_count=3, source_ids=[17]),
            ],
        ),
        ProductRecommendationInput(
            id=23,
            name="Soft Cleansing Gel",
            brand="Aura",
            segment="budget",
            product_type="Гель для умывания",
            purpose=["cleansing"],
            skin_type=["dry"],
            composition="Panthenol",
            application_info="Используйте утром для очищения",
        ),
    ]

    result = build_recommendation(
        answers={"concerns": ["dryness"], "skin_type": ["dry"]},
        accepted_insights=[],
        sensor_readings=[],
        procedures=[],
        products=products,
        rules=[
            _rule(51, "retinol", "concern", "dryness", weight_delta=8),
            _rule(52, "glycolic acid", "concern", "dryness", weight_delta=8),
        ],
    )

    budget = next(line for line in result["lines"] if line["key"] == "budget")
    morning_names = [step["product_name"] for step in budget["routine"]["morning"]]
    assert "Hydration Barrier Cream" in morning_names
    assert "Soft Cleansing Gel" in morning_names
    assert "Basic Barrier Cream" not in morning_names
    assert len(morning_names) == 2


def test_limited_frequency_products_go_to_weekly_routine_with_usage_notes():
    products = [
        ProductRecommendationInput(
            id=31,
            name="Enzyme Powder",
            brand="Aura",
            segment="professional",
            product_type="Энзимная пудра",
            purpose=["texture"],
            skin_type=["combination"],
            composition="Papain",
            application_info="Используйте вечером 1-2 раза в неделю",
        ),
        ProductRecommendationInput(
            id=32,
            name="Daily Cream",
            brand="Aura",
            segment="professional",
            product_type="Крем",
            purpose=["hydration"],
            skin_type=["combination"],
            composition="Glycerin",
            application_info="Наносите утром ежедневно",
        ),
    ]

    result = build_recommendation(
        answers={"concerns": ["texture"], "skin_type": ["combination"]},
        accepted_insights=[],
        sensor_readings=[],
        procedures=[],
        products=products,
        rules=[_rule(41, "papain", "concern", "texture", weight_delta=50)],
    )

    professional = next(line for line in result["lines"] if line["key"] == "professional")
    daily_names = [
        step["product_name"]
        for bucket in ("morning", "evening")
        for step in professional["routine"][bucket]
    ]
    weekly = professional["routine"]["weekly"]
    assert "Enzyme Powder" not in daily_names
    assert weekly[0]["product_name"] == "Enzyme Powder"
    assert weekly[0]["usage_type"] == "weekly"
    assert weekly[0]["recommended_frequency"] == "1-2 раза в неделю"
    assert "ежедневно" not in weekly[0]["frequency"].lower()


def test_strict_expert_mode_keeps_medium_match_as_alternative_and_marks_missing_slot():
    products = [
        ProductRecommendationInput(
            id=71,
            name="Medium Barrier Cream",
            brand="Aura",
            segment="budget",
            product_type="Крем",
            purpose=["barrier"],
            skin_type=["dry"],
            composition="Ceramide",
            application_info="Наносите утром",
        )
    ]

    result = build_recommendation(
        answers={"concerns": ["dryness"], "skin_type": ["dry"]},
        accepted_insights=[],
        sensor_readings=[],
        procedures=[],
        products=products,
        rules=[
            _rule(51, "retinol", "concern", "dryness", weight_delta=8),
            _rule(52, "glycolic acid", "concern", "dryness", weight_delta=8),
        ],
    )

    budget = next(line for line in result["lines"] if line["key"] == "budget")
    assert budget["routine"]["morning"] == []
    assert result["expert_mode"] is True
    assert any(item["line_key"] == "budget" and item["care_slot"] == "moisturizer" for item in result["missing_slots"])
    assert result["alternatives"]["budget"]["moisturizer"][0]["product_name"] == "Medium Barrier Cream"


def test_strict_expert_mode_includes_only_high_confidence_products():
    products = [
        ProductRecommendationInput(
            id=72,
            name="High Confidence Hydration Cream",
            brand="Aura",
            segment="budget",
            product_type="Крем",
            purpose=["hydration"],
            skin_type=["dry"],
            composition="Glycerin, Panthenol",
            application_info="Наносите утром",
            function_signals=[
                ProductFunctionSignal(function_key="hydration", score=0.95, evidence_status="confirmed", evidence_count=3, source_ids=[17]),
            ],
        )
    ]

    result = build_recommendation(
        answers={"concerns": ["dryness"], "skin_type": ["dry"]},
        accepted_insights=[],
        sensor_readings=[],
        procedures=[],
        products=products,
        rules=[],
    )

    budget = next(line for line in result["lines"] if line["key"] == "budget")
    assert budget["routine"]["morning"][0]["product_name"] == "High Confidence Hydration Cream"
    assert budget["routine"]["morning"][0]["compatibility_percent"] >= 70


def test_combination_validator_removes_weekly_exfoliation_when_daily_active_selected():
    products = [
        ProductRecommendationInput(
            id=81,
            name="Retinol Serum",
            brand="Aura",
            segment="cosmeceutical",
            product_type="Сыворотка",
            purpose=["active"],
            skin_type=["normal"],
            composition="Retinol, Glycerin",
            application_info="Наносите вечером",
            function_signals=[
                ProductFunctionSignal(function_key="hydration", score=0.95, evidence_status="confirmed", evidence_count=3, source_ids=[21]),
            ],
        ),
        ProductRecommendationInput(
            id=82,
            name="Acid Peel",
            brand="Aura",
            segment="cosmeceutical",
            product_type="Кислотный пилинг",
            purpose=["weekly"],
            skin_type=["normal"],
            composition="Glycolic acid, Glycerin",
            application_info="Используйте вечером 1 раз в неделю",
            function_signals=[
                ProductFunctionSignal(function_key="hydration", score=0.95, evidence_status="confirmed", evidence_count=3, source_ids=[22]),
            ],
        ),
    ]

    result = build_recommendation(
        answers={"concerns": ["dryness"], "skin_type": ["dry"]},
        accepted_insights=[],
        sensor_readings=[],
        procedures=[],
        products=products,
        rules=[
            _rule(51, "retinol", "concern", "dryness", weight_delta=8),
            _rule(52, "glycolic acid", "concern", "dryness", weight_delta=8),
        ],
    )

    cosmeceutical = next(line for line in result["lines"] if line["key"] == "cosmeceutical")
    assert any(step["product_name"] == "Retinol Serum" for step in cosmeceutical["routine"]["evening"])
    assert cosmeceutical["routine"]["weekly"] == []
    assert any("кислот" in warning.lower() or "acid" in warning.lower() for warning in result["combination_warnings"])
