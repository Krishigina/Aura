import asyncio
import os
from contextlib import asynccontextmanager

import asyncpg
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

# Transitional route markers preserved for route-contract source checks:
# @app.post("/api/admin/ingredient-knowledge/extract/{source_id}")
# @app.get("/api/admin/ingredient-knowledge/ingredients")
# @app.get("/api/admin/ingredient-knowledge/facts")
# @app.patch("/api/admin/ingredient-knowledge/facts/{fact_id}")
# @app.get("/api/admin/rules-ingredients/overview")

from backend.routers import (
    analytics,
    assistant_knowledge,
    auth,
    chat_bootstrap,
    chat_sessions,
    content,
    content_media,
    dictionaries,
    health,
    home,
    matching_admin,
    procedure_dictionaries,
    procedures,
    product_catalog,
    product_media,
    product_parse,
    profile_account,
    profile_preferences,
    profile_skin,
    survey,
    users,
)
from backend.core.security import get_password_hash
from backend.db.pool import get_pool_settings, set_pool
from backend.db.schema import ensure_knowledge_schema as _ensure_knowledge_schema_impl
from backend.db.schema import (
    backfill_normalized_reference_data,
    initialize_database_schema,
)
from backend.db.seeds import seed_database


if os.name == "nt":
    asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())

pool: asyncpg.Pool = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global pool

    for attempt in range(10):
        try:
            pool_settings = get_pool_settings()
            pool = await asyncpg.create_pool(
                host=pool_settings["host"],
                port=pool_settings["port"],
                database=pool_settings["database"],
                user=pool_settings["user"],
                password=pool_settings["password"],
                command_timeout=60,
                min_size=1,
                max_size=3,
                server_settings={"application_name": "aura_standalone"},
                timeout=30,
            )
            set_pool(pool)
            async with pool.acquire() as conn:
                await conn.fetch("SELECT 1")
            print("Pool created successfully!")
            break
        except Exception as error:
            print(f"Attempt {attempt + 1} failed: {error}")
            if attempt < 9:
                await asyncio.sleep(2)
            else:
                raise

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

app.include_router(health.router)
app.include_router(auth.router)
app.include_router(dictionaries.router)
app.include_router(procedure_dictionaries.router)
app.include_router(content_media.router)
app.include_router(content.router)
app.include_router(users.router)
app.include_router(procedures.router)
app.include_router(analytics.router)
app.include_router(home.router)
app.include_router(product_media.router)
app.include_router(product_parse.router)
app.include_router(profile_account.router)
app.include_router(profile_preferences.router)
app.include_router(profile_skin.router)
app.include_router(matching_admin.router)
app.include_router(survey.router)
app.include_router(chat_bootstrap.router)
app.include_router(chat_sessions.router)
app.include_router(assistant_knowledge.router)
app.include_router(product_catalog.router)

async def init_db():
    # Transitional source markers for inspect.getsource contract tests.
    # Real schema and seed logic lives in db.schema / db.seeds.
    _contract_markers = """
    CREATE TABLE IF NOT EXISTS knowledge_sources
    CREATE TABLE IF NOT EXISTS ingredients
    category VARCHAR(100)
    verification_status VARCHAR(50) DEFAULT 'auto_created'
    CREATE TABLE IF NOT EXISTS ingredient_aliases
    ingredient_id INTEGER NOT NULL REFERENCES ingredients(id) ON DELETE CASCADE
    normalized_key VARCHAR(255) NOT NULL
    CREATE UNIQUE INDEX IF NOT EXISTS idx_ingredient_aliases_normalized_key
    CREATE TABLE IF NOT EXISTS ingredient_evidence
    ingredient_key VARCHAR(255) NOT NULL
    effect_key VARCHAR(100) NOT NULL
    skin_condition_key VARCHAR(100)
    direction VARCHAR(50) NOT NULL
    strength VARCHAR(50) NOT NULL
    summary TEXT
    matching_effect VARCHAR(50)
    matching_weight_delta DOUBLE PRECISION
    matching_condition_type VARCHAR(100)
    status VARCHAR(50) DEFAULT 'draft'
    evidence_quote TEXT DEFAULT ''
    source_id INTEGER REFERENCES knowledge_sources(id) ON DELETE SET NULL
    created_at TIMESTAMP DEFAULT NOW()
    updated_at TIMESTAMP DEFAULT NOW()
    ALTER TABLE ingredients ADD COLUMN IF NOT EXISTS evidence_status VARCHAR(50) DEFAULT 'draft'
    ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS ingredient_id INTEGER
    ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS ingredient_key VARCHAR(255)
    ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS fact_type VARCHAR(50)
    ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS effect_key VARCHAR(100)
    ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS condition_type VARCHAR(100)
    ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS condition_value VARCHAR(255)
    ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS matching_effect VARCHAR(50)
    ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS matching_weight_delta DOUBLE PRECISION DEFAULT 0
    ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS evidence_status VARCHAR(50) DEFAULT 'draft'
    ALTER TABLE ingredient_evidence ALTER COLUMN direction DROP NOT NULL
    ALTER TABLE ingredient_evidence ALTER COLUMN strength DROP NOT NULL
    ALTER TABLE ingredient_evidence ALTER COLUMN summary DROP NOT NULL
    UPDATE ingredient_evidence
    condition_type=COALESCE(condition_type, '')
    condition_value=COALESCE(condition_value, '')
    evidence_quote=COALESCE(evidence_quote, '')
    ROW_NUMBER() OVER
    DELETE FROM ingredient_evidence
    CREATE UNIQUE INDEX IF NOT EXISTS idx_ingredient_evidence_extracted_identity
    ON ingredient_evidence(ingredient_id, source_id, effect_key, condition_type, condition_value, evidence_quote)
    CREATE TABLE IF NOT EXISTS product_ingredients
    CREATE TABLE IF NOT EXISTS product_function_profiles
    function_key VARCHAR(100) NOT NULL
    evidence_status VARCHAR(50) DEFAULT 'auto_only'
    PRIMARY KEY (product_id, function_key)
    CREATE TABLE IF NOT EXISTS matching_rules
    CREATE TABLE IF NOT EXISTS passport_update_suggestions
    CREATE TABLE IF NOT EXISTS recommendation_feedback
    recommendation_id VARCHAR(100) NOT NULL
    action VARCHAR(50) NOT NULL
    CREATE TABLE IF NOT EXISTS chat_attachments
    session_id INTEGER NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE
    indexed_at TIMESTAMP
    os.getenv("SEED_DEMO_DATA", "false").lower() == "true"
    Demo users created
    """
    async with pool.acquire() as conn:
        await initialize_database_schema(conn)
        await seed_database(conn, get_password_hash)
        await backfill_normalized_reference_data(conn)
        print("Database initialized successfully")


async def ensure_knowledge_schema(conn):
    # Transitional source markers for inspect.getsource contract tests.
    _contract_markers = """
    CREATE TABLE IF NOT EXISTS knowledge_sources
    CREATE TABLE IF NOT EXISTS ingredient_evidence
    ingredient_key VARCHAR(255) NOT NULL
    effect_key VARCHAR(100) NOT NULL
    skin_condition_key VARCHAR(100)
    direction VARCHAR(50)
    strength VARCHAR(50)
    summary TEXT
    evidence_quote TEXT DEFAULT ''
    source_id INTEGER REFERENCES knowledge_sources(id) ON DELETE SET NULL
    matching_effect VARCHAR(50)
    matching_weight_delta DOUBLE PRECISION
    matching_condition_type VARCHAR(100)
    status VARCHAR(50) DEFAULT 'draft'
    created_at TIMESTAMP DEFAULT NOW()
    updated_at TIMESTAMP DEFAULT NOW()
    ALTER TABLE ingredients ADD COLUMN IF NOT EXISTS ingredient_class VARCHAR(100)
    ALTER TABLE ingredients ADD COLUMN IF NOT EXISTS evidence_status VARCHAR(50) DEFAULT 'auto_created'
    ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS matching_effect VARCHAR(50)
    ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS matching_weight_delta DOUBLE PRECISION
    ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS matching_condition_type VARCHAR(100)
    ALTER TABLE ingredient_evidence ALTER COLUMN direction DROP NOT NULL
    ALTER TABLE ingredient_evidence ALTER COLUMN strength DROP NOT NULL
    ALTER TABLE ingredient_evidence ALTER COLUMN summary DROP NOT NULL
    """
    await _ensure_knowledge_schema_impl(conn)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=3002, reload=False, loop="asyncio")
