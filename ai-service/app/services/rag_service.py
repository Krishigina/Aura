from typing import List, Dict, Any, Optional
import uuid
import logging
from app.infrastructure.vector_store import get_vector_store
from app.infrastructure.llm_client import get_llm_client
from app.models.schemas import RAGRequest, RAGResponse

logger = logging.getLogger(__name__)

class RAGService:
    def __init__(self):
        self.vector_store = get_vector_store()
        self.llm = get_llm_client()
    
    async def query(self, request: RAGRequest) -> RAGResponse:
        try:
            from app.core.config import settings
            
            results = self.vector_store.search(
                collection_name=settings.collection_knowledge,
                query=request.query,
                limit=request.max_results
            )
            
            if not results:
                return RAGResponse(
                    answer="Извините, я не нашёл релевантной информации.",
                    sources=[]
                )
            
            context = [r["properties"] for r in results]
            
            system_prompt = """Ты - эксперт по косметике и уходу за кожей."""
            
            answer = await self.llm.generate_with_context(request.query, context, system_prompt)
            
            sources = [{"id": str(r["id"]), "title": r["properties"].get("name", ""), "content": r["properties"].get("text", "")[:200]} for r in results]
            
            return RAGResponse(answer=answer, sources=sources, conversation_id=str(uuid.uuid4()))
            
        except Exception as e:
            logger.error(f"RAG query error: {e}")
            return RAGResponse(answer="Произошла ошибка.", sources=[])
    
    def add_knowledge(self, documents: List[Dict[str, Any]]):
        from app.core.config import settings
        texts = [doc.get("text", doc.get("description", "")) for doc in documents]
        self.vector_store.add_documents(settings.collection_knowledge, documents, texts)
        logger.info(f"Added {len(documents)} documents")

_rag_service: Optional[RAGService] = None

def get_rag_service() -> RAGService:
    global _rag_service
    if _rag_service is None:
        _rag_service = RAGService()
    return _rag_service