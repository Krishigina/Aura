import json
from typing import Any, Dict, List, Optional

from fastapi import HTTPException

from backend.core.matching.domain import ProductFunctionSignal
from backend.core.products import coerce_list_field
from backend.core.recommendations.profiles import ProductRecommendationInput


MATCHING_DECISION_PRIORITY = {"recommend": 0, "caution": 1, "exclude": 2}
MATCHING_RULE_EFFECTS = {"block", "warning", "boost", "penalty"}
INGREDIENT_FACT_EVIDENCE_STATUSES = {"auto_high_confidence", "draft", "confirmed", "rejected"}
MATCHING_RULE_UPDATE_FIELDS = {
    "rule_type",
    "target_type",
    "target_id",
    "target_key",
    "condition_type",
    "condition_value",
    "effect",
    "weight_delta",
    "severity",
    "source_id",
    "evidence_quote",
    "confidence",
    "status",
}
MATCHING_RULE_ADMIN_ROLES = {"admin", "administrator", "администратор", "manager", "менеджер"}
MATCHING_RULE_STATUSES = {"draft", "confirmed", "disabled"}


def decode_jsonb_value(value):
    if isinstance(value, str):
        try:
            return json.loads(value)
        except json.JSONDecodeError:
            return value
    return value


async def load_accepted_passport_insights(conn, user_id: int) -> List[str]:
    rows = await conn.fetch(
        """
        SELECT proposed_value
        FROM passport_update_suggestions
        WHERE user_id=$1 AND status='accepted' AND suggestion_type='append_insight'
        """,
        user_id,
    )
    insights: List[str] = []
    for row in rows:
        proposed_value = decode_jsonb_value(row["proposed_value"])
        if isinstance(proposed_value, dict):
            value = proposed_value.get("normalized_value")
            if value is None:
                value = proposed_value.get("value")
        else:
            value = proposed_value
        if value is None:
            continue
        text = str(value).strip()
        if text:
            insights.append(text)
    return insights


def product_match_sort_key(item: Dict[str, Any]):
    return (MATCHING_DECISION_PRIORITY.get(item.get("decision"), 99), -float(item.get("final_score") or 0))


def filter_product_match_results(results: List[Dict[str, Any]], payload) -> List[Dict[str, Any]]:
    min_percent = max(0, min(int(payload.min_compatibility_percent), 100))
    limit = max(1, min(payload.limit, 500))
    filtered = [
        item
        for item in results
        if item.get("decision") != "exclude" and int(item.get("compatibility_percent") or 0) >= min_percent
    ]
    return filtered[:limit]


async def load_product_function_signals(conn, product_ids: List[int]) -> Dict[int, List[ProductFunctionSignal]]:
    if not product_ids:
        return {}
    rows = await conn.fetch(
        """
        SELECT product_id, function_key, score, evidence_status, evidence_count, source_ids
        FROM product_function_profiles
        WHERE product_id = ANY($1::int[])
        """,
        product_ids,
    )
    function_signals_by_product: Dict[int, List[ProductFunctionSignal]] = {}
    for row in rows:
        source_ids = decode_jsonb_value(row["source_ids"])
        if not isinstance(source_ids, list):
            source_ids = []
        function_signals_by_product.setdefault(row["product_id"], []).append(
            ProductFunctionSignal(
                function_key=row["function_key"] or "",
                score=float(row["score"] or 0),
                evidence_status=row["evidence_status"] or "auto_only",
                evidence_count=int(row["evidence_count"] or 0),
                source_ids=[int(source_id) for source_id in source_ids if str(source_id).isdigit()],
            )
        )
    return function_signals_by_product


def product_row_to_recommendation_input(
    row,
    function_signals: Optional[List[ProductFunctionSignal]] = None,
) -> ProductRecommendationInput:
    return ProductRecommendationInput(
        id=row["id"],
        name=row["name"] or "Без названия",
        brand=row["brand"] or "",
        segment=row["segment"] or "",
        product_type=row["product_type"] or "",
        purpose=coerce_list_field(row["purpose"]),
        skin_type=coerce_list_field(row["skin_type"]),
        composition=row["composition"] or "",
        application_info=row["application_info"] or "",
        function_signals=function_signals or [],
    )


def validate_matching_rule_effect(effect: str) -> str:
    if effect not in MATCHING_RULE_EFFECTS:
        raise HTTPException(status_code=400, detail="Некорректный эффект правила")
    return effect


def validate_matching_rule_status(status: str) -> str:
    normalized = (status or "").strip().lower()
    if normalized not in MATCHING_RULE_STATUSES:
        raise HTTPException(status_code=400, detail="Некорректный статус правила")
    return normalized


def validate_ingredient_fact_evidence_status(evidence_status: str) -> str:
    if evidence_status not in INGREDIENT_FACT_EVIDENCE_STATUSES:
        raise HTTPException(status_code=400, detail="Некорректный статус факта")
    return evidence_status


def require_matching_rule_admin(current_user: dict):
    role = str(current_user.get("role") or "").lower()
    if role not in MATCHING_RULE_ADMIN_ROLES:
        raise HTTPException(status_code=403, detail="Недостаточно прав для управления правилами подбора")


async def ensure_matching_rule_source_exists(conn, source_id: Optional[int]):
    if source_id is None:
        return
    exists = await conn.fetchval("SELECT EXISTS(SELECT 1 FROM knowledge_sources WHERE id=$1)", source_id)
    if not exists:
        raise HTTPException(status_code=400, detail="Источник знаний для правила не найден")


def validate_recommendation_feedback_action(action: str) -> str:
    normalized = str(action or "").strip().lower()
    allowed = {"viewed", "clicked", "saved_to_favorites", "dismissed"}
    if normalized not in allowed:
        raise HTTPException(status_code=400, detail="Недопустимое действие рекомендации")
    return normalized
