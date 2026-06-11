from typing import List, Dict, Any, Optional
import base64
import re
import uuid
import logging
import zipfile
import json
from io import BytesIO
from app.infrastructure.vector_store import get_vector_store
from app.infrastructure.llm_client import get_llm_client
from app.infrastructure.reranker_client import get_reranker
from app.models.schemas import (
    AttachmentIngestResponse,
    RAGAttachmentIngestRequest,
    RAGRequest,
    RAGResponse,
    Source,
    LLMStructuredAnswer,
)
from app.core.config import settings

logger = logging.getLogger(__name__)

STRUCTURED_ANSWER_SCHEMA = {
    "type": "object",
    "properties": {
        "answer": {"type": "string"},
        "confidence": {"type": "string", "enum": ["high", "medium", "low", "none"]},
        "sources_used": {"type": "array", "items": {"type": "integer"}},
    },
    "required": ["answer", "confidence", "sources_used"],
    "additionalProperties": False,
}

class RAGService:
    def __init__(self, vector_store=None, llm=None, reranker=None):
        self.vector_store = vector_store
        self.llm = llm
        self.reranker = reranker

    async def query(self, request: RAGRequest) -> RAGResponse:
        system_prompt = self._build_system_prompt(request.context)
        session_filters = {"user_id": request.user_id, "session_id": request.session_id} if request.session_id else None
        product_context = (request.context or {}).get("product_context")
        try:
            vector_store = self.vector_store or get_vector_store()
            session_results = vector_store.search(
                collection_name=settings.collection_knowledge,
                query=request.query,
                limit=request.max_results * 2,
                filters=session_filters,
            )
            global_results = []
            if request.session_id:
                global_results = vector_store.search(
                    collection_name=settings.collection_knowledge,
                    query=request.query,
                    limit=request.max_results * 2,
                    filters=None,
                )
            results = self._merge_ranked_results(
                session_results,
                global_results,
                request.max_results * 2,
                product_context=product_context if isinstance(product_context, dict) else None,
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
            has_product_context = bool((request.context or {}).get("product_context"))
            if has_product_context:
                try:
                    llm = self.llm or get_llm_client()
                    answer = await llm.generate_with_context(
                        request.query,
                        self._product_context_entries(product_context),
                        system_prompt,
                    )
                    return RAGResponse(answer=answer, sources=[])
                except Exception as llm_error:
                    logger.error(f"LLM product-context fallback unavailable: {llm_error}")
            return RAGResponse(
                answer="Извините, я не нашёл релевантной информации в базе знаний.",
                sources=[]
            )

        try:
            reranker = self.reranker or get_reranker()
            doc_texts = []
            for result in results:
                properties = result.get("properties", {})
                content = (
                    properties.get("content")
                    or properties.get("text")
                    or properties.get("description", "")
                )
                doc_texts.append(content)
            reranked_indices = await reranker.rerank(request.query, doc_texts, request.max_results)
        except Exception as error:
            logger.warning(f"Reranker unavailable, using vector search order: {error}")
            reranked_indices = None

        if reranked_indices is not None:
            results = [results[i] for i in reranked_indices if i < len(results)]

        logger.debug(
            "RAG retrieval debug: %s",
            self._build_retrieval_debug(
                filters=session_filters,
                session_results=session_results,
                global_results=global_results,
                reranker_used=reranked_indices is not None,
            ),
        )

        context = self._product_context_entries(product_context)
        for result in results:
            properties = result.get("properties", {})
            context.append({
                "title": properties.get("title") or properties.get("name", ""),
                "content": properties.get("content") or properties.get("text") or properties.get("description", ""),
                "category": properties.get("category", ""),
            })

        try:
            llm = self.llm or get_llm_client()
            structured = await llm.generate_structured(
                request.query,
                context,
                system_prompt,
                STRUCTURED_ANSWER_SCHEMA,
            )
            answer = structured.get("answer", "")
            confidence = structured.get("confidence", "low")
            sources_used = structured.get("sources_used", [])
        except Exception as llm_error:
            logger.warning(f"Structured output failed, falling back to free text: {llm_error}")
            llm = self.llm or get_llm_client()
            answer = await llm.generate_with_context(request.query, context, system_prompt)
            confidence = "unknown"
            sources_used = list(range(len(results)))

        if confidence == "none":
            answer = "Недостаточно данных."

        sources = []
        for i, result in enumerate(results):
            properties = result.get("properties", {})
            content = properties.get("content") or properties.get("text") or properties.get("description", "")
            sources.append(Source(
                id=str(result.get("id")),
                title=properties.get("title") or properties.get("name", ""),
                content=content[:300],
                score=result.get("score"),
                source_type=properties.get("source_type"),
                category=properties.get("category"),
            ))

        return RAGResponse(answer=answer, sources=sources, conversation_id=str(uuid.uuid4()))

    def _product_context_entries(self, product_context: Any) -> List[Dict[str, str]]:
        if not isinstance(product_context, dict):
            return []
        return [{
            "title": "Контекст текущего продукта",
            "content": json.dumps(product_context, ensure_ascii=False),
            "category": "product_context",
        }]

    def add_knowledge(self, documents: List[Dict[str, Any]]):
        chunk_documents = []
        for doc in documents:
            chunk_documents.extend(self._chunk_document(doc))
        texts = [self._search_text(doc) for doc in chunk_documents]
        vector_store = self.vector_store or get_vector_store()
        vector_store.add_documents(settings.collection_knowledge, chunk_documents, texts)
        logger.info(f"Added {len(chunk_documents)} chunks from {len(documents)} documents")
        return {"status": "success", "indexed_count": len(chunk_documents)}

    def delete_knowledge(self):
        vector_store = self.vector_store or get_vector_store()
        deleted = vector_store.delete_collection(settings.collection_knowledge)
        return {"status": "success", "collection": settings.collection_knowledge, "deleted": deleted}

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
        chunk_documents = self._chunk_document(document)
        vector_store = self.vector_store or get_vector_store()
        vector_store.add_documents(
            settings.collection_knowledge,
            chunk_documents,
            [self._search_text(doc) for doc in chunk_documents],
        )
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
            "Если в контексте недостаточно надежной информации, "
            "установи confidence в 'none' и напиши 'Недостаточно данных.' в поле answer. "
            "Никогда не выдумывай факты, которых нет в контексте."
        )
        skin_passport = (context or {}).get("skin_passport")
        if skin_passport:
            prompt += f"\n\nПаспорт кожи пользователя (skin_passport): {skin_passport}"
        product_context = (context or {}).get("product_context")
        if product_context:
            serialized_product_context = json.dumps(product_context, ensure_ascii=False)
            prompt += (
                "\n\nКонтекст текущего продукта (product_context): "
                f"{serialized_product_context}"
                "\nИспользуй этот контекст как приоритетный источник фактов о продукте: "
                "состав, характеристики, назначение, совместимость, ограничения."
            )
        return prompt

    def _search_text(self, doc: Dict[str, Any]) -> str:
        parts = [
            doc.get("title"),
            doc.get("content") or doc.get("text") or doc.get("description"),
            doc.get("category"),
        ]
        return " ".join(part for part in parts if part)

    def _chunk_text(self, text: str, chunk_size: int = 1000, overlap: int = 150) -> List[str]:
        normalized = re.sub(r"\s+", " ", text or "").strip()
        if not normalized:
            return []
        if len(normalized) <= chunk_size:
            return [normalized]

        chunks = []
        start = 0
        while start < len(normalized):
            end = min(start + chunk_size, len(normalized))
            chunks.append(normalized[start:end].strip())
            if end == len(normalized):
                break
            start = max(end - overlap, start + 1)
        return chunks

    def _chunk_document(self, doc: Dict[str, Any]) -> List[Dict[str, Any]]:
        text = doc.get("content") or doc.get("text") or doc.get("description") or ""
        chunks = self._chunk_text(text)
        document_id = str(doc.get("document_id") or doc.get("id") or uuid.uuid4())
        chunk_count = len(chunks)
        chunk_documents = []
        for index, chunk in enumerate(chunks):
            chunk_documents.append({
                **doc,
                "document_id": document_id,
                "content": chunk,
                "chunk_index": index,
                "chunk_count": chunk_count,
                "embedding_model": settings.embedding_model,
                "schema_version": "rag_v1",
            })
        return chunk_documents

    def _build_retrieval_debug(
        self,
        filters: Optional[Dict[str, Any]],
        session_results: List[Dict[str, Any]],
        global_results: List[Dict[str, Any]],
        reranker_used: bool,
    ) -> Dict[str, Any]:
        combined_results = (session_results or []) + (global_results or [])
        return {
            "collection": settings.collection_knowledge,
            "filters": filters,
            "session_result_count": len(session_results or []),
            "global_result_count": len(global_results or []),
            "source_ids": [str(result.get("id")) for result in combined_results],
            "scores": [result.get("score") for result in combined_results],
            "reranker_used": reranker_used,
            "embedding_model": settings.embedding_model,
            "schema_version": "rag_v1",
        }

    def _merge_ranked_results(
        self,
        primary_results: Optional[List[Dict[str, Any]]],
        secondary_results: Optional[List[Dict[str, Any]]],
        limit: int,
        product_context: Optional[Dict[str, Any]] = None,
    ) -> List[Dict[str, Any]]:
        merged: List[Dict[str, Any]] = []
        seen_keys = set()
        for result in (primary_results or []) + (secondary_results or []):
            key = self._result_identity(result)
            if key in seen_keys:
                continue
            seen_keys.add(key)
            merged.append(result)
        boost_terms = self._extract_product_boost_terms(product_context)
        merged.sort(
            key=lambda item: self._effective_score(item, boost_terms),
            reverse=True,
        )
        return merged[:limit]

    def _result_identity(self, result: Dict[str, Any]) -> str:
        result_id = result.get("id")
        if result_id is not None:
            return f"id:{result_id}"
        properties = result.get("properties", {}) or {}
        title = properties.get("title") or properties.get("name") or ""
        content = properties.get("content") or properties.get("text") or properties.get("description") or ""
        return f"content:{title}|{content[:120]}"

    def _extract_product_boost_terms(self, product_context: Optional[Dict[str, Any]]) -> List[str]:
        if not product_context:
            return []
        product = product_context.get("product") if isinstance(product_context.get("product"), dict) else {}
        raw_terms = [
            product.get("name"),
            product.get("brand"),
            product.get("product_type"),
            product.get("active_ingredient"),
            product.get("composition"),
        ]
        terms = []
        for value in raw_terms:
            if not value:
                continue
            text = str(value).strip().lower()
            if len(text) >= 3:
                terms.append(text)
        return terms

    def _effective_score(self, result: Dict[str, Any], boost_terms: List[str]) -> float:
        base = float(result.get("score") or 0.0)
        if not boost_terms:
            return base
        properties = result.get("properties", {}) or {}
        haystack = " ".join(
            str(part or "")
            for part in [
                properties.get("title") or properties.get("name"),
                properties.get("content") or properties.get("text") or properties.get("description"),
            ]
        ).lower()
        if any(term in haystack for term in boost_terms):
            return base + 0.35
        return base

_rag_service: Optional[RAGService] = None

def get_rag_service() -> RAGService:
    global _rag_service
    if _rag_service is None:
        _rag_service = RAGService()
    return _rag_service
