from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Dict, Any
from app.services.hybrid_recommendation import get_recommendation_engine

router = APIRouter(prefix="/recommendations", tags=["Hybrid Recommendations"])

class HybridRecommendationRequest(BaseModel):
    user_id: str
    skin_type: str
    concerns: List[str] = []
    allergies: List[str] = []
    goals: List[str] = []
    limit: int = 10

@router.post("/hybrid")
async def get_hybrid_recommendations(request: HybridRecommendationRequest):
    """
    Hybrid Recommendation Engine
    Combines rule-based filtering + AI vector search
    """
    try:
        engine = get_recommendation_engine()
        
        user_profile = {
            "skin_type": request.skin_type,
            "concerns": request.concerns,
            "allergies": request.allergies,
            "goals": request.goals
        }
        
        # Get products from database (would be from API in real app)
        products = get_sample_products()
        
        recommendations = engine.get_recommendations(user_profile, products, request.limit)
        
        return {
            "recommendations": recommendations,
            "total": len(recommendations),
            "type": "hybrid"
        }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

def get_sample_products() -> List[Dict[str, Any]]:
    """Sample products for demo"""
    return [
        {
            "id": "1",
            "name": "Увлажняющий крем с гиалуроновой кислотой",
            "brand": "Aura",
            "description": "Легкий увлажняющий крем",
            "suitable_skin_types": ["DRY", "NORMAL", "COMBINATION"],
            "target_concerns": ["DRYNESS", "AGING"],
            "ingredients": "water, glycerin, hyaluronic acid, ceramides",
            "popularity": 0.8
        },
        {
            "id": "2", 
            "name": "Очищающий гель для жирной кожи",
            "brand": "Aura",
            "description": "Гель с салициловой кислотой",
            "suitable_skin_types": ["OILY", "COMBINATION"],
            "target_concerns": ["ACNE", "OILINESS"],
            "ingredients": "water, salicylic acid, glycerin",
            "popularity": 0.7
        },
        {
            "id": "3",
            "name": "Сыворотка с витамином C",
            "brand": "Aura",
            "description": "Осветляющая сыворотка",
            "suitable_skin_types": ["NORMAL", "COMBINATION", "OILY"],
            "target_concerns": ["HYPERPIGMENTATION", "AGING"],
            "ingredients": "water, vitamin c, niacinamide, glycerin",
            "popularity": 0.9
        },
        {
            "id": "4",
            "name": "Ночной крем с ретинолом",
            "brand": "Aura",
            "description": "Антивозрастной крем",
            "suitable_skin_types": ["NORMAL", "OILY"],
            "target_concerns": ["AGING", "HYPERPIGMENTATION"],
            "ingredients": "water, retinol, niacinamide, ceramides",
            "popularity": 0.6
        }
    ]