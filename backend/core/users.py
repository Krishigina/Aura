import re
from typing import Optional

from fastapi import HTTPException


def is_valid_email(email: str) -> bool:
    return bool(re.match(r"^[^\s@]+@[^\s@]+\.[^\s@]{2,}$", email or ""))


def normalize_login(login: Optional[str]) -> Optional[str]:
    if not login:
        return None
    cleaned = re.sub(r"\s+", "", login.strip())
    if not cleaned:
        return None
    if not cleaned.startswith("@"):
        cleaned = f"@{cleaned.replace('@', '')}"
    else:
        cleaned = "@" + cleaned[1:].replace("@", "")
    return cleaned.lower()


def is_valid_login(login: Optional[str]) -> bool:
    if not login:
        return True
    return bool(re.match(r"^@[a-z0-9_]{3,32}$", login))


async def ensure_users_columns(conn):
    await conn.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS nickname VARCHAR(255)")


async def ensure_unique_login(conn, login: Optional[str], exclude_user_id: Optional[int] = None):
    if not login:
        return
    if exclude_user_id is None:
        existing = await conn.fetchrow("SELECT id FROM users WHERE LOWER(nickname)=LOWER($1) LIMIT 1", login)
    else:
        existing = await conn.fetchrow(
            "SELECT id FROM users WHERE LOWER(nickname)=LOWER($1) AND id<>$2 LIMIT 1",
            login,
            exclude_user_id,
        )
    if existing:
        raise HTTPException(status_code=400, detail="Логин уже занят")
