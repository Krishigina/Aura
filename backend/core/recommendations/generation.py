from typing import Any, Awaitable, Callable, Dict, List, Optional

from fastapi import HTTPException

from backend.core.matching.domain import MatchingRule
from backend.core.products import product_select_sql
from backend.core.recommendations.builder import build_recommendation
from backend.core.recommendations.profiles import RecommendationGenerationError


async def build_recommendation_response(
    conn,
    user_id: int,
    *,
    load_skin_passport_context: Callable[[int], Awaitable[Optional[Dict[str, Any]]]],
    sanitize_skin_passport_answers: Callable[[Any], Dict[str, List[str]]],
    load_accepted_passport_insights: Callable[[Any, int], Awaitable[List[str]]],
    load_user_skin_journal: Callable[[Any, int], Awaitable[Dict[str, Any]]],
    load_product_function_signals: Callable[[Any, List[int]], Awaitable[Dict[int, List[Any]]]],
    product_row_to_recommendation_input: Callable[[Any, Optional[List[Any]]], Any],
):
    skin_passport = await load_skin_passport_context(user_id) or {"answers": {}}
    answers = sanitize_skin_passport_answers(skin_passport.get("answers"))

    accepted_insights = await load_accepted_passport_insights(conn, user_id)
    journal = await load_user_skin_journal(conn, user_id)
    product_rows = await conn.fetch(
        f"""
        SELECT id, name, brand, segment, product_type, purpose, skin_type, composition, application_info
        FROM ({product_select_sql('p')}) AS hydrated_products
        ORDER BY id DESC
        """
    )
    function_signals_by_product = await load_product_function_signals(conn, [row["id"] for row in product_rows])
    rule_rows = await conn.fetch(
        """
        SELECT *
        FROM matching_rules
        WHERE status = 'confirmed'
        """
    )

    rules = [
        MatchingRule(
            id=row["id"],
            status=row["status"],
            target_type=row["target_type"],
            target_key=row["target_key"] or "",
            condition_type=row["condition_type"],
            condition_value=row["condition_value"],
            effect=row["effect"],
            weight_delta=float(row["weight_delta"] or 0),
            severity=row["severity"],
            source_id=row["source_id"],
            evidence_quote=row["evidence_quote"] or "",
        )
        for row in rule_rows
    ]
    products = [
        product_row_to_recommendation_input(row, function_signals_by_product.get(row["id"], []))
        for row in product_rows
    ]

    try:
        return build_recommendation(
            answers=answers,
            accepted_insights=accepted_insights,
            sensor_readings=journal.get("sensor_readings", []),
            procedures=journal.get("procedures", []),
            products=products,
            rules=rules,
        )
    except RecommendationGenerationError as error:
        raise HTTPException(status_code=error.status_code, detail=error.message) from error
