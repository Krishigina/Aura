from typing import Dict

from fastapi import HTTPException


async def ensure_assistant_knowledge_schema(conn):
    await conn.execute("""
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
    """)
    await conn.execute("ALTER TABLE knowledge_sources ADD COLUMN IF NOT EXISTS filename VARCHAR(255)")
    await conn.execute("ALTER TABLE knowledge_sources ADD COLUMN IF NOT EXISTS content TEXT DEFAULT ''")
    await conn.execute("ALTER TABLE knowledge_sources ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT NOW()")


async def list_knowledge_sources_for_admin(conn, knowledge_source_response):
    await ensure_assistant_knowledge_schema(conn)
    rows = await conn.fetch("""
        SELECT id, title, filename, source_type, owner_user_id, scope, weight, enabled, created_at, updated_at
        FROM knowledge_sources
        ORDER BY created_at DESC, id DESC
    """)
    return {"items": [knowledge_source_response(row) for row in rows]}


async def update_knowledge_source_for_admin(
    conn,
    source_id: int,
    payload: Dict,
    *,
    normalize_knowledge_scope,
    knowledge_source_response,
):
    updates = extract_knowledge_source_updates(payload, normalize_knowledge_scope)
    await ensure_assistant_knowledge_schema(conn)
    row = await conn.fetchrow(
        f"""
        UPDATE knowledge_sources
        SET {', '.join(build_update_set_parts(updates))}, updated_at=NOW()
        WHERE id=${len(updates) + 1}
        RETURNING id, title, filename, source_type, owner_user_id, scope, weight, enabled, created_at, updated_at
        """,
        *build_update_values(updates, source_id),
    )
    if not row:
        raise HTTPException(status_code=404, detail="Источник знаний не найден")
    return knowledge_source_response(row)


async def create_admin_knowledge_document(
    conn,
    *,
    filename: str,
    content: bytes,
    scope: str,
    weight: float,
    normalize_knowledge_scope,
    get_knowledge_source_type,
    extract_knowledge_text,
    knowledge_source_response,
):
    normalized_scope = normalize_knowledge_scope(scope)
    text, source_type = extract_knowledge_document_payload(
        filename=filename,
        content=content,
        get_knowledge_source_type=get_knowledge_source_type,
        extract_knowledge_text=extract_knowledge_text,
    )
    await ensure_assistant_knowledge_schema(conn)
    row = await conn.fetchrow(
        """
        INSERT INTO knowledge_sources (title, filename, source_type, owner_user_id, scope, weight, enabled, content)
        VALUES ($1, $2, $3, NULL, $4, $5, true, $6)
        RETURNING id, title, filename, source_type, owner_user_id, scope, weight, enabled, created_at, updated_at
        """,
        filename, filename, source_type, normalized_scope, float(weight), text,
    )
    return knowledge_source_response(row)


async def create_user_knowledge_document(
    conn,
    *,
    filename: str,
    content: bytes,
    user_id: int,
    get_knowledge_source_type,
    extract_knowledge_text,
    knowledge_source_response,
):
    text, source_type = extract_knowledge_document_payload(
        filename=filename,
        content=content,
        get_knowledge_source_type=get_knowledge_source_type,
        extract_knowledge_text=extract_knowledge_text,
    )
    await ensure_assistant_knowledge_schema(conn)
    row = await conn.fetchrow(
        """
        INSERT INTO knowledge_sources (title, filename, source_type, owner_user_id, scope, weight, enabled, content)
        VALUES ($1, $2, $3, $4, 'both', 1.0, true, $5)
        RETURNING id, title, filename, source_type, owner_user_id, scope, weight, enabled, created_at, updated_at
        """,
        filename, filename, source_type, user_id, text,
    )
    return knowledge_source_response(row)


async def count_reindexable_knowledge_sources(conn):
    await ensure_assistant_knowledge_schema(conn)
    indexed_count = await conn.fetchval("""
        SELECT COUNT(*)
        FROM knowledge_sources
        WHERE enabled = true AND owner_user_id IS NULL
    """)
    return {"indexed_count": indexed_count or 0}


def extract_knowledge_document_payload(*, filename: str, content: bytes, get_knowledge_source_type, extract_knowledge_text):
    if not content:
        raise HTTPException(status_code=400, detail="Файл пустой")
    source_type = get_knowledge_source_type(filename)
    text = extract_knowledge_text(filename, content)
    if not text:
        raise HTTPException(status_code=400, detail="Не удалось извлечь текст из документа")
    return text, source_type


def extract_knowledge_source_updates(payload: Dict, normalize_knowledge_scope):
    allowed = {"scope", "weight", "enabled"}
    updates = {key: payload[key] for key in allowed if key in payload}
    if not updates:
        raise HTTPException(status_code=400, detail="Нет данных для обновления")
    if "scope" in updates:
        updates["scope"] = normalize_knowledge_scope(updates["scope"])
    if "weight" in updates:
        try:
            updates["weight"] = float(updates["weight"])
        except (TypeError, ValueError) as error:
            raise HTTPException(status_code=400, detail="Некорректный вес источника") from error
    if "enabled" in updates:
        updates["enabled"] = bool(updates["enabled"])
    return updates


def build_update_set_parts(updates: Dict):
    return [f"{key}=${index}" for index, key in enumerate(updates.keys(), start=1)]


def build_update_values(updates: Dict, source_id: int):
    return [*updates.values(), source_id]
