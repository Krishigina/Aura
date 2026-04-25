from typing import Any, Dict, List

from backend.core.recommendations.profiles import ProductRecommendationInput


WEEKLY_PRODUCT_TERMS = (
    "mask",
    "маск",
    "маска",
    "scrub",
    "скраб",
    "peel",
    "пилинг",
    "enzyme",
    "энзим",
    "exfoliat",
    "aha",
    "bha",
    "pha",
)


def routine_bucket(product: ProductRecommendationInput) -> str:
    info = str(product.application_info or "").lower()
    if "вечер" in info or "night" in info or "pm" in info or "evening" in info:
        return "evening"
    text = product_text(product)
    if any(term in text for term in ("retinol", "acid", "aha", "bha", "pha", "peel", "exfoliat")):
        return "evening"
    return "morning"


def instruction(product: ProductRecommendationInput) -> str:
    return product.application_info or f"Используйте средство: {product.product_type}"


def frequency(product: ProductRecommendationInput) -> str:
    info = str(product.application_info or "").lower()
    if "ежеднев" in info or "daily" in info or "every day" in info:
        return "ежедневно"
    if "вечер" in info or "evening" in info:
        return "вечером"
    if "утр" in info or "morning" in info or "am" in info:
        return "утром"
    return "по рекомендации специалиста"


def product_text(product: ProductRecommendationInput) -> str:
    return " ".join(
        [
            str(product.product_type or ""),
            " ".join(product.purpose_values()),
            str(product.application_info or ""),
            str(product.composition or ""),
        ]
    ).lower()


def is_weekly_product(product: ProductRecommendationInput) -> bool:
    text = product_text(product)
    return (
        any(term in text for term in WEEKLY_PRODUCT_TERMS)
        or "1-2" in text
        or "1 раз" in text
        or "в неделю" in text
        or "week" in text
    )


def care_slot(product: ProductRecommendationInput) -> str:
    text = product_text(product)
    product_type = str(product.product_type or "").strip().lower()
    if is_weekly_product(product):
        if "маск" in text or "mask" in text:
            return "weekly_mask"
        return "weekly_exfoliation"
    if any(term in text for term in ("spf", "солнц", "санскрин", "защит")):
        return "spf"
    if any(term in text for term in ("очищ", "умыван", "cleanser", "cleansing", "пенк", "мицел")):
        return "cleansing"
    if any(term in text for term in ("тоник", "тонер", "toner")):
        return "toner"
    if any(term in text for term in ("сыворот", "serum", "ретин", "retinol", "кислот", "acid", "niacinamide", "ниацинамид", "active")):
        return "active"
    if any(term in text for term in ("крем", "cream", "barrier", "ceramide", "эмульс", "emulsion", "лосьон", "lotion", "увлаж", "hydration")):
        return "moisturizer"
    return product_type or "other"


def recommended_frequency(product: ProductRecommendationInput, extended_skin_profile: Dict[str, Any]) -> str:
    text = product_text(product)
    sensitive = "sensitive" in extended_skin_profile.get("state_tags", []) or "sensitivity" in extended_skin_profile.get("concerns", [])
    if sensitive:
        return "1 раз в неделю"
    if "скраб" in text or "scrub" in text or "пилинг" in text or "peel" in text:
        return "1 раз в неделю"
    return "1-2 раза в неделю"


def usage_type(product: ProductRecommendationInput) -> str:
    return "weekly" if is_weekly_product(product) else "daily"


def conflicts_with(product: ProductRecommendationInput) -> List[str]:
    text = product_text(product)
    conflicts: List[str] = []
    if any(term in text for term in ("ретин", "retinol")):
        conflicts.extend(["acid", "peeling", "scrub"])
    if any(term in text for term in ("кислот", "acid", "aha", "bha", "pha", "пилинг", "peel")):
        conflicts.extend(["retinol", "scrub"])
    if any(term in text for term in ("скраб", "scrub")):
        conflicts.extend(["retinol", "acid", "peeling"])
    return list(dict.fromkeys(conflicts))


def usage_note(usage_kind: str, recommended_value: str) -> str:
    if usage_kind == "weekly":
        return (
            f"Используйте {recommended_value}, лучше вечером и не сочетайте "
            "с другими агрессивными активами в тот же день"
        )
    return "Подходит для регулярного ежедневного ухода"
