from fastapi import APIRouter, HTTPException
from app.models.dictionary import DictionaryValue, DictionaryUpdate
from app.services import DictionaryService

router = APIRouter()


@router.get("/{key}")
def get_dictionary(key: str):
    values = DictionaryService.get_values(key)
    if not DictionaryService.get_table(key):
        raise HTTPException(status_code=400, detail="Unknown dictionary key")
    return values


@router.post("/{key}")
def create_dictionary_value(key: str, data: DictionaryValue):
    try:
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
