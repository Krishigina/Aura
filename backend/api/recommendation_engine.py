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
    purpose: List[str]
    skin_type: List[str]
    composition: str
    application_info: str = ""


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
    if match_result.matched_concerns:
        return f"Подходит для коррекции: {', '.join(match_result.matched_concerns)}"
    if match_result.matched_goals:
        return f"Подходит для целей: {', '.join(match_result.matched_goals)}"
    if product.purpose:
        return f"Соответствует задаче ухода: {', '.join(product.purpose)}"
    return "Подобрано по данным анкеты кожи"


def _summary_title(answers: Dict[str, List[str]]) -> str:
    concerns = answers.get("concerns") or answers.get("concern") or []
    if concerns:
        return f"Персональная линейка для: {', '.join(concerns)}"
    return "Персональная линейка ухода"


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

    if _has_recent_procedure(procedures):
        input_quality["procedures"] = "recent"
        procedure_context.append("После недавних процедур коже нужен восстановительный режим")
        warnings.append("После процедур используйте SPF и избегайте агрессивных активов")

    profile = MatchingProfile(answers=answers or {}, accepted_insights=accepted_insights or [])
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
        candidate = ProductCandidate(
            id=product.id,
            name=product.name,
            composition=product.composition,
            product_type=product.product_type,
            purpose=", ".join(product.purpose),
            skin_type=", ".join(product.skin_type),
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
                "name": product.name,
                "brand": product.brand,
                "product_type": product.product_type,
                "compatibility_percent": match_result.compatibility_percent,
                "instruction": _instruction(product),
                "frequency": _frequency(product),
                "reason": _reason(product, match_result),
                "warnings": match_result.warnings,
            }
        )

    lines = [lines_by_key[key] for key in LINE_KEYS]
    for line in lines:
        if not line["routine"]["morning"] and not line["routine"]["evening"]:
            line["warnings"].append("Недостаточно продуктов для полноценной линейки")

    return {
        "id": str(uuid4()),
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "summary": {
            "title": _summary_title(answers or {}),
            "line_count": len(lines),
        },
        "input_quality": input_quality,
        "lines": lines,
        "procedure_context": procedure_context,
        "warnings": warnings,
    }
