from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Optional
from app.models.dictionary import DictionaryValue, DictionaryUpdate
from app.services import DictionaryService

router = APIRouter()


class BrandUpdate(BaseModel):
    value: str
    description: Optional[str] = None
    country: Optional[str] = None
    country_origin: Optional[str] = None
    manufacturer: Optional[str] = None


@router.put("/brands")
def update_brand(data: BrandUpdate):
    try:
        return DictionaryService.update_brand(data.value, data.description, data.country, data.country_origin, data.manufacturer)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/{key}")
def get_dictionary(key: str):
    if key == 'brands':
        return DictionaryService.get_brands()
    values = DictionaryService.get_values(key)
    if not DictionaryService.get_table(key):
        raise HTTPException(status_code=400, detail="Unknown dictionary key")
    return values


@router.post("/{key}")
def create_dictionary_value(key: str, data: DictionaryValue):
    try:
        if key == 'brands':
            return DictionaryService.create_brand(data.value)
        return DictionaryService.create_value(key, data.value)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.put("/{key}")
def update_dictionary_value(key: str, data: DictionaryUpdate):
    try:
        return DictionaryService.update_value(key, data.oldValue, data.newValue)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.delete("/{key}/{value}")
def delete_dictionary_value(key: str, value: str):
    try:
        DictionaryService.delete_value(key, value)
        return {"success": True}
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
