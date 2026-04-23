import json
from datetime import datetime

from fastapi import HTTPException


async def load_chat_bootstrap_payload(conn, *, user_id: int):
    profile_row = await conn.fetchrow(
        "SELECT extra_data FROM user_profiles WHERE user_id=$1",
        user_id,
    )
    return {
        "assistant_name": "Aura AI - Помощник",
        "assistant_context": "Здоровье кожи",
        "messages": extract_chat_messages(profile_row),
    }


async def append_chat_message(conn, *, user_id: int, payload):
    text = (payload.text or "").strip()
    if not text:
        raise HTTPException(status_code=400, detail="Пустое сообщение")

    timestamp = payload.timestamp or datetime.utcnow().strftime("%H:%M")
    row = await conn.fetchrow(
        "SELECT extra_data FROM user_profiles WHERE user_id=$1",
        user_id,
    )

    extra_data = {}
    if row and isinstance(row.get("extra_data"), dict):
        extra_data = dict(row["extra_data"])

    chat_blob = extra_data.get("chat")
    if not isinstance(chat_blob, dict):
        chat_blob = {}

    messages = chat_blob.get("messages")
    if not isinstance(messages, list):
        messages = []

    message = {
        "text": text,
        "is_from_user": bool(payload.is_from_user),
        "timestamp": str(timestamp),
    }
    messages.append(message)

    chat_blob["messages"] = messages[-100:]
    extra_data["chat"] = chat_blob

    await conn.execute(
        """
        INSERT INTO user_profiles (user_id, extra_data, updated_at)
        VALUES ($1, $2::jsonb, NOW())
        ON CONFLICT (user_id)
        DO UPDATE SET
            extra_data = $2::jsonb,
            updated_at = NOW()
        """,
        user_id,
        json.dumps(extra_data, ensure_ascii=False),
    )
    return {"success": True, "message": message}


def extract_chat_messages(profile_row):
    messages = []
    if profile_row and isinstance(profile_row.get("extra_data"), dict):
        chat_blob = profile_row["extra_data"].get("chat")
        if isinstance(chat_blob, dict):
            stored = chat_blob.get("messages")
            if isinstance(stored, list):
                for item in stored:
                    if isinstance(item, dict):
                        text = str(item.get("text") or "").strip()
                        if not text:
                            continue
                        messages.append(
                            {
                                "text": text,
                                "is_from_user": bool(item.get("is_from_user", False)),
                                "timestamp": str(item.get("timestamp") or ""),
                            }
                        )
    return messages
