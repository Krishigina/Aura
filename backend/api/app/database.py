import os
import asyncpg

_pool: asyncpg.Pool = None


def get_pool():
    return _pool


async def get_db_pool():
    return _pool


async def init_db():
    global _pool
    _pool = await asyncpg.create_pool(
        host=os.getenv("DB_HOST", "localhost"),
        port=int(os.getenv("DB_PORT", "5432")),
        database=os.getenv("DB_NAME", "aura"),
        user=os.getenv("DB_USER", "aura_user"),
        password=os.getenv("DB_PASSWORD", "aura_password"),
        command_timeout=60
    )

    async with _pool.acquire() as conn:
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

        await _seed_dictionary(conn, "brands", [
            'Aura', 'La Roche-Posay', 'Vichy', 'Bioderma', 'CeraVe', 
            'The Ordinary', "Paula's Choice", 'Cosrx', 'Eucerin', 'Nivea'
        ])
        await _seed_dictionary(conn, "categories", [
            'Очищение', 'Увлажнение', 'Сыворотки', 'SPF', 'Уход', 'Маска', 'Тоник', 'Крем', 'Масло'
        ])
        await _seed_dictionary(conn, "segments", [
            'Бюджетная', 'Люкс', 'Профессиональная', 'Космецевтика'
        ])
        await _seed_dictionary(conn, "volumes", [
            '15мл', '30мл', '50мл', '75мл', '100мл', '150мл', '200мл', '250мл', '500мл', '1л'
        ])
        await _seed_dictionary(conn, "procedure_categories", [
            'Чистка', 'Увлажнение', 'Инъекции', 'Эпиляция', 'Массаж', 'Пилинг', 'Уход'
        ])
        await _seed_dictionary(conn, "content_categories", [
            'Уход за кожей', 'Ингредиенты', 'Защита', 'Процедуры', 'Питание', 'Образ жизни'
        ])
        await _seed_dictionary(conn, "user_roles", [
            'Пользователь', 'Косметолог', 'Менеджер', 'Администратор'
        ])
        await _seed_dictionary(conn, "skin_types", [
            'Нормальная', 'Сухая', 'Жирная', 'Комбинированная', 'Чувствительная'
        ])

        print("Database initialized successfully")


async def _seed_dictionary(conn, table: str, values: list):
    count = await conn.fetchval(f"SELECT COUNT(*) FROM {table}")
    if count == 0:
        for value in values:
            await conn.execute(f"INSERT INTO {table} (value) VALUES ($1)", value)
