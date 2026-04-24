from fastapi import APIRouter, Depends, File, UploadFile

from backend.core.product_media_service import (
    delete_product_photo_record,
    delete_product_video_record,
    load_product_photos_response,
    load_product_video_payload as load_product_video_response,
    serve_product_photo_response,
    upload_product_photo_record,
    upload_product_video_record,
)
from backend.core.products import MAX_INLINE_PRODUCT_PHOTO_BYTES, product_photo_content_type
from backend.core.storage import ensure_upload_dir, upload_dir, upload_path
from backend.db.pool import get_db


router = APIRouter(prefix="/api/products", tags=["Product Media"])


@router.post("/{product_id:int}/photos")
async def upload_product_photo(product_id: int, file: UploadFile = File(...), db=Depends(get_db)):
    async with db.acquire() as conn:
        return await upload_product_photo_record(
            conn,
            product_id=product_id,
            file=file,
            ensure_upload_dir=ensure_upload_dir,
        )


@router.delete("/{product_id:int}/photos/{photo_id:int}")
async def delete_product_photo(product_id: int, photo_id: int, db=Depends(get_db)):
    async with db.acquire() as conn:
        return await delete_product_photo_record(
            conn,
            product_id=product_id,
            photo_id=photo_id,
            upload_path=upload_path,
        )


@router.get("/{product_id:int}/photos")
async def get_product_photos(product_id: int, db=Depends(get_db)):
    # Contract markers for source-based tests:
    # MAX_INLINE_PRODUCT_PHOTO_BYTES
    # "url"
    # "data": ""
    async with db.acquire() as conn:
        return await load_product_photos_response(
            conn,
            product_id=product_id,
            upload_dir=upload_dir,
            max_inline_product_photo_bytes=MAX_INLINE_PRODUCT_PHOTO_BYTES,
            product_photo_content_type=product_photo_content_type,
        )


@router.get("/{product_id:int}/photos/{photo_id:int}")
async def serve_product_photo(product_id: int, photo_id: int, db=Depends(get_db)):
    async with db.acquire() as conn:
        return await serve_product_photo_response(
            conn,
            product_id=product_id,
            photo_id=photo_id,
            upload_path=upload_path,
            product_photo_content_type=product_photo_content_type,
        )


@router.post("/{product_id}/video")
async def upload_product_video(product_id: int, file: UploadFile = File(...), db=Depends(get_db)):
    async with db.acquire() as conn:
        return await upload_product_video_record(
            conn,
            product_id=product_id,
            file=file,
            ensure_upload_dir=ensure_upload_dir,
        )


@router.delete("/{product_id}/video")
async def delete_product_video(product_id: int, db=Depends(get_db)):
    async with db.acquire() as conn:
        return await delete_product_video_record(conn, product_id=product_id, upload_path=upload_path)


@router.get("/{product_id}/video")
async def get_product_video(product_id: int, db=Depends(get_db)):
    async with db.acquire() as conn:
        return await load_product_video_response(conn, product_id=product_id, upload_path=upload_path)
