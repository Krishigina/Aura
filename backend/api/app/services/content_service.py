from typing import List, Optional
from app.database import get_pool
from app.models.content import Content, ContentCreate


class ContentService:
    @staticmethod
    def get_all() -> List[dict]:
        pool = get_pool()
        conn = pool.getconn()
        try:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM content ORDER BY id DESC")
            rows = cursor.fetchall()
            columns = [desc[0] for desc in cursor.description]
            return [dict(zip(columns, row)) for row in rows]
        finally:
            pool.putconn(conn)

    @staticmethod
    def create(data: ContentCreate) -> dict:
        pool = get_pool()
        conn = pool.getconn()
        try:
            cursor = conn.cursor()
            cursor.execute(
                """INSERT INTO content (title, type, body, image_url, published) 
                   VALUES (%s, %s, %s, %s, %s) RETURNING *""",
                (data.title, data.type, data.body, data.image_url, data.published)
            )
            row = cursor.fetchone()
            conn.commit()
            columns = [desc[0] for desc in cursor.description]
            return dict(zip(columns, row))
        finally:
            pool.putconn(conn)

    @staticmethod
    def update(content_id: int, data: ContentCreate) -> Optional[dict]:
        pool = get_pool()
        conn = pool.getconn()
        try:
            cursor = conn.cursor()
            cursor.execute(
                """UPDATE content SET title=%s, type=%s, body=%s, image_url=%s, published=%s 
                   WHERE id=%s RETURNING *""",
                (data.title, data.type, data.body, data.image_url, data.published, content_id)
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
    def delete(content_id: int) -> None:
        pool = get_pool()
        conn = pool.getconn()
        try:
            cursor = conn.cursor()
            cursor.execute("DELETE FROM content WHERE id=%s", (content_id,))
            conn.commit()
        finally:
            pool.putconn(conn)
