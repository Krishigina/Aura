from fastapi import APIRouter, Body, Depends, HTTPException

from backend.core.dictionary_admin import (
    create_dictionary_record,
    delete_dictionary_record,
    list_dictionary_values,
    update_brand_record,
    update_dictionary_record,
)
from backend.core.dictionaries import resolve_dict_table
from backend.db.pool import get_db
from backend.schemas.dictionaries import BrandUpdate, DictionaryValue


router = APIRouter(prefix="/api/dictionaries", tags=["Dictionaries"])


@router.put("/brands")
async def update_brand(data: BrandUpdate, db=Depends(get_db)):
    async with db.acquire() as conn:
        return await update_brand_record(conn, data)


@router.get("/{key}")
async def get_dictionary(key: str, db=Depends(get_db)):
    table = resolve_dict_table(key)
    if not table:
        raise HTTPException(status_code=400, detail="Unknown dictionary key")

    async with db.acquire() as conn:
        return await list_dictionary_values(conn, key=key, table=table)


@router.post("/{key}")
async def create_dictionary_value(key: str, data: DictionaryValue, db=Depends(get_db)):
    table = resolve_dict_table(key)
    if not table:
        raise HTTPException(status_code=400, detail="Unknown dictionary key")

    async with db.acquire() as conn:
        return await create_dictionary_record(conn, key=key, table=table, value=data.value)


@router.put("/{key}")
async def update_dictionary_value(key: str, data: dict = Body(...), db=Depends(get_db)):
    table = resolve_dict_table(key)
    if not table:
        raise HTTPException(status_code=400, detail="Unknown dictionary key")

    async with db.acquire() as conn:
        return await update_dictionary_record(conn, key=key, table=table, data=data)


@router.delete("/{key}/{value}")
async def delete_dictionary_value(key: str, value: str, db=Depends(get_db)):
    table = resolve_dict_table(key)
    if not table:
        raise HTTPException(status_code=400, detail="Unknown dictionary key")
    async with db.acquire() as conn:
        return await delete_dictionary_record(conn, table=table, value=value)
