from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List
from app.services.ingredient_analyzer import get_ingredient_analyzer

router = APIRouter(prefix="/ingredients", tags=["Ingredient Analysis"])

class AnalyzeRequest(BaseModel):
    ingredients: List[str]
    skin_type: str = "NORMAL"

class AnalysisResult(BaseModel):
    total_count: int
    safe_count: int
    moderate_count: int
    caution_count: int
    avoid_count: int
    overall_score: float
    warnings: List[str]
    compatibility: dict = {}

@router.post("/analyze")
async def analyze_ingredients(request: AnalyzeRequest) -> AnalysisResult:
    """Analyze ingredients for safety and compatibility"""
    try:
        analyzer = get_ingredient_analyzer()
        
        # Analyze safety
        analysis = analyzer.analyze(request.ingredients)
        
        # Check skin type compatibility
        compatibility = analyzer.check_skin_type_compatibility(
            request.ingredients, 
            request.skin_type
        )
        
        return AnalysisResult(
            total_count=analysis["total_count"],
            safe_count=analysis["safe_count"],
            moderate_count=analysis["moderate_count"],
            caution_count=analysis["caution_count"],
            avoid_count=analysis["avoid_count"],
            overall_score=analysis["overall_score"],
            warnings=analysis["warnings"],
            compatibility=compatibility
        )
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/db/{ingredient}")
async def get_ingredient_info(ingredient: str):
    """Get information about a specific ingredient"""
    analyzer = get_ingredient_analyzer()
    info = analyzer._get_ingredient_info(ingredient)
    
    if info:
        return info
    raise HTTPException(status_code=404, detail="Ingredient not found")