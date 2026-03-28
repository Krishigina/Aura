from fastapi import APIRouter, HTTPException
from app.models.product import ProductCreate
from app.services import ProductService

router = APIRouter()


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
