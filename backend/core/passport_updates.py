import json
from typing import Any, Callable, Dict, List, Optional

from fastapi import HTTPException

from backend.core.skin_journal import coerce_extra_data
from backend.core.passport_context import (
    get_skin_passport_for_user,
    load_skin_passport_context,
    save_skin_passport_for_user,
)
from backend.core.passport_suggestion_store import (
    create_passport_suggestion_for_user,
    decode_jsonb_value,
    extract_passport_update_values,
    list_passport_suggestions_for_user,
    passport_suggestion_response,
)


async def apply_passport_field_update(
    conn,
    user_id: int,
    target_field: Optional[str],
    proposed_value: Dict[str, Any],
    sanitize_answers: Callable[[Any], Dict[str, List[str]]],
):
    if not target_field:
        raise HTTPException(status_code=400, detail="Для обновления поля анкеты нужно указать target_field")
    values = extract_passport_update_values(proposed_value)
    if not values:
        raise HTTPException(status_code=400, detail="Предложение обновления анкеты не содержит значений")

    await conn.fetchrow("SELECT id FROM users WHERE id=$1 FOR UPDATE", user_id)
    row = await conn.fetchrow("SELECT extra_data FROM user_profiles WHERE user_id=$1 FOR UPDATE", user_id)
    extra_data = coerce_extra_data(row["extra_data"]) if row else {}
    skin_passport = extra_data.get("skin_passport") if isinstance(extra_data.get("skin_passport"), dict) else {}
    answers = sanitize_answers(skin_passport.get("answers"))
    answers[str(target_field)] = values
    skin_passport["answers"] = answers
    extra_data["skin_passport"] = skin_passport

    await conn.execute(
        """
        INSERT INTO user_profiles (user_id, extra_data, updated_at)
        VALUES ($1, $2::jsonb, NOW())
        ON CONFLICT (user_id)
        DO UPDATE SET extra_data=$2::jsonb, updated_at=NOW()
        """,
        user_id,
        json.dumps(extra_data, ensure_ascii=False),
    )


async def update_passport_suggestion_for_user(
    conn,
    user_id: int,
    suggestion_id: int,
    status: str,
    sanitize_answers: Callable[[Any], Dict[str, List[str]]],
) -> Dict[str, Any]:
    existing = await conn.fetchrow(
        "SELECT * FROM passport_update_suggestions WHERE id=$1 AND user_id=$2 FOR UPDATE",
        suggestion_id,
        user_id,
    )
    if not existing:
        raise HTTPException(status_code=404, detail="Предложение не найдено")
    if existing["status"] != "proposed":
        raise HTTPException(status_code=400, detail="Предложение уже обработано")

    proposed_value = decode_jsonb_value(existing["proposed_value"])
    if status == "accepted" and existing["suggestion_type"] == "update_field":
        await apply_passport_field_update(conn, user_id, existing["target_field"], proposed_value, sanitize_answers)

    row = await conn.fetchrow(
        """
        UPDATE passport_update_suggestions
        SET status=$1, accepted_at=CASE WHEN $1='accepted' THEN NOW() ELSE NULL END
        WHERE id=$2 AND user_id=$3 AND status='proposed'
        RETURNING *
        """,
        status,
        suggestion_id,
        user_id,
    )
    return passport_suggestion_response(row)
