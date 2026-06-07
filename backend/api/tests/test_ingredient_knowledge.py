from backend.api.ingredient_knowledge import (
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
    text = "В тексте кратко упоминается ретинол без рекомендации, риска или состояния кожи."

    facts = extract_ingredient_facts(text, source_id=3, source_title="notes.txt")

    assert facts
    assert all(fact.evidence_status == "draft" for fact in facts)
    assert all(fact.confidence < 0.82 for fact in facts)


def test_aggregate_function_profile_weights_confirmed_above_auto_facts():
    facts = extract_ingredient_facts(
        "Глицерин рекомендуется при сухости кожи. Пантенол помогает поддерживать кожный барьер.",
        source_id=11,
        source_title="source.docx",
    )
    facts[0].evidence_status = "confirmed"

    profile = aggregate_function_profile(facts)

    assert profile["hydration"].score > 0
    assert profile["hydration"].evidence_count >= 1
    assert profile["hydration"].evidence_status in {"confirmed", "mixed"}
