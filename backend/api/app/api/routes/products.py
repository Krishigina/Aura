from fastapi import APIRouter, HTTPException, UploadFile, File
from fastapi.responses import StreamingResponse, RedirectResponse
from pydantic import BaseModel
from app.models.product import ProductCreate
from app.services import ProductService
from app.database import get_pool
import io
import base64
import uuid
import json

router = APIRouter()


class ParseRequest(BaseModel):
    url: str


@router.get("")
def get_products():
    return ProductService.get_all()


@router.get("/{product_id}")
def get_product(product_id: int):
    product = ProductService.get_by_id(product_id)
    if not product:
        raise HTTPException(status_code=404, detail="Product not found")
    return product


@router.post("")
def create_product(product: ProductCreate):
    print(f"Create product:", product.model_dump())
    try:
        return ProductService.create(product)
    except Exception as e:
        print(f"Error creating product: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.put("/{product_id}")
def update_product(product_id: int, product: ProductCreate):
    print(f"Update product {product_id}:", product.model_dump())
    try:
        result = ProductService.update(product_id, product)
        if not result:
            raise HTTPException(status_code=404, detail="Product not found")
        return result
    except Exception as e:
        print(f"Error updating product: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.delete("/{product_id}")
def delete_product(product_id: int):
    ProductService.delete(product_id)
    return {"success": True}


@router.post("/{product_id}/photos")
async def upload_photo(product_id: int, file: UploadFile = File(...)):
    if not file.content_type.startswith('image/'):
        raise HTTPException(400, "Only images allowed")
    
    contents = await file.read()
    photo_data = base64.b64encode(contents).decode()
    
    product = ProductService.get_by_id(product_id)
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
    
    ProductService.update(product_id, ProductCreate(**{**product, "photos": photos, "has_video": product.get('has_video', False)}))
    return {"id": photo_item["id"], "filename": file.filename}


@router.delete("/{product_id}/photos/{photo_id}")
async def delete_photo(product_id: int, photo_id: str):
    product = ProductService.get_by_id(product_id)
    if not product:
        raise HTTPException(404, "Product not found")
    
    photos = [p for p in (product.get('photos') or []) if p.get('id') != photo_id]
    ProductService.update(product_id, ProductCreate(**{**product, "photos": photos, "has_video": product.get('has_video', False)}))
    return {"success": True}


@router.get("/{product_id}/photos/{photo_id}")
async def get_photo(product_id: int, photo_id: str):
    product = ProductService.get_by_id(product_id)
    if not product:
        raise HTTPException(404, "Product not found")
    
    photos = product.get('photos') or []
    photo = next((p for p in photos if p.get('id') == photo_id), None)
    if not photo:
        raise HTTPException(404, "Photo not found")
    
    if photo.get('data') and len(photo.get('data', '')) > 0:
        import base64
        img_data = base64.b64decode(photo['data'])
        content_type = photo.get('content_type', 'image/jpeg')
        return StreamingResponse(
            io.BytesIO(img_data),
            media_type=content_type,
            headers={"Content-Disposition": f"inline; filename={photo.get('filename', 'photo')}"}
        )
    
    pool = get_pool()
    conn = pool.getconn()
    try:
        cursor = conn.cursor()
        cursor.execute("SELECT image_url FROM api_product WHERE id = %s", (product_id,))
        row = cursor.fetchone()
        if row and row[0]:
            return RedirectResponse(url=row[0])
        raise HTTPException(404, "Photo not found")
    finally:
        pool.putconn(conn)


@router.post("/{product_id}/video")
async def upload_video(product_id: int, file: UploadFile = File(...)):
    if file.content_type not in ["video/mp4", "video/webm"]:
        raise HTTPException(400, "Only MP4 and WebM videos allowed")
    
    contents = await file.read()
    
    product = ProductService.get_by_id(product_id)
    if not product:
        raise HTTPException(404, "Product not found")
    
    import base64
    video_data = base64.b64encode(contents).decode()
    
    ProductService.update(product_id, ProductCreate(**{**product, "video": video_data, "has_video": True}))
    return {"success": True, "filename": file.filename}


@router.delete("/{product_id}/video")
async def delete_video(product_id: int):
    product = ProductService.get_by_id(product_id)
    if not product:
        raise HTTPException(404, "Product not found")
    
    ProductService.update(product_id, ProductCreate(**{**product, "video": None, "has_video": False}))
    return {"success": True}


@router.get("/{product_id}/video")
async def get_video(product_id: int):
    product = ProductService.get_by_id(product_id)
    if not product or not product.get('video'):
        raise HTTPException(404, "Video not found")
    
    video_data = product['video']
    if isinstance(video_data, str):
        video_data = base64.b64decode(video_data)
    return StreamingResponse(
        io.BytesIO(video_data),
        media_type="video/mp4",
        headers={"Content-Disposition": f"inline; filename=video.mp4"}
    )


@router.post("/parse")
async def parse_product_url(request: ParseRequest):
    return await ProductService.parse_url(request.url)
