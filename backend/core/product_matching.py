from typing import Any, Awaitable, Callable, Dict, List, Optional

from backend.core.matching.domain import MatchingProfile, MatchingRule, ProductCandidate
from backend.core.matching.scoring import match_product
from backend.core.products import coerce_list_field
from backend.core.products import product_select_sql
from backend.core.recommendations.profiles import build_extended_skin_profile


async def build_product_matching_response(
    conn,
    user_id: int,
    payload,
    *,
    load_skin_passport_context: Callable[[int], Awaitable[Optional[Dict[str, Any]]]],
    sanitize_skin_passport_answers: Callable[[Any], Dict[str, List[str]]],
    load_accepted_passport_insights: Callable[[Any, int], Awaitable[List[str]]],
    load_user_skin_journal: Callable[[Any, int], Awaitable[Dict[str, Any]]],
    load_product_function_signals: Callable[[Any, List[int]], Awaitable[Dict[int, List[Any]]]],
    product_match_sort_key: Callable[[Dict[str, Any]], Any],
    filter_product_match_results: Callable[[List[Dict[str, Any]], Any], List[Dict[str, Any]]],
):
    skin_passport = await load_skin_passport_context(user_id) or {"answers": {}}
    answers = sanitize_skin_passport_answers(skin_passport.get("answers"))

    accepted_insights = await load_accepted_passport_insights(conn, user_id)
    journal = await load_user_skin_journal(conn, user_id)
    product_rows = await conn.fetch(
        f"""
        SELECT id, name, composition, category, product_type, purpose, skin_type
        FROM ({product_select_sql('p')}) AS hydrated_products
        ORDER BY id DESC
        """
    )
    product_ids = [row["id"] for row in product_rows]
    function_signals_by_product = await load_product_function_signals(conn, product_ids)
    rule_rows = await conn.fetch(
        """
        SELECT mr.*
        FROM matching_rules mr
        WHERE mr.status = 'confirmed'
        """
    )

    extended_skin_profile = build_extended_skin_profile(
        answers,
        accepted_insights,
        journal.get("sensor_readings", []),
    )
    profile = MatchingProfile(answers=answers, accepted_insights=accepted_insights, skin_state=extended_skin_profile)
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

    results = []
    for row in product_rows:
        candidate = ProductCandidate(
            id=row["id"],
            name=row["name"] or "Без названия",
            composition=row["composition"] or "",
            category=row["category"] or "",
            product_type=row["product_type"] or "",
            purpose=", ".join(coerce_list_field(row["purpose"])),
            skin_type=", ".join(coerce_list_field(row["skin_type"])),
            function_signals=function_signals_by_product.get(row["id"], []),
        )
        match = match_product(candidate, profile, rules)
        results.append({**match.__dict__, "product_name": row["name"]})

    results.sort(key=product_match_sort_key)
    filtered_results = filter_product_match_results(results, payload)
    if not filtered_results:
        return {"items": [], "message": "Нет продуктов с совместимостью выше заданного порога"}
    return {"items": filtered_results}
