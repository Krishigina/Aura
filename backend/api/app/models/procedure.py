from pydantic import BaseModel
from typing import Optional, List


class ProcedureBase(BaseModel):
    name: str
    category: Optional[str] = None
    duration: Optional[int] = None
    price: Optional[float] = None
    description: Optional[str] = None
    contraindications: Optional[str] = None
    
    direction: Optional[str] = None
    method_type: Optional[str] = None
    equipment: Optional[str] = None
    zones: Optional[List[str]] = []
    effects: Optional[List[str]] = []
    problems: Optional[List[str]] = []
    
    procedure_about: Optional[str] = None
    advantages: Optional[str] = None
    indications: Optional[str] = None
    principle: Optional[str] = None
    how_it_goes: Optional[str] = None
    for_whom: Optional[str] = None
    problems_solved: Optional[str] = None
    contraindications_full: Optional[str] = None
    preparation: Optional[str] = None
    recommended_course: Optional[str] = None
    rehabilitation: Optional[str] = None
    post_care: Optional[str] = None
    side_effects: Optional[str] = None
    
    photos: Optional[List[dict]] = []


class ProcedureCreate(ProcedureBase):
    pass


class Procedure(ProcedureBase):
    id: int
    created_at: Optional[str] = None

    class Config:
        from_attributes = True
