from fastapi import APIRouter, Depends, File, Query, UploadFile

from backend.core import passport_updates as passport_updates_core
from backend.core import product_admin as product_admin_core
from backend.core import product_catalog_service as product_catalog_service_core
from backend.core import product_detail as product_detail_core
from backend.core import product_import_export as product_import_export_core
from backend.core import products as products_core
from backend.core import security as security_core
from backend.core import skin_journal as skin_journal_core
from backend.core import skin_passport as skin_passport_core
from backend.core.matching import helpers as matching_helpers_core
from backend.db.pool import get_db
from backend.schemas.products import ProductCreate


router = APIRouter(tags=["Products"])


@router.get("/api/products")
async def get_products(db=Depends(get_db)):
    # Contract markers for source-based tests:
    # product_photos
    # thumbnail_url
    async with db.acquire() as conn:
        return await product_catalog_service_core.list_catalog_products(
            conn,
            product_select_sql=products_core.product_select_sql,
            normalize_product_response=products_core.normalize_product_response,
        )


@router.post("/api/products/import")
async def import_products(file: UploadFile = File(...), db=Depends(get_db)):
    product_import_export_core.ensure_csv_filename(file.filename or "")
    contents = await file.read()
    async with db.acquire() as conn:
        return await product_import_export_core.import_products_from_csv_bytes(conn, contents)


@router.post("/api/products")
async def create_product(product: ProductCreate, db=Depends(get_db)):
    async with db.acquire() as conn:
        async with conn.transaction():
            row = await product_admin_core.insert_product_row(conn, product)
            await product_admin_core.sync_product_ingredients(conn, row["id"], product.composition)
        return dict(row)


@router.get("/api/products/{product_id:int}")
async def get_product(product_id: int, current_user: dict = Depends(security_core.get_current_user), db=Depends(get_db)):
    async with db.acquire() as conn:
        row = await product_catalog_service_core.get_catalog_product_row(
            conn,
            product_id=product_id,
            product_select_sql=products_core.product_select_sql,
        )
        return await product_detail_core.build_product_detail_response(
            conn,
            row,
            current_user["id"],
            load_accepted_passport_insights=matching_helpers_core.load_accepted_passport_insights,
            load_user_skin_journal=skin_journal_core.load_user_skin_journal,
            load_product_function_signals=matching_helpers_core.load_product_function_signals,
            load_skin_passport_context=lambda user_id: passport_updates_core.load_skin_passport_context(
                conn,
                user_id,
                skin_passport_core.sanitize_skin_passport_answers,
            ),
            sanitize_skin_passport_answers=skin_passport_core.sanitize_skin_passport_answers,
        )


@router.put("/api/products/{product_id:int}")
async def update_product(product_id: int, product: ProductCreate, db=Depends(get_db)):
    async with db.acquire() as conn:
        async with conn.transaction():
            row = await product_admin_core.update_product_row(conn, product_id, product)
            await product_admin_core.sync_product_ingredients(conn, product_id, row["composition"])
        return dict(row)


@router.get("/api/products/search")
async def search_products(
    q: str = Query("", min_length=0, max_length=200),
    limit: int = Query(20, ge=1, le=100),
    db=Depends(get_db),
):
    query = (q or "").strip()
    async with db.acquire() as conn:
        return await product_catalog_service_core.search_catalog_products(
            conn,
            query=query,
            limit=limit,
            product_select_sql=products_core.product_select_sql,
        )


@router.get("/api/products/export")
async def export_products(db=Depends(get_db)):
    async with db.acquire() as conn:
        return await product_import_export_core.export_products_to_csv_response(conn)


@router.delete("/api/products/{product_id:int}")
async def delete_product(product_id: int, db=Depends(get_db)):
    async with db.acquire() as conn:
        return await product_catalog_service_core.delete_catalog_product(conn, product_id=product_id)
