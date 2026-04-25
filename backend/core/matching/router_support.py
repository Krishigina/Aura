from typing import Any, Callable, Dict, Optional

from fastapi import HTTPException


def build_matching_runtime_dependencies(
    conn,
    *,
    load_skin_passport_context: Callable[..., Any],
    sanitize_skin_passport_answers: Callable[..., Any],
    load_accepted_passport_insights: Callable[..., Any],
    load_user_skin_journal: Callable[..., Any],
    load_product_function_signals: Callable[..., Any],
    product_match_sort_key: Optional[Callable[..., Any]] = None,
    filter_product_match_results: Optional[Callable[..., Any]] = None,
    product_row_to_recommendation_input: Optional[Callable[..., Any]] = None,
) -> Dict[str, Any]:
    dependencies = {
        "load_skin_passport_context": lambda user_id: load_skin_passport_context(conn, user_id, sanitize_skin_passport_answers),
        "sanitize_skin_passport_answers": sanitize_skin_passport_answers,
        "load_accepted_passport_insights": load_accepted_passport_insights,
        "load_user_skin_journal": load_user_skin_journal,
        "load_product_function_signals": load_product_function_signals,
    }
    if product_match_sort_key is not None:
        dependencies["product_match_sort_key"] = product_match_sort_key
    if filter_product_match_results is not None:
        dependencies["filter_product_match_results"] = filter_product_match_results
    if product_row_to_recommendation_input is not None:
        dependencies["product_row_to_recommendation_input"] = product_row_to_recommendation_input
    return dependencies


def build_matching_rule_updates(
    payload,
    *,
    allowed_fields: set[str],
    validate_matching_rule_status: Callable[[str], str],
    validate_matching_rule_effect: Callable[[str], str],
) -> Dict[str, Any]:
    raw_updates = payload.model_dump(exclude_unset=True)
    updates = {key: value for key, value in raw_updates.items() if key in allowed_fields}
    if not updates:
        raise HTTPException(status_code=400, detail="Нет данных для обновления")
    if "status" in updates:
        updates["status"] = validate_matching_rule_status(updates["status"])
    if "effect" in updates:
        updates["effect"] = validate_matching_rule_effect(updates["effect"])
    return updates
