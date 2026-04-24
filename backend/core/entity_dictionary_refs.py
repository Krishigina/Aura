import json
from typing import Any, Optional


def user_select_sql(alias: str = "u") -> str:
    return f"""
        SELECT
            {alias}.id,
            {alias}.name,
            {alias}.email,
            role_dict.value AS role,
            {alias}.role_id,
            {alias}.nickname,
            {alias}.avatar,
            {alias}.password_hash,
            {alias}.created_at
        FROM users {alias}
        LEFT JOIN user_roles role_dict ON role_dict.id = {alias}.role_id
    """


def content_select_sql(alias: str = "c") -> str:
    return f"""
        SELECT
            {alias}.id,
            {alias}.title,
            category_dict.value AS category,
            {alias}.category_id,
            COALESCE(
                ARRAY(
                    SELECT tag_dict.value
                    FROM content_tag_links content_tag_link
                    JOIN content_tags tag_dict ON tag_dict.id = content_tag_link.tag_id
                    WHERE content_tag_link.content_id = {alias}.id
                    ORDER BY tag_dict.value
                ),
                ARRAY[]::VARCHAR[]
            ) AS tags,
            {alias}.author_id,
            {alias}.author_name,
            {alias}.body,
            {alias}.image_url,
            {alias}.published,
            {alias}.created_at
        FROM content {alias}
        LEFT JOIN content_categories category_dict ON category_dict.id = {alias}.category_id
    """


def normalize_dictionary_value(value: Any) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
    return text or None


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


def parse_product_reference_id(value: Any) -> Optional[int]:
    if value is None:
        return None
    if isinstance(value, int):
        return value if value > 0 else None
    text = str(value).strip()
    if not text:
        return None
    if text.isdigit():
        product_id = int(text)
        return product_id if product_id > 0 else None
    prefix = "product-"
    if text.lower().startswith(prefix):
        suffix = text[len(prefix):].strip()
        if suffix.isdigit():
            product_id = int(suffix)
            return product_id if product_id > 0 else None
    return None


async def sync_user_role_ref(conn, row) -> None:
    role_id = await ensure_dictionary_value(conn, "user_roles", row.get("role"))
    await conn.execute("UPDATE users SET role_id=$1 WHERE id=$2", role_id, row["id"])


async def sync_content_category_ref(conn, row) -> None:
    category_id = await ensure_dictionary_value(conn, "content_categories", row.get("category"))
    await conn.execute("UPDATE content SET category_id=$1 WHERE id=$2", category_id, row["id"])


async def sync_content_tag_refs(conn, row) -> None:
    content_id = row["id"]
    values = parse_dictionary_list(row.get("tags"))
    await conn.execute("DELETE FROM content_tag_links WHERE content_id = $1", content_id)
    for value in values:
        tag_id = await ensure_dictionary_value(conn, "content_tags", value)
        if tag_id is None:
            continue
        await conn.execute(
            """
            INSERT INTO content_tag_links (content_id, tag_id)
            VALUES ($1, $2)
            ON CONFLICT (content_id, tag_id) DO NOTHING
            """,
            content_id,
            tag_id,
        )


async def sync_recommendation_feedback_product_ref(conn, row) -> None:
    product_ref_id = parse_product_reference_id(row.get("product_id"))
    if product_ref_id is not None:
        exists = await conn.fetchval("SELECT EXISTS(SELECT 1 FROM products WHERE id=$1)", product_ref_id)
        if not exists:
            product_ref_id = None
    await conn.execute("UPDATE recommendation_feedback SET product_ref_id=$1 WHERE id=$2", product_ref_id, row["id"])


async def backfill_user_role_refs(conn) -> None:
    existing_columns = {
        row["column_name"]
        for row in await conn.fetch(
            "SELECT column_name FROM information_schema.columns WHERE table_name='users'"
        )
    }
    if "role" not in existing_columns:
        return
    rows = await conn.fetch("SELECT id, role FROM users ORDER BY id")
    for row in rows:
        await sync_user_role_ref(conn, row)


async def backfill_content_category_refs(conn) -> None:
    existing_columns = {
        row["column_name"]
        for row in await conn.fetch(
            "SELECT column_name FROM information_schema.columns WHERE table_name='content'"
        )
    }
    if "category" not in existing_columns:
        return
    rows = await conn.fetch("SELECT id, category FROM content ORDER BY id")
    for row in rows:
        await sync_content_category_ref(conn, row)


async def backfill_content_tag_refs(conn) -> None:
    existing_columns = {
        row["column_name"]
        for row in await conn.fetch(
            "SELECT column_name FROM information_schema.columns WHERE table_name='content'"
        )
    }
    if "tags" not in existing_columns:
        return
    rows = await conn.fetch("SELECT id, tags FROM content ORDER BY id")
    for row in rows:
        await sync_content_tag_refs(conn, row)


async def backfill_recommendation_feedback_product_refs(conn) -> None:
    rows = await conn.fetch("SELECT id, product_id FROM recommendation_feedback ORDER BY id")
    for row in rows:
        await sync_recommendation_feedback_product_ref(conn, row)
