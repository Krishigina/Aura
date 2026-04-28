from typing import List, Dict, Any, Optional
import base64
import re
import uuid
import logging
import zipfile
from io import BytesIO
from app.infrastructure.vector_store import get_vector_store
from app.infrastructure.llm_client import get_llm_client
from app.models.schemas import AttachmentIngestResponse, RAGAttachmentIngestRequest, RAGRequest, RAGResponse
from app.core.config import settings

logger = logging.getLogger(__name__)

class RAGService:
    def __init__(self, vector_store=None, llm=None):
        self.vector_store = vector_store
        self.llm = llm
    
    async def query(self, request: RAGRequest) -> RAGResponse:
        system_prompt = self._build_system_prompt(request.context)
        try:
            vector_store = self.vector_store or get_vector_store()
            results = vector_store.search(
                collection_name=settings.collection_knowledge,
                query=request.query,
                limit=request.max_results,
                filters={"user_id": request.user_id, "session_id": request.session_id} if request.session_id else None,
            )
        except Exception as error:
            logger.error(f"Vector search unavailable: {error}")
            try:
                llm = self.llm or get_llm_client()
                answer = await llm.generate_with_context(
                    request.query,
                    [],
                    system_prompt,
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

    async def ingest_attachment(self, request: RAGAttachmentIngestRequest) -> AttachmentIngestResponse:
        attachment_bytes = base64.b64decode(request.content_base64)
        extracted_text = await self._extract_attachment_text(request, attachment_bytes)
        summary = extracted_text[:500]

        if request.content_type.startswith("image/"):
            llm = self.llm or get_llm_client()
            summary = await llm.summarize_image(
                attachment_bytes,
                request.content_type,
                "Опиши изображение для RAG-консультации по косметике и уходу за кожей. "
                "Укажи видимые продукты, составы, состояние кожи или важные ограничения, если они видны.",
            )
            extracted_text = summary

        document = {
            "title": request.filename,
            "content": extracted_text,
            "category": "user_attachment",
            "source_type": "user_attachment",
            "attachment_id": request.attachment_id,
            "user_id": request.user_id,
            "session_id": request.session_id,
            "filename": request.filename,
            "content_type": request.content_type,
        }
        vector_store = self.vector_store or get_vector_store()
        vector_store.add_documents(settings.collection_knowledge, [document], [self._search_text(document)])
        return AttachmentIngestResponse(
            attachment_id=request.attachment_id,
            summary=summary,
            extracted_text=extracted_text,
            indexed=True,
        )

    async def _extract_attachment_text(self, request: RAGAttachmentIngestRequest, attachment_bytes: bytes) -> str:
        if request.content_type == "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
            return self._extract_docx_text(attachment_bytes)
        if request.content_type == "application/pdf":
            return self._extract_pdf_text(attachment_bytes)
        return attachment_bytes.decode("utf-8", errors="ignore")

    def _extract_docx_text(self, attachment_bytes: bytes) -> str:
        with zipfile.ZipFile(BytesIO(attachment_bytes)) as docx:
            xml = docx.read("word/document.xml").decode("utf-8", errors="ignore")
        text = re.sub(r"<[^>]+>", " ", xml)
        return re.sub(r"\s+", " ", text).strip()

    def _extract_pdf_text(self, attachment_bytes: bytes) -> str:
        text = attachment_bytes.decode("latin-1", errors="ignore")
        matches = re.findall(r"\(([^()]*)\)\s*Tj", text)
        if matches:
            return re.sub(r"\s+", " ", " ".join(matches)).strip()
        return re.sub(r"\s+", " ", text).strip()[:4000]

    def _build_system_prompt(self, context: Optional[Dict[str, Any]] = None) -> str:
        prompt = (
            "Ты - эксперт по косметике и уходу за кожей. "
            "Отвечай только на русском языке. "
            "Используй предоставленный контекст базы знаний, пользовательских вложений и паспорта кожи. "
            "Если в контексте недостаточно надежной информации, честно скажи, что информации недостаточно."
        )
        skin_passport = (context or {}).get("skin_passport")
        if skin_passport:
            prompt += f"\n\nПаспорт кожи пользователя (skin_passport): {skin_passport}"
        return prompt

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
