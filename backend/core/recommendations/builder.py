from datetime import datetime, timezone
from typing import Any, Dict, List
from uuid import uuid4

from backend.core.matching.domain import MatchingProfile, MatchingRule, ProductCandidate
from backend.core.matching.scoring import match_product
from backend.core.recommendations.profiles import (
    LINE_KEYS,
    LINE_POSITIONING,
    LINE_TITLES,
    ProductRecommendationInput,
    RecommendationGenerationError,
    build_extended_skin_profile,
    has_aggressive_active,
    has_answers,
    has_recent_procedure,
    latest_sensor_reading,
    normalize_product_segment,
    sensor_value,
)
from backend.core.recommendations.routines import (
    MIN_ALTERNATIVE_COMPATIBILITY_PERCENT,
    MIN_RECOMMENDATION_COMPATIBILITY_PERCENT,
    add_alternative,
    append_missing_slots,
    care_slot,
    conflicts_with,
    excluded_product,
    expert_compatibility_percent,
    frequency,
    instruction,
    product_function_signals,
    reason_text,
    recommended_frequency,
    routine_bucket,
    select_best_daily_steps,
    select_weekly_steps,
    summary_text,
    usage_note,
    usage_type,
    validate_combinations,
)


def build_recommendation(
    *,
    answers: Dict[str, List[str]],
    accepted_insights: List[str],
    sensor_readings: List[Dict[str, Any]],
    procedures: List[Dict[str, Any]],
    products: List[ProductRecommendationInput],
    rules: List[MatchingRule],
) -> Dict[str, Any]:
    if not has_answers(answers):
        raise RecommendationGenerationError(400, "Чтобы собрать точную линейку, пройдите анкету кожи")

    notes: List[str] = []
    warnings: List[str] = []
    procedure_context: List[str] = []
    input_quality = {
        "skin_passport": "available",
        "sensor_readings": "available" if sensor_readings else "missing",
        "procedures": "available" if procedures else "missing",
        "notes": notes,
    }

    if not sensor_readings:
        notes.append("Замеры не учтены")

    recent_procedure = has_recent_procedure(procedures)
    if recent_procedure:
        input_quality["procedures"] = "recent"
        procedure_context.append("После недавних процедур коже нужен восстановительный режим")
        warnings.append("После процедур используйте SPF и избегайте агрессивных активов")

    latest_sensor = latest_sensor_reading(sensor_readings)
    extended_skin_profile = build_extended_skin_profile(answers or {}, accepted_insights or [], sensor_readings or [])
    profile = MatchingProfile(answers=answers or {}, accepted_insights=accepted_insights or [], skin_state=extended_skin_profile)
    lines_by_key = {
        key: {
            "key": key,
            "title": LINE_TITLES[key],
            "positioning": LINE_POSITIONING[key],
            "routine": {"morning": [], "evening": [], "weekly": []},
            "warnings": [],
        }
        for key in LINE_KEYS
    }
    alternatives: Dict[str, Dict[str, List[Dict[str, Any]]]] = {}
    excluded_products: List[Dict[str, Any]] = []

    for product in products or []:
        if recent_procedure and has_aggressive_active(product):
            excluded_products.append(
                {
                    "product_id": product.id,
                    "product_name": product.name,
                    "brand": product.brand,
                    "step": product.product_type,
                    "care_slot": care_slot(product),
                    "compatibility_percent": 0,
                    "decision": "exclude",
                    "reason": "Исключено после недавней процедуры: агрессивные активы временно небезопасны",
                    "warnings": ["После процедур избегайте агрессивных активов"],
                }
            )
            continue

        candidate = ProductCandidate(
            id=product.id,
            name=product.name,
            composition=product.composition,
            product_type=product.product_type,
            purpose=", ".join(product.purpose_values()),
            skin_type=", ".join(product.skin_type_values()),
            function_signals=product_function_signals(product),
        )
        match_result = match_product(candidate, profile, rules or [])
        expert_percent = expert_compatibility_percent(product, match_result)
        line_key = normalize_product_segment(product.segment)
        usage_kind = usage_type(product)
        recommended_value = recommended_frequency(product, extended_skin_profile) if usage_kind == "weekly" else frequency(product)
        item = {
            "sequence": 0,
            "product_id": product.id,
            "product_name": product.name,
            "brand": product.brand,
            "step": product.product_type,
            "care_slot": care_slot(product),
            "usage_type": usage_kind,
            "recommended_frequency": recommended_value,
            "compatibility_percent": expert_percent,
            "score_breakdown": match_result.score_breakdown,
            "explanations": match_result.explanations,
            "decision": match_result.decision,
            "instruction": instruction(product),
            "frequency": recommended_value,
            "usage_note": usage_note(usage_kind, recommended_value),
            "conflicts_with": conflicts_with(product),
            "reason": reason_text(product, match_result),
            "warnings": match_result.warnings,
            "matched_functions": match_result.matched_functions,
            "evidence_explanations": match_result.evidence_explanations,
        }

        if match_result.decision != "recommend":
            excluded_products.append(
                excluded_product(
                    product,
                    match_result,
                    "Исключено строгим экспертным режимом: есть предупреждения или противопоказания",
                )
            )
            continue
        if expert_percent < MIN_ALTERNATIVE_COMPATIBILITY_PERCENT:
            excluded_products.append(excluded_product(product, match_result, "Совместимость ниже экспертного минимума"))
            continue
        if usage_kind == "weekly":
            line = lines_by_key[line_key]
            item["sequence"] = len(line["routine"]["weekly"]) + 1
            line["routine"]["weekly"].append(item)
            continue
        if expert_percent < MIN_RECOMMENDATION_COMPATIBILITY_PERCENT:
            add_alternative(alternatives, line_key, item)
            continue

        line = lines_by_key[line_key]
        bucket = "weekly" if usage_kind == "weekly" else routine_bucket(product)
        item["sequence"] = len(line["routine"][bucket]) + 1
        line["routine"][bucket].append(item)

    for line in lines_by_key.values():
        for bucket in ("morning", "evening"):
            line["routine"][bucket].sort(key=lambda item: -int(item.get("compatibility_percent") or 0))
            line["routine"][bucket] = select_best_daily_steps(line["routine"][bucket])
            for index, item in enumerate(line["routine"][bucket], start=1):
                item["sequence"] = index
        line["routine"]["weekly"].sort(key=lambda item: -int(item.get("compatibility_percent") or 0))
        line["routine"]["weekly"] = select_weekly_steps(line["routine"]["weekly"])
        for index, item in enumerate(line["routine"]["weekly"], start=1):
            item["sequence"] = index

    combination_warnings = validate_combinations(lines_by_key, alternatives)
    missing_slots = append_missing_slots(lines_by_key, alternatives)

    lines = [lines_by_key[key] for key in LINE_KEYS]
    for line in lines:
        if not line["routine"]["morning"] and not line["routine"]["evening"]:
            line["warnings"].append("Недостаточно продуктов для полноценной линейки")

    summary = summary_text(answers or {}, latest_sensor, sensor_value)

    return {
        "id": str(uuid4()),
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "summary": {
            "title": summary["title"],
            "description": summary["description"],
            "line_count": len(lines),
        },
        "input_quality": input_quality,
        "extended_skin_profile": extended_skin_profile,
        "lines": lines,
        "expert_mode": True,
        "alternatives": alternatives,
        "excluded_products": excluded_products,
        "missing_slots": missing_slots,
        "combination_warnings": combination_warnings,
        "procedure_context": procedure_context,
        "warnings": warnings,
    }
