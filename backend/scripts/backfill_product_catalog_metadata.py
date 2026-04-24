import asyncio
import re
import unicodedata
from typing import Any

import asyncpg

from backend.core.config import DB_HOST, DB_NAME, DB_PASSWORD, DB_PORT, DB_USER
from backend.core.product_dictionary_refs import sync_product_dictionary_refs
from backend.core.products import product_select_sql


SEGMENT_BY_BRAND = {
    "La Roche-Posay": "Космецевтика",
    "Vichy": "Космецевтика",
    "Bioderma": "Космецевтика",
    "CeraVe": "Космецевтика",
    "Eucerin": "Космецевтика",
    "Paula's Choice": "Космецевтика",
    "The Ordinary": "Космецевтика",
    "Cosrx": "Космецевтика",
    "Round Lab": "Космецевтика",
    "Caudalie": "Люкс",
    "Clarins": "Люкс",
    "Clinique": "Люкс",
    "Christina": "Профессиональная",
    "Aura": "Бюджетная",
    "Belinda": "Бюджетная",
    "Eveline": "Бюджетная",
    "Mixit": "Бюджетная",
    "Nivea": "Бюджетная",
}

PRODUCT_TYPE_PATTERNS = [
    ("Мицеллярная вода", ["мицеллярная вода", "micellar water"]),
    ("Гидрофильное масло", ["гидрофильное масло", "cleansing oil", "hydrophilic oil"]),
    ("Пудра энзимная", ["энзимная пудра", "enzyme powder"]),
    ("Крем-гель для лица", ["крем-гель", "cream-gel"]),
    ("Крем для лица", ["крем для лица", "face cream", "hydrator"]),
    ("Эссенция для лица", ["эссенция", "essence"]),
    ("Пенка для лица", ["пенка", "foam cleanser", "foam wash"]),
    ("Мусс для лица", ["мусс", "mousse"]),
    ("Гель для лица", ["гель для умывания", "очищающий гель", "cleanser gel", "face wash gel"]),
    ("Пилинг для лица", ["пилинг", "peel", "exfoliator"]),
    ("Сыворотка", ["сыворотка", "serum"]),
    ("Тоник", ["тоник", "toner"]),
    ("Лосьон", ["лосьон", "lotion"]),
    ("Крем", ["крем", "cream"]),
    ("Маска", ["маска", "mask"]),
    ("Масло", ["масло", "oil"]),
    ("Спрей", ["спрей", "spray", "mist"]),
    ("Эмульсия", ["эмульсия", "emulsion"]),
    ("Гель", ["гель", "gel"]),
    ("Бальзам", ["бальзам", "balm"]),
]

CATEGORY_PATTERNS = [
    ("SPF", ["spf", "sun screen", "sunscreen", "защита от солнца", "санскрин"]),
    ("Очищение", ["очищ", "умыван", "cleanser", "face wash", "micellar", "мицелляр", "пенка", "гель для умывания", "отшелушивающий лосьон", "exfoliator"]),
    ("Тоник", ["тоник", "toner"]),
    ("Сыворотки", ["сыворот", "serum", "эссенц"]),
    ("Крем", ["крем", "cream", "hydrator"]),
    ("Маска", ["маска", "mask"]),
    ("Масло", ["масло", "oil"]),
    ("Увлажнение", ["увлажн", "hydrat", "moistur", "hyaluronic"]),
    ("Уход", ["уход", "care"]),
]

SKIN_TYPE_PATTERNS = [
    ("Чувствительная", ["чувствительной кожи", "чувствительная кожа", "sensitive skin", "reactive skin"]),
    ("Сухая", ["сухой кожи", "очень сухой кожи", "сухая кожа", "dry skin"]),
    ("Жирная", ["жирной кожи", "очень жирной кожи", "жирная кожа", "oily skin"]),
    ("Комбинированная", ["комбинированной кожи", "комбинированная кожа", "combination skin", "dry combination"]),
    ("Нормальная", ["нормальной кожи", "нормальная кожа", "normal skin"]),
]

GENERIC_ALL_SKIN_PATTERNS = [
    "для всех типов кожи",
    "для любого типа кожи",
    "для всех типов",
    "for all skin types",
    "all skin types",
]

PROBLEM_SKIN_PATTERNS = [
    "проблемной кожи",
    "проблемная кожа",
    "акне",
    "постакне",
    "post-acne",
    "комедон",
    "черные точки",
    "угрев",
    "жирный блеск",
    "избыток кожного сала",
    "sebum",
    "acne",
]

SENSITIVE_SKIN_HINT_PATTERNS = [
    "покраснен",
    "купероз",
    "реактивной кожи",
    "реактивная кожа",
    "раздражен",
    "rosacea",
    "reactive skin",
]

DRY_SKIN_HINT_PATTERNS = [
    "обезвож",
    "шелуш",
    "поврежденную кожу",
    "очень сухой",
    "пересушенной кожи",
    "липидный барьер",
    "dehydrated",
    "dehydration",
]

OILY_SKIN_HINT_PATTERNS = [
    "жирной кожи",
    "излишков себума",
    "избыток кожного сала",
    "матиру",
    "расширенных пор",
    "блеска",
    "oily",
    "sebum",
]

COMBINATION_SKIN_HINT_PATTERNS = [
    "комбинированной кожи",
    "комбинированная кожа",
    "смешанного типа",
    "смешанной кожи",
    "combination skin",
]


def normalize_text(value: str | None) -> str:
    if not value:
        return ""
    normalized = unicodedata.normalize("NFKD", value)
    return "".join(ch for ch in normalized if not unicodedata.combining(ch)).lower()


def compact_whitespace(value: str | None) -> str:
    return re.sub(r"\s+", " ", (value or "")).strip()


def is_placeholder_value(value: str | None) -> bool:
    normalized = compact_whitespace(value)
    return bool(normalized) and all(ch in {"?", "�"} for ch in normalized)


def infer_brand(name: str, what_is_it: str, description: str, brands: list[str]) -> str | None:
    haystack = f"{name} {what_is_it} {description}"
    normalized_haystack = normalize_text(haystack)
    for brand in sorted(brands, key=len, reverse=True):
        normalized_brand = normalize_text(brand)
        if normalized_brand and normalized_brand in normalized_haystack:
            return brand
    return None


def infer_product_type(name: str, what_is_it: str, description: str, known_product_types: set[str]) -> str | None:
    haystack = normalize_text(f"{what_is_it} {name} {description}")
    for product_type, patterns in PRODUCT_TYPE_PATTERNS:
        if product_type not in known_product_types:
            continue
        if any(pattern in haystack for pattern in patterns):
            return product_type
    return None


def infer_category(name: str, what_is_it: str, description: str, product_type: str | None, known_categories: set[str]) -> str | None:
    haystack = normalize_text(f"{what_is_it} {name} {description}")
    for category, patterns in CATEGORY_PATTERNS:
        if category not in known_categories:
            continue
        if any(pattern in haystack for pattern in patterns):
            return category
    if product_type in {"Крем", "Крем для лица", "Крем-гель для лица"} and "Крем" in known_categories:
        return "Крем"
    if product_type == "Тоник" and "Тоник" in known_categories:
        return "Тоник"
    if product_type == "Маска" and "Маска" in known_categories:
        return "Маска"
    if product_type == "Масло" and "Масло" in known_categories:
        return "Масло"
    if product_type in {"Сыворотка", "Эссенция для лица"} and "Сыворотки" in known_categories:
        return "Сыворотки"
    return None


def infer_segment(brand: str | None, category: str | None, product_type: str | None, known_segments: set[str]) -> str | None:
    if brand in SEGMENT_BY_BRAND and SEGMENT_BY_BRAND[brand] in known_segments:
        return SEGMENT_BY_BRAND[brand]
    if category == "SPF" and "Космецевтика" in known_segments:
        return "Космецевтика"
    if product_type in {"Пудра энзимная", "Пилинг для лица"} and "Профессиональная" in known_segments:
        return "Профессиональная"
    return None


def infer_skin_types(name: str, what_is_it: str, description: str, known_skin_types: set[str]) -> list[str]:
    haystack = normalize_text(f"{what_is_it} {name} {description}")
    values: list[str] = []

    def add_if_known(value: str) -> None:
        if value in known_skin_types and value not in values:
            values.append(value)

    for skin_type, patterns in SKIN_TYPE_PATTERNS:
        if skin_type not in known_skin_types:
            continue
        if any(pattern in haystack for pattern in patterns):
            add_if_known(skin_type)

    if any(pattern in haystack for pattern in SENSITIVE_SKIN_HINT_PATTERNS):
        add_if_known("Чувствительная")
    if any(pattern in haystack for pattern in DRY_SKIN_HINT_PATTERNS):
        add_if_known("Сухая")
    if any(pattern in haystack for pattern in OILY_SKIN_HINT_PATTERNS):
        add_if_known("Жирная")
    if any(pattern in haystack for pattern in COMBINATION_SKIN_HINT_PATTERNS):
        add_if_known("Комбинированная")

    if any(pattern in haystack for pattern in PROBLEM_SKIN_PATTERNS):
        add_if_known("Проблемная")
        add_if_known("Жирная")
        add_if_known("Комбинированная")

    if any(pattern in haystack for pattern in GENERIC_ALL_SKIN_PATTERNS):
        add_if_known("Для всех типов кожи")

    return values


async def load_dictionary_values(conn: asyncpg.Connection, table_name: str) -> list[str]:
    rows = await conn.fetch(f"SELECT value FROM {table_name} ORDER BY value")
    return [compact_whitespace(row["value"]) for row in rows if compact_whitespace(row["value"])]


async def main() -> None:
    conn = await asyncpg.connect(
        host=DB_HOST,
        port=DB_PORT,
        database=DB_NAME,
        user=DB_USER,
        password=DB_PASSWORD,
    )
    try:
        brands = await load_dictionary_values(conn, "brands")
        categories = await load_dictionary_values(conn, "categories")
        product_types = await load_dictionary_values(conn, "product_types")
        segments = await load_dictionary_values(conn, "segments")
        skin_types = await load_dictionary_values(conn, "skin_types")

        known_categories = set(categories)
        known_product_types = set(product_types)
        known_segments = set(segments)
        known_skin_types = set(skin_types)

        rows = await conn.fetch(
            f"""
            SELECT *
            FROM ({product_select_sql('p')}) AS hydrated_products
            ORDER BY id
            """
        )

        updated = 0
        for row in rows:
            current_brand = "" if is_placeholder_value(row["brand"]) else compact_whitespace(row["brand"])
            current_category = "" if is_placeholder_value(row["category"]) else compact_whitespace(row["category"])
            current_segment = "" if is_placeholder_value(row["segment"]) else compact_whitespace(row["segment"])
            current_product_type = "" if is_placeholder_value(row["product_type"]) else compact_whitespace(row["product_type"])

            name = compact_whitespace(row["name"])
            what_is_it = compact_whitespace(row["what_is_it"])
            description = compact_whitespace(row["description"])

            inferred_brand = current_brand or infer_brand(name, what_is_it, description, brands)
            inferred_product_type = current_product_type or infer_product_type(name, what_is_it, description, known_product_types)
            inferred_category = current_category or infer_category(name, what_is_it, description, inferred_product_type, known_categories)
            inferred_segment = current_segment or infer_segment(inferred_brand, inferred_category, inferred_product_type, known_segments)
            existing_skin_types = [value for value in (row["skin_type"] or []) if compact_whitespace(value)]
            inferred_skin_types = existing_skin_types or infer_skin_types(name, what_is_it, description, known_skin_types)

            should_update = any(
                [
                    not current_brand and inferred_brand,
                    not current_product_type and inferred_product_type,
                    not current_category and inferred_category,
                    not current_segment and inferred_segment,
                    row["brand_id"] is None and inferred_brand,
                    row["product_type_id"] is None and inferred_product_type,
                    row["category_id"] is None and inferred_category,
                    row["segment_id"] is None and inferred_segment,
                    not existing_skin_types and inferred_skin_types,
                ]
            )
            if not should_update:
                continue

            payload: dict[str, Any] = {
                "id": row["id"],
                "brand": inferred_brand,
                "category": inferred_category,
                "segment": inferred_segment,
                "product_type": inferred_product_type,
                "purpose": row["purpose"],
                "skin_type": inferred_skin_types,
                "volume": row["volume"],
                "for_whom": row["for_whom"],
                "application_time": row["application_time"],
                "area": row["area"],
                "country": row["country"],
            }
            await sync_product_dictionary_refs(conn, payload)
            updated += 1
            print(
                f"updated product {row['id']}: "
                f"brand={inferred_brand!r}, category={inferred_category!r}, "
                f"segment={inferred_segment!r}, product_type={inferred_product_type!r}, "
                f"skin_type={inferred_skin_types!r}"
            )

        print(f"done: updated {updated} products")
    finally:
        await conn.close()


if __name__ == "__main__":
    asyncio.run(main())
