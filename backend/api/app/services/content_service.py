from typing import List, Optional
from app.database import get_pool
from app.models.content import Content, ContentCreate


class ContentService:
    @staticmethod
    async def get_all() -> List[dict]:
        async with get_pool().acquire() as conn:
            rows = await conn.fetch("SELECT * FROM content ORDER BY id DESC")
            return [dict(row) for row in rows]

    @staticmethod
    async def create(data: ContentCreate) -> dict:
        async with get_pool().acquire() as conn:
            row = await conn.fetchrow(
                """INSERT INTO content (title, type, body, image_url, published) 
                   VALUES ($1, $2, $3, $4, $5) RETURNING *""",
                data.title, data.type, data.body, data.image_url, data.published
            )
            return dict(row)

    @staticmethod
    async def update(content_id: int, data: ContentCreate) -> Optional[dict]:
        async with get_pool().acquire() as conn:
            row = await conn.fetchrow(
                """UPDATE content SET title=$1, type=$2, body=$3, image_url=$4, published=$5 
                   WHERE id=$6 RETURNING *""",
                data.title, data.type, data.body, data.image_url, data.published, content_id
            )
            return dict(row) if row else None

    @staticmethod
    async def delete(content_id: int) -> None:
        async with get_pool().acquire() as conn:
            await conn.execute("DELETE FROM content WHERE id=$1", content_id)
