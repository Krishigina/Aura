from fastapi import APIRouter, HTTPException
from app.models.dictionary import DictionaryValue, DictionaryUpdate
from app.services import DictionaryService

router = APIRouter()


@router.get("/{key}")
async def get_dictionary(key: str):
    values = await DictionaryService.get_values(key)
    if not DictionaryService.get_table(key):
        raise HTTPException(status_code=400, detail="Unknown dictionary key")
    return values


@router.post("/{key}")
async def create_dictionary_value(key: str, data: DictionaryValue):
    try:
        return await DictionaryService.create_value(key, data.value)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.put("/{key}")
async def update_dictionary_value(key: str, data: DictionaryUpdate):
    try:
        return await DictionaryService.update_value(key, data.oldValue, data.newValue)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.delete("/{key}/{value}")
async def delete_dictionary_value(key: str, value: str):
    try:
        await DictionaryService.delete_value(key, value)
        return {"success": True}
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
