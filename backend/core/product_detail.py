from typing import Any, Awaitable, Callable, Dict, List, Optional

from backend.core.matching.domain import MatchingProfile, MatchingRule, ProductCandidate
from backend.core.matching.scoring import match_product
from backend.core.products import (
    coerce_list_field,
    load_product_photos_payload,
    load_product_video_payload,
    normalize_product_response,
)
from backend.core.recommendations.profiles import build_extended_skin_profile


async def build_product_detail_response(
    conn,
    row,
    current_user_id: int,
    *,
    load_accepted_passport_insights: Callable[[Any, int], Awaitable[List[str]]],
    load_user_skin_journal: Callable[[Any, int], Awaitable[Dict[str, Any]]],
    load_product_function_signals: Callable[[Any, List[int]], Awaitable[Dict[int, List[Any]]]],
    load_skin_passport_context: Callable[[int], Awaitable[Optional[Dict[str, Any]]]],
    sanitize_skin_passport_answers: Callable[[Any], Dict[str, List[str]]],
):
    accepted_insights = await load_accepted_passport_insights(conn, current_user_id)
    journal = await load_user_skin_journal(conn, current_user_id)
    function_signals_by_product = await load_product_function_signals(conn, [row["id"]])
    rule_rows = await conn.fetch(
        """
        SELECT *
        FROM matching_rules
        WHERE status = 'confirmed'
        """
    )
    photo_rows = await conn.fetch(
        "SELECT id, product_id, filename FROM product_photos WHERE product_id=$1 ORDER BY id",
        row["id"],
    )

    product_response = normalize_product_response(row)
    product_response["photos"] = load_product_photos_payload(photo_rows, include_data=False)
    product_response["video"] = load_product_video_payload(row["id"], row["video"], include_data=False)
    skin_passport = await load_skin_passport_context(current_user_id) or {"answers": {}}
    answers = sanitize_skin_passport_answers(skin_passport.get("answers"))
    extended_skin_profile = build_extended_skin_profile(
        answers,
        accepted_insights,
        journal.get("sensor_readings", []),
    )
    rules = [
        MatchingRule(
            id=rule_row["id"],
            status=rule_row["status"],
            target_type=rule_row["target_type"],
            target_key=rule_row["target_key"] or "",
            condition_type=rule_row["condition_type"],
            condition_value=rule_row["condition_value"],
            effect=rule_row["effect"],
            weight_delta=float(rule_row["weight_delta"] or 0),
            severity=rule_row["severity"],
            source_id=rule_row["source_id"],
            evidence_quote=rule_row["evidence_quote"] or "",
        )
        for rule_row in rule_rows
    ]
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
    profile = MatchingProfile(answers=answers, accepted_insights=accepted_insights, skin_state=extended_skin_profile)
    match = match_product(candidate, profile, rules)
    matching_payload = {**match.__dict__, "score_breakdown": match.score_breakdown, "explanations": match.explanations}
    assistant_context = {
        "product": product_response,
        "matching": matching_payload,
        "user_context": {
            "skin_passport": skin_passport,
            "extended_skin_profile": extended_skin_profile,
            "accepted_insights": accepted_insights,
        },
    }
    return {
        **product_response,
        "product": product_response,
        "matching": matching_payload,
        "extended_skin_profile": extended_skin_profile,
        "assistant_context": assistant_context,
    }
