from typing import List, Optional
from app.database import get_pool
from app.models.product import Product, ProductCreate


DICT_TABLE_MAP = {
    "brands": "brands",
    "categories": "categories",
    "segments": "segments",
    "volumes": "volumes",
    "procedureCategories": "procedure_categories",
    "contentCategories": "content_categories",
    "userRoles": "user_roles",
    "skinTypes": "skin_types"
}


class ProductService:
    @staticmethod
    async def get_all() -> List[dict]:
        async with get_pool().acquire() as conn:
            rows = await conn.fetch("SELECT * FROM products ORDER BY id DESC")
            return [dict(row) for row in rows]

    @staticmethod
    async def create(data: ProductCreate) -> dict:
        async with get_pool().acquire() as conn:
            row = await conn.fetchrow(
                """INSERT INTO products (name, brand, category, description, images, volume, segment) 
                   VALUES ($1, $2, $3, $4, $5, $6, $7) RETURNING *""",
                data.name, data.brand, data.category, data.description,
                data.images, data.volume, data.segment
            )
            return dict(row)

    @staticmethod
    async def update(product_id: int, data: ProductCreate) -> Optional[dict]:
        async with get_pool().acquire() as conn:
            row = await conn.fetchrow(
                """UPDATE products SET name=$1, brand=$2, category=$3, description=$4, images=$5, volume=$6, segment=$7 
                   WHERE id=$8 RETURNING *""",
                data.name, data.brand, data.category, data.description,
                data.images, data.volume, data.segment, product_id
            )
            return dict(row) if row else None

    @staticmethod
    async def delete(product_id: int) -> None:
        async with get_pool().acquire() as conn:
            await conn.execute("DELETE FROM products WHERE id=$1", product_id)


class DictionaryService:
    @staticmethod
    def get_table(key: str) -> Optional[str]:
        return DICT_TABLE_MAP.get(key)

    @staticmethod
    async def get_values(key: str) -> List[str]:
        table = DictionaryService.get_table(key)
        if not table:
            return []
        async with get_pool().acquire() as conn:
            rows = await conn.fetch(f"SELECT value FROM {table} ORDER BY id")
            return [row["value"] for row in rows]

    @staticmethod
    async def create_value(key: str, value: str) -> dict:
        table = DictionaryService.get_table(key)
        if not table:
            raise ValueError(f"Unknown dictionary key: {key}")
        async with get_pool().acquire() as conn:
            row = await conn.fetchrow(
                f"INSERT INTO {table} (value) VALUES ($1) RETURNING *",
                value
            )
            return dict(row)

    @staticmethod
    async def update_value(key: str, old_value: str, new_value: str) -> dict:
        table = DictionaryService.get_table(key)
        if not table:
            raise ValueError(f"Unknown dictionary key: {key}")
        async with get_pool().acquire() as conn:
            row = await conn.fetchrow(
                f"UPDATE {table} SET value=$1 WHERE value=$2 RETURNING *",
                new_value, old_value
            )
            return dict(row) if row else {}

    @staticmethod
    async def delete_value(key: str, value: str) -> None:
        table = DictionaryService.get_table(key)
        if not table:
            raise ValueError(f"Unknown dictionary key: {key}")
        async with get_pool().acquire() as conn:
            await conn.execute(f"DELETE FROM {table} WHERE value=$1", value)
