import os
import io
import re
import json
import asyncio
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException, UploadFile, File, Depends, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import Response
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel
from typing import List, Optional, Union, Dict, Any
import asyncpg
import aiohttp
import json
import uuid
from bs4 import BeautifulSoup
from datetime import datetime, timedelta
from passlib.context import CryptContext

# JWT imports
try:
    from jose import JWTError, jwt
except ImportError:
    jwt = None
    JWTError = Exception

if os.name == 'nt':
    asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())

# JWT Configuration
JWT_SECRET = os.getenv("JWT_SECRET", "aura-super-secret-key-change-in-production")
JWT_ALGORITHM = "HS256"
JWT_EXPIRATION_HOURS = 24

# Password hashing
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

# Security scheme
security = HTTPBearer()

DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "postgresql://aura_user:aura_password@localhost:5432/aura"
)

pool: asyncpg.Pool = None
auth_logger = logging.getLogger("aura.auth")


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
                port=int(os.getenv("DB_PORT", "5433")),
                database=os.getenv("DB_NAME", "aura"),
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
        # Create users FIRST (referenced by other tables)
        await conn.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id SERIAL PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                email VARCHAR(255) UNIQUE NOT NULL,
                role VARCHAR(50) DEFAULT 'user',
                nickname VARCHAR(255),
                avatar VARCHAR(500),
                password_hash VARCHAR(255),
                created_at TIMESTAMP DEFAULT NOW()
            );

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

            -- Add missing columns if table already exists (idempotent)
            ALTER TABLE products ADD COLUMN IF NOT EXISTS what_is_it VARCHAR(255);
            ALTER TABLE products ADD COLUMN IF NOT EXISTS product_type VARCHAR(100);
            ALTER TABLE products ADD COLUMN IF NOT EXISTS for_whom VARCHAR(100);
            ALTER TABLE products ADD COLUMN IF NOT EXISTS purpose TEXT;
            ALTER TABLE products ADD COLUMN IF NOT EXISTS skin_type TEXT;
            ALTER TABLE products ADD COLUMN IF NOT EXISTS application_time VARCHAR(100);
            ALTER TABLE products ADD COLUMN IF NOT EXISTS area VARCHAR(100);
            ALTER TABLE products ADD COLUMN IF NOT EXISTS active_ingredient TEXT;
            ALTER TABLE products ADD COLUMN IF NOT EXISTS composition TEXT;
            ALTER TABLE products ADD COLUMN IF NOT EXISTS application_info TEXT;
            ALTER TABLE products ADD COLUMN IF NOT EXISTS country VARCHAR(100);

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

            -- Add missing columns if table already exists (idempotent)
            ALTER TABLE procedures ADD COLUMN IF NOT EXISTS contraindications_full TEXT;
            -- Copy data from contraindications if contraindications_full is empty
            UPDATE procedures SET contraindications_full = contraindications WHERE contraindications_full IS NULL AND contraindications IS NOT NULL;

            CREATE TABLE IF NOT EXISTS procedure_photos (
                id SERIAL PRIMARY KEY,
                procedure_id INTEGER REFERENCES procedures(id) ON DELETE CASCADE,
                filename VARCHAR(255) NOT NULL,
                created_at TIMESTAMP DEFAULT NOW()
            );

            CREATE TABLE IF NOT EXISTS content (
                id SERIAL PRIMARY KEY,
                title VARCHAR(255) NOT NULL,
                category VARCHAR(255),
                tags TEXT,
                author_id INTEGER REFERENCES users(id),
                author_name VARCHAR(255),
                body TEXT,
                image_url VARCHAR(500),
                published BOOLEAN DEFAULT false,
                created_at TIMESTAMP DEFAULT NOW()
            );

            CREATE TABLE IF NOT EXISTS content_images (
                id SERIAL PRIMARY KEY,
                content_id INTEGER REFERENCES content(id) ON DELETE CASCADE,
                filename VARCHAR(255) NOT NULL,
                created_at TIMESTAMP DEFAULT NOW()
            );

            CREATE TABLE IF NOT EXISTS knowledge_sources (
                id SERIAL PRIMARY KEY,
                source_type VARCHAR(50) NOT NULL,
                source_ref_id INTEGER,
                owner_user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
                title VARCHAR(500) NOT NULL,
                category VARCHAR(255),
                content_text TEXT,
                file_path VARCHAR(1000),
                mime_type VARCHAR(255),
                scope VARCHAR(30) DEFAULT 'both',
                weight DOUBLE PRECISION DEFAULT 1.0,
                enabled BOOLEAN DEFAULT true,
                metadata JSONB DEFAULT '{}'::jsonb,
                created_at TIMESTAMP DEFAULT NOW(),
                updated_at TIMESTAMP DEFAULT NOW()
            );

            CREATE INDEX IF NOT EXISTS idx_knowledge_sources_owner ON knowledge_sources(owner_user_id);
            CREATE INDEX IF NOT EXISTS idx_knowledge_sources_type_ref ON knowledge_sources(source_type, source_ref_id);
            CREATE INDEX IF NOT EXISTS idx_knowledge_sources_enabled_scope ON knowledge_sources(enabled, scope);

            CREATE UNIQUE INDEX IF NOT EXISTS uq_knowledge_source_product ON knowledge_sources(source_ref_id)
                WHERE source_type='product' AND owner_user_id IS NULL;
            CREATE UNIQUE INDEX IF NOT EXISTS uq_knowledge_source_procedure ON knowledge_sources(source_ref_id)
                WHERE source_type='procedure' AND owner_user_id IS NULL;
            CREATE UNIQUE INDEX IF NOT EXISTS uq_knowledge_source_content ON knowledge_sources(source_ref_id)
                WHERE source_type='content' AND owner_user_id IS NULL;

            CREATE TABLE IF NOT EXISTS admin_journal (
                id SERIAL PRIMARY KEY,
                event_type VARCHAR(100) NOT NULL,
                severity VARCHAR(20) NOT NULL DEFAULT 'info',
                message TEXT NOT NULL,
                user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
                context JSONB DEFAULT '{}'::jsonb,
                created_at TIMESTAMP DEFAULT NOW()
            );

            CREATE INDEX IF NOT EXISTS idx_admin_journal_created_at ON admin_journal(created_at DESC);
            CREATE INDEX IF NOT EXISTS idx_admin_journal_event_type ON admin_journal(event_type);

            CREATE TABLE IF NOT EXISTS user_profiles (
                id SERIAL PRIMARY KEY,
                user_id INTEGER UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                extra_data JSONB DEFAULT '{}'::jsonb,
                created_at TIMESTAMP DEFAULT NOW(),
                updated_at TIMESTAMP DEFAULT NOW()
            );

            -- Dictionary tables (referenced by products/procedures/content)
            CREATE TABLE IF NOT EXISTS brands (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) UNIQUE NOT NULL
            );
            CREATE TABLE IF NOT EXISTS categories (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) UNIQUE NOT NULL
            );
            CREATE TABLE IF NOT EXISTS segments (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) UNIQUE NOT NULL
            );
            CREATE TABLE IF NOT EXISTS volumes (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) UNIQUE NOT NULL
            );
            CREATE TABLE IF NOT EXISTS procedure_categories (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) UNIQUE NOT NULL
            );
            CREATE TABLE IF NOT EXISTS content_categories (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) UNIQUE NOT NULL
            );
            CREATE TABLE IF NOT EXISTS user_roles (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) UNIQUE NOT NULL
            );
            CREATE TABLE IF NOT EXISTS skin_types (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) UNIQUE NOT NULL
            );
            CREATE TABLE IF NOT EXISTS product_types (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) UNIQUE NOT NULL
            );
            CREATE TABLE IF NOT EXISTS for_whom (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) UNIQUE NOT NULL
            );
            CREATE TABLE IF NOT EXISTS purposes (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) UNIQUE NOT NULL
            );
            CREATE TABLE IF NOT EXISTS application_times (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) UNIQUE NOT NULL
            );
            CREATE TABLE IF NOT EXISTS areas (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) UNIQUE NOT NULL
            );
            CREATE TABLE IF NOT EXISTS countries (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) UNIQUE NOT NULL
            );

            -- Procedure dictionary tables
            CREATE TABLE IF NOT EXISTS procedure_method_types (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) UNIQUE NOT NULL
            );
            CREATE TABLE IF NOT EXISTS procedure_durations (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) UNIQUE NOT NULL
            );
            CREATE TABLE IF NOT EXISTS procedure_zones (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) UNIQUE NOT NULL
            );
            CREATE TABLE IF NOT EXISTS procedure_equipment (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) UNIQUE NOT NULL
            );
            CREATE TABLE IF NOT EXISTS procedure_effects (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) UNIQUE NOT NULL
            );
            CREATE TABLE IF NOT EXISTS procedure_problems (
                id SERIAL PRIMARY KEY,
                value VARCHAR(255) UNIQUE NOT NULL
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

        # Fix ownership issue - transfer table ownership to aura_user
        try:
            await conn.execute("ALTER TABLE users OWNER TO aura_user")
        except Exception as e:
            print(f"Could not change ownership: {e}")
        
        # Check and add columns if they don't exist (PostgreSQL 15 compatible)
        columns_to_add = [
            ("nickname", "VARCHAR(255)"),
            ("password_hash", "VARCHAR(255)"),
        ]
        
        existing_columns = await conn.fetch(
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'users'"
        )
        existing_column_names = {row['column_name'] for row in existing_columns}
        
        for col_name, col_type in columns_to_add:
            if col_name not in existing_column_names:
                try:
                    await conn.execute(f"ALTER TABLE users ADD COLUMN {col_name} {col_type}")
                except Exception as e:
                    print(f"Could not add column {col_name}: {e}")
        
        # Create demo users if none exist
        users_count = await conn.fetchval("SELECT COUNT(*) FROM users")
        if users_count == 0:
            demo_users = [
                ('admin@aura.ru', 'Администратор', 'admin', 'admin123', 'Администратор'),
                ('manager@aura.ru', 'Менеджер', 'manager', 'manager123', 'Менеджер'),
                ('cosmetolog@aura.ru', 'Косметолог', 'cosmo', 'cosmo123', 'Косметолог'),
            ]
            for email, name, nickname, password, role in demo_users:
                password_hash = get_password_hash(password)
                await conn.execute(
                    """INSERT INTO users (name, email, role, nickname, password_hash) 
                       VALUES ($1, $2, $3, $4, $5)""",
                    name, email, role, f'@{nickname}', password_hash
                )
            print("Demo users created")

        # Create demo products if none exist
        products_count = await conn.fetchval("SELECT COUNT(*) FROM products")
        if products_count == 0:
            demo_products = [
                ('La Roche-Posay Effaclar Duo+', 'Очищение', 'Крем для проблемной кожи с ниацинамидом и цинком. Корректирует несовершенства, предотвращает появление новых.', '50мл', 'Космецевтика'),
                ('Vichy Minéral 89', 'Увлажнение', 'Ежедневный укрепляющий уход. 89% термальной воды Vichy + гиалуроновая кислота.', '50мл', 'Космецевтика'),
                ('CeraVe Hydrating Cleanser', 'Очищение', 'Увлажняющий очищающий лосьон для нормальной и сухой кожи. С церамидами и гиалуроновой кислотой.', '250мл', 'Космецевтика'),
                ('The Ordinary Niacinamide 10% + Zinc 1%', 'Сыворотки', 'Сыворотка с высокой концентрацией ниацинамида для сужения пор и контроля жирности.', '30мл', 'Бюджетная'),
                ('Bioderma Sensibio H2O', 'Очищение', 'Мицеллярная вода для чувствительной кожи. Мягко очищает и успокаивает.', '250мл', 'Космецевтика'),
                ('Cosrx Advanced Snail 96 Mucin', 'Увлажнение', 'Эссенция с муцином улитки 96%. Интенсивное увлажнение и восстановление кожи.', '100мл', 'Бюджетная'),
            ]
            for name, category, description, volume, segment in demo_products:
                await conn.execute(
                    """INSERT INTO products (name, category, description, volume, segment) 
                       VALUES ($1, $2, $3, $4, $5)""",
                    name, category, description, volume, segment
                )
            print("Demo products created")

        # Create demo procedures if none exist
        procedures_count = await conn.fetchval("SELECT COUNT(*) FROM procedures")
        if procedures_count == 0:
            demo_procedures = [
                ('Пилинг лица', 'Эстетическая косметология', 'Химический', '30-60 мин', 'Не требуется', 'Лицо', 'Глубокое очищение и обновление кожи', 'Процедура химического пилинга для обновления кожи лица.', 'Улучшение текстуры и тона кожи', 'Неровный тон, акне, пигментация', 'Принцип контролируемого повреждения верхних слоёв кожи', 'Очищение, нанесение раствора, нейтрализация', 'Для всех типов кожи', 'Акне, пигментация, неровная текстура', 'Беременность, воспаления, герпес', 'За неделю исключить ретиноиды и кислоты', '3-6 процедур с интервалом 2 недели', 'Избегать солнца, использовать SPF', 'Временное покраснение'),
                ('Мезотерапия', 'Инъекционная косметология', 'Инъекционный', '45-90 мин', 'Инъекционный аппарат', 'Лицо, шея, декольте', 'Глубокое увлажнение и питание', 'Инъекционное введение витаминных коктейлей.', 'Сияющая увлажнённая кожа', 'Сухость, морщины, тусклый цвет', 'Доставка активных веществ в дерму', 'Консультация, анестезия, инъекции', 'Сухая, возрастная, уставшая кожа', 'Морщины, сухость, потеря тонуса', 'Беременность, аллергия, воспаления', 'Не принимать алкоголь за 2 дня', '4-8 процедур с интервалом 1-2 недели', 'Не посещать сауну 3 дня', 'Отёки, гематомы'),
                ('RF-лифтинг лица', 'Аппаратная косметология', 'Аппаратный', '30-60 мин', 'RF-аппарат', 'Лицо, шея', 'Подтяжка и омоложение кожи', 'Радиочастотная подтяжка кожи.', 'Видимый лифтинг-эффект', 'Дряблость, морщины, овал', 'Нагрев дермы радиочастотной энергией', 'Очищение, нанесение геля, обработка', 'Возрастная кожа с признаками дряблости', 'Морщины, птоз, потеря упругости', 'Кардиостимулятор, беременность, металл', 'Увлажнять кожу, пить воду', '4-6 процедур с интервалом 2 недели', 'Лёгкое покраснение 1-2 часа', 'Покраснение, тепло'),
            ]
            for name, direction, method_type, duration, equipment, zones, effects, description, advantages, problems, principle, how_it_goes, for_whom, problems_solved, contraindications, preparation, recommended_course, rehabilitation, side_effects in demo_procedures:
                await conn.execute(
                    """INSERT INTO procedures (name, direction, method_type, duration, equipment, zones, effects, description, advantages, problems, principle, how_it_goes, for_whom, problems_solved, contraindications, preparation, recommended_course, rehabilitation, side_effects) 
                       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17,$18,$19)""",
                    name, direction, method_type, duration, equipment, zones, effects, description, advantages, problems, principle, how_it_goes, for_whom, problems_solved, contraindications, preparation, recommended_course, rehabilitation, side_effects
                )
            print("Demo procedures created")

        # Create demo content if none exist
        content_count = await conn.fetchval("SELECT COUNT(*) FROM content")
        if content_count == 0:
            demo_content = [
                ('Как правильно очищать кожу', 'Уход за кожей', 'очищение, уход, кожа', 'Администратор', 'Правильное очищение — основа здоровой кожи. Разбираем типы средств и технику нанесения.', True),
                ('Ниацинамид: полный гид', 'Ингредиенты', 'ниацинамид, витамин B3, поры', 'Администратор', 'Ниацинамид — один из самых исследованных ингредиентов. Сужает поры, выравнивает тон, контролирует жирность.', True),
                ('SPF: зачем и как использовать', 'Защита', 'SPF, защита от солнца, фотостарение', 'Администратор', 'SPF-защита — обязательный шаг в уходе. Разбираем мифы и даём практические рекомендации.', True),
            ]
            for title, category, tags, author_name, body, published in demo_content:
                await conn.execute(
                    """INSERT INTO content (title, category, tags, author_name, body, published) 
                       VALUES ($1, $2, $3, $4, $5, $6)""",
                    title, category, tags, author_name, body, published
                )
            print("Demo content created")

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
    category: Optional[str] = None
    tags: Optional[Union[List[str], str]] = None
    author_id: Optional[int] = None
    author_name: Optional[str] = None
    body: Optional[str] = None
    image_url: Optional[str] = None
    published: Optional[bool] = False


class UserCreate(BaseModel):
    name: str
    email: str
    password: Optional[str] = None
    role: Optional[str] = "user"
    nickname: Optional[str] = None
    avatar: Optional[str] = None


class LoginRequest(BaseModel):
    email: str
    password: str


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: dict


class SkinPassportUpdateRequest(BaseModel):
    answers: Dict[str, List[str]]
    completed_at_epoch_millis: Optional[int] = None


class SkinPassportResponse(BaseModel):
    completed_at_epoch_millis: Optional[int] = None
    answers: Dict[str, List[str]] = {}


class AssistantChatRequest(BaseModel):
    message: str
    max_results: int = 5


class AssistantSource(BaseModel):
    id: Optional[str] = None
    title: str = ""
    content: str = ""
    relevance: Optional[float] = None


class AssistantChatResponse(BaseModel):
    answer: str
    sources: List[AssistantSource] = []


class AssistantReindexResponse(BaseModel):
    status: str
    indexed_count: int = 0
    products_count: int = 0
    procedures_count: int = 0
    content_count: int = 0


class KnowledgeSourceUpdateRequest(BaseModel):
    enabled: Optional[bool] = None
    scope: Optional[str] = None
    weight: Optional[float] = None


class KnowledgeSourceItem(BaseModel):
    id: int
    source_type: str
    source_ref_id: Optional[int] = None
    owner_user_id: Optional[int] = None
    title: str
    category: Optional[str] = None
    scope: str = "both"
    weight: float = 1.0
    enabled: bool = True
    mime_type: Optional[str] = None
    created_at: Optional[str] = None
    updated_at: Optional[str] = None


class KnowledgeSourceListResponse(BaseModel):
    items: List[KnowledgeSourceItem] = []
    total: int = 0


class UserDocumentUploadResponse(BaseModel):
    status: str
    source_id: int
    title: str


# JWT Functions
def create_access_token(data: dict, expires_delta: Optional[timedelta] = None):
    to_encode = data.copy()
    subject = to_encode.get("sub")
    if subject is not None and not isinstance(subject, str):
        to_encode["sub"] = str(subject)
    if expires_delta:
        expire = datetime.utcnow() + expires_delta
    else:
        expire = datetime.utcnow() + timedelta(hours=JWT_EXPIRATION_HOURS)
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, JWT_SECRET, algorithm=JWT_ALGORITHM)


def verify_password(plain_password: str, hashed_password: str) -> bool:
    try:
        return pwd_context.verify(plain_password, hashed_password)
    except:
        return False


def get_password_hash(password: str) -> str:
    return pwd_context.hash(password)


# Authentication dependency
async def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(security)):
    if not jwt:
        raise HTTPException(status_code=500, detail="JWT library not installed")
    
    try:
        token = credentials.credentials
        payload = jwt.decode(token, JWT_SECRET, algorithms=[JWT_ALGORITHM])
        raw_user_id = payload.get("sub")
        if raw_user_id is None:
            raise HTTPException(status_code=401, detail="Invalid token")
        user_id = int(raw_user_id)
        if user_id <= 0:
            raise HTTPException(status_code=401, detail="Invalid token")
    except (TypeError, ValueError):
        raise HTTPException(status_code=401, detail="Invalid token")
    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid token")
    
    async with pool.acquire() as conn:
        row = await conn.fetchrow("SELECT * FROM users WHERE id=$1", user_id)
        if not row:
            raise HTTPException(status_code=401, detail="User not found")
        return dict(row)


async def get_current_user_optional(credentials: Optional[HTTPAuthorizationCredentials] = Depends(None)):
    if not credentials:
        return None
    try:
        return await get_current_user(credentials)
    except HTTPException:
        return None


def is_valid_email(email: str) -> bool:
    return bool(re.match(r"^[^\s@]+@[^\s@]+\.[^\s@]{2,}$", email or ""))


def normalize_login(login: Optional[str]) -> Optional[str]:
    if not login:
        return None
    cleaned = re.sub(r"\s+", "", login.strip())
    if not cleaned:
        return None
    if not cleaned.startswith("@"):
        cleaned = f"@{cleaned.replace('@', '')}"
    else:
        cleaned = "@" + cleaned[1:].replace("@", "")
    return cleaned.lower()


def is_valid_login(login: Optional[str]) -> bool:
    if not login:
        return True
    return bool(re.match(r"^@[a-z0-9_]{3,32}$", login))


def _extract_upstream_error(response_text: str) -> Optional[str]:
    if not response_text:
        return None

    try:
        payload = json.loads(response_text)
    except (TypeError, ValueError, json.JSONDecodeError):
        return None

    if isinstance(payload, dict):
        detail = payload.get("detail") or payload.get("message")
        if detail:
            return str(detail)

    return None


def _is_admin_role(role: Optional[str]) -> bool:
    role_value = (role or "").strip().lower()
    return role_value in {"admin", "administrator", "администратор", "админ"}


def _normalize_scope(scope: Optional[str], default: str = "both") -> str:
    value = (scope or default).strip().lower()
    if value not in {"rag", "recommendations", "both"}:
        raise HTTPException(status_code=400, detail="scope должен быть одним из: rag, recommendations, both")
    return value


def _normalize_weight(weight: Optional[float], default: float = 1.0) -> float:
    if weight is None:
        return default
    try:
        value = float(weight)
    except (TypeError, ValueError):
        raise HTTPException(status_code=400, detail="weight должен быть числом")
    if value < 0 or value > 10:
        raise HTTPException(status_code=400, detail="weight должен быть в диапазоне 0..10")
    return value


async def _upsert_global_knowledge_source(
    conn: asyncpg.Connection,
    source_type: str,
    source_ref_id: int,
    title: str,
    category: Optional[str],
    content_text: str,
) -> None:
    existing = await conn.fetchrow(
        """
        SELECT id, enabled, scope, weight
        FROM knowledge_sources
        WHERE source_type=$1 AND source_ref_id=$2 AND owner_user_id IS NULL
        LIMIT 1
        """,
        source_type,
        source_ref_id,
    )

    if existing:
        await conn.execute(
            """
            UPDATE knowledge_sources
            SET title=$1, category=$2, content_text=$3, updated_at=NOW()
            WHERE id=$4
            """,
            title,
            category,
            content_text,
            existing["id"],
        )
        return

    await conn.execute(
        """
        INSERT INTO knowledge_sources (
            source_type, source_ref_id, owner_user_id,
            title, category, content_text,
            scope, weight, enabled, metadata
        )
        VALUES ($1, $2, NULL, $3, $4, $5, 'both', 1.0, true, '{}'::jsonb)
        """,
        source_type,
        source_ref_id,
        title,
        category,
        content_text,
    )


async def _sync_global_knowledge_registry(conn: asyncpg.Connection) -> None:
    products_rows = await conn.fetch("SELECT * FROM products")
    procedures_rows = await conn.fetch("SELECT * FROM procedures")
    content_rows = await conn.fetch("SELECT * FROM content")

    for doc in _build_products_knowledge_docs(products_rows):
        source_id = doc.get("source_id")
        if source_id is None:
            continue
        await _upsert_global_knowledge_source(
            conn,
            source_type="product",
            source_ref_id=int(source_id),
            title=str(doc.get("title") or "Продукт"),
            category=doc.get("category"),
            content_text=str(doc.get("content") or ""),
        )

    for doc in _build_procedures_knowledge_docs(procedures_rows):
        source_id = doc.get("source_id")
        if source_id is None:
            continue
        await _upsert_global_knowledge_source(
            conn,
            source_type="procedure",
            source_ref_id=int(source_id),
            title=str(doc.get("title") or "Процедура"),
            category=doc.get("category"),
            content_text=str(doc.get("content") or ""),
        )

    for doc in _build_content_knowledge_docs(content_rows):
        source_id = doc.get("source_id")
        if source_id is None:
            continue
        await _upsert_global_knowledge_source(
            conn,
            source_type="content",
            source_ref_id=int(source_id),
            title=str(doc.get("title") or "Контент"),
            category=doc.get("category"),
            content_text=str(doc.get("content") or ""),
        )


def _extract_text_from_document(filename: str, content_type: Optional[str], content_bytes: bytes) -> str:
    if not content_bytes:
        return ""

    lower_name = (filename or "").lower()
    mime = (content_type or "").lower()

    is_pdf = lower_name.endswith(".pdf") or "pdf" in mime
    if is_pdf:
        try:
            from pypdf import PdfReader

            reader = PdfReader(io.BytesIO(content_bytes))
            pages = []
            for page in reader.pages:
                text = page.extract_text() or ""
                if text.strip():
                    pages.append(text.strip())
            return "\n\n".join(pages)
        except Exception as exc:
            raise HTTPException(status_code=400, detail=f"Не удалось прочитать PDF: {exc}")

    try:
        return content_bytes.decode("utf-8")
    except UnicodeDecodeError:
        return content_bytes.decode("utf-8", errors="ignore")


async def _ingest_docs_to_ai_service(
    docs: List[Dict[str, Any]],
    ai_service_url: str,
    clear_global_first: bool = False,
) -> None:
    if not docs:
        return

    timeout = aiohttp.ClientTimeout(total=90)
    try:
        async with aiohttp.ClientSession(timeout=timeout) as session:
            if clear_global_first:
                await session.delete(f"{ai_service_url}/api/v1/rag/knowledge")
            async with session.post(f"{ai_service_url}/api/v1/rag/ingest", json=docs) as response:
                response_text = await response.text()
                if response.status >= 400:
                    detail = _extract_upstream_error(response_text) or f"AI service error ({response.status})"
                    raise HTTPException(status_code=502, detail=detail)
    except aiohttp.ClientError:
        raise HTTPException(status_code=503, detail="AI сервис недоступен.")
    except asyncio.TimeoutError:
        raise HTTPException(status_code=504, detail="AI сервис не ответил вовремя.")


def _stringify_for_knowledge(value: Any) -> str:
    if value is None:
        return ""
    if isinstance(value, str):
        return value
    if isinstance(value, (int, float, bool)):
        return str(value)
    if isinstance(value, list):
        return ", ".join(_stringify_for_knowledge(item) for item in value if item is not None)
    if isinstance(value, dict):
        return json.dumps(value, ensure_ascii=False)
    return str(value)


def _build_products_knowledge_docs(rows: List[asyncpg.Record]) -> List[Dict[str, Any]]:
    docs: List[Dict[str, Any]] = []
    for row in rows:
        data = dict(row)
        title = _stringify_for_knowledge(data.get("name")) or "Продукт"
        category = _stringify_for_knowledge(data.get("category")) or "products"
        content_parts = [
            f"Бренд: {_stringify_for_knowledge(data.get('brand'))}",
            f"Тип: {_stringify_for_knowledge(data.get('product_type') or data.get('what_is_it'))}",
            f"Для кого: {_stringify_for_knowledge(data.get('for_whom'))}",
            f"Назначение: {_stringify_for_knowledge(data.get('purpose'))}",
            f"Тип кожи: {_stringify_for_knowledge(data.get('skin_type'))}",
            f"Активные ингредиенты: {_stringify_for_knowledge(data.get('active_ingredient'))}",
            f"Описание: {_stringify_for_knowledge(data.get('description'))}",
            f"Состав: {_stringify_for_knowledge(data.get('composition'))}",
            f"Применение: {_stringify_for_knowledge(data.get('application_info'))}",
        ]
        content = "\n".join(part for part in content_parts if not part.endswith(": "))
        docs.append(
            {
                "title": title,
                "content": content,
                "category": category,
                "source_type": "product",
                "source_id": data.get("id"),
            }
        )
    return docs


def _build_procedures_knowledge_docs(rows: List[asyncpg.Record]) -> List[Dict[str, Any]]:
    docs: List[Dict[str, Any]] = []
    for row in rows:
        data = dict(row)
        title = _stringify_for_knowledge(data.get("name")) or "Процедура"
        category = _stringify_for_knowledge(data.get("direction") or data.get("method_type")) or "procedures"
        content_parts = [
            f"Направление: {_stringify_for_knowledge(data.get('direction'))}",
            f"Тип метода: {_stringify_for_knowledge(data.get('method_type'))}",
            f"Описание: {_stringify_for_knowledge(data.get('description') or data.get('procedure_about'))}",
            f"Преимущества: {_stringify_for_knowledge(data.get('advantages'))}",
            f"Показания: {_stringify_for_knowledge(data.get('indications'))}",
            f"Для кого: {_stringify_for_knowledge(data.get('for_whom'))}",
            f"Противопоказания: {_stringify_for_knowledge(data.get('contraindications_full') or data.get('contraindications'))}",
            f"Реабилитация: {_stringify_for_knowledge(data.get('rehabilitation'))}",
        ]
        content = "\n".join(part for part in content_parts if not part.endswith(": "))
        docs.append(
            {
                "title": title,
                "content": content,
                "category": category,
                "source_type": "procedure",
                "source_id": data.get("id"),
            }
        )
    return docs


def _build_content_knowledge_docs(rows: List[asyncpg.Record]) -> List[Dict[str, Any]]:
    docs: List[Dict[str, Any]] = []
    for row in rows:
        data = dict(row)
        title = _stringify_for_knowledge(data.get("title")) or "Контент"
        category = _stringify_for_knowledge(data.get("category")) or "content"
        body = _stringify_for_knowledge(data.get("body"))
        tags = _stringify_for_knowledge(data.get("tags"))
        author = _stringify_for_knowledge(data.get("author_name"))
        content_parts = [
            f"Автор: {author}",
            f"Теги: {tags}",
            f"Текст: {body}",
        ]
        content = "\n".join(part for part in content_parts if not part.endswith(": "))
        docs.append(
            {
                "title": title,
                "content": content,
                "category": category,
                "source_type": "content",
                "source_id": data.get("id"),
            }
        )
    return docs


async def ensure_users_columns(conn):
    await conn.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS nickname VARCHAR(255)")


async def ensure_unique_login(conn, login: Optional[str], exclude_user_id: Optional[int] = None):
    if not login:
        return
    if exclude_user_id is None:
        existing = await conn.fetchrow("SELECT id FROM users WHERE LOWER(nickname)=LOWER($1) LIMIT 1", login)
    else:
        existing = await conn.fetchrow(
            "SELECT id FROM users WHERE LOWER(nickname)=LOWER($1) AND id<>$2 LIMIT 1",
            login,
            exclude_user_id,
        )
    if existing:
        raise HTTPException(status_code=400, detail="Логин уже занят")


async def write_admin_journal(
    event_type: str,
    message: str,
    severity: str = "info",
    user_id: Optional[int] = None,
    context: Optional[Dict[str, Any]] = None,
):
    if pool is None:
        return

    try:
        async with pool.acquire() as conn:
            await conn.execute(
                """
                INSERT INTO admin_journal (event_type, severity, message, user_id, context)
                VALUES ($1, $2, $3, $4, $5::jsonb)
                """,
                event_type,
                severity,
                message,
                user_id,
                json.dumps(context or {}, ensure_ascii=False),
            )
    except Exception as exc:
        auth_logger.warning("admin_journal_write_failed event_type=%s error=%s", event_type, exc)


class DictionaryValue(BaseModel):
    value: str


class DictionaryUpdate(BaseModel):
    oldValue: str
    newValue: str


@app.get("/api/health")
async def health_check():
    return {"status": "ok"}


@app.post("/api/auth/login", response_model=TokenResponse)
async def login(request: LoginRequest):
    auth_logger.info("auth_login_attempt email=%s", request.email.lower().strip())
    async with pool.acquire() as conn:
        # Find user by email
        row = await conn.fetchrow(
            "SELECT * FROM users WHERE LOWER(email) = LOWER($1)",
            request.email
        )
        
        if not row:
            auth_logger.warning("auth_login_failed reason=email_not_found email=%s", request.email.lower().strip())
            await write_admin_journal(
                event_type="auth_login_failed",
                severity="warning",
                message="Неуспешный вход: email не найден",
                context={"email": request.email.lower().strip(), "reason": "email_not_found"},
            )
            raise HTTPException(status_code=401, detail="Неверный email или пароль")
        
        # Check password
        user_dict = dict(row)
        stored_password = user_dict.get('password_hash')
        
        if not stored_password or not verify_password(request.password, stored_password):
            auth_logger.warning("auth_login_failed reason=bad_password email=%s", request.email.lower().strip())
            await write_admin_journal(
                event_type="auth_login_failed",
                severity="warning",
                message="Неуспешный вход: неверный пароль",
                user_id=user_dict.get("id"),
                context={"email": request.email.lower().strip(), "reason": "bad_password"},
            )
            raise HTTPException(status_code=401, detail="Неверный email или пароль")
        
        # Create token
        access_token = create_access_token(data={"sub": user_dict['id']})
        
        # Return user without password
        user_dict.pop('password_hash', None)
        auth_logger.info("auth_login_success user_id=%s email=%s", user_dict.get("id"), user_dict.get("email"))
        await write_admin_journal(
            event_type="auth_login_success",
            severity="info",
            message="Успешный вход пользователя",
            user_id=user_dict.get("id"),
            context={"email": user_dict.get("email")},
        )
        
        return TokenResponse(
            access_token=access_token,
            user=user_dict
        )


@app.post("/api/auth/register", response_model=TokenResponse)
async def register(user: UserCreate):
    auth_logger.info("auth_register_attempt email=%s", user.email.lower().strip())
    if not user.password:
        auth_logger.warning("auth_register_failed reason=missing_password email=%s", user.email.lower().strip())
        await write_admin_journal(
            event_type="auth_register_failed",
            severity="warning",
            message="Неуспешная регистрация: отсутствует пароль",
            context={"email": user.email.lower().strip(), "reason": "missing_password"},
        )
        raise HTTPException(status_code=400, detail="Пароль обязателен")
    
    if not is_valid_email(user.email):
        auth_logger.warning("auth_register_failed reason=invalid_email email=%s", user.email.lower().strip())
        await write_admin_journal(
            event_type="auth_register_failed",
            severity="warning",
            message="Неуспешная регистрация: некорректный email",
            context={"email": user.email.lower().strip(), "reason": "invalid_email"},
        )
        raise HTTPException(status_code=400, detail="Некорректный email")
    
    login = normalize_login(user.nickname)
    
    if login and not is_valid_login(login):
        auth_logger.warning("auth_register_failed reason=invalid_login email=%s login=%s", user.email.lower().strip(), login)
        await write_admin_journal(
            event_type="auth_register_failed",
            severity="warning",
            message="Неуспешная регистрация: некорректный логин",
            context={"email": user.email.lower().strip(), "reason": "invalid_login", "login": login},
        )
        raise HTTPException(status_code=400, detail="Логин должен быть в формате @login")
    
    async with pool.acquire() as conn:
        await ensure_users_columns(conn)
        
        # Check if email exists
        existing = await conn.fetchrow(
            "SELECT id FROM users WHERE LOWER(email) = LOWER($1)",
            user.email
        )
        if existing:
            auth_logger.warning("auth_register_failed reason=email_exists email=%s", user.email.lower().strip())
            await write_admin_journal(
                event_type="auth_register_failed",
                severity="warning",
                message="Неуспешная регистрация: email уже занят",
                context={"email": user.email.lower().strip(), "reason": "email_exists"},
            )
            raise HTTPException(status_code=400, detail="Email уже занят")
        
        # Check if login exists
        if login:
            await ensure_unique_login(conn, login)
        
        # Hash password
        password_hash = get_password_hash(user.password)
        
        # Set default role
        role = user.role or "Пользователь"
        
        row = await conn.fetchrow(
            """INSERT INTO users (name, email, role, nickname, avatar, password_hash) 
               VALUES ($1, $2, $3, $4, $5, $6) RETURNING *""",
            user.name, user.email, role, login, user.avatar, password_hash
        )
        
        user_dict = dict(row)
        access_token = create_access_token(data={"sub": user_dict['id']})
        user_dict.pop('password_hash', None)
        auth_logger.info(
            "auth_register_success user_id=%s email=%s role=%s",
            user_dict.get("id"),
            user_dict.get("email"),
            user_dict.get("role")
        )
        await write_admin_journal(
            event_type="auth_register_success",
            severity="info",
            message="Успешная регистрация пользователя",
            user_id=user_dict.get("id"),
            context={"email": user_dict.get("email"), "role": user_dict.get("role")},
        )
        
        return TokenResponse(
            access_token=access_token,
            user=user_dict
        )


@app.get("/api/auth/me")
async def get_me(current_user: dict = Depends(get_current_user)):
    user = current_user.copy()
    user.pop('password_hash', None)
    return user


@app.get("/api/admin/journal")
async def get_admin_journal(
    limit: int = 100,
    current_user: dict = Depends(get_current_user)
):
    if not _is_admin_role(current_user.get("role")):
        raise HTTPException(status_code=403, detail="Только администратор может просматривать журнал")

    safe_limit = max(1, min(limit, 500))
    async with pool.acquire() as conn:
        rows = await conn.fetch(
            """
            SELECT id, event_type, severity, message, user_id, context, created_at
            FROM admin_journal
            ORDER BY created_at DESC
            LIMIT $1
            """,
            safe_limit,
        )

    return [dict(row) for row in rows]


def _sanitize_skin_passport_answers(raw_answers) -> Dict[str, List[str]]:
    if not isinstance(raw_answers, dict):
        return {}

    sanitized: Dict[str, List[str]] = {}
    for key, value in raw_answers.items():
        if isinstance(value, list):
            sanitized[str(key)] = [str(item) for item in value]
    return sanitized


@app.get("/api/profile/skin-passport", response_model=SkinPassportResponse)
async def get_skin_passport(current_user: dict = Depends(get_current_user)):
    user_id = current_user.get("id")

    async with pool.acquire() as conn:
        row = await conn.fetchrow("SELECT extra_data FROM user_profiles WHERE user_id=$1", user_id)

    if not row:
        return SkinPassportResponse(completed_at_epoch_millis=None, answers={})

    extra_data = row["extra_data"]
    if not isinstance(extra_data, dict):
        return SkinPassportResponse(completed_at_epoch_millis=None, answers={})

    skin_passport = extra_data.get("skin_passport")
    if not isinstance(skin_passport, dict):
        return SkinPassportResponse(completed_at_epoch_millis=None, answers={})

    completed_at = skin_passport.get("completed_at_epoch_millis")
    try:
        completed_at = int(completed_at) if completed_at is not None else None
    except (TypeError, ValueError):
        completed_at = None

    answers = _sanitize_skin_passport_answers(skin_passport.get("answers"))
    return SkinPassportResponse(completed_at_epoch_millis=completed_at, answers=answers)


@app.put("/api/profile/skin-passport", response_model=SkinPassportResponse)
async def save_skin_passport(payload: SkinPassportUpdateRequest, current_user: dict = Depends(get_current_user)):
    user_id = current_user.get("id")
    completed_at = payload.completed_at_epoch_millis or int(datetime.utcnow().timestamp() * 1000)
    answers = _sanitize_skin_passport_answers(payload.answers)

    update_blob = {
        "skin_passport": {
            "completed_at_epoch_millis": completed_at,
            "answers": answers,
        }
    }

    async with pool.acquire() as conn:
        await conn.execute(
            """
            INSERT INTO user_profiles (user_id, extra_data, updated_at)
            VALUES ($1, $2::jsonb, NOW())
            ON CONFLICT (user_id)
            DO UPDATE SET
                extra_data = COALESCE(user_profiles.extra_data, '{}'::jsonb) || $2::jsonb,
                updated_at = NOW()
            """,
            user_id,
            json.dumps(update_blob, ensure_ascii=False),
        )

    return SkinPassportResponse(completed_at_epoch_millis=completed_at, answers=answers)


@app.post("/api/assistant/chat", response_model=AssistantChatResponse)
async def assistant_chat(payload: AssistantChatRequest, current_user: dict = Depends(get_current_user)):
    message = (payload.message or "").strip()
    if not message:
        raise HTTPException(status_code=400, detail="Сообщение не может быть пустым")

    max_results = max(1, min(payload.max_results, 10))
    ai_service_url = os.getenv("AI_SERVICE_URL", "http://localhost:9001").rstrip("/")
    upstream_payload = {
        "query": message,
        "user_id": str(current_user.get("id")),
        "max_results": max_results,
    }

    try:
        timeout = aiohttp.ClientTimeout(total=25)
        async with aiohttp.ClientSession(timeout=timeout) as session:
            async with session.post(f"{ai_service_url}/api/v1/rag/query", json=upstream_payload) as response:
                response_text = await response.text()

                if response.status >= 400:
                    detail = _extract_upstream_error(response_text) or f"AI service error ({response.status})"
                    raise HTTPException(status_code=502, detail=detail)

    except aiohttp.ClientError:
        raise HTTPException(status_code=503, detail="AI сервис недоступен. Попробуйте позже.")
    except asyncio.TimeoutError:
        raise HTTPException(status_code=504, detail="AI сервис не ответил вовремя. Попробуйте позже.")

    try:
        data = json.loads(response_text) if response_text else {}
    except (TypeError, ValueError, json.JSONDecodeError):
        raise HTTPException(status_code=502, detail="Некорректный ответ AI сервиса")

    answer = ""
    if isinstance(data, dict):
        answer = str(data.get("answer") or "").strip()

    if not answer:
        answer = "Не удалось получить ответ ассистента. Попробуйте переформулировать вопрос."

    sources: List[AssistantSource] = []
    raw_sources = data.get("sources") if isinstance(data, dict) else []
    if isinstance(raw_sources, list):
        for raw_source in raw_sources:
            if not isinstance(raw_source, dict):
                continue

            relevance = raw_source.get("relevance", raw_source.get("score"))
            relevance_value: Optional[float] = None
            if relevance is not None:
                try:
                    relevance_value = float(relevance)
                except (TypeError, ValueError):
                    relevance_value = None

            source_id = raw_source.get("id")
            sources.append(
                AssistantSource(
                    id=str(source_id) if source_id is not None else None,
                    title=str(raw_source.get("title") or ""),
                    content=str(raw_source.get("content") or ""),
                    relevance=relevance_value,
                )
            )

    return AssistantChatResponse(answer=answer, sources=sources)


@app.post("/api/assistant/knowledge/reindex", response_model=AssistantReindexResponse)
async def assistant_reindex_knowledge(current_user: dict = Depends(get_current_user)):
    if not _is_admin_role(current_user.get("role")):
        raise HTTPException(status_code=403, detail="Только администратор может запускать переиндексацию")

    ai_service_url = os.getenv("AI_SERVICE_URL", "http://localhost:9001").rstrip("/")

    async with pool.acquire() as conn:
        await _sync_global_knowledge_registry(conn)

        rows = await conn.fetch(
            """
            SELECT *
            FROM knowledge_sources
            WHERE owner_user_id IS NULL
              AND enabled = true
              AND scope IN ('both', 'rag')
            ORDER BY id DESC
            """
        )

    docs: List[Dict[str, Any]] = []
    products_count = 0
    procedures_count = 0
    content_count = 0

    for row in rows:
        data = dict(row)
        source_type = str(data.get("source_type") or "")
        if source_type == "product":
            products_count += 1
        elif source_type == "procedure":
            procedures_count += 1
        elif source_type == "content":
            content_count += 1

        docs.append(
            {
                "title": str(data.get("title") or "Источник"),
                "content": str(data.get("content_text") or ""),
                "category": data.get("category"),
                "source_type": source_type,
                "source_id": data.get("source_ref_id") if data.get("source_ref_id") is not None else data.get("id"),
                "source_scope": "global",
                "owner_user_id": None,
                "weight": float(data.get("weight") or 1.0),
            }
        )

    if not docs:
        return AssistantReindexResponse(status="success", indexed_count=0)

    await _ingest_docs_to_ai_service(docs, ai_service_url, clear_global_first=True)

    return AssistantReindexResponse(
        status="success",
        indexed_count=len(docs),
        products_count=products_count,
        procedures_count=procedures_count,
        content_count=content_count,
    )


@app.get("/api/assistant/knowledge/sources", response_model=KnowledgeSourceListResponse)
async def list_knowledge_sources(current_user: dict = Depends(get_current_user)):
    if not _is_admin_role(current_user.get("role")):
        raise HTTPException(status_code=403, detail="Только администратор может просматривать источники")

    async with pool.acquire() as conn:
        await _sync_global_knowledge_registry(conn)
        rows = await conn.fetch(
            """
            SELECT id, source_type, source_ref_id, owner_user_id, title, category,
                   scope, weight, enabled, mime_type, created_at, updated_at
            FROM knowledge_sources
            ORDER BY owner_user_id NULLS FIRST, id DESC
            """
        )

    items = [
        KnowledgeSourceItem(
            id=row["id"],
            source_type=row["source_type"],
            source_ref_id=row["source_ref_id"],
            owner_user_id=row["owner_user_id"],
            title=row["title"],
            category=row["category"],
            scope=row["scope"] or "both",
            weight=float(row["weight"] or 1.0),
            enabled=bool(row["enabled"]),
            mime_type=row["mime_type"],
            created_at=str(row["created_at"]) if row["created_at"] is not None else None,
            updated_at=str(row["updated_at"]) if row["updated_at"] is not None else None,
        )
        for row in rows
    ]

    return KnowledgeSourceListResponse(items=items, total=len(items))


@app.patch("/api/assistant/knowledge/sources/{source_id}", response_model=KnowledgeSourceItem)
async def update_knowledge_source(
    source_id: int,
    payload: KnowledgeSourceUpdateRequest,
    current_user: dict = Depends(get_current_user),
):
    if not _is_admin_role(current_user.get("role")):
        raise HTTPException(status_code=403, detail="Только администратор может менять источники")

    updates: List[str] = []
    values: List[Any] = []
    idx = 1

    if payload.enabled is not None:
        updates.append(f"enabled=${idx}")
        values.append(bool(payload.enabled))
        idx += 1

    if payload.scope is not None:
        updates.append(f"scope=${idx}")
        values.append(_normalize_scope(payload.scope))
        idx += 1

    if payload.weight is not None:
        updates.append(f"weight=${idx}")
        values.append(_normalize_weight(payload.weight))
        idx += 1

    if not updates:
        raise HTTPException(status_code=400, detail="Нет полей для обновления")

    updates.append("updated_at=NOW()")
    values.append(source_id)

    query = f"""
        UPDATE knowledge_sources
        SET {', '.join(updates)}
        WHERE id=${idx}
        RETURNING id, source_type, source_ref_id, owner_user_id, title, category,
                  scope, weight, enabled, mime_type, created_at, updated_at
    """

    async with pool.acquire() as conn:
        row = await conn.fetchrow(query, *values)

    if not row:
        raise HTTPException(status_code=404, detail="Источник не найден")

    return KnowledgeSourceItem(
        id=row["id"],
        source_type=row["source_type"],
        source_ref_id=row["source_ref_id"],
        owner_user_id=row["owner_user_id"],
        title=row["title"],
        category=row["category"],
        scope=row["scope"] or "both",
        weight=float(row["weight"] or 1.0),
        enabled=bool(row["enabled"]),
        mime_type=row["mime_type"],
        created_at=str(row["created_at"]) if row["created_at"] is not None else None,
        updated_at=str(row["updated_at"]) if row["updated_at"] is not None else None,
    )


@app.post("/api/assistant/knowledge/user-documents", response_model=UserDocumentUploadResponse)
async def upload_user_document(
    file: UploadFile = File(...),
    current_user: dict = Depends(get_current_user),
):
    filename = file.filename or "document"
    content_bytes = await file.read()
    if not content_bytes:
        raise HTTPException(status_code=400, detail="Файл пустой")

    text = _extract_text_from_document(filename, file.content_type, content_bytes).strip()
    if not text:
        raise HTTPException(status_code=400, detail="Не удалось извлечь текст из документа")

    upload_dir = os.path.join(os.path.dirname(__file__), "uploads", "knowledge", "user")
    os.makedirs(upload_dir, exist_ok=True)
    stored_name = f"{uuid.uuid4()}_{os.path.basename(filename)}"
    stored_path = os.path.join(upload_dir, stored_name)
    with open(stored_path, "wb") as output_file:
        output_file.write(content_bytes)

    scope = "both"
    weight = 1.0

    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            """
            INSERT INTO knowledge_sources (
                source_type, source_ref_id, owner_user_id,
                title, category, content_text, file_path, mime_type,
                scope, weight, enabled, metadata
            )
            VALUES (
                'user_document', NULL, $1,
                $2, NULL, $3, $4, $5,
                $6, $7, true, '{}'::jsonb
            )
            RETURNING id
            """,
            current_user.get("id"),
            filename,
            text,
            stored_path,
            file.content_type,
            scope,
            weight,
        )

    source_id = row["id"]
    ai_service_url = os.getenv("AI_SERVICE_URL", "http://localhost:9001").rstrip("/")
    await _ingest_docs_to_ai_service(
        docs=[
            {
                "title": filename,
                "content": text,
                "category": "user_documents",
                "source_type": "user_document",
                "source_id": source_id,
                "source_scope": "user",
                "owner_user_id": str(current_user.get("id")),
                "weight": weight,
            }
        ],
        ai_service_url=ai_service_url,
        clear_global_first=False,
    )

    return UserDocumentUploadResponse(status="success", source_id=source_id, title=filename)


@app.post("/api/assistant/knowledge/admin-documents", response_model=UserDocumentUploadResponse)
async def upload_admin_document(
    file: UploadFile = File(...),
    scope: str = "both",
    weight: float = 1.0,
    current_user: dict = Depends(get_current_user),
):
    if not _is_admin_role(current_user.get("role")):
        raise HTTPException(status_code=403, detail="Только администратор может загружать global документы")

    normalized_scope = _normalize_scope(scope)
    normalized_weight = _normalize_weight(weight)

    filename = file.filename or "document"
    content_bytes = await file.read()
    if not content_bytes:
        raise HTTPException(status_code=400, detail="Файл пустой")

    text = _extract_text_from_document(filename, file.content_type, content_bytes).strip()
    if not text:
        raise HTTPException(status_code=400, detail="Не удалось извлечь текст из документа")

    upload_dir = os.path.join(os.path.dirname(__file__), "uploads", "knowledge", "admin")
    os.makedirs(upload_dir, exist_ok=True)
    stored_name = f"{uuid.uuid4()}_{os.path.basename(filename)}"
    stored_path = os.path.join(upload_dir, stored_name)
    with open(stored_path, "wb") as output_file:
        output_file.write(content_bytes)

    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            """
            INSERT INTO knowledge_sources (
                source_type, source_ref_id, owner_user_id,
                title, category, content_text, file_path, mime_type,
                scope, weight, enabled, metadata
            )
            VALUES (
                'admin_document', NULL, NULL,
                $1, 'scientific_publication', $2, $3, $4,
                $5, $6, true, '{}'::jsonb
            )
            RETURNING id
            """,
            filename,
            text,
            stored_path,
            file.content_type,
            normalized_scope,
            normalized_weight,
        )

    source_id = row["id"]
    ai_service_url = os.getenv("AI_SERVICE_URL", "http://localhost:9001").rstrip("/")
    await _ingest_docs_to_ai_service(
        docs=[
            {
                "title": filename,
                "content": text,
                "category": "scientific_publication",
                "source_type": "admin_document",
                "source_id": source_id,
                "source_scope": "global",
                "owner_user_id": None,
                "weight": normalized_weight,
            }
        ],
        ai_service_url=ai_service_url,
        clear_global_first=False,
    )

    return UserDocumentUploadResponse(status="success", source_id=source_id, title=filename)


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
    async with pool.acquire() as conn:
        product = await conn.fetchrow("SELECT id FROM products WHERE id=$1", product_id)
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


@app.get("/api/products/{product_id:int}/photos")
async def get_product_photos(product_id: int):
    async with pool.acquire() as conn:
        rows = await conn.fetch(
            "SELECT id, filename FROM product_photos WHERE product_id=$1 ORDER BY id",
            product_id
        )
        photos = []
        base_dir = os.path.dirname(os.path.abspath(__file__))
        uploads_dir = os.path.join(base_dir, "product_photos")
        for row in rows:
            filename = row["filename"]
            file_path = os.path.join(uploads_dir, filename)
            if os.path.exists(file_path):
                with open(file_path, 'rb') as f:
                    import base64
                    data = base64.b64encode(f.read()).decode()
                    ext = filename.split('.')[-1].lower()
                    content_type_map = {
                        'jpg': 'image/jpeg', 'jpeg': 'image/jpeg',
                        'png': 'image/png', 'webp': 'image/webp', 'gif': 'image/gif'
                    }
                    content_type = content_type_map.get(ext, 'image/jpeg')
                    photos.append({
                        "id": row["id"],
                        "filename": filename,
                        "content_type": content_type,
                        "data": data
                    })
        return photos


@app.get("/api/products/{product_id:int}/photos/{photo_id:int}")
async def serve_product_photo(product_id: int, photo_id: int):
    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            "SELECT filename FROM product_photos WHERE id=$1 AND product_id=$2",
            photo_id, product_id
        )
        if not row:
            raise HTTPException(status_code=404, detail="Photo not found")
    
    base_dir = os.path.dirname(os.path.abspath(__file__))
    file_path = os.path.join(base_dir, "product_photos", row["filename"])
    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="Photo file not found")
    
    ext = row["filename"].split('.')[-1].lower()
    content_type_map = {
        'jpg': 'image/jpeg', 'jpeg': 'image/jpeg',
        'png': 'image/png', 'webp': 'image/webp', 'gif': 'image/gif'
    }
    content_type = content_type_map.get(ext, 'image/jpeg')
    
    with open(file_path, 'rb') as f:
        return Response(content=f.read(), media_type=content_type)


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
    tags_json = json.dumps(content.tags, ensure_ascii=False) if isinstance(content.tags, list) else content.tags
    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            """INSERT INTO content (title, category, tags, author_id, author_name, body, image_url, published) 
               VALUES ($1, $2, $3, $4, $5, $6, $7, $8) RETURNING *""",
            content.title, content.category, tags_json, content.author_id, 
            content.author_name, content.body, content.image_url, content.published
        )
        return dict(row)


@app.put("/api/content/{content_id}")
async def update_content(content_id: int, content: ContentCreate):
    tags_json = json.dumps(content.tags, ensure_ascii=False) if isinstance(content.tags, list) else content.tags

    def pick(next_value, current_value):
        return current_value if next_value is None else next_value

    async with pool.acquire() as conn:
        existing = await conn.fetchrow("SELECT * FROM content WHERE id=$1", content_id)
        if not existing:
            raise HTTPException(status_code=404, detail="Content not found")
        row = await conn.fetchrow(
            """UPDATE content SET title=$1, category=$2, tags=$3, author_id=$4, author_name=$5, 
               body=$6, image_url=$7, published=$8 WHERE id=$9 RETURNING *""",
            pick(content.title, existing['title']),
            pick(content.category, existing['category']),
            tags_json if content.tags is not None else existing['tags'],
            pick(content.author_id, existing['author_id']),
            pick(content.author_name, existing['author_name']),
            pick(content.body, existing['body']),
            pick(content.image_url, existing['image_url']),
            content.published if content.published is not None else existing['published'],
            content_id
        )
        return dict(row)


@app.delete("/api/content/{content_id}")
async def delete_content(content_id: int):
    async with pool.acquire() as conn:
        await conn.execute("DELETE FROM content WHERE id=$1", content_id)
        return {"success": True}


@app.post("/api/content/{content_id}/images")
async def upload_content_image(content_id: int, file: UploadFile = File(...)):
    async with pool.acquire() as conn:
        content_item = await conn.fetchrow("SELECT id FROM content WHERE id=$1", content_id)
        if not content_item:
            raise HTTPException(status_code=404, detail="Content not found")
    
    base_dir = os.path.dirname(os.path.abspath(__file__))
    uploads_dir = os.path.join(base_dir, "content_images")
    os.makedirs(uploads_dir, exist_ok=True)
    
    file_ext = file.filename.split('.')[-1] if '.' in file.filename else 'jpg'
    filename = f"{content_id}_{uuid.uuid4()}.{file_ext}"
    file_path = os.path.join(uploads_dir, filename)
    
    content = await file.read()
    with open(file_path, 'wb') as f:
        f.write(content)
    
    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            "INSERT INTO content_images (content_id, filename) VALUES ($1, $2) RETURNING *",
            content_id, filename
        )
        return {"id": row["id"], "filename": row["filename"]}


@app.delete("/api/content/{content_id}/images/{image_id}")
async def delete_content_image(content_id: int, image_id: int):
    async with pool.acquire() as conn:
        row = await conn.fetchrow(
            "SELECT filename FROM content_images WHERE id=$1 AND content_id=$2",
            image_id, content_id
        )
        if row:
            base_dir = os.path.dirname(os.path.abspath(__file__))
            file_path = os.path.join(base_dir, "content_images", row["filename"])
            if os.path.exists(file_path):
                os.remove(file_path)
            await conn.execute("DELETE FROM content_images WHERE id=$1", image_id)
    return {"success": True}


@app.get("/api/content/{content_id}/images")
async def get_content_images(content_id: int):
    async with pool.acquire() as conn:
        rows = await conn.fetch(
            "SELECT id, filename FROM content_images WHERE content_id=$1 ORDER BY id",
            content_id
        )
        base_dir = os.path.dirname(os.path.abspath(__file__))
        images = []
        for row in rows:
            file_path = os.path.join(base_dir, "content_images", row["filename"])
            if os.path.exists(file_path):
                with open(file_path, 'rb') as f:
                    import base64
                    data = base64.b64encode(f.read()).decode()
                    ext = row["filename"].split('.')[-1]
                    content_type = f"image/{ext}" if ext in ['jpg', 'jpeg', 'png', 'gif', 'webp'] else "image/jpeg"
                    images.append({
                        "id": row["id"],
                        "filename": row["filename"],
                        "data": data,
                        "content_type": content_type
                    })
        return images


@app.get("/api/content/{content_id}/image-url")
async def get_content_image_url(content_id: int):
    return {"url": f"/api/content/{content_id}/images"}


@app.post("/api/content/{content_id}/card-image")
async def upload_content_card_image(content_id: int, file: UploadFile = File(...)):
    async with pool.acquire() as conn:
        content_item = await conn.fetchrow("SELECT id FROM content WHERE id=$1", content_id)
        if not content_item:
            raise HTTPException(status_code=404, detail="Content not found")
    
    base_dir = os.path.dirname(os.path.abspath(__file__))
    uploads_dir = os.path.join(base_dir, "content_card_images")
    os.makedirs(uploads_dir, exist_ok=True)
    
    file_ext = file.filename.split('.')[-1] if '.' in file.filename else 'jpg'
    filename = f"{content_id}_{uuid.uuid4()}.{file_ext}"
    file_path = os.path.join(uploads_dir, filename)
    
    content_data = await file.read()
    with open(file_path, 'wb') as f:
        f.write(content_data)
    
    async with pool.acquire() as conn:
        await conn.execute(
            "UPDATE content SET image_url=$1 WHERE id=$2",
            f"/api/content/card-image/{filename}",
            content_id
        )
    return {"success": True, "filename": filename, "url": f"/api/content/card-image/{filename}"}


@app.get("/api/content/card-image/{filename}")
async def get_content_card_image(filename: str):
    base_dir = os.path.dirname(os.path.abspath(__file__))
    file_path = os.path.join(base_dir, "content_card_images", filename)
    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="Image not found")
    
    with open(file_path, 'rb') as f:
        content_data = f.read()
    
    ext = filename.split('.')[-1]
    content_type = f"image/{ext}" if ext in ['jpg', 'jpeg', 'png', 'gif', 'webp'] else "image/jpeg"
    return Response(content=content_data, media_type=content_type)


@app.delete("/api/content/{content_id}/card-image")
async def delete_content_card_image(content_id: int):
    async with pool.acquire() as conn:
        row = await conn.fetchrow("SELECT image_url FROM content WHERE id=$1", content_id)
        if row and row['image_url'] and row['image_url'].startswith('/api/content/card-image/'):
            filename = row['image_url'].split('/')[-1]
            base_dir = os.path.dirname(os.path.abspath(__file__))
            file_path = os.path.join(base_dir, "content_card_images", filename)
            if os.path.exists(file_path):
                os.remove(file_path)
            await conn.execute("UPDATE content SET image_url=NULL WHERE id=$1", content_id)
    return {"success": True}


@app.get("/api/users")
async def get_users(role: Optional[str] = None):
    async with pool.acquire() as conn:
        await ensure_users_columns(conn)
        if role and role != "all":
            rows = await conn.fetch("SELECT * FROM users WHERE role=$1 ORDER BY id DESC", role)
        else:
            rows = await conn.fetch("SELECT * FROM users ORDER BY id DESC")
        return [dict(row) for row in rows]


@app.post("/api/users")
async def create_user(user: UserCreate):
    login = normalize_login(user.nickname)
    if not is_valid_email(user.email):
        raise HTTPException(status_code=400, detail="Некорректный email")
    if login and not is_valid_login(login):
        raise HTTPException(status_code=400, detail="Логин должен быть в формате @login, только a-z, 0-9 и _")

    async with pool.acquire() as conn:
        await ensure_users_columns(conn)
        await ensure_unique_login(conn, login)
        
        # Hash password if provided
        password_hash = None
        if user.password:
            password_hash = get_password_hash(user.password)
        
        row = await conn.fetchrow(
            """INSERT INTO users (name, email, role, nickname, avatar, password_hash) 
               VALUES ($1, $2, $3, $4, $5, $6) RETURNING *""",
            user.name, user.email, user.role, login, user.avatar, password_hash
        )
        return dict(row)


@app.put("/api/users/{user_id}")
async def update_user(user_id: int, user: UserCreate):
    login = normalize_login(user.nickname)
    if not is_valid_email(user.email):
        raise HTTPException(status_code=400, detail="Некорректный email")
    if not is_valid_login(login):
        raise HTTPException(status_code=400, detail="Логин должен быть в формате @login, только a-z, 0-9 и _")

    async with pool.acquire() as conn:
        await ensure_users_columns(conn)
        await ensure_unique_login(conn, login, user_id)
        row = await conn.fetchrow(
            """UPDATE users SET name=$1, email=$2, role=$3, nickname=$4, avatar=$5
               WHERE id=$6 RETURNING *""",
            user.name, user.email, user.role, login, user.avatar, user_id
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
    # Run directly - uvicorn handles the async loop
    uvicorn.run("main:app", host="127.0.0.1", port=3002, reload=False, loop="asyncio")
