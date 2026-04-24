import csv
import io
import json
from typing import Any, Dict

from fastapi import HTTPException

from backend.core.ingredient_knowledge_admin import rebuild_product_function_profile
from backend.core.matching.domain import parse_inci_ingredients
from backend.core.product_dictionary_refs import sync_product_dictionary_refs
from backend.core.products import product_select_sql
from backend.schemas.products import ProductCreate


def serialize_product_field(field: Any):
    if field is None:
        return None
    if isinstance(field, list):
        return json.dumps(field, ensure_ascii=False)
    return field


def build_product_create_from_csv_row(row: Dict[str, Any]) -> ProductCreate:
    purpose = [p.strip() for p in (row.get("purpose") or "").split("|") if p.strip()]
    skin_type = [s.strip() for s in (row.get("skin_type") or "").split("|") if s.strip()]

    product_data = ProductCreate(
        name=(row.get("name") or "").strip(),
        what_is_it=(row.get("what_is_it") or "").strip() or None,
        brand=(row.get("brand") or "").strip() or None,
        product_type=(row.get("product_type") or "").strip() or None,
        for_whom=(row.get("for_whom") or "").strip() or None,
        purpose=purpose or None,
        skin_type=skin_type or None,
        application_time=(row.get("application_time") or "").strip() or None,
        area=(row.get("area") or "").strip() or None,
        active_ingredient=(row.get("active_ingredient") or "").strip() or None,
        volume=(row.get("volume") or "").strip() or None,
        segment=(row.get("segment") or "").strip() or None,
        composition=(row.get("composition") or "").strip() or None,
        application_info=(row.get("application_info") or "").strip() or None,
        country=(row.get("country") or "").strip() or None,
        category=(row.get("category") or "").strip() or None,
        description=(row.get("description") or "").strip() or None,
        images=[],
        has_video=False,
    )
    if not product_data.name:
        raise ValueError("name is required")
    return product_data


def parse_product_import_csv(contents: bytes) -> list[dict[str, Any]]:
    try:
        text = contents.decode("utf-8-sig")
    except UnicodeDecodeError as error:
        raise HTTPException(status_code=400, detail="CSV must be UTF-8 encoded") from error
    return list(csv.DictReader(io.StringIO(text)))


async def sync_product_ingredients(conn, product_id: int, composition: str | None):
    parsed = parse_inci_ingredients(composition or "")
    await conn.execute("DELETE FROM product_ingredients WHERE product_id=$1", product_id)

    for item in parsed:
        ingredient = await conn.fetchrow(
            "SELECT ingredient_id AS id FROM ingredient_aliases WHERE normalized_key=$1",
            item["normalized_key"],
        )
        if not ingredient:
            ingredient = await conn.fetchrow(
                """
                INSERT INTO ingredients (canonical_name, normalized_key, inci_name, verification_status)
                VALUES ($1, $2, $1, 'auto_created')
                ON CONFLICT (normalized_key)
                DO UPDATE SET updated_at=NOW()
                RETURNING id
                """,
                item["raw_name"],
                item["normalized_key"],
            )
        await conn.execute(
            """
            INSERT INTO product_ingredients (product_id, ingredient_id, raw_name, position, confidence)
            VALUES ($1, $2, $3, $4, 1.0)
            ON CONFLICT (product_id, ingredient_id, position)
            DO UPDATE SET raw_name=$3, confidence=1.0
            """,
            product_id,
            ingredient["id"],
            item["raw_name"],
            item["position"],
        )

    await rebuild_product_function_profile(conn, product_id)


async def insert_product_row(conn, product: ProductCreate):
    row = await conn.fetchrow(
        """INSERT INTO products (
           name, what_is_it, active_ingredient, composition,
           application_info, description, images, has_video
        )
           VALUES (
           $1, $2, $3, $4,
           $5, $6, $7, $8
        ) RETURNING *""",
        product.name,
        product.what_is_it,
        product.active_ingredient,
        product.composition,
        product.application_info,
        product.description,
        product.images,
        product.has_video if product.has_video is not None else False,
    )
    await sync_product_dictionary_refs(conn, {
        "id": row["id"],
        "brand": product.brand,
        "category": product.category,
        "segment": product.segment,
        "volume": product.volume,
        "product_type": product.product_type,
        "for_whom": product.for_whom,
        "application_time": product.application_time,
        "area": product.area,
        "country": product.country,
        "purpose": product.purpose,
        "skin_type": product.skin_type,
    })
    return await conn.fetchrow(f"SELECT * FROM ({product_select_sql('p')}) AS hydrated_products WHERE id=$1", row["id"])


async def update_product_row(conn, product_id: int, product: ProductCreate):
    existing = await conn.fetchrow(f"SELECT * FROM ({product_select_sql('p')}) AS hydrated_products WHERE id=$1", product_id)
    if not existing:
        raise HTTPException(status_code=404, detail="Product not found")

    await conn.fetchrow(
        """UPDATE products SET
           name=$1, what_is_it=$2, active_ingredient=$3, composition=$4,
           application_info=$5, description=$6, has_video=$7
           WHERE id=$8 RETURNING *""",
        product.name,
        product.what_is_it or existing["what_is_it"],
        product.active_ingredient or existing["active_ingredient"],
        product.composition or existing["composition"],
        product.application_info or existing["application_info"],
        product.description or existing["description"],
        product.has_video if product.has_video is not None else existing["has_video"],
        product_id,
    )
    await sync_product_dictionary_refs(conn, {
        "id": product_id,
        "brand": product.brand if product.brand is not None else existing["brand"],
        "category": product.category if product.category is not None else existing["category"],
        "segment": product.segment if product.segment is not None else existing["segment"],
        "volume": product.volume if product.volume is not None else existing["volume"],
        "product_type": product.product_type if product.product_type is not None else existing["product_type"],
        "for_whom": product.for_whom if product.for_whom is not None else existing["for_whom"],
        "application_time": product.application_time if product.application_time is not None else existing["application_time"],
        "area": product.area if product.area is not None else existing["area"],
        "country": product.country if product.country is not None else existing["country"],
        "purpose": product.purpose if product.purpose is not None else existing["purpose"],
        "skin_type": product.skin_type if product.skin_type is not None else existing["skin_type"],
    })
    return await conn.fetchrow(f"SELECT * FROM ({product_select_sql('p')}) AS hydrated_products WHERE id=$1", product_id)
