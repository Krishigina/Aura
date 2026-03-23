from fastapi import APIRouter, HTTPException
from app.models.schemas import RAGRequest, RAGResponse
from app.services.rag_service import get_rag_service
import logging

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/rag", tags=["RAG"])

@router.post("/query", response_model=RAGResponse)
async def query_rag(request: RAGRequest):
    try:
        rag_service = get_rag_service()
        return await rag_service.query(request)
    except Exception as e:
        logger.error(f"RAG query error: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/knowledge/add")
async def add_knowledge(documents: list):
    try:
        rag_service = get_rag_service()
        rag_service.add_knowledge(documents)
        return {"status": "success", "count": len(documents)}
    except Exception as e:
        logger.error(f"Add knowledge error: {e}")
        raise HTTPException(status_code=500, detail=str(e))