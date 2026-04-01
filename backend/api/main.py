import os
import re
import json
import asyncio
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional, Union
import asyncpg
import aiohttp
import uuid
from bs4 import BeautifulSoup

if os.name == 'nt':
    asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())

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
    import asyncio
    
    for attempt in range(10):
        try:
            pool = await asyncpg.create_pool(
                host=os.getenv("DB_HOST", "localhost"),
                port=int(os.getenv("DB_PORT", "5432")),
                database=os.getenv("DB_NAME", "beauty_db"),
                user=os.getenv("DB_USER", "aura_user"),
                password=os.getenv("DB_PASSWORD", "aura_password"),
                command_timeout=60,
                min_size=1,
                max_size=3,
                server_settings={'application_name': 'aura_standalone'},
                timeout=30
            )
            async with pool.acquire() as conn:
                await conn.fetch('SELECT 1')
            print("Pool created successfully!")
            break
        except Exception as e:
            print(f"Attempt {attempt+1} failed: {e}")
            if attempt < 9:
                await asyncio.sleep(2)
            else:
                raise e
    
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
                video VARCHAR(255),
                has_video BOOLEAN DEFAULT false,
                created_at TIMESTAMP DEFAULT NOW()
            );

            CREATE TABLE IF NOT EXISTS product_photos (
                id SERIAL PRIMARY KEY,
                product_id INTEGER REFERENCES products(id) ON DELETE CASCADE,
                filename VARCHAR(255) NOT NULL,
                created_at TIMESTAMP DEFAULT NOW()
            );

            CREATE TABLE IF NOT EXISTS procedures (
                id SERIAL PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                direction VARCHAR(255),
                method_type VARCHAR(255),
                duration VARCHAR(255),
                equipment VARCHAR(255),
                zones TEXT,
                effects TEXT,
                problems TEXT,
                description TEXT,
                procedure_about TEXT,
                advantages TEXT,
                indications TEXT,
                principle TEXT,
                how_it_goes TEXT,
                for_whom TEXT,
                problems_solved TEXT,
                contraindications TEXT,
                preparation TEXT,
                recommended_course TEXT,
                rehabilitation TEXT,
                post_care TEXT,
                side_effects TEXT,
                created_at TIMESTAMP DEFAULT NOW()
            );

            CREATE TABLE IF NOT EXISTS procedure_photos (
                id SERIAL PRIMARY KEY,
                procedure_id INTEGER REFERENCES procedures(id) ON DELETE CASCADE,
                filename VARCHAR(255) NOT NULL,
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
                value VARCHAR(255) NOT NULL UNIQUE,
                description TEXT,
                country VARCHAR(100),
                country_origin VARCHAR(100),
                manufacturer VARCHAR(255)
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

            CREATE TABLE IF NOT EXISTS product_types (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) NOT NULL UNIQUE
            );

            CREATE TABLE IF NOT EXISTS for_whom (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) NOT NULL UNIQUE
            );

            CREATE TABLE IF NOT EXISTS purposes (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) NOT NULL UNIQUE
            );

            CREATE TABLE IF NOT EXISTS application_times (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) NOT NULL UNIQUE
            );

            CREATE TABLE IF NOT EXISTS areas (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) NOT NULL UNIQUE
            );

            CREATE TABLE IF NOT EXISTS countries (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) NOT NULL UNIQUE
            );

            CREATE TABLE IF NOT EXISTS procedure_method_types (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) NOT NULL UNIQUE
            );

            CREATE TABLE IF NOT EXISTS procedure_durations (
                id SERIAL PRIMARY KEY,
                value VARCHAR(100) NOT NULL UNIQUE
            );

            CREATE TABLE IF NOT EXISTS procedure_equipment (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) NOT NULL UNIQUE
            );

            CREATE TABLE IF NOT EXISTS procedure_zones (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) NOT NULL UNIQUE
            );

            CREATE TABLE IF NOT EXISTS procedure_effects (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) NOT NULL UNIQUE
            );

            CREATE TABLE IF NOT EXISTS procedure_problems (
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

        # Reset procedure categories to new directions
        await conn.execute("DELETE FROM procedure_categories")
        default_procedure_categories = ['Аппаратная косметология', 'Инъекционная косметология', 'Эстетическая косметология']
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

        product_types_count = await conn.fetchval("SELECT COUNT(*) FROM product_types")
        if product_types_count == 0:
            default_product_types = ['Крем', 'Сыворотка', 'Лосьон', 'Тоник', 'Маска', 'Масло', 'Спрей', 'Гель', 'Эмульсия', 'Бальзам']
            for value in default_product_types:
                await conn.execute("INSERT INTO product_types (value) VALUES ($1)", value)

        for_whom_count = await conn.fetchval("SELECT COUNT(*) FROM for_whom")
        if for_whom_count == 0:
            default_for_whom = ['Универсальный', 'Мужчинам', 'Женщинам', 'Детям', 'Беременным']
            for value in default_for_whom:
                await conn.execute("INSERT INTO for_whom (value) VALUES ($1)", value)

        purposes_count = await conn.fetchval("SELECT COUNT(*) FROM purposes")
        if purposes_count == 0:
            default_purposes = ['Увлажнение', 'Питание', 'Очищение', 'Омоложение', 'Отбеливание', 'Защита от солнца', 'Против акне', 'Восстановление']
            for value in default_purposes:
                await conn.execute("INSERT INTO purposes (value) VALUES ($1)", value)

        application_times_count = await conn.fetchval("SELECT COUNT(*) FROM application_times")
        if application_times_count == 0:
            default_application_times = ['Утро', 'Вечер', 'Утро и вечер', 'По необходимости']
            for value in default_application_times:
                await conn.execute("INSERT INTO application_times (value) VALUES ($1)", value)

        areas_count = await conn.fetchval("SELECT COUNT(*) FROM areas")
        if areas_count == 0:
            default_areas = ['Лицо', 'Тело', 'Волосы', 'Губы', 'Глаза', 'Шея', 'Руки', 'Ноги']
            for value in default_areas:
                await conn.execute("INSERT INTO areas (value) VALUES ($1)", value)

        countries_count = await conn.fetchval("SELECT COUNT(*) FROM countries")
        if countries_count == 0:
            default_countries = ['Франция', 'Корея', 'Япония', 'США', 'Германия', 'Швейцария', 'Россия', 'Италия', 'Испания', 'Израиль']
            for value in default_countries:
                await conn.execute("INSERT INTO countries (value) VALUES ($1)", value)

        print("Database initialized successfully")


DICT_TABLE_MAP = {
    "brands": "brands",
    "categories": "categories",
    "segments": "segments",
    "volumes": "volumes",
    "procedureCategories": "procedure_categories",
    "contentCategories": "content_categories",
    "userRoles": "user_roles",
    "skin_types": "skin_types",
    "product_types": "product_types",
    "for_whom": "for_whom",
    "purposes": "purposes",
    "application_times": "application_times",
    "areas": "areas",
    "countries": "countries",
    "methodTypes": "procedure_method_types",
    "procedureDurations": "procedure_durations",
    "procedureEquipment": "procedure_equipment",
    "procedureZones": "procedure_zones",
    "procedureEffects": "procedure_effects",
    "procedureProblems": "procedure_problems",
}


class ProductCreate(BaseModel):
    name: str
    what_is_it: Optional[str] = None
    brand: Optional[str] = None
    product_type: Optional[str] = None
    for_whom: Optional[str] = None
    purpose: Optional[Union[List[str], str]] = None
    skin_type: Optional[Union[List[str], str]] = None
    application_time: Optional[str] = None
    area: Optional[str] = None
    active_ingredient: Optional[str] = None
    volume: Optional[str] = None
    segment: Optional[str] = None
    composition: Optional[str] = None
    application_info: Optional[str] = None
    country: Optional[str] = None
    category: Optional[str] = None
    description: Optional[str] = None
    images: Optional[List[str]] = None
    has_video: Optional[bool] = False


class ProcedureCreate(BaseModel):
    name: str
    direction: Optional[str] = None
    method_type: Optional[str] = None
    duration: Optional[Union[str, int]] = None
    equipment: Optional[str] = None
    zones: Optional[Union[List[str], str]] = None
    effects: Optional[Union[List[str], str]] = None
    problems: Optional[Union[List[str], str]] = None
    description: Optional[str] = None
    procedure_about: Optional[str] = None
    advantages: Optional[str] = None
    indications: Optional[str] = None
    principle: Optional[str] = None
    how_it_goes: Optional[str] = None
    for_whom: Optional[str] = None
    problems_solved: Optional[str] = None
    contraindications_full: Optional[str] = None
    preparation: Optional[str] = None
    recommended_course: Optional[str] = None
    rehabilitation: Optional[str] = None
    post_care: Optional[str] = None
    side_effects: Optional[str] = None


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


@app.get("/api/products/{product_id:int}")
async def get_product(product_id: int):
    async with pool.acquire() as conn:
        row = await conn.fetchrow("SELECT * FROM products WHERE id=$1", product_id)
        if not row:
            raise HTTPException(status_code=404, detail="Product not found")
        return dict(row)


@app.put("/api/products/{product_id:int}")
async def update_product(product_id: int, product: ProductCreate):
    async with pool.acquire() as conn:
        existing = await conn.fetchrow("SELECT * FROM products WHERE id=$1", product_id)
        if not existing:
            raise HTTPException(status_code=404, detail="Product not found")
        
        def serialize_field(field):
            if field is None:
                return None
            if isinstance(field, list):
                return json.dumps(field, ensure_ascii=False)
            return field
        
        row = await conn.fetchrow(
            """UPDATE products SET 
               name=$1, what_is_it=$2, brand=$3, product_type=$4, for_whom=$5,
               purpose=$6, skin_type=$7, application_time=$8, area=$9,
               active_ingredient=$10, volume=$11, segment=$12, composition=$13,
               application_info=$14, country=$15, description=$16, has_video=$17
               WHERE id=$18 RETURNING *""",
            product.name,
            product.what_is_it or existing['what_is_it'],
            product.brand or existing['brand'],
            product.product_type or existing['product_type'],
            product.for_whom or existing['for_whom'],
            serialize_field(product.purpose) if product.purpose is not None else existing['purpose'],
            serialize_field(product.skin_type) if product.skin_type is not None else existing['skin_type'],
            product.application_time or existing['application_time'],
            product.area or existing['area'],
            product.active_ingredient or existing['active_ingredient'],
            product.volume or existing['volume'],
            product.segment or existing['segment'],
            product.composition or existing['composition'],
            product.application_info or existing['application_info'],
            product.country or existing['country'],
            product.description or existing['description'],
            product.has_video if product.has_video is not None else existing['has_video'],
            product_id
        )
        return dict(row)


@app.delete("/api/products/{product_id:int}")
async def delete_product(product_id: int):
    async with pool.acquire() as conn:
        await conn.execute("DELETE FROM products WHERE id=$1", product_id)
        return {"success": True}


@app.post("/api/products/{product_id:int}/photos")
async def upload_product_photo(product_id: int, file: UploadFile = File(...)):
    product = await get_product_by_id(product_id)
    if not product:
        raise HTTPException(status_code=404, detail="Product not found")
    
    base_dir = os.path.dirname(os.path.abspath(__file__))
    uploads_dir = os.path.join(base_dir, "product_photos")
    os.makedirs(uploads_dir, exist_ok=True)
    
    file_ext = file.filename.split('.')[-1] if '.' in file.filename else 'jpg'
    filename = f"{product_id}_{uuid.uuid4()}.{file_ext}"
    file_path = os.path.join(uploads_dir, filename)
    
    content = await file.read()
    with open(file_path, 'wb') as f:
        f.write(content)
    
    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            "INSERT INTO product_photos (product_id, filename) VALUES ($1, $2) RETURNING *",
            product_id, filename
        )
        return {"id": row["id"], "filename": row["filename"]}


@app.delete("/api/products/{product_id:int}/photos/{photo_id:int}")
async def delete_product_photo(product_id: int, photo_id: int):
    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            "SELECT filename FROM product_photos WHERE id=$1 AND product_id=$2",
            photo_id, product_id
        )
        if row:
            base_dir = os.path.dirname(os.path.abspath(__file__))
            file_path = os.path.join(base_dir, "product_photos", row["filename"])
            if os.path.exists(file_path):
                os.remove(file_path)
            await conn.execute("DELETE FROM product_photos WHERE id=$1", photo_id)
    return {"success": True}


@app.post("/api/products/{product_id}/video")
async def upload_product_video(product_id: int, file: UploadFile = File(...)):
    async with pool.acquire() as conn:
        product = await conn.fetchrow("SELECT id FROM products WHERE id=$1", product_id)
        if not product:
            raise HTTPException(status_code=404, detail="Product not found")
    
    base_dir = os.path.dirname(os.path.abspath(__file__))
    uploads_dir = os.path.join(base_dir, "product_videos")
    os.makedirs(uploads_dir, exist_ok=True)
    
    file_ext = file.filename.split('.')[-1] if '.' in file.filename else 'mp4'
    filename = f"{product_id}_{uuid.uuid4()}.{file_ext}"
    file_path = os.path.join(uploads_dir, filename)
    
    content = await file.read()
    with open(file_path, 'wb') as f:
        f.write(content)
    
    async with pool.acquire() as conn:
        await conn.execute("UPDATE products SET video=$1, has_video=true WHERE id=$2", filename, product_id)
    return {"success": True, "filename": filename}


@app.delete("/api/products/{product_id}/video")
async def delete_product_video(product_id: int):
    print(f"DEBUG: delete_product_video called with product_id={product_id}")
    async with pool.acquire() as conn:
        row = await conn.fetchrow("SELECT video FROM products WHERE id=$1", product_id)
        if row and row["video"]:
            base_dir = os.path.dirname(os.path.abspath(__file__))
            file_path = os.path.join(base_dir, "product_videos", row["video"])
            if os.path.exists(file_path):
                os.remove(file_path)
            await conn.execute("UPDATE products SET video=NULL, has_video=false WHERE id=$1", product_id)
    return {"success": True}


@app.get("/api/products/{product_id}/video")
async def get_product_video(product_id: int):
    print(f"DEBUG: get_product_video called with product_id={product_id}")
    async with pool.acquire() as conn:
        row = await conn.fetchrow("SELECT video FROM products WHERE id=$1", product_id)
        if not row or not row["video"]:
            return None
        base_dir = os.path.dirname(os.path.abspath(__file__))
        file_path = os.path.join(base_dir, "product_videos", row["video"])
        if os.path.exists(file_path):
            with open(file_path, 'rb') as f:
                import base64
                data = base64.b64encode(f.read()).decode()
                return {"filename": row["video"], "data": data}
        return None


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
    def serialize_field(field):
        if field is None:
            return None
        if isinstance(field, list):
            return json.dumps(field, ensure_ascii=False)
        return field
    
    def parse_duration(duration):
        if duration is None:
            return None
        if isinstance(duration, int):
            return duration
        try:
            return int(duration)
        except (ValueError, TypeError):
            return None
    
    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            """INSERT INTO procedures (name, direction, method_type, duration, equipment, 
               zones, effects, problems, description, procedure_about, advantages, 
               indications, principle, how_it_goes, for_whom, problems_solved, 
               contraindications_full, preparation, recommended_course, rehabilitation, 
               post_care, side_effects, photos) 
               VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18, $19, $20, $21, $22, $23) 
               RETURNING *""",
            procedure.name, procedure.direction, procedure.method_type, parse_duration(procedure.duration),
            procedure.equipment, serialize_field(procedure.zones), serialize_field(procedure.effects),
            serialize_field(procedure.problems), procedure.description, procedure.procedure_about,
            procedure.advantages, procedure.indications, procedure.principle, procedure.how_it_goes,
            procedure.for_whom, procedure.problems_solved, procedure.contraindications_full,
            procedure.preparation, procedure.recommended_course, procedure.rehabilitation,
            procedure.post_care, procedure.side_effects, None
        )
        return dict(row)


@app.put("/api/procedures/{procedure_id:int}")
async def update_procedure(procedure_id: int, procedure: ProcedureCreate):
    def serialize_field(field):
        if field is None:
            return None
        if isinstance(field, list):
            return json.dumps(field, ensure_ascii=False)
        return field
    
    def parse_duration(duration):
        if duration is None:
            return None
        if isinstance(duration, int):
            return duration
        try:
            return int(duration)
        except (ValueError, TypeError):
            return None
    
    async with pool.acquire() as conn:
        existing = await conn.fetchrow("SELECT * FROM procedures WHERE id=$1", procedure_id)
        if not existing:
            raise HTTPException(status_code=404, detail="Procedure not found")
        
        row = await conn.fetchrow(
            """UPDATE procedures SET 
               name=$1, direction=$2, method_type=$3, duration=$4, equipment=$5,
               zones=$6, effects=$7, problems=$8, description=$9, procedure_about=$10,
               advantages=$11, indications=$12, principle=$13, how_it_goes=$14,
               for_whom=$15, problems_solved=$16, contraindications_full=$17, preparation=$18,
               recommended_course=$19, rehabilitation=$20, post_care=$21, side_effects=$22
               WHERE id=$23 RETURNING *""",
            procedure.name,
            procedure.direction or existing['direction'],
            procedure.method_type or existing['method_type'],
            parse_duration(procedure.duration) if procedure.duration is not None else existing['duration'],
            procedure.equipment or existing['equipment'],
            serialize_field(procedure.zones) if procedure.zones is not None else existing['zones'],
            serialize_field(procedure.effects) if procedure.effects is not None else existing['effects'],
            serialize_field(procedure.problems) if procedure.problems is not None else existing['problems'],
            procedure.description or existing['description'],
            procedure.procedure_about or existing['procedure_about'],
            procedure.advantages or existing['advantages'],
            procedure.indications or existing['indications'],
            procedure.principle or existing['principle'],
            procedure.how_it_goes or existing['how_it_goes'],
            procedure.for_whom or existing['for_whom'],
            procedure.problems_solved or existing['problems_solved'],
            procedure.contraindications_full or existing['contraindications_full'],
            procedure.preparation or existing['preparation'],
            procedure.recommended_course or existing['recommended_course'],
            procedure.rehabilitation or existing['rehabilitation'],
            procedure.post_care or existing['post_care'],
            procedure.side_effects or existing['side_effects'],
            procedure_id
        )
        return dict(row)


@app.delete("/api/procedures/{procedure_id:int}")
async def delete_procedure(procedure_id: int):
    async with pool.acquire() as conn:
        await conn.execute("DELETE FROM procedures WHERE id=$1", procedure_id)
        return {"success": True}


@app.post("/api/procedures/{procedure_id:int}/photos")
async def upload_procedure_photo(procedure_id: int, file: UploadFile = File(...)):
    async with pool.acquire() as conn:
        procedure = await conn.fetchrow("SELECT id FROM procedures WHERE id=$1", procedure_id)
        if not procedure:
            raise HTTPException(status_code=404, detail="Procedure not found")
    
    base_dir = os.path.dirname(os.path.abspath(__file__))
    uploads_dir = os.path.join(base_dir, "procedure_photos")
    os.makedirs(uploads_dir, exist_ok=True)
    
    file_ext = file.filename.split('.')[-1] if '.' in file.filename else 'jpg'
    filename = f"{procedure_id}_{uuid.uuid4()}.{file_ext}"
    file_path = os.path.join(uploads_dir, filename)
    
    content = await file.read()
    with open(file_path, 'wb') as f:
        f.write(content)
    
    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            "INSERT INTO procedure_photos (procedure_id, filename) VALUES ($1, $2) RETURNING *",
            procedure_id, filename
        )
        return {"id": row["id"], "filename": row["filename"]}


@app.delete("/api/procedures/{procedure_id:int}/photos/{photo_id:int}")
async def delete_procedure_photo(procedure_id: int, photo_id: int):
    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            "SELECT filename FROM procedure_photos WHERE id=$1 AND procedure_id=$2",
            photo_id, procedure_id
        )
        if row:
            base_dir = os.path.dirname(os.path.abspath(__file__))
            file_path = os.path.join(base_dir, "procedure_photos", row["filename"])
            if os.path.exists(file_path):
                os.remove(file_path)
            await conn.execute("DELETE FROM procedure_photos WHERE id=$1", photo_id)
    return {"success": True}


@app.get("/api/procedures/{procedure_id:int}/photos")
async def get_procedure_photos(procedure_id: int):
    async with pool.acquire() as conn:
        rows = await conn.fetch(
            "SELECT id, filename FROM procedure_photos WHERE procedure_id=$1 ORDER BY id",
            procedure_id
        )
        base_dir = os.path.dirname(os.path.abspath(__file__))
        photos = []
        for row in rows:
            file_path = os.path.join(base_dir, "procedure_photos", row["filename"])
            if os.path.exists(file_path):
                with open(file_path, 'rb') as f:
                    import base64
                    data = base64.b64encode(f.read()).decode()
                    ext = row["filename"].split('.')[-1]
                    content_type = f"image/{ext}" if ext in ['jpg', 'jpeg', 'png', 'gif', 'webp'] else "image/jpeg"
                    photos.append({
                        "id": row["id"],
                        "filename": row["filename"],
                        "data": data,
                        "content_type": content_type
                    })
        return photos


@app.get("/api/procedures/dictionaries/method-types")
async def get_method_types():
    async with pool.acquire() as conn:
        rows = await conn.fetch("SELECT id, value FROM procedure_method_types ORDER BY id")
        return [{"id": row["id"], "value": row["value"]} for row in rows]


@app.get("/api/procedures/dictionaries/durations")
async def get_durations():
    async with pool.acquire() as conn:
        rows = await conn.fetch("SELECT id, value FROM procedure_durations ORDER BY id")
        return [{"id": row["id"], "value": row["value"]} for row in rows]


@app.get("/api/procedures/dictionaries/equipment")
async def get_equipment():
    async with pool.acquire() as conn:
        rows = await conn.fetch("SELECT id, value FROM procedure_equipment ORDER BY id")
        return [{"id": row["id"], "value": row["value"]} for row in rows]


@app.get("/api/procedures/dictionaries/zones")
async def get_zones():
    async with pool.acquire() as conn:
        rows = await conn.fetch("SELECT id, value FROM procedure_zones ORDER BY id")
        return [{"id": row["id"], "value": row["value"]} for row in rows]


@app.get("/api/procedures/dictionaries/effects")
async def get_effects():
    async with pool.acquire() as conn:
        rows = await conn.fetch("SELECT id, value FROM procedure_effects ORDER BY id")
        return [{"id": row["id"], "value": row["value"]} for row in rows]


@app.get("/api/procedures/dictionaries/problems")
async def get_problems():
    async with pool.acquire() as conn:
        rows = await conn.fetch("SELECT id, value FROM procedure_problems ORDER BY id")
        return [{"id": row["id"], "value": row["value"]} for row in rows]


@app.post("/api/procedures/dictionaries/{dict_type}")
async def add_procedure_dict_value(dict_type: str, value: str):
    table_map = {
        "methodTypes": "procedure_method_types",
        "procedureDurations": "procedure_durations",
        "procedureEquipment": "procedure_equipment",
        "procedureZones": "procedure_zones",
        "procedureEffects": "procedure_effects",
        "procedureProblems": "procedure_problems",
    }
    table_name = table_map.get(dict_type)
    if not table_name:
        raise HTTPException(status_code=400, detail="Invalid dictionary type")
    async with pool.acquire() as conn:
        row = await conn.fetchrow(f"INSERT INTO {table_name} (value) VALUES ($1) RETURNING *", value)
        return dict(row)


@app.delete("/api/procedures/dictionaries/{dict_type}/{value}")
async def delete_procedure_dict_value(dict_type: str, value: str):
    table_map = {
        "methodTypes": "procedure_method_types",
        "procedureDurations": "procedure_durations",
        "procedureEquipment": "procedure_equipment",
        "procedureZones": "procedure_zones",
        "procedureEffects": "procedure_effects",
        "procedureProblems": "procedure_problems",
    }
    table_name = table_map.get(dict_type)
    if not table_name:
        raise HTTPException(status_code=400, detail="Invalid dictionary type")
    async with pool.acquire() as conn:
        await conn.execute(f"DELETE FROM {table_name} WHERE value=$1", value)
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


class ProductParseRequest(BaseModel):
    url: str


class ProductParseResponse(BaseModel):
    name: Optional[str] = None
    brand: Optional[str] = None
    category: Optional[str] = None
    description: Optional[str] = None
    images: Optional[List[str]] = None
    volume: Optional[str] = None


@app.post("/api/products/parse")
async def parse_product_url(request: ProductParseRequest):
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
    
    try:
        async with aiohttp.ClientSession() as session:
            async with session.get(request.url, headers=headers, timeout=aiohttp.ClientTimeout(total=15)) as response:
                if response.status != 200:
                    raise HTTPException(status_code=400, detail=f"Failed to fetch URL: status {response.status}")
                html = await response.text()
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Error fetching URL: {str(e)}")
    
    soup = BeautifulSoup(html, 'html.parser')
    
    result = {
        "name": None,
        "brand": None,
        "category": None,
        "description": None,
        "images": [],
        "volume": None
    }
    
    og_title = soup.find("meta", property="og:title")
    if og_title:
        result["name"] = og_title.get("content", "").strip()
    
    if not result["name"]:
        title_tag = soup.find("title")
        if title_tag:
            result["name"] = title_tag.string.strip() if title_tag.string else None
    
    h1_tag = soup.find("h1")
    if h1_tag and h1_tag.string and not result["name"]:
        result["name"] = h1_tag.string.strip()
    
    brand_meta = soup.find("meta", property="product:brand")
    if brand_meta:
        result["brand"] = brand_meta.get("content", "").strip()
    
    if not result["brand"]:
        brand_patterns = [
            (r'(?:Brand|Marque|Marca):\s*([^<\n]+)', 1),
            (r'"brand"\s*:\s*"([^"]+)"', 1),
            (r'<span[^>]*class="[^"]*brand[^"]*"[^>]*>([^<]+)</span>', 1),
            (r'"name"\s*:\s*"([^"]+)"', 1),
        ]
        for pattern, group in brand_patterns:
            match = re.search(pattern, html, re.IGNORECASE)
            if match:
                result["brand"] = match.group(group).strip()
                break
    
    known_brands = ['La Roche-Posay', 'Vichy', 'Bioderma', 'CeraVe', 'The Ordinary', 
                   "Paula's Choice", 'Cosrx', 'Eucerin', 'Nivea', 'Aura', 'A-Derma',
                   'Uriage', 'Filorga', 'Nuxe', 'Darphin', 'Clarins', 'Estee Lauder',
                   'Lancome', 'Shiseido', 'Clinique', 'Origins', 'Decathlon']
    if not result["brand"]:
        for brand in known_brands:
            if brand.lower() in html.lower():
                result["brand"] = brand
                break
    
    desc_meta = soup.find("meta", property="og:description")
    if desc_meta:
        result["description"] = desc_meta.get("content", "").strip()
    
    if not result["description"]:
        desc_tag = soup.find("meta", attrs={"name": "description"})
        if desc_tag:
            result["description"] = desc_tag.get("content", "").strip()
    
    if not result["description"]:
        desc_div = soup.find("div", class_=re.compile(r'description', re.I))
        if desc_div:
            result["description"] = desc_div.get_text(strip=True)[:500]
    
    img_tags = soup.find_all("img")
    for img in img_tags[:5]:
        src = img.get("src") or img.get("data-src") or img.get("data-lazy") or img.get("data-srcset", "").split()[0]
        if src and src.startswith("http"):
            if src not in result["images"]:
                result["images"].append(src)
    
    og_image = soup.find("meta", property="og:image")
    if og_image:
        img_url = og_image.get("content", "")
        if img_url and img_url not in result["images"]:
            result["images"].insert(0, img_url)
    
    json_ld = soup.find("script", type="application/ld+json")
    if json_ld and json_ld.string:
        try:
            import json
            data = json.loads(json_ld.string)
            if isinstance(data, dict):
                if not result["name"] and data.get("name"):
                    result["name"] = data["name"]
                if not result["brand"] and data.get("brand"):
                    brand_val = data["brand"]
                    result["brand"] = brand_val if isinstance(brand_val, str) else brand_val.get("name")
                if not result["description"] and data.get("description"):
                    result["description"] = data["description"][:500] if len(data.get("description", "")) > 500 else data.get("description")
                if not result["images"] and data.get("image"):
                    img_data = data["image"]
                    if isinstance(img_data, list):
                        result["images"] = [i for i in img_data if isinstance(i, str)][:5]
                    elif isinstance(img_data, str):
                        result["images"] = [img_data]
                    elif isinstance(img_data, dict) and img_data.get("url"):
                        result["images"] = [img_data["url"]]
                if not result["volume"] and data.get("offers"):
                    offers = data["offers"]
                    if isinstance(offers, dict) and offers.get("sku"):
                        sku = offers.get("sku", "")
                        vol_match = re.search(r'(\d+\s*ml|\d+\s*мл|\d+\s*г|\d+\s*ml)', sku, re.I)
                        if vol_match:
                            result["volume"] = vol_match.group(1)
        except:
            pass
    
    volume_patterns = [
        r'(\d+\s*ml)',
        r'(\d+\s*мл)',
        r'(\d+\s*г)',
        r'(\d+\s*ml)',
        r'(\d+\s*l)',
    ]
    for pattern in volume_patterns:
        if not result["volume"]:
            match = re.search(pattern, html, re.IGNORECASE)
            if match:
                result["volume"] = match.group(1).lower()
    
    text_content = soup.get_text()
    category_keywords = {
        'Очищение': ['очищающий', 'гель для умывания', 'мицеллярная вода', 'пенка', 'mousse', 'cleanser', 'wash'],
        'Увлажнение': ['увлажняющий', 'крем для лица', 'hydration', 'moisturizer', 'cream'],
        'Сыворотка': ['сыворотка', 'serum', 'эссенция', 'essence'],
        'SPF': ['spf', 'sun protection', 'защита от солнца', 'sunscreen', 'uv'],
        'Маска': ['маска для лица', 'mask', 'sheet mask'],
        'Тоник': ['тоник', 'toner'],
        'Масло': ['масло для лица', 'oil', 'huile'],
    }
    
    for cat, keywords in category_keywords.items():
        for kw in keywords:
            if kw.lower() in text_content.lower():
                result["category"] = cat
                break
        if result["category"]:
            break
    
    if not result["category"]:
        category_tag = soup.find("span", class_=re.compile(r'category|cat', re.I))
        if category_tag:
            cat_text = category_tag.get_text(strip=True)
            for cat, keywords in category_keywords.items():
                for kw in keywords:
                    if kw.lower() in cat_text.lower():
                        result["category"] = cat
                        break
                if result["category"]:
                    break
    
    return result


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=3001)
