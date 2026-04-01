from fastapi import APIRouter, HTTPException, UploadFile, File, Response
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


@router.get("/export")
def export_products():
    products = ProductService.get_all()
    import csv
    import io
    
    output = io.StringIO()
    
    if not products:
        return {"message": "No products to export"}
    
    fieldnames = ['name', 'what_is_it', 'brand', 'product_type', 'for_whom', 
                  'purpose', 'skin_type', 'application_time', 'area', 
                  'active_ingredient', 'volume', 'segment', 'composition', 
                  'application_info', 'country', 'country_origin', 'manufacturer', 'description']
    
    writer = csv.DictWriter(output, fieldnames=fieldnames, extrasaction='ignore')
    writer.writeheader()
    
    for product in products:
        row = {}
        for field in fieldnames:
            val = product.get(field)
            if isinstance(val, list):
                row[field] = '|'.join(val) if val else ''
            else:
                row[field] = val or ''
        writer.writerow(row)
    
    output.seek(0)
    
    return StreamingResponse(
        io.BytesIO(output.getvalue().encode('utf-8-sig')),
        media_type='text/csv; charset=utf-8',
        headers={'Content-Disposition': 'attachment; filename=products.csv'}
    )


@router.post("/import")
async def import_products(file: UploadFile = File(...)):
    import csv
    import io
    
    if not file.filename.endswith('.csv'):
        raise HTTPException(400, "Only CSV files allowed")
    
    contents = await file.read()
    text = contents.decode('utf-8-sig')
    reader = csv.DictReader(io.StringIO(text))
    
    created = 0
    errors = []
    
    for i, row in enumerate(reader):
        try:
            purpose = row.get('purpose', '').split('|') if row.get('purpose') else []
            purpose = [p.strip() for p in purpose if p.strip()]
            
            skin_type = row.get('skin_type', '').split('|') if row.get('skin_type') else []
            skin_type = [s.strip() for s in skin_type if s.strip()]
            
            product_data = ProductCreate(
                name=row.get('name', ''),
                what_is_it=row.get('what_is_it', ''),
                brand=row.get('brand', ''),
                product_type=row.get('product_type', ''),
                for_whom=row.get('for_whom', ''),
                purpose=purpose,
                skin_type=skin_type,
                application_time=row.get('application_time', ''),
                area=row.get('area', ''),
                active_ingredient=row.get('active_ingredient', ''),
                volume=row.get('volume', ''),
                segment=row.get('segment', ''),
                composition=row.get('composition', ''),
                application_info=row.get('application_info', ''),
                country=row.get('country', ''),
                country_origin=row.get('country_origin', ''),
                manufacturer=row.get('manufacturer', ''),
                description=row.get('description', ''),
                photos=[],
                has_video=False
            )
            ProductService.create(product_data)
            created += 1
        except Exception as e:
            errors.append(f"Row {i+2}: {str(e)}")
    
    return {"success": True, "created": created, "errors": errors if errors else None}


@router.get("/{product_id}")
def get_product(product_id: int):
    product = ProductService.get_by_id(product_id)
    if not product:
        raise HTTPException(status_code=404, detail="Product not found")
    return product


@router.post("")
def create_product(product: ProductCreate):
    result = ProductService.create(product)
    if not result:
        raise HTTPException(status_code=500, detail="Failed to create product")
    # Deserialize for consistent API response
    if result.get('purpose') and isinstance(result['purpose'], str):
        from app.services.product_service import deserialize_purpose
        result['purpose'] = deserialize_purpose(result['purpose'])
    if result.get('skin_type') and isinstance(result['skin_type'], str):
        from app.services.product_service import deserialize_purpose
        result['skin_type'] = deserialize_purpose(result['skin_type'])
    if result.get('photos') and isinstance(result['photos'], str):
        result['photos'] = json.loads(result['photos'])
    return result


@router.put("/{product_id}")
def update_product(product_id: int, product: ProductCreate):
    result = ProductService.update(product_id, product)
    if not result:
        raise HTTPException(status_code=404, detail="Product not found")
    # Deserialize for consistent API response
    if result.get('purpose') and isinstance(result['purpose'], str):
        from app.services.product_service import deserialize_purpose
        result['purpose'] = deserialize_purpose(result['purpose'])
    if result.get('skin_type') and isinstance(result['skin_type'], str):
        from app.services.product_service import deserialize_purpose
        result['skin_type'] = deserialize_purpose(result['skin_type'])
    if result.get('photos') and isinstance(result['photos'], str):
        result['photos'] = json.loads(result['photos'])
    return result


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
    return photo_item


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


import os
import shutil
from pathlib import Path

VIDEO_DIR = Path("C:/Users/krish/OneDrive/Desktop/Aura/backend/api/videos")
VIDEO_DIR.mkdir(exist_ok=True)

@router.post("/{product_id}/video")
async def upload_video(product_id: int, file: UploadFile = File(...)):
    try:
        print(f"Upload video request for product {product_id}, file: {file.filename}, content_type: {file.content_type}")
        
        if file.content_type != "video/mp4":
            raise HTTPException(400, "Only MP4 videos allowed")
        
        contents = await file.read()
        print(f"Read {len(contents)} bytes")
        
        product = ProductService.get_by_id(product_id)
        if not product:
            raise HTTPException(404, "Product not found")
        print(f"Product found: {product.get('name')}")
        
        # Save to disk
        video_path = VIDEO_DIR / f"{product_id}.mp4"
        print(f"Saving to: {video_path}")
        with open(video_path, "wb") as f:
            f.write(contents)
        print(f"Saved, file exists: {video_path.exists()}")
        
        result = ProductService.update(product_id, ProductCreate(**{**product, "video": str(video_path), "has_video": True}))
        print(f"Updated product, new video path in DB: {result.get('video') if result else 'None'}")
        
        return {"success": True, "filename": file.filename}
    except Exception as e:
        print(f"Upload error: {type(e).__name__}: {e}")
        raise HTTPException(500, f"Upload failed: {str(e)}")


@router.delete("/{product_id}/video")
async def delete_video(product_id: int):
    product = ProductService.get_by_id(product_id)
    if not product:
        raise HTTPException(404, "Product not found")
    
    video_path = VIDEO_DIR / f"{product_id}.mp4"
    if video_path.exists():
        video_path.unlink()
    
    ProductService.update(product_id, ProductCreate(**{**product, "video": None, "has_video": False}))
    return {"success": True}


@router.get("/{product_id}/video")
async def get_video(product_id: int):
    product = ProductService.get_by_id(product_id)
    if not product:
        raise HTTPException(404, "Product not found")
    
    video_path = product.get('video')
    print(f"Product {product_id} video path from DB: {video_path}")
    
    if not video_path:
        raise HTTPException(404, "Video not found")
    
    # Handle both disk path and legacy base64
    if os.path.exists(video_path):
        print(f"Serving video from disk: {video_path}")
        return StreamingResponse(
            open(video_path, "rb"),
            media_type="video/mp4",
            headers={"Content-Disposition": f"attachment; filename=video_{product_id}.mp4"}
        )
    
    # Legacy base64
    print("Serving video from legacy base64")
    video_data = product.get('video')
    if isinstance(video_data, str):
        video_data = base64.b64decode(video_data)
    
    return Response(
        content=video_data,
        media_type="video/mp4",
        headers={"Content-Disposition": f"attachment; filename=video_{product_id}.mp4"}
    )


@router.post("/parse")
async def parse_product_url(request: ParseRequest):
    return await ProductService.parse_url(request.url)


@router.get("/export")
def export_products():
    products = ProductService.get_all()
    import csv
    import io
    
    output = io.StringIO()
    
    if not products:
        return {"message": "No products to export"}
    
    fieldnames = ['name', 'what_is_it', 'brand', 'product_type', 'for_whom', 
                  'purpose', 'skin_type', 'application_time', 'area', 
                  'active_ingredient', 'volume', 'segment', 'composition', 
                  'application_info', 'country', 'country_origin', 'manufacturer', 'description']
    
    writer = csv.DictWriter(output, fieldnames=fieldnames, extrasaction='ignore')
    writer.writeheader()
    
    for product in products:
        row = {}
        for field in fieldnames:
            val = product.get(field)
            if isinstance(val, list):
                row[field] = '|'.join(val) if val else ''
            else:
                row[field] = val or ''
        writer.writerow(row)
    
    output.seek(0)
    
    return StreamingResponse(
        io.BytesIO(output.getvalue().encode('utf-8-sig')),
        media_type='text/csv; charset=utf-8',
        headers={'Content-Disposition': 'attachment; filename=products.csv'}
    )


@router.post("/import")
async def import_products(file: UploadFile = File(...)):
    import csv
    import io
    
    if not file.filename.endswith('.csv'):
        raise HTTPException(400, "Only CSV files allowed")
    
    contents = await file.read()
    text = contents.decode('utf-8-sig')
    reader = csv.DictReader(io.StringIO(text))
    
    created = 0
    errors = []
    
    for i, row in enumerate(reader):
        try:
            purpose = row.get('purpose', '').split('|') if row.get('purpose') else []
            purpose = [p.strip() for p in purpose if p.strip()]
            
            skin_type = row.get('skin_type', '').split('|') if row.get('skin_type') else []
            skin_type = [s.strip() for s in skin_type if s.strip()]
            
            product_data = ProductCreate(
                name=row.get('name', ''),
                what_is_it=row.get('what_is_it', ''),
                brand=row.get('brand', ''),
                product_type=row.get('product_type', ''),
                for_whom=row.get('for_whom', ''),
                purpose=purpose,
                skin_type=skin_type,
                application_time=row.get('application_time', ''),
                area=row.get('area', ''),
                active_ingredient=row.get('active_ingredient', ''),
                volume=row.get('volume', ''),
                segment=row.get('segment', ''),
                composition=row.get('composition', ''),
                application_info=row.get('application_info', ''),
                country=row.get('country', ''),
                country_origin=row.get('country_origin', ''),
                manufacturer=row.get('manufacturer', ''),
                description=row.get('description', ''),
                photos=[],
                has_video=False
            )
            ProductService.create(product_data)
            created += 1
        except Exception as e:
            errors.append(f"Row {i+2}: {str(e)}")
    
    return {"success": True, "created": created, "errors": errors if errors else None}
