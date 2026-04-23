from backend.core.chat import format_chat_timestamp


async def list_chat_sessions_for_user(conn, user_id: int):
    return await conn.fetch(
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
        WHERE s.user_id=$1
        GROUP BY s.id, s.title, s.updated_at
        ORDER BY s.updated_at DESC
        LIMIT 50
        """,
        user_id,
    )


def build_chat_session_summary(row, summary_model):
    return summary_model(
        id=int(row["id"]),
        title=str(row["title"] or ""),
        last_message=str(row["last_message"] or ""),
        updated_at=format_chat_timestamp(row["updated_at"]),
        message_count=int(row["message_count"] or 0),
    )


async def create_chat_session_for_user(conn, user_id: int, title: str):
    return await conn.fetchrow(
        """
        INSERT INTO chat_sessions (user_id, title)
        VALUES ($1, $2)
        RETURNING id, title
        """,
        user_id,
        title,
    )


def build_chat_session_messages(message_rows, message_model):
    return [
        message_model(
            role=str(row["role"] or "user"),
            content=str(row["content"] or ""),
            timestamp=format_chat_timestamp(row["timestamp"]),
        )
        for row in message_rows
    ]
