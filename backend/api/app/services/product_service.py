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
                """INSERT INTO products (
                    name, what_is_it, brand, product_type, for_whom, purpose,
                    skin_type, application_time, area, active_ingredient,
                    volume, segment, composition, application_info,
                    country, manufacturer, description, photos, has_video
                ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18, $19) RETURNING *""",
                data.name, data.what_is_it, data.brand, data.product_type, data.for_whom,
                data.purpose, data.skin_type, data.application_time, data.area,
                data.active_ingredient, data.volume, data.segment, data.composition,
                data.application_info, data.country, data.manufacturer,
                data.description, data.photos, data.has_video
            )
            return dict(row)

    @staticmethod
    async def update(product_id: int, data: ProductCreate) -> Optional[dict]:
        async with get_pool().acquire() as conn:
            row = await conn.fetchrow(
                """UPDATE products SET 
                    name=$1, what_is_it=$2, brand=$3, product_type=$4, for_whom=$5,
                    purpose=$6, skin_type=$7, application_time=$8, area=$9,
                    active_ingredient=$10, volume=$11, segment=$12, composition=$13,
                    application_info=$14, country=$15, manufacturer=$16,
                    description=$17, photos=$18, has_video=$19
                WHERE id=$20 RETURNING *""",
                data.name, data.what_is_it, data.brand, data.product_type, data.for_whom,
                data.purpose, data.skin_type, data.application_time, data.area,
                data.active_ingredient, data.volume, data.segment, data.composition,
                data.application_info, data.country, data.manufacturer,
                data.description, data.photos, data.has_video, product_id
            )
            return dict(row) if row else None

    @staticmethod
    async def get_by_id(product_id: int) -> Optional[dict]:
        async with get_pool().acquire() as conn:
            row = await conn.fetchrow("SELECT * FROM products WHERE id=$1", product_id)
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
