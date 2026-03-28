from typing import List, Optional
from app.database import get_pool
from app.models.procedure import Procedure, ProcedureCreate


class ProcedureService:
    @staticmethod
    async def get_all() -> List[dict]:
        async with get_pool().acquire() as conn:
            rows = await conn.fetch("SELECT * FROM procedures ORDER BY id DESC")
            return [dict(row) for row in rows]

    @staticmethod
    async def create(data: ProcedureCreate) -> dict:
        async with get_pool().acquire() as conn:
            row = await conn.fetchrow(
                """INSERT INTO procedures (name, category, duration, price, description, contraindications) 
                   VALUES ($1, $2, $3, $4, $5, $6) RETURNING *""",
                data.name, data.category, data.duration, data.price,
                data.description, data.contraindications
            )
            return dict(row)

    @staticmethod
    async def update(procedure_id: int, data: ProcedureCreate) -> Optional[dict]:
        async with get_pool().acquire() as conn:
            row = await conn.fetchrow(
                """UPDATE procedures SET name=$1, category=$2, duration=$3, price=$4, description=$5, contraindications=$6 
                   WHERE id=$7 RETURNING *""",
                data.name, data.category, data.duration, data.price,
                data.description, data.contraindications, procedure_id
            )
            return dict(row) if row else None

    @staticmethod
    async def delete(procedure_id: int) -> None:
        async with get_pool().acquire() as conn:
            await conn.execute("DELETE FROM procedures WHERE id=$1", procedure_id)
