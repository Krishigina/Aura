import json
from datetime import datetime
from typing import Any, Callable, Dict, List, Optional

from fastapi import HTTPException

from backend.core.skin_journal import coerce_extra_data


async def load_skin_passport_context(
    conn,
    user_id: int,
    sanitize_answers: Callable[[Any], Dict[str, List[str]]],
) -> Optional[Dict[str, Any]]:
    row = await conn.fetchrow("SELECT extra_data FROM user_profiles WHERE user_id=$1", user_id)
    if not row:
        return None
    extra_data = coerce_extra_data(row["extra_data"])
    skin_passport = extra_data.get("skin_passport")
    if not isinstance(skin_passport, dict):
        return None
    answers = sanitize_answers(skin_passport.get("answers"))
    return {
        "completed_at_epoch_millis": skin_passport.get("completed_at_epoch_millis"),
        "answers": answers,
    }


async def get_skin_passport_for_user(conn, user_id: int, sanitize_answers, response_model):
    skin_passport = await load_skin_passport_context(conn, user_id, sanitize_answers)
    if not skin_passport:
        return response_model(completed_at_epoch_millis=None, answers={})

    completed_at = skin_passport.get("completed_at_epoch_millis")
    try:
        completed_at = int(completed_at) if completed_at is not None else None
    except (TypeError, ValueError):
        completed_at = None

    return response_model(completed_at_epoch_millis=completed_at, answers=skin_passport.get("answers", {}))


async def save_skin_passport_for_user(conn, user_id: int, payload, sanitize_answers, response_model):
    completed_at = payload.completed_at_epoch_millis or int(datetime.utcnow().timestamp() * 1000)
    answers = sanitize_answers(payload.answers)
    if not answers:
        raise HTTPException(status_code=400, detail="Анкета не содержит ответов")

    update_blob = {
        "skin_passport": {
            "completed_at_epoch_millis": completed_at,
            "answers": answers,
        }
    }

    await conn.execute(
        """
        INSERT INTO user_profiles (user_id, extra_data, updated_at)
        VALUES ($1, $2::jsonb, NOW())
        ON CONFLICT (user_id)
        DO UPDATE SET
            extra_data = COALESCE(user_profiles.extra_data, '{}'::jsonb) || $2::jsonb,
            updated_at = NOW()
        """,
        user_id,
        json.dumps(update_blob, ensure_ascii=False),
    )

    return response_model(completed_at_epoch_millis=completed_at, answers=answers)
