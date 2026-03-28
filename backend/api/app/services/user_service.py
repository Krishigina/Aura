from typing import List, Optional
from app.database import get_pool
from app.models.user import User, UserCreate


class UserService:
    @staticmethod
    async def get_all() -> List[dict]:
        async with get_pool().acquire() as conn:
            rows = await conn.fetch("SELECT * FROM users ORDER BY id DESC")
            return [dict(row) for row in rows]

    @staticmethod
    async def create(data: UserCreate) -> dict:
        async with get_pool().acquire() as conn:
            row = await conn.fetchrow(
                """INSERT INTO users (name, email, role, avatar) 
                   VALUES ($1, $2, $3, $4) RETURNING *""",
                data.name, data.email, data.role, data.avatar
            )
            return dict(row)

    @staticmethod
    async def update(user_id: int, data: UserCreate) -> Optional[dict]:
        async with get_pool().acquire() as conn:
            row = await conn.fetchrow(
                """UPDATE users SET name=$1, email=$2, role=$3, avatar=$4 
                   WHERE id=$5 RETURNING *""",
                data.name, data.email, data.role, data.avatar, user_id
            )
            return dict(row) if row else None

    @staticmethod
    async def delete(user_id: int) -> None:
        async with get_pool().acquire() as conn:
            await conn.execute("DELETE FROM users WHERE id=$1", user_id)
