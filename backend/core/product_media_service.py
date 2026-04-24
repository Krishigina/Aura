import base64
import uuid

from fastapi import HTTPException
from fastapi.responses import Response


async def upload_product_photo_record(
    conn,
    *,
    product_id: int,
    file,
    ensure_upload_dir,
):
    await ensure_product_exists(conn, product_id)
    uploads_dir = ensure_upload_dir("product_photos")
    filename = build_product_media_filename(product_id, file.filename or "", default_ext="jpg")
    file_path = uploads_dir / filename

    content = await file.read()
    with file_path.open("wb") as output_file:
        output_file.write(content)

    row = await conn.fetchrow(
        "INSERT INTO product_photos (product_id, filename) VALUES ($1, $2) RETURNING *",
        product_id,
        filename,
    )
    return {"id": row["id"], "filename": row["filename"]}


async def delete_product_photo_record(conn, *, product_id: int, photo_id: int, upload_path):
    row = await conn.fetchrow(
        "SELECT filename FROM product_photos WHERE id=$1 AND product_id=$2",
        photo_id,
        product_id,
    )
    if row:
        file_path = upload_path("product_photos", row["filename"])
        if file_path.exists():
            file_path.unlink()
        await conn.execute("DELETE FROM product_photos WHERE id=$1", photo_id)
    return {"success": True}


async def load_product_photos_response(
    conn,
    *,
    product_id: int,
    upload_dir,
    max_inline_product_photo_bytes: int,
    product_photo_content_type,
):
    rows = await conn.fetch(
        "SELECT id, product_id, filename FROM product_photos WHERE product_id=$1 ORDER BY id",
        product_id,
    )
    photos = []
    uploads_dir = upload_dir("product_photos")
    for row in rows:
        filename = row["filename"]
        photo_url = f"/api/products/{row['product_id']}/photos/{row['id']}"
        file_path = uploads_dir / filename
        if not file_path.exists():
            continue
        if file_path.stat().st_size > max_inline_product_photo_bytes:
            photos.append({
                "id": row["id"],
                "filename": filename,
                "content_type": product_photo_content_type(filename),
                "data": "",
                "url": photo_url,
            })
            continue
        with file_path.open("rb") as input_file:
            data = base64.b64encode(input_file.read()).decode()
        photos.append({
            "id": row["id"],
            "filename": filename,
            "content_type": product_photo_content_type(filename),
            "data": data,
            "url": photo_url,
        })
    return photos


async def serve_product_photo_response(conn, *, product_id: int, photo_id: int, upload_path, product_photo_content_type):
    row = await conn.fetchrow(
        "SELECT filename FROM product_photos WHERE id=$1 AND product_id=$2",
        photo_id,
        product_id,
    )
    if not row:
        raise HTTPException(status_code=404, detail="Photo not found")

    file_path = upload_path("product_photos", row["filename"])
    if not file_path.exists():
        raise HTTPException(status_code=404, detail="Photo file not found")

    with file_path.open("rb") as input_file:
        return Response(content=input_file.read(), media_type=product_photo_content_type(row["filename"]))


async def upload_product_video_record(conn, *, product_id: int, file, ensure_upload_dir):
    await ensure_product_exists(conn, product_id)
    uploads_dir = ensure_upload_dir("product_videos")
    filename = build_product_media_filename(product_id, file.filename or "", default_ext="mp4")
    file_path = uploads_dir / filename

    content = await file.read()
    with file_path.open("wb") as output_file:
        output_file.write(content)

    await conn.execute("UPDATE products SET video=$1, has_video=true WHERE id=$2", filename, product_id)
    return {"success": True, "filename": filename}


async def delete_product_video_record(conn, *, product_id: int, upload_path):
    row = await conn.fetchrow("SELECT video FROM products WHERE id=$1", product_id)
    if row and row["video"]:
        file_path = upload_path("product_videos", row["video"])
        if file_path.exists():
            file_path.unlink()
        await conn.execute("UPDATE products SET video=NULL, has_video=false WHERE id=$1", product_id)
    return {"success": True}


async def load_product_video_payload(conn, *, product_id: int, upload_path):
    row = await conn.fetchrow("SELECT video FROM products WHERE id=$1", product_id)
    if not row or not row["video"]:
        return None
    file_path = upload_path("product_videos", row["video"])
    if not file_path.exists():
        return None
    with file_path.open("rb") as input_file:
        data = base64.b64encode(input_file.read()).decode()
    return {"filename": row["video"], "data": data}


async def ensure_product_exists(conn, product_id: int):
    product = await conn.fetchrow("SELECT id FROM products WHERE id=$1", product_id)
    if not product:
        raise HTTPException(status_code=404, detail="Product not found")


def build_product_media_filename(product_id: int, original_filename: str, *, default_ext: str):
    file_ext = original_filename.split(".")[-1] if "." in original_filename else default_ext
    return f"{product_id}_{uuid.uuid4()}.{file_ext}"
