from typing import List, Optional
from app.database import get_pool
from app.models.procedure import Procedure, ProcedureCreate


class ProcedureService:
    @staticmethod
    def get_all() -> List[dict]:
        pool = get_pool()
        conn = pool.getconn()
        try:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM procedures ORDER BY id DESC")
            rows = cursor.fetchall()
            columns = [desc[0] for desc in cursor.description]
            return [dict(zip(columns, row)) for row in rows]
        finally:
            pool.putconn(conn)

    @staticmethod
    def create(data: ProcedureCreate) -> dict:
        pool = get_pool()
        conn = pool.getconn()
        try:
            cursor = conn.cursor()
            cursor.execute(
                """INSERT INTO procedures (name, category, duration, price, description, contraindications) 
                   VALUES (%s, %s, %s, %s, %s, %s) RETURNING *""",
                (data.name, data.category, data.duration, data.price,
                 data.description, data.contraindications)
            )
            row = cursor.fetchone()
            conn.commit()
            columns = [desc[0] for desc in cursor.description]
            return dict(zip(columns, row))
        finally:
            pool.putconn(conn)

    @staticmethod
    def update(procedure_id: int, data: ProcedureCreate) -> Optional[dict]:
        pool = get_pool()
        conn = pool.getconn()
        try:
            cursor = conn.cursor()
            cursor.execute(
                """UPDATE procedures SET name=%s, category=%s, duration=%s, price=%s, description=%s, contraindications=%s 
                   WHERE id=%s RETURNING *""",
                (data.name, data.category, data.duration, data.price,
                 data.description, data.contraindications, procedure_id)
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
    def delete(procedure_id: int) -> None:
        pool = get_pool()
        conn = pool.getconn()
        try:
            cursor = conn.cursor()
            cursor.execute("DELETE FROM procedures WHERE id=%s", (procedure_id,))
            conn.commit()
        finally:
            pool.putconn(conn)
