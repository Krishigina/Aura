MAIN_SCHEMA_SQL = """
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

ALTER TABLE products ADD COLUMN IF NOT EXISTS what_is_it VARCHAR(255);
ALTER TABLE products ADD COLUMN IF NOT EXISTS brand VARCHAR(255);
ALTER TABLE products ADD COLUMN IF NOT EXISTS category VARCHAR(255);
ALTER TABLE products ADD COLUMN IF NOT EXISTS volume VARCHAR(50);
ALTER TABLE products ADD COLUMN IF NOT EXISTS segment VARCHAR(50);
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
ALTER TABLE products ADD COLUMN IF NOT EXISTS brand_id INTEGER;
ALTER TABLE products ADD COLUMN IF NOT EXISTS category_id INTEGER;
ALTER TABLE products ADD COLUMN IF NOT EXISTS segment_id INTEGER;
ALTER TABLE products ADD COLUMN IF NOT EXISTS volume_id INTEGER;
ALTER TABLE products ADD COLUMN IF NOT EXISTS product_type_id INTEGER;
ALTER TABLE products ADD COLUMN IF NOT EXISTS for_whom_id INTEGER;
ALTER TABLE products ADD COLUMN IF NOT EXISTS application_time_id INTEGER;
ALTER TABLE products ADD COLUMN IF NOT EXISTS area_id INTEGER;
ALTER TABLE products ADD COLUMN IF NOT EXISTS country_id INTEGER;

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

ALTER TABLE procedures ADD COLUMN IF NOT EXISTS contraindications_full TEXT;
ALTER TABLE procedures ADD COLUMN IF NOT EXISTS direction_id INTEGER;
ALTER TABLE procedures ADD COLUMN IF NOT EXISTS method_type_id INTEGER;
ALTER TABLE procedures ADD COLUMN IF NOT EXISTS duration_id INTEGER;
ALTER TABLE procedures ADD COLUMN IF NOT EXISTS equipment_id INTEGER;
ALTER TABLE procedures ADD COLUMN IF NOT EXISTS for_whom_id INTEGER;
ALTER TABLE users ADD COLUMN IF NOT EXISTS role_id INTEGER;
ALTER TABLE content ADD COLUMN IF NOT EXISTS category_id INTEGER;

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

CREATE TABLE IF NOT EXISTS content_tags (
    id SERIAL PRIMARY KEY,
    value VARCHAR(255) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS content_tag_links (
    content_id INTEGER NOT NULL REFERENCES content(id) ON DELETE CASCADE,
    tag_id INTEGER NOT NULL REFERENCES content_tags(id) ON DELETE CASCADE,
    PRIMARY KEY (content_id, tag_id)
);

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
);

CREATE TABLE IF NOT EXISTS ingredients (
    id SERIAL PRIMARY KEY,
    canonical_name VARCHAR(255) NOT NULL,
    normalized_key VARCHAR(255) UNIQUE NOT NULL,
    inci_name VARCHAR(255),
    aliases JSONB DEFAULT '[]'::jsonb,
    category VARCHAR(100),
    verification_status VARCHAR(50) DEFAULT 'auto_created',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS ingredient_aliases (
    id SERIAL PRIMARY KEY,
    ingredient_id INTEGER NOT NULL REFERENCES ingredients(id) ON DELETE CASCADE,
    alias VARCHAR(255) NOT NULL,
    normalized_key VARCHAR(255) NOT NULL,
    language VARCHAR(20),
    source VARCHAR(50) DEFAULT 'extracted',
    confidence DOUBLE PRECISION DEFAULT 1.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (ingredient_id, normalized_key)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_ingredient_aliases_normalized_key
ON ingredient_aliases(normalized_key);

CREATE TABLE IF NOT EXISTS ingredient_evidence (
    id SERIAL PRIMARY KEY,
    ingredient_id INTEGER REFERENCES ingredients(id) ON DELETE CASCADE,
    ingredient_key VARCHAR(255) NOT NULL,
    fact_type VARCHAR(50),
    effect_key VARCHAR(100) NOT NULL,
    skin_condition_key VARCHAR(100),
    condition_type VARCHAR(100),
    condition_value VARCHAR(255),
    direction VARCHAR(50) NOT NULL DEFAULT 'benefit',
    strength VARCHAR(50) NOT NULL DEFAULT 'medium',
    summary TEXT NOT NULL DEFAULT '',
    matching_effect VARCHAR(50),
    matching_weight_delta DOUBLE PRECISION,
    matching_condition_type VARCHAR(100),
    status VARCHAR(50) DEFAULT 'draft',
    evidence_status VARCHAR(50) DEFAULT 'draft',
    confidence DOUBLE PRECISION DEFAULT 0,
    source_id INTEGER REFERENCES knowledge_sources(id) ON DELETE SET NULL,
    evidence_quote TEXT DEFAULT '',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS product_ingredients (
    product_id INTEGER NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    ingredient_id INTEGER NOT NULL REFERENCES ingredients(id) ON DELETE CASCADE,
    raw_name TEXT NOT NULL,
    position INTEGER NOT NULL,
    confidence DOUBLE PRECISION DEFAULT 1.0,
    created_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (product_id, ingredient_id, position)
);

CREATE TABLE IF NOT EXISTS product_function_profiles (
    product_id INTEGER NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    function_key VARCHAR(100) NOT NULL,
    score DOUBLE PRECISION NOT NULL DEFAULT 0,
    evidence_status VARCHAR(50) DEFAULT 'auto_only',
    evidence_count INTEGER DEFAULT 0,
    source_ids JSONB DEFAULT '[]'::jsonb,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (product_id, function_key)
);

CREATE TABLE IF NOT EXISTS matching_rules (
    id SERIAL PRIMARY KEY,
    rule_type VARCHAR(50) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id INTEGER,
    target_key VARCHAR(255),
    condition_type VARCHAR(100) NOT NULL,
    condition_value TEXT NOT NULL,
    effect VARCHAR(50) NOT NULL,
    weight_delta DOUBLE PRECISION DEFAULT 0,
    severity VARCHAR(50) DEFAULT 'info',
    source_id INTEGER REFERENCES knowledge_sources(id) ON DELETE SET NULL,
    evidence_quote TEXT DEFAULT '',
    confidence DOUBLE PRECISION DEFAULT 1.0,
    status VARCHAR(50) DEFAULT 'draft',
    reviewed_by INTEGER REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS passport_update_suggestions (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    suggestion_type VARCHAR(50) NOT NULL,
    target_field VARCHAR(100),
    old_value JSONB,
    proposed_value JSONB NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_message_id INTEGER REFERENCES chat_messages(id) ON DELETE SET NULL,
    evidence_text TEXT DEFAULT '',
    confidence DOUBLE PRECISION DEFAULT 0,
    conflict_status VARCHAR(50) DEFAULT 'none',
    status VARCHAR(50) DEFAULT 'proposed',
    created_at TIMESTAMP DEFAULT NOW(),
    accepted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_profiles (
    id SERIAL PRIMARY KEY,
    user_id INTEGER UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    extra_data JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS recommendation_feedback (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recommendation_id VARCHAR(100) NOT NULL,
    product_id VARCHAR(100) NOT NULL,
    rank INTEGER CHECK (rank IS NULL OR rank > 0),
    action VARCHAR(50) NOT NULL,
    algorithm_version VARCHAR(100) DEFAULT 'backend-matching-v1',
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS chat_sessions (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(80) NOT NULL DEFAULT 'Новый чат',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS chat_messages (
    id SERIAL PRIMARY KEY,
    session_id INTEGER NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    text TEXT NOT NULL,
    is_from_user BOOLEAN DEFAULT true,
    timestamp VARCHAR(32),
    created_at TIMESTAMP DEFAULT NOW()
);

ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS role VARCHAR(32);
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS content TEXT;
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS sources JSONB;

CREATE TABLE IF NOT EXISTS chat_attachments (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_id INTEGER NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    filename TEXT NOT NULL,
    content_type TEXT NOT NULL,
    storage_path TEXT NOT NULL,
    extracted_text TEXT,
    summary TEXT,
    indexed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chat_sessions_user_updated
    ON chat_sessions(user_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_messages_session_created
    ON chat_messages(session_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_chat_attachments_session_created
    ON chat_attachments(session_id, created_at ASC);

CREATE TABLE IF NOT EXISTS brands (id SERIAL PRIMARY KEY, value VARCHAR(255) UNIQUE NOT NULL);
CREATE TABLE IF NOT EXISTS categories (id SERIAL PRIMARY KEY, value VARCHAR(255) UNIQUE NOT NULL);
CREATE TABLE IF NOT EXISTS segments (id SERIAL PRIMARY KEY, value VARCHAR(255) UNIQUE NOT NULL);
CREATE TABLE IF NOT EXISTS volumes (id SERIAL PRIMARY KEY, value VARCHAR(255) UNIQUE NOT NULL);
CREATE TABLE IF NOT EXISTS procedure_categories (id SERIAL PRIMARY KEY, value VARCHAR(255) UNIQUE NOT NULL);
CREATE TABLE IF NOT EXISTS content_categories (id SERIAL PRIMARY KEY, value VARCHAR(255) UNIQUE NOT NULL);
CREATE TABLE IF NOT EXISTS user_roles (id SERIAL PRIMARY KEY, value VARCHAR(255) UNIQUE NOT NULL);
CREATE TABLE IF NOT EXISTS skin_types (id SERIAL PRIMARY KEY, value VARCHAR(255) UNIQUE NOT NULL);
CREATE TABLE IF NOT EXISTS product_types (id SERIAL PRIMARY KEY, value VARCHAR(255) UNIQUE NOT NULL);
CREATE TABLE IF NOT EXISTS for_whom (id SERIAL PRIMARY KEY, value VARCHAR(255) UNIQUE NOT NULL);
CREATE TABLE IF NOT EXISTS purposes (id SERIAL PRIMARY KEY, value VARCHAR(255) UNIQUE NOT NULL);
CREATE TABLE IF NOT EXISTS application_times (id SERIAL PRIMARY KEY, value VARCHAR(255) UNIQUE NOT NULL);
CREATE TABLE IF NOT EXISTS areas (id SERIAL PRIMARY KEY, value VARCHAR(255) UNIQUE NOT NULL);
CREATE TABLE IF NOT EXISTS countries (id SERIAL PRIMARY KEY, value VARCHAR(255) UNIQUE NOT NULL);

CREATE TABLE IF NOT EXISTS procedure_method_types (id SERIAL PRIMARY KEY, value VARCHAR(255) UNIQUE NOT NULL);
CREATE TABLE IF NOT EXISTS procedure_durations (id SERIAL PRIMARY KEY, value VARCHAR(255) UNIQUE NOT NULL);
CREATE TABLE IF NOT EXISTS procedure_zones (id SERIAL PRIMARY KEY, value VARCHAR(255) UNIQUE NOT NULL);
CREATE TABLE IF NOT EXISTS procedure_equipment (id SERIAL PRIMARY KEY, value VARCHAR(255) UNIQUE NOT NULL);
CREATE TABLE IF NOT EXISTS procedure_effects (id SERIAL PRIMARY KEY, value VARCHAR(255) UNIQUE NOT NULL);
CREATE TABLE IF NOT EXISTS procedure_problems (id SERIAL PRIMARY KEY, value VARCHAR(255) UNIQUE NOT NULL);

CREATE TABLE IF NOT EXISTS product_purpose_links (
    product_id INTEGER NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    purpose_id INTEGER NOT NULL REFERENCES purposes(id) ON DELETE CASCADE,
    PRIMARY KEY (product_id, purpose_id)
);

CREATE TABLE IF NOT EXISTS product_skin_type_links (
    product_id INTEGER NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    skin_type_id INTEGER NOT NULL REFERENCES skin_types(id) ON DELETE CASCADE,
    PRIMARY KEY (product_id, skin_type_id)
);

CREATE TABLE IF NOT EXISTS procedure_zone_links (
    procedure_id INTEGER NOT NULL REFERENCES procedures(id) ON DELETE CASCADE,
    zone_id INTEGER NOT NULL REFERENCES procedure_zones(id) ON DELETE CASCADE,
    PRIMARY KEY (procedure_id, zone_id)
);

CREATE TABLE IF NOT EXISTS procedure_effect_links (
    procedure_id INTEGER NOT NULL REFERENCES procedures(id) ON DELETE CASCADE,
    effect_id INTEGER NOT NULL REFERENCES procedure_effects(id) ON DELETE CASCADE,
    PRIMARY KEY (procedure_id, effect_id)
);

CREATE TABLE IF NOT EXISTS procedure_problem_links (
    procedure_id INTEGER NOT NULL REFERENCES procedures(id) ON DELETE CASCADE,
    problem_id INTEGER NOT NULL REFERENCES procedure_problems(id) ON DELETE CASCADE,
    PRIMARY KEY (procedure_id, problem_id)
);
"""
