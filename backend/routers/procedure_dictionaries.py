from fastapi import APIRouter, Depends, HTTPException

from backend.db.pool import get_db


router = APIRouter(prefix="/api/procedures/dictionaries", tags=["Procedure Dictionaries"])


PROCEDURE_DICT_TABLES = {
    "methodTypes": "procedure_method_types",
    "procedureDurations": "procedure_durations",
    "procedureEquipment": "procedure_equipment",
    "procedureZones": "procedure_zones",
    "procedureEffects": "procedure_effects",
    "procedureProblems": "procedure_problems",
}


async def _list_values(db, table_name: str):
    async with db.acquire() as conn:
        rows = await conn.fetch(f"SELECT id, value FROM {table_name} ORDER BY id")
        return [{"id": row["id"], "value": row["value"]} for row in rows]


@router.get("/method-types")
async def get_method_types(db=Depends(get_db)):
    return await _list_values(db, "procedure_method_types")


@router.get("/durations")
async def get_durations(db=Depends(get_db)):
    return await _list_values(db, "procedure_durations")


@router.get("/equipment")
async def get_equipment(db=Depends(get_db)):
    return await _list_values(db, "procedure_equipment")


@router.get("/zones")
async def get_zones(db=Depends(get_db)):
    return await _list_values(db, "procedure_zones")


@router.get("/effects")
async def get_effects(db=Depends(get_db)):
    return await _list_values(db, "procedure_effects")


@router.get("/problems")
async def get_problems(db=Depends(get_db)):
    return await _list_values(db, "procedure_problems")


@router.post("/{dict_type}")
async def add_procedure_dict_value(dict_type: str, value: str, db=Depends(get_db)):
    table_name = PROCEDURE_DICT_TABLES.get(dict_type)
    if not table_name:
        raise HTTPException(status_code=400, detail="Invalid dictionary type")
    async with db.acquire() as conn:
        row = await conn.fetchrow(f"INSERT INTO {table_name} (value) VALUES ($1) RETURNING *", value)
        return dict(row)


@router.delete("/{dict_type}/{value}")
async def delete_procedure_dict_value(dict_type: str, value: str, db=Depends(get_db)):
    table_name = PROCEDURE_DICT_TABLES.get(dict_type)
    if not table_name:
        raise HTTPException(status_code=400, detail="Invalid dictionary type")
    async with db.acquire() as conn:
        await conn.execute(f"DELETE FROM {table_name} WHERE value=$1", value)
        return {"success": True}
