from backend.core.chat_recommendations import (
    compact_recommendation_context,
    should_attach_catalog_context,
    wants_personal_product_recommendations,
)


def test_detects_personal_product_recommendation_intent():
    assert wants_personal_product_recommendations("which products from the catalog fit me?")
    assert wants_personal_product_recommendations("pick a serum and cream from the catalog")
    assert not wants_personal_product_recommendations("how to use retinol")


def test_follow_up_after_product_request_keeps_catalog_context():
    assert should_attach_catalog_context(
        "which one is better in the morning?",
        chat_history=[
            {"role": "user", "content": "which products from the catalog fit me?"},
            {"role": "assistant", "content": "I will pick products from the Aura catalog."},
        ],
        product_context=None,
    )


def test_product_detail_chat_does_not_attach_catalog_context():
    assert not should_attach_catalog_context(
        "does this product fit me?",
        chat_history=[],
        product_context={"product": {"id": 1, "name": "Example Serum"}},
    )


def test_compact_recommendation_context_keeps_best_matching_products():
    recommendation = {
        "summary": {"title": "Selection", "description": "Personal routine"},
        "extended_skin_profile": {"global_skin_type": "dry"},
        "warnings": [],
        "combination_warnings": [],
        "lines": [
            {
                "key": "budget",
                "title": "Budget line",
                "routine": {
                    "morning": [
                        {
                            "product_id": 1,
                            "product_name": "Retinol Serum",
                            "brand": "Aura",
                            "step": "Serum",
                            "care_slot": "active",
                            "usage_type": "daily",
                            "recommended_frequency": "evening",
                            "compatibility_percent": 92,
                            "reason": "Supports skin renewal with retinol",
                            "instruction": "Use in the evening",
                            "warnings": [],
                            "matched_functions": ["renewal"],
                        },
                        {
                            "product_id": 2,
                            "product_name": "Barrier Cream",
                            "brand": "Aura",
                            "step": "Cream",
                            "care_slot": "moisturizer",
                            "usage_type": "daily",
                            "recommended_frequency": "daily",
                            "compatibility_percent": 88,
                            "reason": "Supports barrier repair",
                            "instruction": "Use after serum",
                            "warnings": [],
                            "matched_functions": ["barrier_support"],
                        },
                    ],
                    "evening": [],
                    "weekly": [],
                },
            }
        ],
    }

    context = compact_recommendation_context(recommendation, "which retinol serum fits me?")

    assert context["status"] == "available"
    assert context["products"][0]["product_id"] == 1
    assert context["products"][0]["product_name"] == "Retinol Serum"
