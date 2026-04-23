from fastapi import HTTPException

from backend.core.passport_updates import load_skin_passport_context
from backend.core.chat_session_attachments import (
    build_attachment_processing_state,
    build_chat_attachment_response,
    create_chat_attachment_record,
    list_chat_session_attachments_for_user,
    persist_attachment_file,
    save_chat_attachment_processing_result,
)
from backend.core.chat_session_listing import (
    build_chat_session_messages,
    build_chat_session_summary,
    create_chat_session_for_user,
    list_chat_sessions_for_user,
)
from backend.core.chat_session_rag import (
    get_owned_chat_session_overview,
    normalize_rag_sources,
    persist_rag_chat_messages,
)


async def get_chat_session_detail_for_user(conn, session_id: int, user_id: int):
    session_row = await conn.fetchrow(
        """
        SELECT
            s.id,
            s.title,
            s.updated_at,
            COUNT(m.id)::int AS message_count,
            COALESCE((
                SELECT COALESCE(latest.content, latest.text, '')
                FROM chat_messages latest
                WHERE latest.session_id = s.id
                ORDER BY latest.created_at DESC, latest.id DESC
                LIMIT 1
            ), '') AS last_message
        FROM chat_sessions s
        LEFT JOIN chat_messages m ON m.session_id = s.id
        WHERE s.id=$1 AND s.user_id=$2
        GROUP BY s.id, s.title, s.updated_at
        """,
        session_id,
        user_id,
    )
    if not session_row:
        raise HTTPException(status_code=404, detail="Чат не найден")

    message_rows = await conn.fetch(
        """
        SELECT
            COALESCE(role, CASE WHEN is_from_user THEN 'user' ELSE 'assistant' END) AS role,
            COALESCE(content, text, '') AS content,
            COALESCE(timestamp, created_at::text) AS timestamp
        FROM chat_messages
        WHERE session_id=$1
        ORDER BY created_at ASC, id ASC
        """,
        session_id,
    )
    return session_row, message_rows


async def ensure_chat_session_owned_by_user(conn, session_id: int, user_id: int):
    session_row = await conn.fetchrow(
        "SELECT id FROM chat_sessions WHERE id=$1 AND user_id=$2",
        session_id,
        user_id,
    )
    if not session_row:
        raise HTTPException(status_code=404, detail="Чат не найден")
    return session_row
