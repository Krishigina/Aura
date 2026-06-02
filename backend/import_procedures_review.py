import asyncio
import json
import os
from pathlib import Path
from typing import Any, Dict, List

import asyncpg

from backend.core.procedure_dictionary_refs import sync_procedure_dictionary_refs


REVIEW_JSON_PATH = Path(__file__).resolve().parent / "data" / "procedures_seed_review.json"

INSERT_COLUMNS = [
    "name",
    "direction",
    "method_type",
    "duration",
    "equipment",
    "zones",
    "effects",
    "problems",
    "description",
    "procedure_about",
    "advantages",
    "indications",
    "principle",
    "how_it_goes",
    "for_whom",
    "problems_solved",
    "contraindications_full",
    "preparation",
    "recommended_course",
    "rehabilitation",
    "post_care",
    "side_effects",
]

PROCEDURE_DICTIONARY_FIELDS = {
    "procedure_zones": "zones",
}


def serialize_field(value: Any) -> Any:
    if value is None:
        return None
    if isinstance(value, list):
        return json.dumps(value, ensure_ascii=False)
    return value


def build_procedure_records(payload: Dict[str, Any]) -> List[Dict[str, Any]]:
    field_order = payload.get("field_order")
    rows = payload.get("procedures")
    expected_count = (payload.get("metadata") or {}).get("count")
    if not isinstance(field_order, list) or not isinstance(rows, list):
        raise ValueError("payload must contain field_order and procedures lists")
    if expected_count is not None and len(rows) != int(expected_count):
        raise ValueError(f"procedure count mismatch: expected {expected_count}, got {len(rows)}")

    records: List[Dict[str, Any]] = []
    for index, row in enumerate(rows, start=1):
        if not isinstance(row, list) or len(row) != len(field_order):
            raise ValueError(f"row {index} field count mismatch")
        record = dict(zip(field_order, row))
        if not str(record.get("name") or "").strip():
            raise ValueError(f"row {index} has empty name")
        records.append(record)
    return records


def load_review_records(path: Path = REVIEW_JSON_PATH) -> List[Dict[str, Any]]:
    payload = json.loads(path.read_text(encoding="utf-8"))
    return build_procedure_records(payload)


def build_procedure_dictionary_values(records: List[Dict[str, Any]]) -> Dict[str, List[str]]:
    dictionaries: Dict[str, List[str]] = {}
    for table_name, field_name in PROCEDURE_DICTIONARY_FIELDS.items():
        values = set()
        for record in records:
            field_value = record.get(field_name)
            if isinstance(field_value, list):
                values.update(str(value).strip() for value in field_value if str(value).strip())
            elif field_value:
                values.add(str(field_value).strip())
        dictionaries[table_name] = sorted(values)
    return dictionaries


async def import_records(records: List[Dict[str, Any]]) -> Dict[str, int]:
    conn = await asyncpg.connect(
        host=os.getenv("DB_HOST", "localhost"),
        port=int(os.getenv("DB_PORT", "5433")),
        database=os.getenv("DB_NAME", "aura"),
        user=os.getenv("DB_USER", "aura_user"),
        password=os.getenv("DB_PASSWORD", "aura_password"),
    )
    inserted = 0
    updated = 0
    try:
        async with conn.transaction():
            for record in records:
                existing_id = await conn.fetchval("SELECT id FROM procedures WHERE name=$1", record["name"])
                values = [serialize_field(record.get(column)) for column in INSERT_COLUMNS]
                if existing_id:
                    assignments = ", ".join(
                        f"{column}=${position}"
                        for position, column in enumerate(INSERT_COLUMNS, start=1)
                    )
                    await conn.execute(
                        f"UPDATE procedures SET {assignments} WHERE id=${len(INSERT_COLUMNS) + 1}",
                        *values,
                        existing_id,
                    )
                    row = await conn.fetchrow("SELECT * FROM procedures WHERE id=$1", existing_id)
                    updated += 1
                else:
                    columns_sql = ", ".join(INSERT_COLUMNS)
                    placeholders = ", ".join(f"${index}" for index in range(1, len(INSERT_COLUMNS) + 1))
                    row = await conn.fetchrow(
                        f"INSERT INTO procedures ({columns_sql}) VALUES ({placeholders}) RETURNING *",
                        *values,
                    )
                    inserted += 1
                await sync_procedure_dictionary_refs(conn, row)
            for table_name, values in build_procedure_dictionary_values(records).items():
                for value in values:
                    await conn.execute(
                        f"INSERT INTO {table_name} (value) VALUES ($1) ON CONFLICT (value) DO NOTHING",
                        value,
                    )
    finally:
        await conn.close()
    return {"inserted": inserted, "updated": updated}


async def main() -> None:
    records = load_review_records()
    result = await import_records(records)
    print(f"Imported procedures: inserted={result['inserted']} updated={result['updated']} total={len(records)}")


if __name__ == "__main__":
    asyncio.run(main())
