from fastapi import APIRouter

from backend.core.product_parse_service import parse_product_page
from backend.schemas.product_parse import ProductParseRequest


router = APIRouter(prefix="/api/products", tags=["Product Parse"])


@router.post("/parse")
async def parse_product_url(request: ProductParseRequest):
    return await parse_product_page(request.url)
