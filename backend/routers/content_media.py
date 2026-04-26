from fastapi import APIRouter, Depends, File, UploadFile

from backend.core.content_media_service import (
    build_content_image_url,
    content_media_root,
    delete_content_card_image_record,
    delete_content_image_record,
    load_content_images,
    serve_content_card_image,
    upload_content_card_image_record,
    upload_content_image_record,
)
from backend.db.pool import get_db


router = APIRouter(prefix="/api/content", tags=["Content Media"])


@router.post("/{content_id}/images")
async def upload_content_image(content_id: int, file: UploadFile = File(...), db=Depends(get_db)):
    async with db.acquire() as conn:
        return await upload_content_image_record(
            conn,
            content_id=content_id,
            file=file,
            api_root=content_media_root(__file__),
        )


@router.delete("/{content_id}/images/{image_id}")
async def delete_content_image(content_id: int, image_id: int, db=Depends(get_db)):
    async with db.acquire() as conn:
        return await delete_content_image_record(
            conn,
            content_id=content_id,
            image_id=image_id,
            api_root=content_media_root(__file__),
        )


@router.get("/{content_id}/images")
async def get_content_images(content_id: int, db=Depends(get_db)):
    async with db.acquire() as conn:
        return await load_content_images(conn, content_id=content_id, api_root=content_media_root(__file__))


@router.get("/{content_id}/image-url")
async def get_content_image_url(content_id: int):
    return build_content_image_url(content_id)


@router.post("/{content_id}/card-image")
async def upload_content_card_image(content_id: int, file: UploadFile = File(...), db=Depends(get_db)):
    async with db.acquire() as conn:
        return await upload_content_card_image_record(
            conn,
            content_id=content_id,
            file=file,
            api_root=content_media_root(__file__),
        )


@router.get("/card-image/{filename}")
async def get_content_card_image(filename: str):
    return serve_content_card_image(filename, api_root=content_media_root(__file__))


@router.delete("/{content_id}/card-image")
async def delete_content_card_image(content_id: int, db=Depends(get_db)):
    async with db.acquire() as conn:
        return await delete_content_card_image_record(conn, content_id=content_id, api_root=content_media_root(__file__))
