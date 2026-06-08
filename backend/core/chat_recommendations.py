import re
from typing import Any, Dict, List, Optional

from fastapi import HTTPException


RECOMMENDATION_INTENT_TERMS = (
    "which products",
    "which items",
    "what product",
    "what products",
    "pick products",
    "pick a serum",
    "recommend products",
    "recommend a product",
    "catalog",
    "routine",
    "\u043a\u0430\u043a\u0438\u0435 \u043f\u0440\u043e\u0434\u0443\u043a\u0442\u044b",
    "\u043a\u0430\u043a\u043e\u0439 \u043f\u0440\u043e\u0434\u0443\u043a\u0442",
    "\u0447\u0442\u043e \u043f\u043e\u0434\u043e\u0431\u0440\u0430\u0442\u044c",
    "\u043f\u043e\u0434\u0431\u0435\u0440\u0438 \u043f\u0440\u043e\u0434\u0443\u043a\u0442\u044b",
    "\u043f\u043e\u0441\u043e\u0432\u0435\u0442\u0443\u0439 \u043f\u0440\u043e\u0434\u0443\u043a\u0442\u044b",
    "\u043f\u043e\u0440\u0435\u043a\u043e\u043c\u0435\u043d\u0434\u0443\u0439 \u043f\u0440\u043e\u0434\u0443\u043a\u0442\u044b",
    "\u0440\u0435\u043a\u043e\u043c\u0435\u043d\u0434",
    "\u043a\u0430\u0442\u0430\u043b\u043e\u0433",
    "\u0440\u0443\u0442\u0438\u043d",
)
PRODUCT_CATEGORY_TERMS = (
    "product",
    "products",
    "item",
    "items",
    "cream",
    "serum",
    "spf",
    "sunscreen",
    "cleanser",
    "toner",
    "peeling",
    "retinol",
    "acid",
    "mask",
    "\u043f\u0440\u043e\u0434\u0443\u043a\u0442",
    "\u043f\u0440\u043e\u0434\u0443\u043a\u0442\u044b",
    "\u0441\u0440\u0435\u0434\u0441\u0442\u0432\u043e",
    "\u0441\u0440\u0435\u0434\u0441\u0442\u0432\u0430",
    "\u043a\u0440\u0435\u043c",
    "\u0441\u044b\u0432\u043e\u0440\u043e\u0442\u043a",
    "\u0441\u0430\u043d\u0441\u043a\u0440\u0438\u043d",
    "\u0441\u043f\u0444",
    "\u043e\u0447\u0438\u0449\u0435\u043d",
    "\u0442\u043e\u043d\u0438\u043a",
    "\u043f\u0438\u043b\u0438\u043d\u0433",
    "\u0440\u0435\u0442\u0438\u043d\u043e\u043b",
    "\u043a\u0438\u0441\u043b\u043e\u0442",
    "\u043c\u0430\u0441\u043a",
)
FOLLOW_UP_TERMS = (
    "which one",
    "which of them",
    "from these",
    "from them",
    "morning",
    "evening",
    "every day",
    "how often",
    "where to start",
    "\u043a\u0430\u043a\u043e\u0439 \u0438\u0437 \u043d\u0438\u0445",
    "\u0438\u0437 \u044d\u0442\u0438\u0445",
    "\u0443\u0442\u0440\u043e\u043c",
    "\u0432\u0435\u0447\u0435\u0440\u043e\u043c",
    "\u043a\u0430\u0436\u0434\u044b\u0439 \u0434\u0435\u043d\u044c",
    "\u043a\u0430\u043a \u0447\u0430\u0441\u0442\u043e",
    "\u0441 \u0447\u0435\u0433\u043e \u043d\u0430\u0447\u0430\u0442\u044c",
)


def _normalize_text(value: Any) -> str:
    return re.sub(r"\s+", " ", str(value or "")).strip().lower()


def wants_personal_product_recommendations(message: str) -> bool:
    text = _normalize_text(message)
    if not text:
        return False
    if any(term in text for term in RECOMMENDATION_INTENT_TERMS):
        return True
    return any(term in text for term in PRODUCT_CATEGORY_TERMS) and any(
        term in text
        for term in (
            "fits",
            "suit",
            "suits",
            "need",
            "needed",
            "better",
            "suggest",
            "recommend",
            "\u043f\u043e\u0434\u043e\u0439\u0434",
            "\u043d\u0443\u0436\u0435\u043d",
            "\u043d\u0443\u0436\u043d\u043e",
            "\u043b\u0443\u0447\u0448\u0435",
            "\u043f\u043e\u0441\u043e\u0432\u0435\u0442",
            "\u0440\u0435\u043a\u043e\u043c\u0435\u043d\u0434",
        )
    )


def should_attach_catalog_context(
    message: str,
    chat_history: Optional[List[Dict[str, Any]]] = None,
    product_context: Optional[Dict[str, Any]] = None,
) -> bool:
    if product_context:
        return False
    if wants_personal_product_recommendations(message):
        return True

    text = _normalize_text(message)
    if not text or len(text) > 120 or not any(term in text for term in FOLLOW_UP_TERMS):
        return False

    for item in reversed(chat_history or []):
        if str(item.get("role") or "").lower() != "user":
            continue
        if wants_personal_product_recommendations(str(item.get("content") or "")):
            return True
    return False


def _query_terms(query: str) -> List[str]:
    terms = []
    for raw_term in re.findall(r"[A-Za-z\u0410-\u042f\u0430-\u044f\u0401\u04510-9-]+", _normalize_text(query)):
        term = raw_term.strip("-")
        if len(term) < 3:
            continue
        if term not in terms:
            terms.append(term)
    return terms


def _flatten_recommendation_products(recommendation: Dict[str, Any]) -> List[Dict[str, Any]]:
    items: List[Dict[str, Any]] = []
    for line in recommendation.get("lines") or []:
        routine = line.get("routine") if isinstance(line, dict) else None
        if not isinstance(routine, dict):
            continue
        for bucket in ("morning", "evening", "weekly"):
            for item in routine.get(bucket) or []:
                if not isinstance(item, dict):
                    continue
                items.append(
                    {
                        "product_id": item.get("product_id"),
                        "product_name": item.get("product_name"),
                        "brand": item.get("brand"),
                        "step": item.get("step"),
                        "care_slot": item.get("care_slot"),
                        "usage_type": item.get("usage_type"),
                        "recommended_frequency": item.get("recommended_frequency"),
                        "compatibility_percent": item.get("compatibility_percent"),
                        "reason": item.get("reason"),
                        "instruction": item.get("instruction"),
                        "warnings": item.get("warnings") or [],
                        "matched_functions": item.get("matched_functions") or [],
                        "segment": line.get("key"),
                        "segment_title": line.get("title"),
                        "routine_bucket": bucket,
                    }
                )
    return items


def _product_rank(item: Dict[str, Any], query_terms: List[str]) -> float:
    score = float(item.get("compatibility_percent") or 0)
    searchable = " ".join(
        [
            str(item.get("product_name") or ""),
            str(item.get("brand") or ""),
            str(item.get("step") or ""),
            str(item.get("care_slot") or ""),
            str(item.get("reason") or ""),
            str(item.get("instruction") or ""),
            " ".join(str(value) for value in item.get("matched_functions") or []),
        ]
    ).lower()
    score += sum(18 for term in query_terms if term in searchable)
    if str(item.get("routine_bucket") or "") == "morning":
        score += 2
    return score


def compact_recommendation_context(recommendation: Dict[str, Any], query: str, limit: int = 4) -> Dict[str, Any]:
    query_terms = _query_terms(query)
    flattened = _flatten_recommendation_products(recommendation)
    ranked = sorted(flattened, key=lambda item: _product_rank(item, query_terms), reverse=True)

    products = []
    seen_ids = set()
    for item in ranked:
        product_id = item.get("product_id")
        if product_id in seen_ids:
            continue
        seen_ids.add(product_id)
        products.append(
            {
                "product_id": product_id,
                "product_name": str(item.get("product_name") or ""),
                "brand": str(item.get("brand") or ""),
                "step": str(item.get("step") or ""),
                "care_slot": str(item.get("care_slot") or ""),
                "routine_bucket": str(item.get("routine_bucket") or ""),
                "segment": str(item.get("segment") or ""),
                "compatibility_percent": int(item.get("compatibility_percent") or 0),
                "recommended_frequency": str(item.get("recommended_frequency") or ""),
                "reason": str(item.get("reason") or "")[:220],
            }
        )
        if len(products) >= limit:
            break

    return {
        "status": "available",
        "summary": recommendation.get("summary") or {},
        "extended_skin_profile": recommendation.get("extended_skin_profile") or {},
        "warnings": [str(value) for value in recommendation.get("warnings") or []][:3],
        "combination_warnings": [str(value) for value in recommendation.get("combination_warnings") or []][:3],
        "products": products,
    }


async def build_catalog_recommendation_context(
    conn,
    *,
    user_id: int,
    query: str,
    skin_passport: Optional[Dict[str, Any]],
    generate_recommendation_for_user,
    recommendation_runtime_dependencies: Dict[str, Any],
) -> Dict[str, Any]:
    answers = (skin_passport or {}).get("answers")
    if not isinstance(answers, dict) or not any(answers.values()):
        return {
            "status": "missing_skin_passport",
            "message": "\u0414\u043b\u044f \u043f\u0435\u0440\u0441\u043e\u043d\u0430\u043b\u044c\u043d\u043e\u0439 \u0440\u0435\u043a\u043e\u043c\u0435\u043d\u0434\u0430\u0446\u0438\u0438 \u043d\u0443\u0436\u043d\u044b \u043e\u0442\u0432\u0435\u0442\u044b \u0430\u043d\u043a\u0435\u0442\u044b \u043a\u043e\u0436\u0438.",
        }

    try:
        recommendation = await generate_recommendation_for_user(
            conn,
            user_id,
            **recommendation_runtime_dependencies,
        )
    except HTTPException as error:
        if int(error.status_code) == 400:
            return {
                "status": "missing_skin_passport",
                "message": str(
                    error.detail
                    or "\u0414\u043b\u044f \u043f\u0435\u0440\u0441\u043e\u043d\u0430\u043b\u044c\u043d\u043e\u0439 \u0440\u0435\u043a\u043e\u043c\u0435\u043d\u0434\u0430\u0446\u0438\u0438 \u043d\u0443\u0436\u043d\u044b \u043e\u0442\u0432\u0435\u0442\u044b \u0430\u043d\u043a\u0435\u0442\u044b \u043a\u043e\u0436\u0438."
                ),
            }
        raise

    compact = compact_recommendation_context(recommendation, query)
    if compact.get("products"):
        return compact
    return {
        "status": "empty_catalog_match",
        "message": "\u0412 \u043a\u0430\u0442\u0430\u043b\u043e\u0433\u0435 \u043f\u043e\u043a\u0430 \u043d\u0435 \u043d\u0430\u0448\u043b\u043e\u0441\u044c \u043f\u043e\u0434\u0445\u043e\u0434\u044f\u0449\u0438\u0445 \u043f\u0440\u043e\u0434\u0443\u043a\u0442\u043e\u0432 \u043f\u043e\u0434 \u044d\u0442\u043e\u0442 \u043f\u0440\u043e\u0444\u0438\u043b\u044c.",
    }
