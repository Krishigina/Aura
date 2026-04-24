from typing import List, Optional, Union

from pydantic import BaseModel


class ProcedureCreate(BaseModel):
    name: str
    direction: Optional[str] = None
    method_type: Optional[str] = None
    duration: Optional[Union[str, int]] = None
    equipment: Optional[str] = None
    zones: Optional[Union[List[str], str]] = None
    effects: Optional[Union[List[str], str]] = None
    problems: Optional[Union[List[str], str]] = None
    description: Optional[str] = None
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
