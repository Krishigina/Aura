from backend.db.schema_specs import KNOWLEDGE_SCHEMA_ALTERS


async def ensure_knowledge_schema(conn):
    await conn.execute(
        """
        CREATE TABLE IF NOT EXISTS knowledge_sources (
            id SERIAL PRIMARY KEY,
            title VARCHAR(255) NOT NULL,
            filename VARCHAR(255),
            source_type VARCHAR(50) NOT NULL,
            owner_user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
            scope VARCHAR(50) DEFAULT 'both',
            weight DOUBLE PRECISION DEFAULT 1.0,
            enabled BOOLEAN DEFAULT true,
            content TEXT DEFAULT '',
            created_at TIMESTAMP DEFAULT NOW(),
            updated_at TIMESTAMP DEFAULT NOW()
        )
        """
    )
    await conn.execute(
        """
        CREATE TABLE IF NOT EXISTS ingredient_evidence (
            id SERIAL PRIMARY KEY,
            ingredient_key VARCHAR(255) NOT NULL,
            effect_key VARCHAR(100) NOT NULL,
            skin_condition_key VARCHAR(100),
            direction VARCHAR(50),
            strength VARCHAR(50),
            summary TEXT,
            evidence_quote TEXT DEFAULT '',
            source_id INTEGER REFERENCES knowledge_sources(id) ON DELETE SET NULL,
            matching_effect VARCHAR(50),
            matching_weight_delta DOUBLE PRECISION,
            matching_condition_type VARCHAR(100),
            status VARCHAR(50) DEFAULT 'draft',
            created_at TIMESTAMP DEFAULT NOW(),
            updated_at TIMESTAMP DEFAULT NOW()
        )
        """
    )
    for statement in KNOWLEDGE_SCHEMA_ALTERS:
        await conn.execute(statement)
