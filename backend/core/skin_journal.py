import json
import uuid
from datetime import datetime
from typing import Any, Dict, Optional

from fastapi import HTTPException


SKIN_JOURNAL_KEY = "skin_journal"
REMINDER_ACTION_TO_STATUS = {
    "done": "done",
    "reschedule": "rescheduled",
    "skip": "skipped",
}


def coerce_extra_data(raw_extra_data) -> dict:
    if isinstance(raw_extra_data, dict):
        return raw_extra_data
    if isinstance(raw_extra_data, str):
        try:
            parsed = json.loads(raw_extra_data)
            return parsed if isinstance(parsed, dict) else {}
        except json.JSONDecodeError:
            return {}
    return {}


def build_empty_skin_journal() -> Dict[str, Any]:
    return {
        "settings": {
            "has_sensor": None,
            "push_enabled": False,
            "sensor_reminder_schedule": None,
        },
        "procedures": [],
        "sensor_readings": [],
        "reminders": [],
    }


def normalize_skin_journal(raw_journal: Any) -> Dict[str, Any]:
    journal = build_empty_skin_journal()
    if not isinstance(raw_journal, dict):
        return journal

    settings = raw_journal.get("settings")
    if isinstance(settings, dict):
        journal["settings"].update(
            {
                "has_sensor": settings.get("has_sensor"),
                "push_enabled": bool(settings.get("push_enabled", False)),
                "sensor_reminder_schedule": settings.get("sensor_reminder_schedule"),
            }
        )

    for key in ["procedures", "sensor_readings", "reminders"]:
        value = raw_journal.get(key)
        if isinstance(value, list):
            journal[key] = [item for item in value if isinstance(item, dict)]
    return journal


async def load_user_skin_journal(conn, user_id: int, for_update: bool = False) -> Dict[str, Any]:
    query = "SELECT extra_data FROM user_profiles WHERE user_id=$1"
    if for_update:
        await conn.execute(
            """
            INSERT INTO user_profiles (user_id, extra_data, updated_at)
            VALUES ($1, '{}'::jsonb, NOW())
            ON CONFLICT (user_id) DO NOTHING
            """,
            user_id,
        )
        query += " FOR UPDATE"
    row = await conn.fetchrow(query, user_id)
    extra_data = coerce_extra_data(row["extra_data"]) if row else {}
    return normalize_skin_journal(extra_data.get(SKIN_JOURNAL_KEY))


async def save_user_skin_journal(conn, user_id: int, journal: Dict[str, Any]) -> Dict[str, Any]:
    row = await conn.fetchrow("SELECT extra_data FROM user_profiles WHERE user_id=$1", user_id)
    extra_data = coerce_extra_data(row["extra_data"]) if row else {}
    extra_data[SKIN_JOURNAL_KEY] = normalize_skin_journal(journal)
    await conn.execute(
        """
        INSERT INTO user_profiles (user_id, extra_data, updated_at)
        VALUES ($1, $2::jsonb, NOW())
        ON CONFLICT (user_id) DO UPDATE SET extra_data=$2::jsonb, updated_at=NOW()
        """,
        user_id,
        json.dumps(extra_data, ensure_ascii=False),
    )
    return extra_data[SKIN_JOURNAL_KEY]


def apply_reminder_action(
    reminder: Dict[str, Any],
    action: str,
    rescheduled_due_at: Optional[str] = None,
) -> Dict[str, Any]:
    if action not in REMINDER_ACTION_TO_STATUS:
        raise HTTPException(status_code=400, detail="Неизвестное действие напоминания")

    updated = dict(reminder)
    updated["status"] = REMINDER_ACTION_TO_STATUS[action]
    if action == "reschedule":
        if not rescheduled_due_at:
            raise HTTPException(status_code=400, detail="Укажите новую дату напоминания")
        updated["due_at"] = rescheduled_due_at
    return updated


async def save_skin_journal_settings_for_user(conn, user_id: int, payload) -> Dict[str, Any]:
    journal = await load_user_skin_journal(conn, user_id, for_update=True)
    settings = journal["settings"]
    if payload.has_sensor is not None:
        settings["has_sensor"] = payload.has_sensor
    if payload.push_enabled is not None:
        settings["push_enabled"] = payload.push_enabled
    if payload.sensor_reminder_schedule is not None:
        settings["sensor_reminder_schedule"] = payload.sensor_reminder_schedule
    journal["settings"] = settings
    return await save_user_skin_journal(conn, user_id, journal)


async def create_skin_journal_procedure_for_user(conn, user_id: int, payload) -> Dict[str, Any]:
    entry = payload.dict()
    entry["zone_amounts"] = [item.dict() for item in payload.zone_amounts]
    entry["id"] = str(uuid.uuid4())
    entry["created_at"] = datetime.utcnow().isoformat() + "Z"

    journal = await load_user_skin_journal(conn, user_id, for_update=True)
    journal["procedures"].append(entry)

    if payload.repeat_due_at:
        journal["reminders"].append(
            {
                "id": str(uuid.uuid4()),
                "type": "procedure_repeat",
                "title": f"Повторить {payload.procedure_name}",
                "due_at": payload.repeat_due_at,
                "status": "planned",
                "related_id": entry["id"],
            }
        )

    for index, text in enumerate(payload.post_care_tasks):
        journal["reminders"].append(
            {
                "id": str(uuid.uuid4()),
                "type": "procedure_post_care",
                "title": text,
                "due_at": payload.performed_at,
                "status": "planned",
                "related_id": entry["id"],
                "sort_order": index,
            }
        )

    return await save_user_skin_journal(conn, user_id, journal)


async def create_skin_journal_sensor_reading_for_user(conn, user_id: int, payload) -> Dict[str, Any]:
    entry = payload.dict()
    entry["id"] = str(uuid.uuid4())
    journal = await load_user_skin_journal(conn, user_id, for_update=True)
    journal["sensor_readings"].append(entry)
    return await save_user_skin_journal(conn, user_id, journal)


async def update_skin_journal_reminder_for_user(conn, user_id: int, reminder_id: str, payload) -> Dict[str, Any]:
    journal = await load_user_skin_journal(conn, user_id, for_update=True)
    for index, reminder in enumerate(journal["reminders"]):
        if reminder.get("id") == reminder_id:
            journal["reminders"][index] = apply_reminder_action(
                reminder,
                payload.action,
                payload.rescheduled_due_at,
            )
            return await save_user_skin_journal(conn, user_id, journal)
    raise HTTPException(status_code=404, detail="Напоминание не найдено")
