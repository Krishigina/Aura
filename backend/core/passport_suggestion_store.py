import json
from typing import Any, Dict, List


def decode_jsonb_value(value):
    if isinstance(value, str):
        try:
            return json.loads(value)
        except json.JSONDecodeError:
            return value
    return value


def passport_suggestion_response(row) -> Dict[str, Any]:
    item = dict(row)
    item["old_value"] = decode_jsonb_value(item.get("old_value"))
    item["proposed_value"] = decode_jsonb_value(item.get("proposed_value"))
    return item


def extract_passport_update_values(proposed_value: Dict[str, Any]) -> List[str]:
    value = proposed_value.get("values")
    if isinstance(value, list):
        return [str(item).strip() for item in value if str(item).strip()]
    value = proposed_value.get("value")
    if value is None:
        value = proposed_value.get("normalized_value")
    if value is None:
        return []
    text = str(value).strip()
    return [text] if text else []


async def list_passport_suggestions_for_user(conn, user_id: int) -> Dict[str, Any]:
    rows = await conn.fetch(
        """
        SELECT * FROM passport_update_suggestions
        WHERE user_id=$1
        ORDER BY created_at DESC, id DESC
        """,
        user_id,
    )
    return {"items": [passport_suggestion_response(row) for row in rows]}


async def create_passport_suggestion_for_user(conn, user_id: int, payload) -> Dict[str, Any]:
    row = await conn.fetchrow(
        """
        INSERT INTO passport_update_suggestions (
            user_id, suggestion_type, target_field, old_value, proposed_value,
            source_type, source_message_id, evidence_text, confidence, conflict_status, status
        )
        VALUES ($1,$2,$3,$4::jsonb,$5::jsonb,$6,$7,$8,$9,$10,'proposed')
        RETURNING *
        """,
        user_id,
        payload.suggestion_type,
        payload.target_field,
        json.dumps(payload.old_value, ensure_ascii=False) if payload.old_value is not None else None,
        json.dumps(payload.proposed_value, ensure_ascii=False),
        payload.source_type,
        payload.source_message_id,
        payload.evidence_text,
        payload.confidence,
        payload.conflict_status,
    )
    return passport_suggestion_response(row)
