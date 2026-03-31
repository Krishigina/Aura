from fastapi import APIRouter, HTTPException
from app.models.procedure import ProcedureCreate
from app.services import ProcedureService

router = APIRouter()


@router.get("")
def get_procedures():
    return ProcedureService.get_all()


@router.post("")
def create_procedure(procedure: ProcedureCreate):
    return ProcedureService.create(procedure)


@router.put("/{procedure_id}")
def update_procedure(procedure_id: int, procedure: ProcedureCreate):
    result = ProcedureService.update(procedure_id, procedure)
    if not result:
        raise HTTPException(status_code=404, detail="Procedure not found")
    return result


@router.delete("/{procedure_id}")
def delete_procedure(procedure_id: int):
    ProcedureService.delete(procedure_id)
    return {"success": True}
