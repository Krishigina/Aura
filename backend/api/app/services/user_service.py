from typing import List, Optional
import re
from app.database import get_pool
from app.models.user import User, UserCreate


def _is_valid_email(email: str) -> bool:
    return bool(re.match(r"^[^\s@]+@[^\s@]+\.[^\s@]{2,}$", email or ""))


def _normalize_login(login: Optional[str]) -> Optional[str]:
    if not login:
        return None
    cleaned = re.sub(r"\s+", "", login.strip()).lower()
    if not cleaned:
        return None
    cleaned = cleaned.replace("@", "")
    cleaned = re.sub(r"[^a-z0-9_]", "", cleaned)
    return f"@{cleaned}" if cleaned else None


def _is_valid_login(login: Optional[str]) -> bool:
    if not login:
        return True
    return bool(re.match(r"^@[a-z0-9_]{3,32}$", login))


class UserService:
    @staticmethod
    def get_all(role: Optional[str] = None) -> List[dict]:
        pool = get_pool()
        conn = pool.getconn()
        try:
            cursor = conn.cursor()
            if role and role != "all":
                cursor.execute("SELECT * FROM users WHERE role=%s ORDER BY id DESC", (role,))
            else:
                cursor.execute("SELECT * FROM users ORDER BY id DESC")
            rows = cursor.fetchall()
            columns = [desc[0] for desc in cursor.description]
            return [dict(zip(columns, row)) for row in rows]
        finally:
            pool.putconn(conn)

    @staticmethod
    def create(data: UserCreate) -> dict:
        pool = get_pool()
        conn = pool.getconn()
        try:
            if not _is_valid_email(data.email):
                raise ValueError("Некорректный email")

            login = _normalize_login(data.nickname)
            if not _is_valid_login(login):
                raise ValueError("Логин должен быть в формате @login, только a-z, 0-9 и _")

            cursor = conn.cursor()
            cursor.execute("SELECT id FROM users WHERE LOWER(nickname)=LOWER(%s) LIMIT 1", (login,))
            if login and cursor.fetchone():
                raise ValueError("Логин уже занят")

            cursor.execute(
                """INSERT INTO users (name, email, role, nickname, avatar) 
                   VALUES (%s, %s, %s, %s, %s) RETURNING *""",
                (data.name, data.email, data.role, login, data.avatar)
            )
            row = cursor.fetchone()
            conn.commit()
            columns = [desc[0] for desc in cursor.description]
            return dict(zip(columns, row))
        finally:
            pool.putconn(conn)

    @staticmethod
    def update(user_id: int, data: UserCreate) -> Optional[dict]:
        pool = get_pool()
        conn = pool.getconn()
        try:
            if not _is_valid_email(data.email):
                raise ValueError("Некорректный email")

            login = _normalize_login(data.nickname)
            if not _is_valid_login(login):
                raise ValueError("Логин должен быть в формате @login, только a-z, 0-9 и _")

            cursor = conn.cursor()
            cursor.execute(
                "SELECT id FROM users WHERE LOWER(nickname)=LOWER(%s) AND id<>%s LIMIT 1",
                (login, user_id)
            )
            if login and cursor.fetchone():
                raise ValueError("Логин уже занят")

            cursor.execute(
                """UPDATE users SET name=%s, email=%s, role=%s, nickname=%s, avatar=%s 
                   WHERE id=%s RETURNING *""",
                (data.name, data.email, data.role, login, data.avatar, user_id)
            )
            row = cursor.fetchone()
            conn.commit()
            if row:
                columns = [desc[0] for desc in cursor.description]
                return dict(zip(columns, row))
            return None
        finally:
            pool.putconn(conn)

    @staticmethod
    def delete(user_id: int) -> None:
        pool = get_pool()
        conn = pool.getconn()
        try:
            cursor = conn.cursor()
            cursor.execute("DELETE FROM users WHERE id=%s", (user_id,))
            conn.commit()
        finally:
            pool.putconn(conn)
