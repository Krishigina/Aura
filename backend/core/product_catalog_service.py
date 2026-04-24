from fastapi import HTTPException


async def list_catalog_products(conn, *, product_select_sql, normalize_product_response):
    rows = await conn.fetch(
        f"""
        SELECT hydrated_products.*, first_photo.id AS thumbnail_photo_id
        FROM ({product_select_sql('p')}) AS hydrated_products
        LEFT JOIN LATERAL (
            SELECT id
            FROM product_photos
            WHERE product_id = hydrated_products.id
            ORDER BY id
            LIMIT 1
        ) first_photo ON TRUE
        ORDER BY hydrated_products.id DESC
        """
    )
    products = []
    for row in rows:
        product = normalize_product_response(row)
        if row["thumbnail_photo_id"]:
            product["thumbnail_url"] = f"/api/products/{row['id']}/photos/{row['thumbnail_photo_id']}"
        products.append(product)
    return products


async def get_catalog_product_row(conn, *, product_id: int, product_select_sql):
    row = await conn.fetchrow(
        f"SELECT * FROM ({product_select_sql('p')}) AS hydrated_products WHERE id=$1",
        product_id,
    )
    if not row:
        raise HTTPException(status_code=404, detail="Product not found")
    return row


async def search_catalog_products(conn, *, query: str, limit: int, product_select_sql):
    if query:
        rows = await conn.fetch(
            f"""
            SELECT
                id,
                COALESCE(brand, '')::text AS brand,
                COALESCE(name, '')::text AS name
            FROM ({product_select_sql('p')}) AS hydrated_products
            WHERE
                COALESCE(brand, '') ILIKE '%' || $1 || '%'
                OR COALESCE(name, '') ILIKE '%' || $1 || '%'
            ORDER BY id DESC
            LIMIT $2
            """,
            query,
            limit,
        )
    else:
        rows = await conn.fetch(
            f"""
            SELECT
                id,
                COALESCE(brand, '')::text AS brand,
                COALESCE(name, '')::text AS name
            FROM ({product_select_sql('p')}) AS hydrated_products
            ORDER BY id DESC
            LIMIT $1
            """,
            limit,
        )
    return [dict(row) for row in rows]


async def delete_catalog_product(conn, *, product_id: int):
    await conn.execute("DELETE FROM products WHERE id=$1", product_id)
    return {"success": True}
