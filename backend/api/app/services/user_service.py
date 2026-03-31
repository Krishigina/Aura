from typing import List, Optional
from app.database import get_pool
from app.models.user import User, UserCreate


class UserService:
    @staticmethod
    def get_all() -> List[dict]:
        pool = get_pool()
        conn = pool.getconn()
        try:
            cursor = conn.cursor()
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
            cursor = conn.cursor()
            cursor.execute(
                """INSERT INTO users (name, email, role, avatar) 
                   VALUES (%s, %s, %s, %s) RETURNING *""",
                (data.name, data.email, data.role, data.avatar)
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
            cursor = conn.cursor()
            cursor.execute(
                """UPDATE users SET name=%s, email=%s, role=%s, avatar=%s 
                   WHERE id=%s RETURNING *""",
                (data.name, data.email, data.role, data.avatar, user_id)
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
