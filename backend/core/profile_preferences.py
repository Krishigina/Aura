import json
from typing import Any, Dict, List


PROFILE_ROUTINE_KEY = "profile_routine"
PROFILE_NOTIFICATIONS_KEY = "profile_notifications"


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


def normalize_profile_routine(raw_routine: Any) -> Dict[str, Any]:
    default = {"steps": []}
    if not isinstance(raw_routine, dict):
        return default

    raw_steps = raw_routine.get("steps")
    if not isinstance(raw_steps, list):
        return default

    steps: List[Dict[str, Any]] = []
    for index, step in enumerate(raw_steps):
        if not isinstance(step, dict):
            continue
        frequency = str(step.get("frequency") or "daily").strip().lower()
        if frequency not in {"daily", "weekly", "monthly"}:
            frequency = "daily"
        order_value = step.get("order")
        try:
            order_value = int(order_value)
        except (TypeError, ValueError):
            order_value = index + 1
        steps.append(
            {
                "id": str(step.get("id") or ""),
                "product_label": str(step.get("product_label") or "").strip(),
                "order": max(1, order_value),
                "frequency": frequency,
                "weekday": step.get("weekday") if isinstance(step.get("weekday"), int) else None,
                "month_day": step.get("month_day") if isinstance(step.get("month_day"), int) else None,
                "reminder_time": None,
            }
        )
    steps.sort(key=lambda x: x.get("order") or 0)
    for idx, step in enumerate(steps):
        step["order"] = idx + 1
    return {"steps": steps}


def normalize_profile_notifications(raw_notifications: Any) -> Dict[str, Any]:
    def normalize_pref(raw_pref: Any) -> Dict[str, Any]:
        if not isinstance(raw_pref, dict):
            return {"frequency": "none", "weekday": None, "month_day": None, "reminder_time": None}
        frequency = str(raw_pref.get("frequency") or "none").strip().lower()
        if frequency not in {"none", "daily", "weekly", "monthly"}:
            frequency = "none"
        return {
            "frequency": frequency,
            "weekday": raw_pref.get("weekday") if isinstance(raw_pref.get("weekday"), int) else None,
            "month_day": raw_pref.get("month_day") if isinstance(raw_pref.get("month_day"), int) else None,
            "reminder_time": raw_pref.get("reminder_time") if isinstance(raw_pref.get("reminder_time"), str) else None,
        }

    if not isinstance(raw_notifications, dict):
        return {
            "disable_all": False,
            "routine": normalize_pref(None),
            "journal": normalize_pref(None),
        }

    return {
        "disable_all": bool(raw_notifications.get("disable_all", False)),
        "routine": normalize_pref(raw_notifications.get("routine")),
        "journal": normalize_pref(raw_notifications.get("journal")),
    }


async def load_user_profile_routine(conn, user_id: int, for_update: bool = False) -> Dict[str, Any]:
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
    return normalize_profile_routine(extra_data.get(PROFILE_ROUTINE_KEY))


async def save_user_profile_routine(conn, user_id: int, routine: Dict[str, Any]) -> Dict[str, Any]:
    row = await conn.fetchrow("SELECT extra_data FROM user_profiles WHERE user_id=$1", user_id)
    extra_data = coerce_extra_data(row["extra_data"]) if row else {}
    extra_data[PROFILE_ROUTINE_KEY] = normalize_profile_routine(routine)
    await conn.execute(
        """
        INSERT INTO user_profiles (user_id, extra_data, updated_at)
        VALUES ($1, $2::jsonb, NOW())
        ON CONFLICT (user_id) DO UPDATE SET extra_data=$2::jsonb, updated_at=NOW()
        """,
        user_id,
        json.dumps(extra_data, ensure_ascii=False),
    )
    return extra_data[PROFILE_ROUTINE_KEY]


async def load_user_profile_notifications(conn, user_id: int, for_update: bool = False) -> Dict[str, Any]:
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
    return normalize_profile_notifications(extra_data.get(PROFILE_NOTIFICATIONS_KEY))


async def save_user_profile_notifications(conn, user_id: int, settings: Dict[str, Any]) -> Dict[str, Any]:
    row = await conn.fetchrow("SELECT extra_data FROM user_profiles WHERE user_id=$1", user_id)
    extra_data = coerce_extra_data(row["extra_data"]) if row else {}
    extra_data[PROFILE_NOTIFICATIONS_KEY] = normalize_profile_notifications(settings)
    await conn.execute(
        """
        INSERT INTO user_profiles (user_id, extra_data, updated_at)
        VALUES ($1, $2::jsonb, NOW())
        ON CONFLICT (user_id) DO UPDATE SET extra_data=$2::jsonb, updated_at=NOW()
        """,
        user_id,
        json.dumps(extra_data, ensure_ascii=False),
    )
    return extra_data[PROFILE_NOTIFICATIONS_KEY]
