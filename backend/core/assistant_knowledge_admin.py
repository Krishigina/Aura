import json
from typing import Dict, List

import aiohttp

from fastapi import HTTPException

from backend.core.config import AI_SERVICE_URL


REINDEX_BATCH_SIZE = 5


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


async def reindex_knowledge_sources_to_ai(conn):
    await ensure_assistant_knowledge_schema(conn)
    rows = await conn.fetch(
        """
        SELECT id, title, source_type, scope, weight, content
        FROM knowledge_sources
        WHERE enabled = true
          AND owner_user_id IS NULL
          AND COALESCE(content, '') <> ''
        ORDER BY id ASC
        """
    )
    documents = [build_reindex_document(row) for row in rows]
    await delete_ai_knowledge()

    vector_documents_indexed = 0
    batches = 0
    for batch_start in range(0, len(documents), REINDEX_BATCH_SIZE):
        batch = documents[batch_start:batch_start + REINDEX_BATCH_SIZE]
        result = await ingest_ai_knowledge_documents(batch)
        vector_documents_indexed += int(result.get("indexed_count") or 0)
        batches += 1

    return {
        "indexed_count": len(documents),
        "vector_documents_indexed": vector_documents_indexed,
        "batches": batches,
    }


def build_reindex_document(row) -> Dict:
    return {
        "title": str(row["title"] or "").strip() or f"knowledge-source-{row['id']}",
        "content": str(row["content"] or "").strip(),
        "category": str(row["source_type"] or "knowledge_source"),
        "source_type": "knowledge_source",
        "source_id": int(row["id"]),
        "source_scope": str(row["scope"] or "global"),
        "weight": float(row["weight"] or 1.0),
    }


async def delete_ai_knowledge() -> Dict:
    timeout = aiohttp.ClientTimeout(total=300)
    async with aiohttp.ClientSession(timeout=timeout) as session:
        async with session.delete(f"{AI_SERVICE_URL}/api/v1/rag/knowledge") as response:
            response_text = await response.text()
            if response.status >= 400:
                raise HTTPException(status_code=502, detail="AI reindex delete failed")
            return {"status": response.status, "body": response_text}


async def ingest_ai_knowledge_documents(documents: List[Dict]) -> Dict:
    timeout = aiohttp.ClientTimeout(total=600)
    async with aiohttp.ClientSession(timeout=timeout) as session:
        async with session.post(f"{AI_SERVICE_URL}/api/v1/rag/ingest", json=documents) as response:
            response_text = await response.text()
            if response.status >= 400:
                raise HTTPException(status_code=502, detail="AI reindex ingest failed")
            return json.loads(response_text) if response_text.strip() else {}


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
