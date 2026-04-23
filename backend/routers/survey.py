from fastapi import APIRouter, Depends

from backend.core.security import get_current_user
from backend.core.survey_schema import SURVEY_SCHEMA


router = APIRouter()


@router.get("/api/survey/schema")
async def get_survey_schema(current_user: dict = Depends(get_current_user)):
    return SURVEY_SCHEMA
