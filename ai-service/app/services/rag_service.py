from typing import List, Dict, Any, Optional
import uuid
import logging
from app.infrastructure.vector_store import get_vector_store
from app.infrastructure.llm_client import get_llm_client
from app.models.schemas import RAGRequest, RAGResponse
from app.core.config import settings

logger = logging.getLogger(__name__)

class RAGService:
    def __init__(self, vector_store=None, llm=None):
        self.vector_store = vector_store
        self.llm = llm
    
    async def query(self, request: RAGRequest) -> RAGResponse:
        try:
            vector_store = self.vector_store or get_vector_store()
            results = vector_store.search(
                collection_name=settings.collection_knowledge,
                query=request.query,
                limit=request.max_results
            )
        except Exception as error:
            logger.error(f"Vector search unavailable: {error}")
            try:
                llm = self.llm or get_llm_client()
                answer = await llm.generate_with_context(
                    request.query,
                    [],
                    "Ты - эксперт по косметике и уходу за кожей. Отвечай на русском языке.",
                )
            except Exception as llm_error:
                logger.error(f"LLM fallback unavailable: {llm_error}")
                answer = "База знаний временно недоступна. Попробуйте повторить запрос чуть позже."
            return RAGResponse(answer=answer, sources=[])

        if not results:
            return RAGResponse(
                answer="Извините, я не нашёл релевантной информации в базе знаний.",
                sources=[]
            )

        context = []
        for result in results:
            properties = result.get("properties", {})
            context.append(
                {
                    "title": properties.get("title") or properties.get("name", ""),
                    "content": properties.get("content") or properties.get("text") or properties.get("description", ""),
                    "category": properties.get("category", ""),
                }
            )

        system_prompt = (
            "Ты - эксперт по косметике и уходу за кожей. "
            "Отвечай только на русском языке. "
            "Используй только предоставленный контекст. "
            "Если в контексте недостаточно надежной информации, честно скажи, что информации недостаточно."
        )

        llm = self.llm or get_llm_client()
        answer = await llm.generate_with_context(request.query, context, system_prompt)

        sources = []
        for result in results:
            properties = result.get("properties", {})
            content = properties.get("content") or properties.get("text") or properties.get("description", "")
            sources.append(
                {
                    "id": str(result.get("id")),
                    "title": properties.get("title") or properties.get("name", ""),
                    "content": content[:300],
                    "score": result.get("score"),
                }
            )

        return RAGResponse(answer=answer, sources=sources, conversation_id=str(uuid.uuid4()))
    
    def add_knowledge(self, documents: List[Dict[str, Any]]):
        texts = [self._search_text(doc) for doc in documents]
        vector_store = self.vector_store or get_vector_store()
        vector_store.add_documents(settings.collection_knowledge, documents, texts)
        logger.info(f"Added {len(documents)} documents")
        return {"status": "success", "indexed_count": len(documents)}

    def _search_text(self, doc: Dict[str, Any]) -> str:
        parts = [
            doc.get("title"),
            doc.get("content") or doc.get("text") or doc.get("description"),
            doc.get("category"),
        ]
        return " ".join(part for part in parts if part)

_rag_service: Optional[RAGService] = None

def get_rag_service() -> RAGService:
    global _rag_service
    if _rag_service is None:
        _rag_service = RAGService()
    return _rag_service
