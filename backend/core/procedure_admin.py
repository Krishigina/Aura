import base64
import os
import uuid

from fastapi import HTTPException

from backend.core.procedure_dictionary_refs import sync_procedure_dictionary_refs
from backend.core.procedures import normalize_procedure_response, procedure_select_sql


def procedure_photos_dir(router_file: str) -> str:
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(router_file)))
    return os.path.join(base_dir, "procedure_photos")


async def list_procedures(conn):
    rows = await conn.fetch(f"SELECT * FROM ({procedure_select_sql('p')}) AS hydrated_procedures ORDER BY id DESC")
    return [normalize_procedure_response(row) for row in rows]


async def create_procedure_record(conn, procedure):
    row = await conn.fetchrow(
        """INSERT INTO procedures (name, description, procedure_about, advantages,
           indications, principle, how_it_goes, problems_solved,
           contraindications_full, preparation, recommended_course, rehabilitation,
           post_care, side_effects)
           VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14)
           RETURNING *""",
        procedure.name,
        procedure.description,
        procedure.procedure_about,
        procedure.advantages,
        procedure.indications,
        procedure.principle,
        procedure.how_it_goes,
        procedure.problems_solved,
        procedure.contraindications_full,
        procedure.preparation,
        procedure.recommended_course,
        procedure.rehabilitation,
        procedure.post_care,
        procedure.side_effects,
    )
    await sync_procedure_dictionary_refs(
        conn,
        {
            "id": row["id"],
            "direction": procedure.direction,
            "method_type": procedure.method_type,
            "duration": procedure.duration,
            "equipment": procedure.equipment,
            "zones": procedure.zones,
            "effects": procedure.effects,
            "problems": procedure.problems,
            "for_whom": procedure.for_whom,
        },
    )
    hydrated_row = await conn.fetchrow(
        f"SELECT * FROM ({procedure_select_sql('p')}) AS hydrated_procedures WHERE id=$1",
        row["id"],
    )
    return normalize_procedure_response(hydrated_row)


async def update_procedure_record(conn, procedure_id: int, procedure):
    existing = await conn.fetchrow(
        f"SELECT * FROM ({procedure_select_sql('p')}) AS hydrated_procedures WHERE id=$1",
        procedure_id,
    )
    if not existing:
        raise HTTPException(status_code=404, detail="Procedure not found")

    await conn.fetchrow(
        """UPDATE procedures SET
           name=$1, description=$2, procedure_about=$3, advantages=$4, indications=$5,
           principle=$6, how_it_goes=$7, problems_solved=$8, contraindications_full=$9,
           preparation=$10, recommended_course=$11, rehabilitation=$12, post_care=$13,
           side_effects=$14
           WHERE id=$15 RETURNING *""",
        procedure.name,
        procedure.description or existing["description"],
        procedure.procedure_about or existing["procedure_about"],
        procedure.advantages or existing["advantages"],
        procedure.indications or existing["indications"],
        procedure.principle or existing["principle"],
        procedure.how_it_goes or existing["how_it_goes"],
        procedure.problems_solved or existing["problems_solved"],
        procedure.contraindications_full or existing["contraindications_full"],
        procedure.preparation or existing["preparation"],
        procedure.recommended_course or existing["recommended_course"],
        procedure.rehabilitation or existing["rehabilitation"],
        procedure.post_care or existing["post_care"],
        procedure.side_effects or existing["side_effects"],
        procedure_id,
    )
    await sync_procedure_dictionary_refs(
        conn,
        {
            "id": procedure_id,
            "direction": procedure.direction if procedure.direction is not None else existing["direction"],
            "method_type": procedure.method_type if procedure.method_type is not None else existing["method_type"],
            "duration": procedure.duration if procedure.duration is not None else existing["duration"],
            "equipment": procedure.equipment if procedure.equipment is not None else existing["equipment"],
            "zones": procedure.zones if procedure.zones is not None else existing["zones"],
            "effects": procedure.effects if procedure.effects is not None else existing["effects"],
            "problems": procedure.problems if procedure.problems is not None else existing["problems"],
            "for_whom": procedure.for_whom if procedure.for_whom is not None else existing["for_whom"],
        },
    )
    hydrated_row = await conn.fetchrow(
        f"SELECT * FROM ({procedure_select_sql('p')}) AS hydrated_procedures WHERE id=$1",
        procedure_id,
    )
    return normalize_procedure_response(hydrated_row)


async def delete_procedure_record(conn, procedure_id: int):
    await conn.execute("DELETE FROM procedures WHERE id=$1", procedure_id)
    return {"success": True}


async def upload_procedure_photo_record(conn, procedure_id: int, file, uploads_dir: str):
    procedure = await conn.fetchrow("SELECT id FROM procedures WHERE id=$1", procedure_id)
    if not procedure:
        raise HTTPException(status_code=404, detail="Procedure not found")

    os.makedirs(uploads_dir, exist_ok=True)
    filename = build_procedure_photo_filename(procedure_id, file.filename or "")
    file_path = os.path.join(uploads_dir, filename)

    content = await file.read()
    with open(file_path, "wb") as output_file:
        output_file.write(content)

    row = await conn.fetchrow(
        "INSERT INTO procedure_photos (procedure_id, filename) VALUES ($1, $2) RETURNING *",
        procedure_id,
        filename,
    )
    return {"id": row["id"], "filename": row["filename"]}


def build_procedure_photo_filename(procedure_id: int, original_filename: str) -> str:
    file_ext = original_filename.split(".")[-1] if "." in original_filename else "jpg"
    return f"{procedure_id}_{uuid.uuid4()}.{file_ext}"


async def delete_procedure_photo_record(conn, procedure_id: int, photo_id: int, uploads_dir: str):
    row = await conn.fetchrow(
        "SELECT filename FROM procedure_photos WHERE id=$1 AND procedure_id=$2",
        photo_id,
        procedure_id,
    )
    if row:
        file_path = os.path.join(uploads_dir, row["filename"])
        if os.path.exists(file_path):
            os.remove(file_path)
        await conn.execute("DELETE FROM procedure_photos WHERE id=$1", photo_id)
    return {"success": True}


async def list_procedure_photos(conn, procedure_id: int, uploads_dir: str):
    rows = await conn.fetch(
        "SELECT id, filename FROM procedure_photos WHERE procedure_id=$1 ORDER BY id",
        procedure_id,
    )
    photos = []
    for row in rows:
        file_path = os.path.join(uploads_dir, row["filename"])
        if os.path.exists(file_path):
            with open(file_path, "rb") as input_file:
                data = base64.b64encode(input_file.read()).decode()
            ext = row["filename"].split(".")[-1]
            content_type = f"image/{ext}" if ext in ["jpg", "jpeg", "png", "gif", "webp"] else "image/jpeg"
            photos.append(
                {
                    "id": row["id"],
                    "filename": row["filename"],
                    "data": data,
                    "content_type": content_type,
                }
            )
    return photos
