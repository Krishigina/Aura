import base64
import os
import uuid

from fastapi import HTTPException
from fastapi.responses import Response


def content_media_root(router_file: str) -> str:
    return os.path.dirname(os.path.dirname(os.path.abspath(router_file)))


def content_image_content_type(filename: str) -> str:
    ext = filename.split(".")[-1]
    return f"image/{ext}" if ext in ["jpg", "jpeg", "png", "gif", "webp"] else "image/jpeg"


async def upload_content_image_record(conn, *, content_id: int, file, api_root: str):
    await ensure_content_exists(conn, content_id)
    uploads_dir = os.path.join(api_root, "content_images")
    os.makedirs(uploads_dir, exist_ok=True)
    filename = build_content_media_filename(content_id, file.filename or "", default_ext="jpg")
    file_path = os.path.join(uploads_dir, filename)

    content = await file.read()
    with open(file_path, "wb") as output_file:
        output_file.write(content)

    row = await conn.fetchrow(
        "INSERT INTO content_images (content_id, filename) VALUES ($1, $2) RETURNING *",
        content_id,
        filename,
    )
    return {"id": row["id"], "filename": row["filename"]}


async def delete_content_image_record(conn, *, content_id: int, image_id: int, api_root: str):
    row = await conn.fetchrow(
        "SELECT filename FROM content_images WHERE id=$1 AND content_id=$2",
        image_id,
        content_id,
    )
    if row:
        file_path = os.path.join(api_root, "content_images", row["filename"])
        if os.path.exists(file_path):
            os.remove(file_path)
        await conn.execute("DELETE FROM content_images WHERE id=$1", image_id)
    return {"success": True}


async def load_content_images(conn, *, content_id: int, api_root: str):
    rows = await conn.fetch("SELECT id, filename FROM content_images WHERE content_id=$1 ORDER BY id", content_id)
    images = []
    for row in rows:
        file_path = os.path.join(api_root, "content_images", row["filename"])
        if os.path.exists(file_path):
            with open(file_path, "rb") as input_file:
                data = base64.b64encode(input_file.read()).decode()
            images.append({
                "id": row["id"],
                "filename": row["filename"],
                "data": data,
                "content_type": content_image_content_type(row["filename"]),
            })
    return images


def build_content_image_url(content_id: int):
    return {"url": f"/api/content/{content_id}/images"}


async def upload_content_card_image_record(conn, *, content_id: int, file, api_root: str):
    await ensure_content_exists(conn, content_id)
    uploads_dir = os.path.join(api_root, "content_card_images")
    os.makedirs(uploads_dir, exist_ok=True)
    filename = build_content_media_filename(content_id, file.filename or "", default_ext="jpg")
    file_path = os.path.join(uploads_dir, filename)

    content_data = await file.read()
    with open(file_path, "wb") as output_file:
        output_file.write(content_data)

    await conn.execute("UPDATE content SET image_url=$1 WHERE id=$2", f"/api/content/card-image/{filename}", content_id)
    return {"success": True, "filename": filename, "url": f"/api/content/card-image/{filename}"}


def serve_content_card_image(filename: str, *, api_root: str):
    file_path = os.path.join(api_root, "content_card_images", filename)
    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="Image not found")
    with open(file_path, "rb") as input_file:
        content_data = input_file.read()
    return Response(content=content_data, media_type=content_image_content_type(filename))


async def delete_content_card_image_record(conn, *, content_id: int, api_root: str):
    row = await conn.fetchrow("SELECT image_url FROM content WHERE id=$1", content_id)
    if row and row["image_url"] and row["image_url"].startswith("/api/content/card-image/"):
        filename = row["image_url"].split("/")[-1]
        file_path = os.path.join(api_root, "content_card_images", filename)
        if os.path.exists(file_path):
            os.remove(file_path)
        await conn.execute("UPDATE content SET image_url=NULL WHERE id=$1", content_id)
    return {"success": True}


async def ensure_content_exists(conn, content_id: int):
    content_item = await conn.fetchrow("SELECT id FROM content WHERE id=$1", content_id)
    if not content_item:
        raise HTTPException(status_code=404, detail="Content not found")


def build_content_media_filename(content_id: int, original_filename: str, *, default_ext: str):
    file_ext = original_filename.split(".")[-1] if "." in original_filename else default_ext
    return f"{content_id}_{uuid.uuid4()}.{file_ext}"
