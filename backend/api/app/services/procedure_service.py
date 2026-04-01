from typing import List, Optional
from app.database import get_pool
from app.models.procedure import Procedure, ProcedureCreate


PROCEDURE_DICT_TABLES = {
    "method-types": "procedure_method_types",
    "durations": "procedure_durations",
    "equipment": "procedure_equipment",
    "zones": "procedure_zones",
    "effects": "procedure_effects",
    "problems": "procedure_problems",
}


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
    def get_by_id(procedure_id: int) -> Optional[dict]:
        pool = get_pool()
        conn = pool.getconn()
        try:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM procedures WHERE id=%s", (procedure_id,))
            row = cursor.fetchone()
            if row:
                columns = [desc[0] for desc in cursor.description]
                return dict(zip(columns, row))
            return None
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

    @staticmethod
    def get_dictionary(table_name: str) -> List[dict]:
        pool = get_pool()
        conn = pool.getconn()
        try:
            cursor = conn.cursor()
            cursor.execute(f"SELECT id, value FROM {table_name} ORDER BY id")
            rows = cursor.fetchall()
            return [{"id": row[0], "value": row[1]} for row in rows]
        finally:
            pool.putconn(conn)

    @staticmethod
    def add_dictionary_value(table_name: str, value: str) -> dict:
        pool = get_pool()
        conn = pool.getconn()
        try:
            cursor = conn.cursor()
            cursor.execute(f"INSERT INTO {table_name} (value) VALUES (%s) RETURNING *", (value,))
            row = cursor.fetchone()
            conn.commit()
            columns = [desc[0] for desc in cursor.description]
            return dict(zip(columns, row))
        finally:
            pool.putconn(conn)

    @staticmethod
    def add_photo(procedure_id: int, filename: str) -> dict:
        pool = get_pool()
        conn = pool.getconn()
        try:
            cursor = conn.cursor()
            cursor.execute(
                "INSERT INTO procedure_photos (procedure_id, filename) VALUES (%s, %s) RETURNING *",
                (procedure_id, filename)
            )
            row = cursor.fetchone()
            conn.commit()
            columns = [desc[0] for desc in cursor.description]
            return dict(zip(columns, row))
        finally:
            pool.putconn(conn)

    @staticmethod
    def get_photo(procedure_id: int, photo_id: int) -> Optional[dict]:
        pool = get_pool()
        conn = pool.getconn()
        try:
            cursor = conn.cursor()
            cursor.execute(
                "SELECT id, procedure_id, filename FROM procedure_photos WHERE id=%s AND procedure_id=%s",
                (photo_id, procedure_id)
            )
            row = cursor.fetchone()
            if row:
                return {"id": row[0], "procedure_id": row[1], "filename": row[2]}
            return None
        finally:
            pool.putconn(conn)

    @staticmethod
    def delete_photo(procedure_id: int, photo_id: int) -> None:
        pool = get_pool()
        conn = pool.getconn()
        try:
            cursor = conn.cursor()
            cursor.execute("DELETE FROM procedure_photos WHERE id=%s AND procedure_id=%s", (photo_id, procedure_id))
            conn.commit()
        finally:
            pool.putconn(conn)
