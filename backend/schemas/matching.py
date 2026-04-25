from typing import Any, Dict, Optional

from pydantic import BaseModel, Field


class MatchingRuleCreate(BaseModel):
    rule_type: str
    target_type: str
    target_id: Optional[int] = None
    target_key: Optional[str] = None
    condition_type: str
    condition_value: str
    effect: str
    weight_delta: float = 0
    severity: str = "info"
    source_id: Optional[int] = None
    evidence_quote: str = ""
    confidence: float = 1.0
    status: str = "draft"


class MatchingRuleUpdate(BaseModel):
    rule_type: Optional[str] = None
    target_type: Optional[str] = None
    target_id: Optional[int] = None
    target_key: Optional[str] = None
    condition_type: Optional[str] = None
    condition_value: Optional[str] = None
    effect: Optional[str] = None
    weight_delta: Optional[float] = None
    severity: Optional[str] = None
    source_id: Optional[int] = None
    evidence_quote: Optional[str] = None
    confidence: Optional[float] = None
    status: Optional[str] = None


class IngredientFactReviewUpdate(BaseModel):
    evidence_status: str
    effect_key: Optional[str] = None
    condition_type: Optional[str] = None
    condition_value: Optional[str] = None
    matching_effect: Optional[str] = None
    matching_weight_delta: Optional[float] = None


class ProductMatchingRequest(BaseModel):
    limit: int = 200
    min_compatibility_percent: int = Field(default=50, ge=0, le=100)


class RecommendationFavoriteCreate(BaseModel):
    recommendation: Dict[str, Any]


class RecommendationFeedbackCreate(BaseModel):
    product_id: str
    action: str
    rank: Optional[int] = Field(default=None, ge=1)
    algorithm_version: str = "backend-matching-v1"
    metadata: Dict[str, Any] = Field(default_factory=dict)
