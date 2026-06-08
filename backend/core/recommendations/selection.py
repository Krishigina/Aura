from typing import Any, Dict, List

from backend.core.matching.domain import ProductFunctionSignal
from backend.core.recommendations.product_routines import care_slot
from backend.core.recommendations.profiles import ProductRecommendationInput


CARE_SLOT_ORDER = {
    "cleansing": 10,
    "toner": 20,
    "active": 30,
    "moisturizer": 40,
    "spf": 50,
}
MIN_RECOMMENDATION_COMPATIBILITY_PERCENT = 65
MIN_ALTERNATIVE_COMPATIBILITY_PERCENT = 55


def routine_sort_key(item: Dict[str, Any]) -> tuple:
    return (CARE_SLOT_ORDER.get(str(item.get("care_slot") or ""), 90), -int(item.get("compatibility_percent") or 0))


def select_best_daily_steps(items: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    best_by_slot: Dict[str, Dict[str, Any]] = {}
    for item in sorted(items, key=lambda value: -int(value.get("compatibility_percent") or 0)):
        slot = str(item.get("care_slot") or "other")
        if slot not in best_by_slot:
            best_by_slot[slot] = item
    return sorted(best_by_slot.values(), key=routine_sort_key)


def select_weekly_steps(items: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    best_by_slot: Dict[str, Dict[str, Any]] = {}
    for item in sorted(items, key=lambda value: -int(value.get("compatibility_percent") or 0)):
        slot = str(item.get("care_slot") or "weekly")
        if slot not in best_by_slot:
            best_by_slot[slot] = item
    return sorted(best_by_slot.values(), key=lambda item: -int(item.get("compatibility_percent") or 0))[:2]


def compact_alternative_item(item: Dict[str, Any]) -> Dict[str, Any]:
    return {
        key: item[key]
        for key in (
            "product_id",
            "product_name",
            "brand",
            "step",
            "care_slot",
            "usage_type",
            "recommended_frequency",
            "compatibility_percent",
            "reason",
            "warnings",
            "conflicts_with",
        )
        if key in item
    }


def expert_compatibility_percent(product: ProductRecommendationInput, match_result: Any) -> int:
    percent = int(getattr(match_result, "compatibility_percent", 0) or 0)
    if getattr(match_result, "warnings", None):
        return percent
    slot = care_slot(product)
    if slot in {"cleansing", "spf"} and percent >= MIN_ALTERNATIVE_COMPATIBILITY_PERCENT:
        return min(100, percent + 8)
    return percent


def add_alternative(alternatives: Dict[str, Dict[str, List[Dict[str, Any]]]], line_key: str, item: Dict[str, Any]) -> None:
    slot = str(item.get("care_slot") or "other")
    line_alternatives = alternatives.setdefault(line_key, {})
    slot_items = line_alternatives.setdefault(slot, [])
    slot_items.append(compact_alternative_item(item))
    slot_items.sort(key=lambda value: -int(value.get("compatibility_percent") or 0))


def excluded_product(product: ProductRecommendationInput, match_result: Any, reason: str) -> Dict[str, Any]:
    return {
        "product_id": product.id,
        "product_name": product.name,
        "brand": product.brand,
        "step": product.product_type,
        "care_slot": care_slot(product),
        "compatibility_percent": int(getattr(match_result, "compatibility_percent", 0) or 0),
        "decision": str(getattr(match_result, "decision", "exclude") or "exclude"),
        "reason": reason,
        "warnings": list(getattr(match_result, "warnings", []) or []),
    }


def append_missing_slots(lines_by_key: Dict[str, Dict[str, Any]], alternatives: Dict[str, Dict[str, List[Dict[str, Any]]]]) -> List[Dict[str, str]]:
    missing: List[Dict[str, str]] = []
    for line_key, slots in alternatives.items():
        line = lines_by_key.get(line_key)
        if not line:
            continue
        selected_slots = {
            str(item.get("care_slot") or "")
            for bucket in ("morning", "evening", "weekly")
            for item in line["routine"].get(bucket, [])
        }
        for slot in slots:
            if slot not in selected_slots:
                missing.append(
                    {
                        "line_key": line_key,
                        "care_slot": slot,
                        "reason": f"Нет продукта с экспертной совместимостью >= {MIN_RECOMMENDATION_COMPATIBILITY_PERCENT}",
                    }
                )
    return missing


def _has_retinoid(item: Dict[str, Any]) -> bool:
    text = " ".join(str(item.get(key) or "") for key in ("product_name", "step", "reason", "instruction")).lower()
    conflicts = {str(value).lower() for value in item.get("conflicts_with", []) or []}
    return "ретин" in text or "retinol" in text or "retinol" in conflicts


def _has_acid_or_exfoliation(item: Dict[str, Any]) -> bool:
    text = " ".join(str(item.get(key) or "") for key in ("product_name", "step", "reason", "instruction", "care_slot")).lower()
    conflicts = {str(value).lower() for value in item.get("conflicts_with", []) or []}
    return any(term in text for term in ("acid", "кислот", "пилинг", "peel", "exfoliation")) or bool(
        {"acid", "peeling", "retinol"}.intersection(conflicts)
    )


def validate_combinations(lines_by_key: Dict[str, Dict[str, Any]], alternatives: Dict[str, Dict[str, List[Dict[str, Any]]]]) -> List[str]:
    warnings: List[str] = []
    for line_key, line in lines_by_key.items():
        daily_items = line["routine"].get("morning", []) + line["routine"].get("evening", [])
        has_retinoid = any(_has_retinoid(item) for item in daily_items)
        if not has_retinoid:
            continue
        weekly_items = line["routine"].get("weekly", [])
        removed_weekly = [item for item in weekly_items if _has_acid_or_exfoliation(item)]
        if removed_weekly:
            line["routine"]["weekly"] = [item for item in weekly_items if item not in removed_weekly]
        weekly_alternatives = [
            item
            for slot_items in alternatives.get(line_key, {}).values()
            for item in slot_items
            if str(item.get("usage_type") or "") == "weekly"
        ]
        if removed_weekly or any(_has_acid_or_exfoliation(item) for item in weekly_alternatives):
            warnings.append("Еженедельные кислотные и отшелушивающие шаги пропущены, потому что в рутине уже есть ретиноид или активный вечерний продукт")
    return warnings


def coerce_source_ids(value: Any) -> List[int]:
    if not isinstance(value, list):
        return []
    source_ids: List[int] = []
    for item in value:
        try:
            source_ids.append(int(item))
        except (TypeError, ValueError):
            continue
    return source_ids


def product_function_signals(product: ProductRecommendationInput) -> List[ProductFunctionSignal]:
    signals: List[ProductFunctionSignal] = []
    for signal in product.function_signals or []:
        if isinstance(signal, ProductFunctionSignal):
            signals.append(signal)
        elif isinstance(signal, dict):
            signals.append(
                ProductFunctionSignal(
                    function_key=str(signal.get("function_key") or ""),
                    score=float(signal.get("score") or 0),
                    evidence_status=str(signal.get("evidence_status") or "auto_only"),
                    evidence_count=int(signal.get("evidence_count") or 0),
                    source_ids=coerce_source_ids(signal.get("source_ids")),
                )
            )
    return signals


def reason_text(product: ProductRecommendationInput, match_result: Any) -> str:
    compatibility = int(getattr(match_result, "compatibility_percent", 0) or 0)
    if compatibility < 40:
        return "Низкая совместимость: средство не является приоритетной рекомендацией для текущего профиля"
    if compatibility < 50:
        return "Возможное средство только с осторожностью: совместимость ниже оптимального уровня"
    if match_result.matched_concerns:
        return f"Подходит для коррекции: {', '.join(match_result.matched_concerns)}"
    if match_result.matched_goals:
        return f"Подходит для целей: {', '.join(match_result.matched_goals)}"
    purpose = product.purpose_values()
    if purpose:
        return f"Соответствует задаче ухода: {', '.join(purpose)}"
    return "Подобрано по данным анкеты кожи"


def summary_text(answers: Dict[str, List[str]], latest_sensor: Dict[str, Any], sensor_value) -> Dict[str, str]:
    hydration = sensor_value(latest_sensor, "hydration")
    oiliness = sensor_value(latest_sensor, "oiliness")

    if hydration is not None and hydration <= 2:
        title = "Фокус на увлажнение и поддержку барьера"
        description = (
            "Замеры показывают низкое увлажнение: сделайте акцент на поддержке барьера "
            "и восстановлении комфорта кожи."
        )
        if oiliness is not None and oiliness >= 4:
            description += " Дополнительно учитывайте sebum control без пересушивания."
        return {"title": title, "description": description}

    if oiliness is not None and oiliness >= 4:
        return {
            "title": "Персональная линейка с sebum control",
            "description": "Замеры показывают повышенную жирность: уход должен помогать контролировать себум и блеск.",
        }

    concerns = answers.get("concerns") or answers.get("concern") or []
    if concerns:
        return {"title": f"Персональная линейка для: {', '.join(concerns)}", "description": ""}
    return {"title": "Персональная линейка ухода", "description": ""}
