from fastapi import APIRouter, HTTPException, UploadFile, File
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from app.models.product import ProductCreate
from app.services import ProductService
import io
import base64
import uuid
import json

router = APIRouter()


class ParseRequest(BaseModel):
    url: str


@router.get("")
async def get_products():
    return await ProductService.get_all()


@router.post("")
async def create_product(product: ProductCreate):
    return await ProductService.create(product)


@router.put("/{product_id}")
async def update_product(product_id: int, product: ProductCreate):
    result = await ProductService.update(product_id, product)
    if not result:
        raise HTTPException(status_code=404, detail="Product not found")
    return result


@router.delete("/{product_id}")
async def delete_product(product_id: int):
    await ProductService.delete(product_id)
    return {"success": True}


@router.post("/{product_id}/photos")
async def upload_photo(product_id: int, file: UploadFile = File(...)):
    if not file.content_type.startswith('image/'):
        raise HTTPException(400, "Only images allowed")
    
    contents = await file.read()
    photo_data = base64.b64encode(contents).decode()
    
    product = await ProductService.get_by_id(product_id)
    if not product:
        raise HTTPException(404, "Product not found")
    
    photos = product.get('photos') or []
    photo_item = {
        "id": str(uuid.uuid4()),
        "filename": file.filename,
        "data": photo_data,
        "content_type": file.content_type
    }
    photos.append(photo_item)
    
    await ProductService.update(product_id, {"photos": photos, "has_video": product.get('has_video', False)})
    return {"id": photo_item["id"], "filename": file.filename}


@router.delete("/{product_id}/photos/{photo_id}")
async def delete_photo(product_id: int, photo_id: str):
    product = await ProductService.get_by_id(product_id)
    if not product:
        raise HTTPException(404, "Product not found")
    
    photos = [p for p in (product.get('photos') or []) if p.get('id') != photo_id]
    await ProductService.update(product_id, {"photos": photos, "has_video": product.get('has_video', False)})
    return {"success": True}


@router.post("/{product_id}/video")
async def upload_video(product_id: int, file: UploadFile = File(...)):
    if file.content_type != "video/mp4":
        raise HTTPException(400, "Only MP4 videos allowed")
    
    contents = await file.read()
    
    product = await ProductService.get_by_id(product_id)
    if not product:
        raise HTTPException(404, "Product not found")
    
    await ProductService.update(product_id, {"video": contents, "has_video": True})
    return {"success": True, "filename": file.filename}


@router.delete("/{product_id}/video")
async def delete_video(product_id: int):
    product = await ProductService.get_by_id(product_id)
    if not product:
        raise HTTPException(404, "Product not found")
    
    await ProductService.update(product_id, {"video": None, "has_video": False})
    return {"success": True}


@router.get("/{product_id}/video")
async def get_video(product_id: int):
    product = await ProductService.get_by_id(product_id)
    if not product or not product.get('video'):
        raise HTTPException(404, "Video not found")
    
    video_data = product['video']
    return StreamingResponse(
        io.BytesIO(video_data),
        media_type="video/mp4",
        headers={"Content-Disposition": f"inline; filename=video.mp4"}
    )


@router.post("/parse")
async def parse_product_url(request: ParseRequest):
    return await ProductService.parse_url(request.url)
