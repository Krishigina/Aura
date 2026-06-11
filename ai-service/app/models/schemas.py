from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any

class RAGRequest(BaseModel):
    query: str
    user_id: str
    session_id: Optional[str] = None
    context: Optional[Dict[str, Any]] = None
    max_results: int = 5

class RAGAttachmentIngestRequest(BaseModel):
    attachment_id: str
    user_id: str
    session_id: str
    filename: str
    content_type: str
    content_base64: str

class AttachmentIngestResponse(BaseModel):
    attachment_id: str
    summary: str
    extracted_text: str = ""
    indexed: bool = True

class Source(BaseModel):
    id: str
    title: str
    content: str = ""
    score: Optional[float] = None
    source_type: Optional[str] = None
    category: Optional[str] = None

    def __getitem__(self, key: str) -> Any:
        return getattr(self, key)

class RAGResponse(BaseModel):
    answer: str
    sources: List[Source]
    conversation_id: Optional[str] = None

class LLMStructuredAnswer(BaseModel):
    answer: str
    confidence: str = Field(..., pattern=r"^(high|medium|low|none)$")
    sources_used: List[int]

class RecommendationRequest(BaseModel):
    user_id: str
    skin_type: str
    concerns: List[str]
    allergies: List[str] = []
    goals: List[str]
    limit: int = 10

class AIRecommendation(BaseModel):
    product_id: str
    score: float = Field(ge=0.0, le=1.0)
    reason: str

class RecommendationResponse(BaseModel):
    recommendations: List[AIRecommendation]
    model_version: str = "v1"

class IngredientAnalysisRequest(BaseModel):
    ingredients: List[str]

class IngredientInfo(BaseModel):
    name: str
    inci_name: str
    safety_level: str
    category: str
    benefits: List[str] = []
    risks: List[str] = []
    description: Optional[str] = None

class IngredientAnalysisResponse(BaseModel):
    ingredients: List[IngredientInfo]
    total_count: int
    safe_count: int
    moderate_count: int
    caution_count: int
    avoid_count: int
    warnings: List[str]

class HealthResponse(BaseModel):
    status: str
    version: str
    model_loaded: bool
