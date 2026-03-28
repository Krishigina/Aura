import os
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional
import asyncpg

DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "postgresql://aura_user:aura_password@localhost:5432/aura"
)

pool: asyncpg.Pool = None


async def get_db():
    return pool


@asynccontextmanager
async def lifespan(app: FastAPI):
    global pool
    pool = await asyncpg.create_pool(
        host=os.getenv("DB_HOST", "localhost"),
        port=int(os.getenv("DB_PORT", "5432")),
        database=os.getenv("DB_NAME", "aura"),
        user=os.getenv("DB_USER", "aura_user"),
        password=os.getenv("DB_PASSWORD", "aura_password"),
        command_timeout=60
    )
    await init_db()
    yield
    await pool.close()


app = FastAPI(title="Aura Admin API", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


async def init_db():
    async with pool.acquire() as conn:
        await conn.execute("""
            CREATE TABLE IF NOT EXISTS products (
                id SERIAL PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                brand VARCHAR(255),
                category VARCHAR(255),
                description TEXT,
                images TEXT[],
                volume VARCHAR(50),
                segment VARCHAR(50),
                created_at TIMESTAMP DEFAULT NOW()
            );

            CREATE TABLE IF NOT EXISTS procedures (
                id SERIAL PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                category VARCHAR(255),
                duration INTEGER,
                price DECIMAL(10,2),
                description TEXT,
                contraindications TEXT,
                created_at TIMESTAMP DEFAULT NOW()
            );

            CREATE TABLE IF NOT EXISTS content (
                id SERIAL PRIMARY KEY,
                title VARCHAR(255) NOT NULL,
                type VARCHAR(50),
                body TEXT,
                image_url VARCHAR(500),
                published BOOLEAN DEFAULT false,
                created_at TIMESTAMP DEFAULT NOW()
            );

            CREATE TABLE IF NOT EXISTS users (
                id SERIAL PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                email VARCHAR(255) UNIQUE NOT NULL,
                role VARCHAR(50) DEFAULT 'user',
                avatar VARCHAR(500),
                created_at TIMESTAMP DEFAULT NOW()
            );

            CREATE TABLE IF NOT EXISTS brands (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) NOT NULL UNIQUE
            );

            CREATE TABLE IF NOT EXISTS categories (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) NOT NULL UNIQUE
            );

            CREATE TABLE IF NOT EXISTS segments (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) NOT NULL UNIQUE
            );

            CREATE TABLE IF NOT EXISTS volumes (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) NOT NULL UNIQUE
            );

            CREATE TABLE IF NOT EXISTS procedure_categories (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) NOT NULL UNIQUE
            );

            CREATE TABLE IF NOT EXISTS content_categories (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) NOT NULL UNIQUE
            );

            CREATE TABLE IF NOT EXISTS user_roles (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) NOT NULL UNIQUE
            );

            CREATE TABLE IF NOT EXISTS skin_types (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) NOT NULL UNIQUE
            );
        """)

        brands_count = await conn.fetchval("SELECT COUNT(*) FROM brands")
        if brands_count == 0:
            default_brands = ['Aura', 'La Roche-Posay', 'Vichy', 'Bioderma', 'CeraVe', 'The Ordinary', "Paula's Choice", 'Cosrx', 'Eucerin', 'Nivea']
            for value in default_brands:
                await conn.execute("INSERT INTO brands (value) VALUES ($1)", value)

        categories_count = await conn.fetchval("SELECT COUNT(*) FROM categories")
        if categories_count == 0:
            default_categories = ['Очищение', 'Увлажнение', 'Сыворотки', 'SPF', 'Уход', 'Маска', 'Тоник', 'Крем', 'Масло']
            for value in default_categories:
                await conn.execute("INSERT INTO categories (value) VALUES ($1)", value)

        segments_count = await conn.fetchval("SELECT COUNT(*) FROM segments")
        if segments_count == 0:
            default_segments = ['Бюджетная', 'Люкс', 'Профессиональная', 'Космецевтика']
            for value in default_segments:
                await conn.execute("INSERT INTO segments (value) VALUES ($1)", value)

        volumes_count = await conn.fetchval("SELECT COUNT(*) FROM volumes")
        if volumes_count == 0:
            default_volumes = ['15мл', '30мл', '50мл', '75мл', '100мл', '150мл', '200мл', '250мл', '500мл', '1л']
            for value in default_volumes:
                await conn.execute("INSERT INTO volumes (value) VALUES ($1)", value)

        procedure_categories_count = await conn.fetchval("SELECT COUNT(*) FROM procedure_categories")
        if procedure_categories_count == 0:
            default_procedure_categories = ['Чистка', 'Увлажнение', 'Инъекции', 'Эпиляция', 'Массаж', 'Пилинг', 'Уход']
            for value in default_procedure_categories:
                await conn.execute("INSERT INTO procedure_categories (value) VALUES ($1)", value)

        content_categories_count = await conn.fetchval("SELECT COUNT(*) FROM content_categories")
        if content_categories_count == 0:
            default_content_categories = ['Уход за кожей', 'Ингредиенты', 'Защита', 'Процедуры', 'Питание', 'Образ жизни']
            for value in default_content_categories:
                await conn.execute("INSERT INTO content_categories (value) VALUES ($1)", value)

        user_roles_count = await conn.fetchval("SELECT COUNT(*) FROM user_roles")
        if user_roles_count == 0:
            default_user_roles = ['Пользователь', 'Косметолог', 'Менеджер', 'Администратор']
            for value in default_user_roles:
                await conn.execute("INSERT INTO user_roles (value) VALUES ($1)", value)

        skin_types_count = await conn.fetchval("SELECT COUNT(*) FROM skin_types")
        if skin_types_count == 0:
            default_skin_types = ['Нормальная', 'Сухая', 'Жирная', 'Комбинированная', 'Чувствительная']
            for value in default_skin_types:
                await conn.execute("INSERT INTO skin_types (value) VALUES ($1)", value)

        print("Database initialized successfully")


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


class ProductCreate(BaseModel):
    name: str
    brand: Optional[str] = None
    category: Optional[str] = None
    description: Optional[str] = None
    images: Optional[List[str]] = None
    volume: Optional[str] = None
    segment: Optional[str] = None


class ProcedureCreate(BaseModel):
    name: str
    category: Optional[str] = None
    duration: Optional[int] = None
    price: Optional[float] = None
    description: Optional[str] = None
    contraindications: Optional[str] = None


class ContentCreate(BaseModel):
    title: str
    type: Optional[str] = None
    body: Optional[str] = None
    image_url: Optional[str] = None
    published: Optional[bool] = False


class UserCreate(BaseModel):
    name: str
    email: str
    role: Optional[str] = "user"
    avatar: Optional[str] = None


class DictionaryValue(BaseModel):
    value: str


class DictionaryUpdate(BaseModel):
    oldValue: str
    newValue: str


@app.get("/api/health")
async def health_check():
    return {"status": "ok"}


@app.get("/api/products")
async def get_products():
    async with pool.acquire() as conn:
        rows = await conn.fetch("SELECT * FROM products ORDER BY id DESC")
        return [dict(row) for row in rows]


@app.post("/api/products")
async def create_product(product: ProductCreate):
    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            """INSERT INTO products (name, brand, category, description, images, volume, segment) 
               VALUES ($1, $2, $3, $4, $5, $6, $7) RETURNING *""",
            product.name, product.brand, product.category, product.description,
            product.images, product.volume, product.segment
        )
        return dict(row)


@app.put("/api/products/{product_id}")
async def update_product(product_id: int, product: ProductCreate):
    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            """UPDATE products SET name=$1, brand=$2, category=$3, description=$4, images=$5, volume=$6, segment=$7 
               WHERE id=$8 RETURNING *""",
            product.name, product.brand, product.category, product.description,
            product.images, product.volume, product.segment, product_id
        )
        if not row:
            raise HTTPException(status_code=404, detail="Product not found")
        return dict(row)


@app.delete("/api/products/{product_id}")
async def delete_product(product_id: int):
    async with pool.acquire() as conn:
        await conn.execute("DELETE FROM products WHERE id=$1", product_id)
        return {"success": True}


@app.get("/api/dictionaries/{key}")
async def get_dictionary(key: str):
    table = DICT_TABLE_MAP.get(key)
    if not table:
        raise HTTPException(status_code=400, detail="Unknown dictionary key")
    async with pool.acquire() as conn:
        rows = await conn.fetch(f"SELECT value FROM {table} ORDER BY id")
        return [row["value"] for row in rows]


@app.post("/api/dictionaries/{key}")
async def create_dictionary_value(key: str, data: DictionaryValue):
    table = DICT_TABLE_MAP.get(key)
    if not table:
        raise HTTPException(status_code=400, detail="Unknown dictionary key")
    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            f"INSERT INTO {table} (value) VALUES ($1) RETURNING *",
            data.value
        )
        return dict(row)


@app.put("/api/dictionaries/{key}")
async def update_dictionary_value(key: str, data: DictionaryUpdate):
    table = DICT_TABLE_MAP.get(key)
    if not table:
        raise HTTPException(status_code=400, detail="Unknown dictionary key")
    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            f"UPDATE {table} SET value=$1 WHERE value=$2 RETURNING *",
            data.newValue, data.oldValue
        )
        return dict(row)


@app.delete("/api/dictionaries/{key}/{value}")
async def delete_dictionary_value(key: str, value: str):
    table = DICT_TABLE_MAP.get(key)
    if not table:
        raise HTTPException(status_code=400, detail="Unknown dictionary key")
    async with pool.acquire() as conn:
        await conn.execute(f"DELETE FROM {table} WHERE value=$1", value)
        return {"success": True}


@app.get("/api/procedures")
async def get_procedures():
    async with pool.acquire() as conn:
        rows = await conn.fetch("SELECT * FROM procedures ORDER BY id DESC")
        return [dict(row) for row in rows]


@app.post("/api/procedures")
async def create_procedure(procedure: ProcedureCreate):
    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            """INSERT INTO procedures (name, category, duration, price, description, contraindications) 
               VALUES ($1, $2, $3, $4, $5, $6) RETURNING *""",
            procedure.name, procedure.category, procedure.duration, procedure.price,
            procedure.description, procedure.contraindications
        )
        return dict(row)


@app.put("/api/procedures/{procedure_id}")
async def update_procedure(procedure_id: int, procedure: ProcedureCreate):
    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            """UPDATE procedures SET name=$1, category=$2, duration=$3, price=$4, description=$5, contraindications=$6 
               WHERE id=$7 RETURNING *""",
            procedure.name, procedure.category, procedure.duration, procedure.price,
            procedure.description, procedure.contraindications, procedure_id
        )
        if not row:
            raise HTTPException(status_code=404, detail="Procedure not found")
        return dict(row)


@app.delete("/api/procedures/{procedure_id}")
async def delete_procedure(procedure_id: int):
    async with pool.acquire() as conn:
        await conn.execute("DELETE FROM procedures WHERE id=$1", procedure_id)
        return {"success": True}


@app.get("/api/content")
async def get_content():
    async with pool.acquire() as conn:
        rows = await conn.fetch("SELECT * FROM content ORDER BY id DESC")
        return [dict(row) for row in rows]


@app.post("/api/content")
async def create_content(content: ContentCreate):
    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            """INSERT INTO content (title, type, body, image_url, published) 
               VALUES ($1, $2, $3, $4, $5) RETURNING *""",
            content.title, content.type, content.body, content.image_url, content.published
        )
        return dict(row)


@app.put("/api/content/{content_id}")
async def update_content(content_id: int, content: ContentCreate):
    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            """UPDATE content SET title=$1, type=$2, body=$3, image_url=$4, published=$5 
               WHERE id=$6 RETURNING *""",
            content.title, content.type, content.body, content.image_url, content.published, content_id
        )
        if not row:
            raise HTTPException(status_code=404, detail="Content not found")
        return dict(row)


@app.delete("/api/content/{content_id}")
async def delete_content(content_id: int):
    async with pool.acquire() as conn:
        await conn.execute("DELETE FROM content WHERE id=$1", content_id)
        return {"success": True}


@app.get("/api/users")
async def get_users():
    async with pool.acquire() as conn:
        rows = await conn.fetch("SELECT * FROM users ORDER BY id DESC")
        return [dict(row) for row in rows]


@app.post("/api/users")
async def create_user(user: UserCreate):
    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            """INSERT INTO users (name, email, role, avatar) 
               VALUES ($1, $2, $3, $4) RETURNING *""",
            user.name, user.email, user.role, user.avatar
        )
        return dict(row)


@app.put("/api/users/{user_id}")
async def update_user(user_id: int, user: UserCreate):
    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            """UPDATE users SET name=$1, email=$2, role=$3, avatar=$4 
               WHERE id=$5 RETURNING *""",
            user.name, user.email, user.role, user.avatar, user_id
        )
        if not row:
            raise HTTPException(status_code=404, detail="User not found")
        return dict(row)


@app.delete("/api/users/{user_id}")
async def delete_user(user_id: int):
    async with pool.acquire() as conn:
        await conn.execute("DELETE FROM users WHERE id=$1", user_id)
        return {"success": True}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=3001)
