from typing import Any, Dict, List, Optional, Union
import logging

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

from app.models.schemas import RAGAttachmentIngestRequest, RAGRequest, RAGResponse
from app.services.rag_pipeline import get_rag_pipeline
from app.services.rag_service import get_rag_service

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/rag", tags=["RAG"])


class RAGQueryRequest(BaseModel):
    query: str
    user_id: str
    session_id: Optional[str] = None
    context: Optional[Dict[str, Any]] = None
    max_results: int = Field(default=5, ge=1, le=50)


@router.post("/query", response_model=RAGResponse)
async def query_rag(request: RAGQueryRequest):
    """Query the RAG system."""
    query = request.query.strip()
    if not query:
        raise HTTPException(status_code=400, detail="query must not be empty")

    try:
        service = get_rag_service()
        return await service.query(
            RAGRequest(
                query=query,
                user_id=request.user_id,
                session_id=request.session_id,
                context=request.context,
                max_results=request.max_results,
            )
        )
    except HTTPException:
        raise
    except Exception:
        logger.exception("RAG query error")
        raise HTTPException(status_code=502, detail="RAG service unavailable")


@router.post("/attachments/ingest")
async def ingest_attachment(request: RAGAttachmentIngestRequest):
    """Ingest one user attachment into RAG context."""
    try:
        service = get_rag_service()
        return await service.ingest_attachment(request)
    except HTTPException:
        raise
    except Exception:
        logger.exception("Attachment ingest error")
        raise HTTPException(status_code=502, detail="RAG service unavailable")


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
    """Ingest documents into knowledge base."""
    try:
        service = get_rag_service()
        docs = [
            d.model_dump() if hasattr(d, "model_dump") else d.dict()
            for d in documents
        ]
        return service.add_knowledge(docs)
    except HTTPException:
        raise
    except Exception:
        logger.exception("Ingest error")
        raise HTTPException(status_code=502, detail="RAG service unavailable")


@router.delete("/knowledge")
async def delete_knowledge():
    """Delete all knowledge."""
    try:
        service = get_rag_service()
        return service.delete_knowledge()
    except Exception as error:
        raise HTTPException(status_code=500, detail=str(error))
