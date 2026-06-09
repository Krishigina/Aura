from backend.core.chat_product_context import _candidate_score


def test_candidate_score_matches_product_name_and_brand_from_history():
    row = {
        "id": 67,
        "name": "Spicule",
        "brand": "Mixit",
        "product_type": "Сыворотка",
        "what_is_it": "Сыворотка",
        "active_ingredient": "",
        "composition": "Aqua, Panthenol",
        "application_info": "",
        "purpose": [],
        "skin_type": [],
    }

    score = _candidate_score(
        "Подойдет ли мне сыворотка Spicule от Mixit? А что в ее составе?",
        row,
    )

    assert score >= 24.0


def test_candidate_score_rejects_unrelated_product():
    row = {
        "id": 10,
        "name": "Barrier Cream",
        "brand": "Aura",
        "product_type": "Крем",
        "what_is_it": "Крем",
        "active_ingredient": "",
        "composition": "Aqua, Glycerin",
        "application_info": "",
        "purpose": [],
        "skin_type": [],
    }

    score = _candidate_score(
        "Подойдет ли мне сыворотка Spicule от Mixit? А что в ее составе?",
        row,
    )

    assert score < 24.0
