from backend.api.ingredient_knowledge import (
    ExtractedIngredientFact,
    aggregate_function_profile,
    extract_ingredient_facts,
    normalize_key,
    resolve_seed_alias,
)


def test_normalize_key_handles_case_spaces_and_russian_text():
    assert normalize_key("  Салициловая   кислота ") == "салициловая кислота"
    assert normalize_key("Sodium Hyaluronate") == "sodium hyaluronate"


def test_seed_aliases_resolve_to_canonical_ingredient():
    resolved = resolve_seed_alias("Пантенол")
    assert resolved is not None
    assert resolved.canonical_name == "panthenol"
    assert "barrier_support" in resolved.default_effects


def test_extracts_high_confidence_urea_dryness_fact_from_local_source_text():
    text = "В рекомендациях по ксерозу указано применение средств с 2-5-10% мочевины при сухости кожи."

    facts = extract_ingredient_facts(text, source_id=7, source_title="КР_Ксероз кожи_общественное обсуждение.pdf")

    assert len(facts) == 1
    fact = facts[0]
    assert fact.ingredient_key == "urea"
    assert fact.effect_key == "hydration"
    assert fact.condition_type == "concern"
    assert fact.condition_value == "dryness"
    assert fact.matching_effect == "boost"
    assert fact.confidence >= 0.82
    assert fact.evidence_status == "auto_high_confidence"
    assert fact.source_id == 7
    assert "мочевины" in fact.evidence_quote


def test_low_confidence_mentions_remain_draft():
    text = "В тексте кратко упоминается ретинол без описания риска или состояния кожи."

    facts = extract_ingredient_facts(text, source_id=3, source_title="notes.txt")

    assert facts
    assert all(fact.evidence_status == "draft" for fact in facts)
    assert all(fact.confidence < 0.82 for fact in facts)


def test_negated_recommendation_extracts_warning_not_positive_boost():
    facts = extract_ingredient_facts("Ретинол не рекомендуется при акне.", source_id=5, source_title="risk.txt")

    assert len(facts) == 1
    fact = facts[0]
    assert fact.ingredient_key == "retinol"
    assert fact.condition_type == "concern"
    assert fact.condition_value == "acne"
    assert fact.matching_effect == "warning"
    assert fact.effect_key in {"warning", "risk", "contraindication"}
    assert fact.confidence >= 0.82
    assert fact.evidence_status == "auto_high_confidence"


def test_aggregate_function_profile_weights_confirmed_above_auto_facts():
    confirmed_fact = ExtractedIngredientFact(
        ingredient_key="glycerin",
        effect_key="hydration",
        condition_type="concern",
        condition_value="dryness",
        matching_effect="boost",
        confidence=0.9,
        evidence_status="confirmed",
        source_id=11,
        source_title="confirmed.docx",
        evidence_quote="Глицерин рекомендуется при сухости кожи.",
    )
    auto_fact = ExtractedIngredientFact(
        ingredient_key="urea",
        effect_key="hydration",
        condition_type="concern",
        condition_value="dryness",
        matching_effect="boost",
        confidence=0.9,
        evidence_status="auto_high_confidence",
        source_id=12,
        source_title="auto.docx",
        evidence_quote="Мочевина рекомендуется при сухости кожи.",
    )

    confirmed_profile = aggregate_function_profile([confirmed_fact])
    auto_profile = aggregate_function_profile([auto_fact])
    mixed_profile = aggregate_function_profile([confirmed_fact, auto_fact])

    assert confirmed_profile["hydration"].score > auto_profile["hydration"].score
    assert mixed_profile["hydration"].evidence_count == 2
    assert mixed_profile["hydration"].evidence_status == "mixed"
