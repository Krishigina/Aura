import json

from backend.core.chat import DEFAULT_CHAT_SESSION_TITLE, normalize_rag_source


def normalize_rag_sources(raw_sources):
    if not isinstance(raw_sources, list):
        raw_sources = []
    sources = [normalize_rag_source(item) for item in raw_sources if isinstance(item, dict)]
    return sources, [source.model_dump() for source in sources]


async def get_owned_chat_session_overview(conn, session_id: int, user_id: int):
    return await conn.fetchrow(
        """
        SELECT s.id, s.title, COUNT(m.id)::int AS message_count
        FROM chat_sessions s
        LEFT JOIN chat_messages m ON m.session_id = s.id
        WHERE s.id=$1 AND s.user_id=$2
        GROUP BY s.id, s.title
        """,
        session_id,
        user_id,
    )


async def load_recent_chat_history(conn, session_id: int, limit: int = 10):
    rows = await conn.fetch(
        """
        SELECT
            COALESCE(role, CASE WHEN is_from_user THEN 'user' ELSE 'assistant' END) AS role,
            COALESCE(content, text, '') AS content
        FROM chat_messages
        WHERE session_id=$1
        ORDER BY created_at DESC, id DESC
        LIMIT $2
        """,
        session_id,
        limit,
    )
    history = []
    for row in reversed(rows):
        role = str(row["role"] or "").strip().lower()
        content = str(row["content"] or "").strip()
        if role not in {"user", "assistant"} or not content:
            continue
        history.append({"role": role, "content": content})
    return history


async def persist_rag_chat_messages(
    conn,
    *,
    session_id,
    session_row,
    user_id: int,
    message: str,
    answer: str,
    sources_json,
    build_chat_session_title,
):
    async with conn.transaction():
        if session_id is None:
            session_id = await conn.fetchval(
                """
                INSERT INTO chat_sessions (user_id, title)
                VALUES ($1, $2)
                RETURNING id
                """,
                user_id,
                build_chat_session_title(message),
            )
        elif session_row["title"] == DEFAULT_CHAT_SESSION_TITLE and int(session_row["message_count"] or 0) == 0:
            await conn.execute(
                "UPDATE chat_sessions SET title=$1 WHERE id=$2 AND user_id=$3",
                build_chat_session_title(message),
                session_id,
                user_id,
            )

        await conn.execute(
            """
            INSERT INTO chat_messages (session_id, text, is_from_user, role, content, timestamp)
            VALUES ($1, $2, true, 'user', $2, NOW()::text)
            """,
            session_id,
            message,
        )
        await conn.execute(
            """
            INSERT INTO chat_messages (session_id, text, is_from_user, role, content, sources, timestamp)
            VALUES ($1, $2, false, 'assistant', $2, $3::jsonb, NOW()::text)
            """,
            session_id,
            answer,
            json.dumps(sources_json, ensure_ascii=False),
        )
        await conn.execute(
            "UPDATE chat_sessions SET updated_at=NOW() WHERE id=$1 AND user_id=$2",
            session_id,
            user_id,
        )
    return session_id
