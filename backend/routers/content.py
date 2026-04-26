from fastapi import APIRouter, Depends, HTTPException

from backend.core.entity_dictionary_refs import content_select_sql, sync_content_category_ref, sync_content_tag_refs
from backend.db.pool import get_db
from backend.schemas.content import ContentCreate


router = APIRouter(prefix="/api/content", tags=["Content"])


@router.get("")
async def get_content(db=Depends(get_db)):
    async with db.acquire() as conn:
        rows = await conn.fetch(f"SELECT * FROM ({content_select_sql('c')}) AS hydrated_content ORDER BY id DESC")
        return [dict(row) for row in rows]


@router.post("")
async def create_content(content: ContentCreate, db=Depends(get_db)):
    async with db.acquire() as conn:
        row = await conn.fetchrow(
            """INSERT INTO content (title, author_id, author_name, body, image_url, published)
               VALUES ($1, $2, $3, $4, $5, $6) RETURNING *""",
            content.title,
            content.author_id,
            content.author_name,
            content.body,
            content.image_url,
            content.published,
        )
        await sync_content_category_ref(conn, {"id": row["id"], "category": content.category})
        await sync_content_tag_refs(conn, {"id": row["id"], "tags": content.tags})
        row = await conn.fetchrow(f"SELECT * FROM ({content_select_sql('c')}) AS hydrated_content WHERE id=$1", row["id"])
        return dict(row)


@router.put("/{content_id}")
async def update_content(content_id: int, content: ContentCreate, db=Depends(get_db)):
    def pick(next_value, current_value):
        return current_value if next_value is None else next_value

    async with db.acquire() as conn:
        existing = await conn.fetchrow(f"SELECT * FROM ({content_select_sql('c')}) AS hydrated_content WHERE id=$1", content_id)
        if not existing:
            raise HTTPException(status_code=404, detail="Content not found")
        row = await conn.fetchrow(
            """UPDATE content SET title=$1, author_id=$2, author_name=$3,
               body=$4, image_url=$5, published=$6 WHERE id=$7 RETURNING *""",
            pick(content.title, existing["title"]),
            pick(content.author_id, existing["author_id"]),
            pick(content.author_name, existing["author_name"]),
            pick(content.body, existing["body"]),
            pick(content.image_url, existing["image_url"]),
            content.published if content.published is not None else existing["published"],
            content_id,
        )
        await sync_content_category_ref(
            conn,
            {
                "id": row["id"],
                "category": content.category if content.category is not None else existing["category"],
            },
        )
        await sync_content_tag_refs(
            conn,
            {
                "id": row["id"],
                "tags": content.tags if content.tags is not None else existing["tags"],
            },
        )
        row = await conn.fetchrow(f"SELECT * FROM ({content_select_sql('c')}) AS hydrated_content WHERE id=$1", row["id"])
        return dict(row)


@router.delete("/{content_id}")
async def delete_content(content_id: int, db=Depends(get_db)):
    async with db.acquire() as conn:
        await conn.execute("DELETE FROM content WHERE id=$1", content_id)
        return {"success": True}
