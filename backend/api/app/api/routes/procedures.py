from fastapi import APIRouter, HTTPException
from app.models.procedure import ProcedureCreate
from app.services import ProcedureService

router = APIRouter()


@router.get("")
async def get_procedures():
    return await ProcedureService.get_all()


@router.post("")
async def create_procedure(procedure: ProcedureCreate):
    return await ProcedureService.create(procedure)


@router.put("/{procedure_id}")
async def update_procedure(procedure_id: int, procedure: ProcedureCreate):
    result = await ProcedureService.update(procedure_id, procedure)
    if not result:
        raise HTTPException(status_code=404, detail="Procedure not found")
    return result


@router.delete("/{procedure_id}")
async def delete_procedure(procedure_id: int):
    await ProcedureService.delete(procedure_id)
    return {"success": True}
