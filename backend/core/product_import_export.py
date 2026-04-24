import csv
import io
import json

from fastapi import HTTPException
from fastapi.responses import Response

from backend.core.product_admin import (
    build_product_create_from_csv_row,
    insert_product_row,
    parse_product_import_csv,
    sync_product_ingredients,
)
from backend.core.products import normalize_product_response, product_select_sql


PRODUCT_EXPORT_FIELDNAMES = [
    "name", "what_is_it", "brand", "product_type", "for_whom",
    "purpose", "skin_type", "application_time", "area",
    "active_ingredient", "volume", "segment", "composition",
    "application_info", "country", "country_origin", "manufacturer", "description",
]


async def import_products_from_csv_bytes(conn, contents: bytes):
    rows = parse_product_import_csv(contents)
    created = 0
    errors = []

    for index, row in enumerate(rows, start=2):
        try:
            product_data = build_product_create_from_csv_row(row)
            async with conn.transaction():
                inserted_row = await insert_product_row(conn, product_data)
                await sync_product_ingredients(conn, inserted_row["id"], product_data.composition)
            created += 1
        except Exception as exc:
            errors.append(f"Row {index}: {str(exc)}")

    return {"success": True, "created": created, "errors": errors if errors else None}


async def export_products_to_csv_response(conn):
    rows = await conn.fetch(f"SELECT * FROM ({product_select_sql('p')}) AS hydrated_products ORDER BY id DESC")
    products = [normalize_product_response(row) for row in rows]

    output = io.StringIO()
    writer = csv.DictWriter(output, fieldnames=PRODUCT_EXPORT_FIELDNAMES, extrasaction="ignore")
    writer.writeheader()

    for product in products:
        writer.writerow(build_product_export_row(product))

    csv_bytes = output.getvalue().encode("utf-8-sig")
    return Response(
        content=csv_bytes,
        media_type="text/csv; charset=utf-8",
        headers={"Content-Disposition": "attachment; filename=products.csv"},
    )


def ensure_csv_filename(filename: str):
    if not filename.lower().endswith(".csv"):
        raise HTTPException(status_code=400, detail="Only CSV files allowed")


def build_product_export_row(product):
    row = {}
    for field in PRODUCT_EXPORT_FIELDNAMES:
        row[field] = serialize_product_export_value(product.get(field))
    return row


def serialize_product_export_value(value):
    if isinstance(value, list):
        return "|".join(str(item) for item in value if str(item).strip())
    if isinstance(value, str):
        raw = value.strip()
        if raw.startswith("[") and raw.endswith("]"):
            try:
                decoded = json.loads(raw)
            except Exception:
                return raw
            if isinstance(decoded, list):
                return "|".join(str(item) for item in decoded if str(item).strip())
        return raw
    return value or ""
