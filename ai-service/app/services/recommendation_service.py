from typing import Optional
from app.services.hybrid_recommendation import get_recommendation_engine
from app.models.schemas import RecommendationRequest, RecommendationResponse

class RecommendationService:
    def __init__(self):
        self.engine = get_recommendation_engine()
    
    async def get_recommendations(self, request: RecommendationRequest) -> RecommendationResponse:
        user_profile = {
            'skin_type': request.skin_type,
            'allergies': request.allergies or [],
            'concerns': request.concerns or [],
            'goals': request.goals or []
        }
        
        products = request.products or []
        
        recommendations = self.engine.get_recommendations(
            user_profile=user_profile,
            products=products,
            limit=request.limit or 10
        )
        
        return RecommendationResponse(
            recommendations=recommendations,
            total=len(recommendations)
        )

_service: Optional[RecommendationService] = None

def get_recommendation_service() -> RecommendationService:
    global _service
    if _service is None:
        _service = RecommendationService()
    return _service