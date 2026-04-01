from fastapi import APIRouter, HTTPException, UploadFile, File
from fastapi.responses import FileResponse
from app.models.procedure import ProcedureCreate
from app.services import ProcedureService
import os
import uuid

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


@router.get("/dictionaries/method-types")
def get_method_types():
    return ProcedureService.get_dictionary("procedure_method_types")


@router.get("/dictionaries/durations")
def get_durations():
    return ProcedureService.get_dictionary("procedure_durations")


@router.get("/dictionaries/equipment")
def get_equipment():
    return ProcedureService.get_dictionary("procedure_equipment")


@router.get("/dictionaries/zones")
def get_zones():
    return ProcedureService.get_dictionary("procedure_zones")


@router.get("/dictionaries/effects")
def get_effects():
    return ProcedureService.get_dictionary("procedure_effects")


@router.get("/dictionaries/problems")
def get_problems():
    return ProcedureService.get_dictionary("procedure_problems")


@router.post("/dictionaries/{dict_type}")
def add_dictionary_value(dict_type: str, value: str):
    table_map = {
        "methodTypes": "procedure_method_types",
        "procedureDurations": "procedure_durations",
        "procedureEquipment": "procedure_equipment",
        "procedureZones": "procedure_zones",
        "procedureEffects": "procedure_effects",
        "procedureProblems": "procedure_problems",
    }
    table_name = table_map.get(dict_type)
    if not table_name:
        raise HTTPException(status_code=400, detail="Invalid dictionary type")
    return ProcedureService.add_dictionary_value(table_name, value)


@router.delete("/dictionaries/{dict_type}/{value}")
def delete_dictionary_value(dict_type: str, value: str):
    table_map = {
        "methodTypes": "procedure_method_types",
        "procedureDurations": "procedure_durations",
        "procedureEquipment": "procedure_equipment",
        "procedureZones": "procedure_zones",
        "procedureEffects": "procedure_effects",
        "procedureProblems": "procedure_problems",
    }
    table_name = table_map.get(dict_type)
    if not table_name:
        raise HTTPException(status_code=400, detail="Invalid dictionary type")
    ProcedureService.delete_dictionary_value(table_name, value)
    return {"success": True}


@router.post("/{procedure_id}/photos")
async def upload_photo(procedure_id: int, file: UploadFile = File(...)):
    procedure = ProcedureService.get_by_id(procedure_id)
    if not procedure:
        raise HTTPException(status_code=404, detail="Procedure not found")
    
    base_dir = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
    uploads_dir = os.path.join(base_dir, "api", "procedure_photos")
    os.makedirs(uploads_dir, exist_ok=True)
    
    file_ext = file.filename.split('.')[-1] if '.' in file.filename else 'jpg'
    filename = f"{procedure_id}_{uuid.uuid4()}.{file_ext}"
    file_path = os.path.join(uploads_dir, filename)
    
    content = await file.read()
    with open(file_path, 'wb') as f:
        f.write(content)
    
    ProcedureService.add_photo(procedure_id, filename)
    return {"success": True, "filename": filename}


@router.delete("/{procedure_id}/photos/{photo_id}")
def delete_photo(procedure_id: int, photo_id: int):
    ProcedureService.delete_photo(procedure_id, photo_id)
    return {"success": True}


@router.get("/{procedure_id}/photos/{photo_id}")
def get_photo(procedure_id: int, photo_id: int):
    photo = ProcedureService.get_photo(procedure_id, photo_id)
    if not photo:
        raise HTTPException(status_code=404, detail="Photo not found")
    
    base_dir = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
    file_path = os.path.join(base_dir, "api", "procedure_photos", photo['filename'])
    return FileResponse(file_path)
