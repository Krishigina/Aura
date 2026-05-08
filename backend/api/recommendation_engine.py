from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional
from uuid import uuid4

try:
    from .matching_engine import MatchingProfile, MatchingRule, ProductCandidate, match_product
except ImportError:  # pragma: no cover - supports direct script-style imports
    from matching_engine import MatchingProfile, MatchingRule, ProductCandidate, match_product


LINE_KEYS = ["budget", "professional", "luxury", "cosmeceutical"]
LINE_TITLES = {
    "budget": "Бюджетная линейка",
    "professional": "Профессиональная линейка",
    "luxury": "Люксовая линейка",
    "cosmeceutical": "Космецевтическая линейка",
}
LINE_POSITIONING = {
    "budget": "Базовый уход с доступными средствами",
    "professional": "Салонный уровень ухода для регулярного применения",
    "luxury": "Премиальный уход с акцентом на комфорт и текстуры",
    "cosmeceutical": "Активный уход с дерматологическим фокусом",
}
AGGRESSIVE_ACTIVE_TERMS = ("retinol", "ретин", "acid", "кислот", "пилинг", "peel")
SEGMENT_ALIASES = {
    "budget": "budget",
    "бюджет": "budget",
    "бюджетная": "budget",
    "mass": "budget",
    "professional": "professional",
    "профессиональная": "professional",
    "профессиональный": "professional",
    "pro": "professional",
    "luxury": "luxury",
    "lux": "luxury",
    "люкс": "luxury",
    "премиум": "luxury",
    "cosmeceutical": "cosmeceutical",
    "cosmeceuticals": "cosmeceutical",
    "космецевтика": "cosmeceutical",
    "космецевтическая": "cosmeceutical",
}


class RecommendationGenerationError(Exception):
    def __init__(self, status_code: int, message: str):
        super().__init__(message)
        self.status_code = status_code
        self.message = message


@dataclass(frozen=True)
class ProductRecommendationInput:
    id: int
    name: str
    brand: str
    segment: str
    product_type: str
    purpose: Optional[List[str]]
    skin_type: Optional[List[str]]
    composition: str
    application_info: str = ""

    def purpose_values(self) -> List[str]:
        return self.purpose or []

    def skin_type_values(self) -> List[str]:
        return self.skin_type or []


def normalize_product_segment(value: str) -> str:
    return SEGMENT_ALIASES.get(str(value or "").strip().lower(), "budget")


def _has_answers(answers: Dict[str, List[str]]) -> bool:
    return any(values for values in (answers or {}).values())


def _parse_dt(value: Any) -> Optional[datetime]:
    if isinstance(value, datetime):
        return value if value.tzinfo else value.replace(tzinfo=timezone.utc)
    if not value:
        return None
    try:
        parsed = datetime.fromisoformat(str(value).replace("Z", "+00:00"))
    except ValueError:
        return None
    return parsed if parsed.tzinfo else parsed.replace(tzinfo=timezone.utc)


def _has_recent_procedure(procedures: List[Dict[str, Any]]) -> bool:
    now = datetime.now(timezone.utc)
    for procedure in procedures or []:
        performed_at = _parse_dt(procedure.get("performed_at"))
        if performed_at and 0 <= (now - performed_at).days <= 14:
            return True
    return False


def _has_aggressive_active(product: ProductRecommendationInput) -> bool:
    searchable = " ".join(
        [
            str(product.product_type or ""),
            str(product.application_info or ""),
            str(product.composition or ""),
        ]
    ).lower()
    return any(term in searchable for term in AGGRESSIVE_ACTIVE_TERMS)


def _latest_sensor_reading(sensor_readings: List[Dict[str, Any]]) -> Dict[str, Any]:
    readings = sensor_readings or []
    if not readings:
        return {}
    return max(
        readings,
        key=lambda reading: _parse_dt(reading.get("measured_at") or reading.get("created_at")) or datetime.min.replace(tzinfo=timezone.utc),
    )


def _sensor_value(reading: Dict[str, Any], key: str) -> Optional[float]:
    try:
        return float(reading[key])
    except (KeyError, TypeError, ValueError):
        return None


def _zone_metric(zone: Dict[str, Any], key: str) -> Optional[float]:
    try:
        return float(zone[key])
    except (KeyError, TypeError, ValueError):
        return None


def _derive_zone_concerns(zone: Dict[str, Any]) -> List[str]:
    concerns: List[str] = []
    hydration = _zone_metric(zone, "hydration")
    oiliness = _zone_metric(zone, "oiliness")
    sensitivity = _zone_metric(zone, "sensitivity")
    if hydration is not None and hydration <= 2:
        concerns.extend(["dryness", "low_hydration"])
    if oiliness is not None and oiliness >= 4:
        concerns.append("oiliness")
    if sensitivity is not None and sensitivity >= 4:
        concerns.append("sensitivity")
    return concerns


def build_extended_skin_profile(
    answers: Dict[str, List[str]],
    accepted_insights: List[str],
    sensor_readings: List[Dict[str, Any]],
) -> Dict[str, Any]:
    latest = _latest_sensor_reading(sensor_readings)
    zone_concerns: Dict[str, List[str]] = {}
    zones = latest.get("zones") if isinstance(latest, dict) else None
    if isinstance(zones, dict):
        for zone_name, zone_data in zones.items():
            if isinstance(zone_data, dict):
                derived = _derive_zone_concerns(zone_data)
                if derived:
                    zone_concerns[str(zone_name)] = derived

    tags = set(str(item) for item in accepted_insights or [])
    flat_zone_values = {value for values in zone_concerns.values() for value in values}
    if {"dryness", "low_hydration"}.intersection(flat_zone_values):
        tags.add("dehydrated_areas")
    if "oiliness" in flat_zone_values:
        tags.add("oily_t_zone")
    if "sensitivity" in flat_zone_values:
        tags.add("sensitive")

    skin_types = answers.get("skin_type") or answers.get("skin_types") or []
    global_skin_type = str(skin_types[0]) if skin_types else ""
    if "dehydrated_areas" in tags and "oily_t_zone" in tags:
        global_skin_type = "combination"
        tags.add("combination_pattern")

    concerns = list(dict.fromkeys((answers.get("concerns") or answers.get("concern") or []) + list(flat_zone_values)))
    return {
        "global_skin_type": global_skin_type,
        "concerns": concerns,
        "zone_concerns": zone_concerns,
        "state_tags": sorted(tags),
    }


def _routine_bucket(product: ProductRecommendationInput) -> str:
    info = str(product.application_info or "").lower()
    if "вечер" in info or "night" in info or "pm" in info:
        return "evening"
    return "morning"


def _instruction(product: ProductRecommendationInput) -> str:
    return product.application_info or f"Используйте средство: {product.product_type}"


def _frequency(product: ProductRecommendationInput) -> str:
    info = str(product.application_info or "").lower()
    if "ежеднев" in info:
        return "ежедневно"
    if "вечер" in info:
        return "вечером"
    if "утр" in info:
        return "утром"
    return "по рекомендации специалиста"


def _reason(product: ProductRecommendationInput, match_result: Any) -> str:
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


def _summary_text(answers: Dict[str, List[str]], latest_sensor: Dict[str, Any]) -> Dict[str, str]:
    hydration = _sensor_value(latest_sensor, "hydration")
    oiliness = _sensor_value(latest_sensor, "oiliness")

    if hydration is not None and hydration <= 2:
        title = "Фокус на увлажнение и поддержку барьера"
        description = "Замеры показывают низкое увлажнение: сделайте акцент на поддержке барьера и восстановлении комфорта кожи."
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


def build_recommendation(
    *,
    answers: Dict[str, List[str]],
    accepted_insights: List[str],
    sensor_readings: List[Dict[str, Any]],
    procedures: List[Dict[str, Any]],
    products: List[ProductRecommendationInput],
    rules: List[MatchingRule],
) -> Dict[str, Any]:
    if not _has_answers(answers):
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

    has_recent_procedure = _has_recent_procedure(procedures)
    if has_recent_procedure:
        input_quality["procedures"] = "recent"
        procedure_context.append("После недавних процедур коже нужен восстановительный режим")
        warnings.append("После процедур используйте SPF и избегайте агрессивных активов")

    latest_sensor = _latest_sensor_reading(sensor_readings)
    extended_skin_profile = build_extended_skin_profile(answers or {}, accepted_insights or [], sensor_readings or [])

    profile = MatchingProfile(answers=answers or {}, accepted_insights=accepted_insights or [], skin_state=extended_skin_profile)
    lines_by_key = {
        key: {
            "key": key,
            "title": LINE_TITLES[key],
            "positioning": LINE_POSITIONING[key],
            "routine": {"morning": [], "evening": []},
            "warnings": [],
        }
        for key in LINE_KEYS
    }

    for product in products or []:
        if has_recent_procedure and _has_aggressive_active(product):
            continue

        candidate = ProductCandidate(
            id=product.id,
            name=product.name,
            composition=product.composition,
            product_type=product.product_type,
            purpose=", ".join(product.purpose_values()),
            skin_type=", ".join(product.skin_type_values()),
        )
        match_result = match_product(candidate, profile, rules or [])
        if match_result.decision == "exclude":
            continue

        line = lines_by_key[normalize_product_segment(product.segment)]
        bucket = _routine_bucket(product)
        line["routine"][bucket].append(
            {
                "sequence": len(line["routine"][bucket]) + 1,
                "product_id": product.id,
                "product_name": product.name,
                "brand": product.brand,
                "step": product.product_type,
                "compatibility_percent": match_result.compatibility_percent,
                "score_breakdown": match_result.score_breakdown,
                "explanations": match_result.explanations,
                "decision": match_result.decision,
                "instruction": _instruction(product),
                "frequency": _frequency(product),
                "reason": _reason(product, match_result),
                "warnings": match_result.warnings,
            }
        )

    for line in lines_by_key.values():
        for bucket in ("morning", "evening"):
            line["routine"][bucket].sort(key=lambda item: -int(item.get("compatibility_percent") or 0))
            for index, item in enumerate(line["routine"][bucket], start=1):
                item["sequence"] = index

    lines = [lines_by_key[key] for key in LINE_KEYS]
    for line in lines:
        if not line["routine"]["morning"] and not line["routine"]["evening"]:
            line["warnings"].append("Недостаточно продуктов для полноценной линейки")

    summary = _summary_text(answers or {}, latest_sensor)

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
        "procedure_context": procedure_context,
        "warnings": warnings,
    }
