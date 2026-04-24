from fastapi import APIRouter, Depends, File, UploadFile

from backend.core.procedure_admin import (
    create_procedure_record,
    delete_procedure_photo_record,
    delete_procedure_record,
    list_procedure_photos,
    list_procedures,
    procedure_photos_dir,
    update_procedure_record,
    upload_procedure_photo_record,
)
from backend.db.pool import get_db
from backend.schemas.procedures import ProcedureCreate


router = APIRouter(prefix="/api/procedures", tags=["Procedures"])


@router.get("")
async def get_procedures(db=Depends(get_db)):
    async with db.acquire() as conn:
        return await list_procedures(conn)


@router.post("")
async def create_procedure(procedure: ProcedureCreate, db=Depends(get_db)):
    async with db.acquire() as conn:
        return await create_procedure_record(conn, procedure)


@router.put("/{procedure_id:int}")
async def update_procedure(procedure_id: int, procedure: ProcedureCreate, db=Depends(get_db)):
    async with db.acquire() as conn:
        return await update_procedure_record(conn, procedure_id, procedure)


@router.delete("/{procedure_id:int}")
async def delete_procedure(procedure_id: int, db=Depends(get_db)):
    async with db.acquire() as conn:
        return await delete_procedure_record(conn, procedure_id)


@router.post("/{procedure_id:int}/photos")
async def upload_procedure_photo(procedure_id: int, file: UploadFile = File(...), db=Depends(get_db)):
    uploads_dir = procedure_photos_dir(__file__)
    async with db.acquire() as conn:
        return await upload_procedure_photo_record(conn, procedure_id, file, uploads_dir)


@router.delete("/{procedure_id:int}/photos/{photo_id:int}")
async def delete_procedure_photo(procedure_id: int, photo_id: int, db=Depends(get_db)):
    uploads_dir = procedure_photos_dir(__file__)
    async with db.acquire() as conn:
        return await delete_procedure_photo_record(conn, procedure_id, photo_id, uploads_dir)


@router.get("/{procedure_id:int}/photos")
async def get_procedure_photos(procedure_id: int, db=Depends(get_db)):
    uploads_dir = procedure_photos_dir(__file__)
    async with db.acquire() as conn:
        return await list_procedure_photos(conn, procedure_id, uploads_dir)
