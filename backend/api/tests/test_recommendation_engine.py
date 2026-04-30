import pytest

from backend.api.matching_engine import MatchingProfile, MatchingRule, ProductCandidate, match_product
from backend.api.recommendation_engine import (
    LINE_KEYS,
    ProductRecommendationInput,
    RecommendationGenerationError,
    build_recommendation,
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
    assert morning_steps[0]["compatibility_percent"] == 80
    assert morning_steps[0]["sequence"] == 1


def test_recent_procedure_adds_recovery_warning():
    result = build_recommendation(
        answers={"concerns": ["dehydration"], "skin_type": ["dry"]},
        accepted_insights=[],
        sensor_readings=[],
        procedures=[{"procedure_name": "Пилинг", "performed_at": "2026-04-29T12:00:00Z"}],
        products=[],
        rules=[],
    )

    assert result["input_quality"]["procedures"] == "recent"
    assert any("процедур" in item.lower() for item in result["procedure_context"])
    assert any("SPF" in item for item in result["warnings"])


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


def test_matching_engine_still_exposes_block_decision_for_reference():
    product = ProductCandidate(id=3, name="Retinol Cream", composition="Retinol")
    profile = MatchingProfile(answers={"pregnancy": ["true"]}, accepted_insights=[])
    result = match_product(product, profile, [_rule(3, "retinol", "pregnancy", "true", effect="block", weight_delta=0)])

    assert result.decision == "exclude"
