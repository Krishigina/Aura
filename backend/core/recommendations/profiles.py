from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional


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
SKIN_TYPE_ALIASES = {
    "dry": "dry",
    "dryness": "dry",
    "сухая": "dry",
    "очень сухая": "dry",
    "oily": "oily",
    "oiliness": "oily",
    "жирная": "oily",
    "очень жирная": "oily",
    "combination": "combination",
    "комбинированная": "combination",
    "sensitive": "sensitive",
    "чувствительная": "sensitive",
    "normal": "normal",
    "нормальная": "normal",
}
CONCERN_ALIASES = {
    "dryness": ["dryness"],
    "low_hydration": ["dryness", "low_hydration"],
    "сухость": ["dryness"],
    "обезвоженность": ["dryness", "low_hydration"],
    "увлажнить кожу": ["dryness", "low_hydration"],
    "увлажнение": ["dryness", "low_hydration"],
    "oiliness": ["oiliness"],
    "жирный блеск": ["oiliness"],
    "уменьшить жирность": ["oiliness"],
    "расширенные поры": ["visible_pores", "oiliness"],
    "acne": ["acne"],
    "акне": ["acne"],
    "избавиться от акне": ["acne"],
    "черные точки": ["comedones"],
    "постакне": ["acne"],
    "sensitivity": ["sensitivity"],
    "покраснения": ["sensitivity"],
    "купероз/сосуды": ["sensitivity"],
    "косметика -> жжение": ["sensitivity"],
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
    function_signals: List[Any] = field(default_factory=list)

    def purpose_values(self) -> List[str]:
        return self.purpose or []

    def skin_type_values(self) -> List[str]:
        return self.skin_type or []


def normalize_product_segment(value: str) -> str:
    return SEGMENT_ALIASES.get(str(value or "").strip().lower(), "budget")


def normalized_answer_values(answers: Dict[str, List[str]], *keys: str) -> List[str]:
    values: List[str] = []
    for key in keys:
        raw_values = answers.get(key) or []
        if isinstance(raw_values, str):
            raw_values = [raw_values]
        values.extend(str(item).strip().lower() for item in raw_values if str(item).strip())
    return values


def canonical_concerns_from_answers(answers: Dict[str, List[str]]) -> List[str]:
    concerns: List[str] = []
    for value in normalized_answer_values(
        answers,
        "concerns",
        "concern",
        "skin_issues",
        "goals",
        "diagnosis",
        "triggers",
    ):
        mapped = CONCERN_ALIASES.get(value)
        if mapped:
            concerns.extend(mapped)
        else:
            concerns.append(value)
    return list(dict.fromkeys(concerns))


def has_answers(answers: Dict[str, List[str]]) -> bool:
    return any(values for values in (answers or {}).values())


def parse_dt(value: Any) -> Optional[datetime]:
    if isinstance(value, datetime):
        return value if value.tzinfo else value.replace(tzinfo=timezone.utc)
    if not value:
        return None
    try:
        parsed = datetime.fromisoformat(str(value).replace("Z", "+00:00"))
    except ValueError:
        return None
    return parsed if parsed.tzinfo else parsed.replace(tzinfo=timezone.utc)


def has_recent_procedure(procedures: List[Dict[str, Any]]) -> bool:
    now = datetime.now(timezone.utc)
    for procedure in procedures or []:
        performed_at = parse_dt(procedure.get("performed_at"))
        if performed_at and 0 <= (now - performed_at).days <= 14:
            return True
    return False


def has_aggressive_active(product: ProductRecommendationInput) -> bool:
    searchable = " ".join(
        [
            str(product.product_type or ""),
            str(product.application_info or ""),
            str(product.composition or ""),
        ]
    ).lower()
    return any(term in searchable for term in AGGRESSIVE_ACTIVE_TERMS)


def latest_sensor_reading(sensor_readings: List[Dict[str, Any]]) -> Dict[str, Any]:
    readings = sensor_readings or []
    if not readings:
        return {}
    return max(
        readings,
        key=lambda reading: parse_dt(reading.get("measured_at") or reading.get("created_at"))
        or datetime.min.replace(tzinfo=timezone.utc),
    )


def sensor_value(reading: Dict[str, Any], key: str) -> Optional[float]:
    try:
        return float(reading[key])
    except (KeyError, TypeError, ValueError):
        return None


def zone_metric(zone: Dict[str, Any], key: str) -> Optional[float]:
    try:
        return float(zone[key])
    except (KeyError, TypeError, ValueError):
        return None


def derive_zone_concerns(zone: Dict[str, Any]) -> List[str]:
    concerns: List[str] = []
    hydration = zone_metric(zone, "hydration")
    oiliness = zone_metric(zone, "oiliness")
    sensitivity = zone_metric(zone, "sensitivity")
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
    latest = latest_sensor_reading(sensor_readings)
    zone_concerns: Dict[str, List[str]] = {}
    sensor_concerns: List[str] = []
    zones = latest.get("zones") if isinstance(latest, dict) else None
    if isinstance(zones, dict):
        for zone_name, zone_data in zones.items():
            if isinstance(zone_data, dict):
                derived = derive_zone_concerns(zone_data)
                if derived:
                    zone_concerns[str(zone_name)] = derived
    if isinstance(latest, dict):
        sensor_concerns = derive_zone_concerns(latest)

    tags = set(str(item) for item in accepted_insights or [])
    flat_zone_values = {value for values in zone_concerns.values() for value in values}
    sensor_values = set(sensor_concerns)
    if {"dryness", "low_hydration"}.intersection(flat_zone_values | sensor_values):
        tags.add("dehydrated_areas")
    if "oiliness" in flat_zone_values or "oiliness" in sensor_values:
        tags.add("oily_t_zone")
    if "sensitivity" in flat_zone_values or "sensitivity" in sensor_values:
        tags.add("sensitive")

    skin_types = answers.get("skin_type") or answers.get("skin_types") or []
    global_skin_type = str(skin_types[0]) if skin_types else ""
    if global_skin_type:
        global_skin_type = SKIN_TYPE_ALIASES.get(global_skin_type.strip().lower(), global_skin_type)
    if "dehydrated_areas" in tags and "oily_t_zone" in tags:
        global_skin_type = "combination"
        tags.add("combination_pattern")

    concerns = list(dict.fromkeys(canonical_concerns_from_answers(answers) + sensor_concerns + list(flat_zone_values)))
    profile = {
        "global_skin_type": global_skin_type,
        "concerns": concerns,
        "zone_concerns": zone_concerns,
        "state_tags": sorted(tags),
    }
    if isinstance(latest, dict):
        for key in ("hydration", "hydration_percent", "moisture", "moisture_percent", "oiliness", "sensitivity"):
            value = sensor_value(latest, key)
            if value is not None:
                profile[key] = int(value) if value.is_integer() else value
    return profile
