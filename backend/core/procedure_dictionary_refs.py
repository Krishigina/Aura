import json
from typing import Any


PROCEDURE_SCALAR_DICTIONARY_MAP = {
    "direction": ("direction_id", "procedure_categories"),
    "method_type": ("method_type_id", "procedure_method_types"),
    "duration": ("duration_id", "procedure_durations"),
    "equipment": ("equipment_id", "procedure_equipment"),
    "for_whom": ("for_whom_id", "for_whom"),
}

PROCEDURE_MULTI_DICTIONARY_MAP = {
    "zones": ("procedure_zones", "procedure_zone_links", "zone_id"),
    "effects": ("procedure_effects", "procedure_effect_links", "effect_id"),
    "problems": ("procedure_problems", "procedure_problem_links", "problem_id"),
}


def normalize_dictionary_value(value: Any) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
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


async def sync_procedure_dictionary_refs(conn, row) -> None:
    procedure_id = row["id"]
    updates: dict[str, int | None] = {}

    for source_field, (target_field, table_name) in PROCEDURE_SCALAR_DICTIONARY_MAP.items():
        updates[target_field] = await ensure_dictionary_value(conn, table_name, row.get(source_field))

    if updates:
        set_sql = ", ".join(f"{field} = ${index}" for index, field in enumerate(updates.keys(), start=1))
        await conn.execute(
            f"UPDATE procedures SET {set_sql} WHERE id = ${len(updates) + 1}",
            *updates.values(),
            procedure_id,
        )

    for source_field, (dictionary_table, link_table, dictionary_id_column) in PROCEDURE_MULTI_DICTIONARY_MAP.items():
        values = parse_dictionary_list(row.get(source_field))
        await conn.execute(f"DELETE FROM {link_table} WHERE procedure_id = $1", procedure_id)
        for value in values:
            dictionary_id = await ensure_dictionary_value(conn, dictionary_table, value)
            if dictionary_id is None:
                continue
            await conn.execute(
                f"""
                INSERT INTO {link_table} (procedure_id, {dictionary_id_column})
                VALUES ($1, $2)
                ON CONFLICT (procedure_id, {dictionary_id_column}) DO NOTHING
                """,
                procedure_id,
                dictionary_id,
            )


async def backfill_procedure_dictionary_refs(conn) -> None:
    existing_columns = {
        row["column_name"]
        for row in await conn.fetch(
            "SELECT column_name FROM information_schema.columns WHERE table_name='procedures'"
        )
    }
    source_text_columns = [
        "direction", "method_type", "duration", "equipment", "zones", "effects", "problems", "for_whom",
    ]
    if not any(column in existing_columns for column in source_text_columns):
        return
    select_columns = [
        column_name if column_name in existing_columns else f"NULL AS {column_name}"
        for column_name in source_text_columns
    ]
    rows = await conn.fetch(
        f"""
        SELECT id, {", ".join(select_columns)}
        FROM procedures
        ORDER BY id
        """
    )
    for row in rows:
        await sync_procedure_dictionary_refs(conn, row)
