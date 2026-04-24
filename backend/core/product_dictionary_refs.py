import json
from typing import Any

from backend.core.products import product_select_sql


PRODUCT_SCALAR_DICTIONARY_MAP = {
    "brand": ("brand_id", "brands"),
    "category": ("category_id", "categories"),
    "segment": ("segment_id", "segments"),
    "volume": ("volume_id", "volumes"),
    "product_type": ("product_type_id", "product_types"),
    "for_whom": ("for_whom_id", "for_whom"),
    "application_time": ("application_time_id", "application_times"),
    "area": ("area_id", "areas"),
    "country": ("country_id", "countries"),
}

PRODUCT_MULTI_DICTIONARY_MAP = {
    "purpose": ("purposes", "product_purpose_links", "purpose_id"),
    "skin_type": ("skin_types", "product_skin_type_links", "skin_type_id"),
}


def normalize_dictionary_value(value: Any) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
    if not text:
        return None
    if all(ch in {"?", "�"} for ch in text):
        return None
    return text or None


def parse_dictionary_list(value: Any) -> list[str]:
    if value is None:
        return []
    if isinstance(value, list):
        return [str(item).strip() for item in value if str(item).strip()]
    if isinstance(value, str):
        raw = value.strip()
        if not raw:
            return []
        if raw.startswith("[") and raw.endswith("]"):
            try:
                parsed = json.loads(raw)
            except json.JSONDecodeError:
                parsed = None
            if isinstance(parsed, list):
                return [str(item).strip() for item in parsed if str(item).strip()]
        if "|" in raw:
            return [item.strip() for item in raw.split("|") if item.strip()]
        if "," in raw:
            return [item.strip() for item in raw.split(",") if item.strip()]
        return [raw]
    return [str(value).strip()]


async def ensure_dictionary_value(conn, table_name: str, value: str | None) -> int | None:
    normalized = normalize_dictionary_value(value)
    if not normalized:
        return None
    row = await conn.fetchrow(
        f"""
        INSERT INTO {table_name} (value)
        VALUES ($1)
        ON CONFLICT (value) DO UPDATE SET value = EXCLUDED.value
        RETURNING id
        """,
        normalized,
    )
    return row["id"] if row else None


async def sync_product_dictionary_refs(conn, row) -> None:
    product_id = row["id"]
    updates: dict[str, int | None] = {}

    for source_field, (target_field, table_name) in PRODUCT_SCALAR_DICTIONARY_MAP.items():
        updates[target_field] = await ensure_dictionary_value(conn, table_name, row.get(source_field))

    if updates:
        set_sql = ", ".join(f"{field} = ${index}" for index, field in enumerate(updates.keys(), start=1))
        await conn.execute(
            f"UPDATE products SET {set_sql} WHERE id = ${len(updates) + 1}",
            *updates.values(),
            product_id,
        )

    for source_field, (dictionary_table, link_table, dictionary_id_column) in PRODUCT_MULTI_DICTIONARY_MAP.items():
        values = parse_dictionary_list(row.get(source_field))
        await conn.execute(f"DELETE FROM {link_table} WHERE product_id = $1", product_id)
        for value in values:
            dictionary_id = await ensure_dictionary_value(conn, dictionary_table, value)
            if dictionary_id is None:
                continue
            await conn.execute(
                f"""
                INSERT INTO {link_table} (product_id, {dictionary_id_column})
                VALUES ($1, $2)
                ON CONFLICT (product_id, {dictionary_id_column}) DO NOTHING
                """,
                product_id,
                dictionary_id,
            )
    await sync_product_legacy_fields(conn, row)


async def sync_product_legacy_fields(conn, row) -> None:
    existing_columns = {
        record["column_name"]
        for record in await conn.fetch(
            "SELECT column_name FROM information_schema.columns WHERE table_name='products'"
        )
    }
    product_id = row["id"]
    scalar_updates = {
        column_name: normalize_dictionary_value(row.get(column_name))
        for column_name in PRODUCT_SCALAR_DICTIONARY_MAP.keys()
        if column_name in existing_columns
    }
    if scalar_updates:
        set_sql = ", ".join(f"{field} = ${index}" for index, field in enumerate(scalar_updates.keys(), start=1))
        await conn.execute(
            f"UPDATE products SET {set_sql} WHERE id = ${len(scalar_updates) + 1}",
            *scalar_updates.values(),
            product_id,
        )

    for column_name in PRODUCT_MULTI_DICTIONARY_MAP.keys():
        if column_name not in existing_columns:
            continue
        values = parse_dictionary_list(row.get(column_name))
        serialized = json.dumps(values, ensure_ascii=False) if values else None
        await conn.execute(f"UPDATE products SET {column_name} = $1 WHERE id = $2", serialized, product_id)


async def backfill_product_dictionary_refs(conn) -> None:
    rows = await conn.fetch(
        f"""
        SELECT *
        FROM ({product_select_sql('p')}) AS hydrated_products
        ORDER BY id
        """
    )
    for row in rows:
        await sync_product_dictionary_refs(conn, row)
