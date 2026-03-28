from pydantic import BaseModel
from typing import Optional


class ProcedureBase(BaseModel):
    name: str
    category: Optional[str] = None
    duration: Optional[int] = None
    price: Optional[float] = None
    description: Optional[str] = None
    contraindications: Optional[str] = None


class ProcedureCreate(ProcedureBase):
    pass


class Procedure(ProcedureBase):
    id: int
    created_at: Optional[str] = None

    class Config:
        from_attributes = True
