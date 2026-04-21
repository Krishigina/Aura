from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Dict, Any, Optional, Union
from app.services.rag_pipeline import get_rag_pipeline
import logging

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/rag", tags=["RAG"])

class RAGQueryRequest(BaseModel):
    query: str
    user_id: str
    max_results: int = 5

@router.post("/query")
async def query_rag(request: RAGQueryRequest):
    """Query the RAG system"""
    try:
        pipeline = get_rag_pipeline()
        result = pipeline.query(request.query, {"user_id": request.user_id}, request.max_results)
        return result
    except Exception as e:
        logger.error(f"RAG query error: {e}")
        raise HTTPException(status_code=500, detail=str(e))

class Document(BaseModel):
    title: str
    content: str
    category: Optional[str] = None
    source_type: Optional[str] = None
    source_id: Optional[Union[int, str]] = None
    source_scope: Optional[str] = "global"
    owner_user_id: Optional[str] = None
    weight: Optional[float] = 1.0

@router.post("/ingest")
async def ingest_documents(documents: List[Document]):
    """Ingest documents into knowledge base"""
    try:
        pipeline = get_rag_pipeline()
        docs = [
            {
                "title": d.title,
                "content": d.content,
                "category": d.category,
                "source_type": d.source_type,
                "source_id": d.source_id,
                "source_scope": d.source_scope,
                "owner_user_id": d.owner_user_id,
                "weight": d.weight,
            }
            for d in documents
        ]
        return pipeline.ingest_documents(docs)
    except Exception as e:
        logger.error(f"Ingest error: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@router.delete("/knowledge")
async def delete_knowledge():
    """Delete all knowledge"""
    try:
        pipeline = get_rag_pipeline()
        return pipeline.delete_knowledge_base()
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
