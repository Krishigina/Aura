from datetime import datetime, timezone

import pytest

from backend.api.matching_engine import MatchingProfile, MatchingRule, ProductCandidate, match_product
from backend.api.recommendation_engine import (
    LINE_KEYS,
    ProductRecommendationInput,
    RecommendationGenerationError,
    build_extended_skin_profile,
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
    assert morning_steps[0]["compatibility_percent"] == 58
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
            purpose=["barrier"],
            skin_type=["combination"],
            composition="Ceramide",
            application_info="Наносите утром",
        ),
    ]

    result = build_recommendation(
        answers={"concerns": ["texture"], "skin_type": ["combination"]},
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
    assert budget["routine"]["morning"][0]["product_id"] == 4


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
