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
        recovery_prompt = self._build_context_recovery_prompt(request.context)
        session_filters = {"user_id": request.user_id, "session_id": request.session_id} if request.session_id else None
        product_context = (request.context or {}).get("product_context")
        recommendation_context = (request.context or {}).get("recommendation_context")
        chat_history = self._normalize_chat_history((request.context or {}).get("chat_history"))
        retrieval_query = self._build_retrieval_query(request.query, chat_history, product_context)
        contextual_entries = (
            self._chat_history_entries(chat_history)
            + self._product_context_entries(product_context)
            + self._recommendation_context_entries(recommendation_context)
        )
        try:
            vector_store = self.vector_store or get_vector_store()
            session_results = vector_store.search(
                collection_name=settings.collection_knowledge,
                query=retrieval_query,
                limit=request.max_results * 2,
                filters=session_filters,
            )
            global_results = []
            if request.session_id:
                global_results = vector_store.search(
                    collection_name=settings.collection_knowledge,
                    query=retrieval_query,
                    limit=request.max_results * 2,
                    filters=None,
                )
            results = self._merge_ranked_results(
                session_results,
                global_results,
                request.max_results * 2,
                query=retrieval_query,
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
            if contextual_entries:
                try:
                    llm = self.llm or get_llm_client()
                    answer = await llm.generate_with_context(
                        request.query,
                        contextual_entries,
                        recovery_prompt,
                    )
                    return RAGResponse(answer=answer, sources=[], conversation_id=str(uuid.uuid4()))
                except Exception as llm_error:
                    logger.error(f"LLM contextual fallback unavailable: {llm_error}")
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
            query_terms = self._extract_query_boost_terms(retrieval_query)
            product_boost_terms = self._extract_product_boost_terms(product_context if isinstance(product_context, dict) else None)
            results.sort(
                key=lambda item: self._effective_score(item, product_boost_terms, query_terms),
                reverse=True,
            )

        logger.debug(
            "RAG retrieval debug: %s",
            self._build_retrieval_debug(
                filters=session_filters,
                session_results=session_results,
                global_results=global_results,
                reranker_used=reranked_indices is not None,
            ),
        )

        context = list(contextual_entries)
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

        direct_source_answer = self._build_direct_source_answer(request.query, results)
        if direct_source_answer and self._looks_like_insufficient_answer(answer):
            answer = direct_source_answer

        if confidence == "none":
            answer = direct_source_answer or "Недостаточно данных."

        if confidence == "none":
            recovered_answer = await self._recover_contextual_answer(
                query=request.query,
                context=context,
                recovery_prompt=recovery_prompt,
            )
            if recovered_answer:
                answer = recovered_answer

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

    def _recommendation_context_entries(self, recommendation_context: Any) -> List[Dict[str, str]]:
        if not isinstance(recommendation_context, dict):
            return []
        return [{
            "title": "РџРµСЂСЃРѕРЅР°Р»СЊРЅС‹Рµ РєР°РЅРґРёРґР°С‚С‹ РёР· РєР°С‚Р°Р»РѕРіР° Aura",
            "content": json.dumps(recommendation_context, ensure_ascii=False),
            "category": "recommendation_context",
        }]

    def _normalize_chat_history(self, chat_history: Any) -> List[Dict[str, str]]:
        if not isinstance(chat_history, list):
            return []
        normalized = []
        for item in chat_history[-10:]:
            if not isinstance(item, dict):
                continue
            role = str(item.get("role") or "").strip().lower()
            content = re.sub(r"\s+", " ", str(item.get("content") or "")).strip()
            if role not in {"user", "assistant"} or not content:
                continue
            normalized.append({"role": role, "content": content[:1500]})
        return normalized

    def _chat_history_entries(self, chat_history: List[Dict[str, str]]) -> List[Dict[str, str]]:
        if not chat_history:
            return []
        return [{
            "title": "История диалога",
            "content": "\n".join(f"{item['role']}: {item['content']}" for item in chat_history),
            "category": "chat_history",
        }]

    def _build_retrieval_query(
        self,
        query: str,
        chat_history: List[Dict[str, str]],
        product_context: Any,
    ) -> str:
        cleaned_query = re.sub(r"\s+", " ", query or "").strip()
        if not chat_history or not self._is_follow_up_query(cleaned_query):
            return cleaned_query

        parts = [cleaned_query]
        if isinstance(product_context, dict):
            product = product_context.get("product") if isinstance(product_context.get("product"), dict) else {}
            product_name = str(product.get("name") or "").strip()
            if product_name:
                parts.append(f"product: {product_name}")
        parts.append(
            "dialog: " + " | ".join(f"{item['role']}: {item['content']}" for item in chat_history[-4:])
        )
        return "\n".join(parts)[:2000]

    def _is_follow_up_query(self, query: str) -> bool:
        lowered = (query or "").strip().lower()
        if len(lowered) <= 80:
            return True
        markers = [
            "а как",
            "а если",
            "а можно",
            "как часто",
            "когда",
            "утром или вечером",
            "подойдет ли",
            "подойдёт ли",
            "он",
            "она",
            "оно",
            "это",
            "этот",
            "эта",
            "его",
            "её",
        ]
        return any(marker in lowered for marker in markers)

    async def _recover_contextual_answer(
        self,
        *,
        query: str,
        context: List[Dict[str, str]],
        recovery_prompt: str,
    ) -> Optional[str]:
        if not context:
            return None
        try:
            llm = self.llm or get_llm_client()
            answer = await llm.generate_with_context(query, context, recovery_prompt)
        except Exception as llm_error:
            logger.warning(f"Context recovery fallback failed: {llm_error}")
            return None
        cleaned_answer = answer.strip()
        if not cleaned_answer or "Недостаточно данных" in cleaned_answer:
            return None
        return cleaned_answer

    def _build_direct_source_answer(self, query: str, results: List[Dict[str, Any]]) -> Optional[str]:
        query_terms = self._extract_query_boost_terms(query)
        preferred_categories = {"active_ingredient", "skincare_knowledge"}
        for result in results:
            properties = result.get("properties", {}) or {}
            category = str(properties.get("category") or "").lower()
            title = str(properties.get("title") or properties.get("name") or "")
            content = str(properties.get("content") or properties.get("text") or properties.get("description") or "")
            haystack = f"{title} {content}".lower()
            if category not in preferred_categories:
                continue
            if query_terms and not any(term in haystack for term in query_terms):
                continue
            return self._summarize_source_content(content)
        return None

    def _summarize_source_content(self, content: str, max_sentences: int = 2, max_chars: int = 320) -> str:
        cleaned = re.sub(r"\s+", " ", content or "").strip()
        if not cleaned:
            return ""
        sentences = re.split(r"(?<=[.!?])\s+", cleaned)
        summary = " ".join(sentence.strip() for sentence in sentences[:max_sentences] if sentence.strip())
        if not summary:
            summary = cleaned
        return summary[:max_chars].rstrip()

    def _looks_like_insufficient_answer(self, answer: str) -> bool:
        lowered = re.sub(r"\s+", " ", answer or "").strip().lower()
        if not lowered:
            return True
        markers = [
            "недостаточно данных",
            "информации недостаточно",
            "уточните вопрос",
            "задайте другой",
            "не могу ответить",
            "не могу точно ответить",
        ]
        return any(marker in lowered for marker in markers)

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
        recommendation_context = (context or {}).get("recommendation_context")
        if recommendation_context:
            prompt += (
                "\n\nРџРµСЂСЃРѕРЅР°Р»СЊРЅС‹Рµ РєР°РЅРґРёРґР°С‚С‹ РёР· РІРЅСѓС‚СЂРµРЅРЅРµРіРѕ РєР°С‚Р°Р»РѕРіР° Aura "
                f"(recommendation_context): {json.dumps(recommendation_context, ensure_ascii=False)}"
                "\nР•СЃР»Рё РїРѕР»СЊР·РѕРІР°С‚РµР»СЊ СЃРїСЂР°С€РёРІР°РµС‚ Рѕ РєРѕРЅРєСЂРµС‚РЅС‹С… РїРѕРґС…РѕРґСЏС‰РёС… "
                "РµРјСѓ РїСЂРѕРґСѓРєС‚Р°С…, РѕРїРёСЂР°Р№СЃСЏ РІ РїРµСЂРІСѓСЋ РѕС‡РµСЂРµРґСЊ РЅР° СЌС‚Рё РєР°РЅРґРёРґР°С‚С‹ "
                "Рё РЅР°Р·С‹РІР°Р№ product_name, brand, step Рё reason РёР· СЌС‚РѕРіРѕ РєРѕРЅС‚РµРєСЃС‚Р°."
            )
        return prompt

    def _build_context_recovery_prompt(self, context: Optional[Dict[str, Any]] = None) -> str:
        prompt = (
            "Ты - эксперт по косметике и уходу за кожей. "
            "Отвечай только на русском языке. "
            "Используй только предоставленный контекст и выбирай самые релевантные фрагменты под вопрос пользователя. "
            "Если среди источников есть частично релевантный ответ, дай практичную короткую рекомендацию по нему. "
            "Если есть неопределенность, честно обозначь ее в формулировке, но не отвечай шаблонно 'Недостаточно данных', "
            "пока в контексте есть хотя бы один полезный факт."
        )
        skin_passport = (context or {}).get("skin_passport")
        if skin_passport:
            prompt += f"\n\nПаспорт кожи пользователя (skin_passport): {skin_passport}"
        product_context = (context or {}).get("product_context")
        if product_context:
            prompt += f"\n\nКонтекст текущего продукта (product_context): {json.dumps(product_context, ensure_ascii=False)}"
        recommendation_context = (context or {}).get("recommendation_context")
        if recommendation_context:
            prompt += (
                "\n\nРџРµСЂСЃРѕРЅР°Р»СЊРЅС‹Рµ РєР°РЅРґРёРґР°С‚С‹ РёР· РєР°С‚Р°Р»РѕРіР° Aura "
                f"(recommendation_context): {json.dumps(recommendation_context, ensure_ascii=False)}"
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
        query: str = "",
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
        query_terms = self._extract_query_boost_terms(query)
        merged.sort(
            key=lambda item: self._effective_score(item, boost_terms, query_terms),
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

    def _extract_query_boost_terms(self, query: str) -> List[str]:
        raw_terms = re.findall(r"[A-Za-zА-Яа-яЁё0-9-]+", (query or "").lower())
        stop_terms = {
            "как", "что", "это", "для", "при", "или", "мне", "подойдет", "подойдёт",
            "можно", "если", "после", "перед", "утром", "вечером", "нужно", "ли",
            "use", "using", "with", "when", "what",
        }
        terms = []
        for term in raw_terms:
            cleaned = term.strip("-")
            if len(cleaned) < 4 or cleaned in stop_terms:
                continue
            if cleaned not in terms:
                terms.append(cleaned)
        return terms

    def _effective_score(self, result: Dict[str, Any], boost_terms: List[str], query_terms: List[str]) -> float:
        base = float(result.get("score") or 0.0)
        properties = result.get("properties", {}) or {}
        title = str(properties.get("title") or properties.get("name") or "").lower()
        content = str(properties.get("content") or properties.get("text") or properties.get("description") or "").lower()
        haystack = f"{title} {content}"
        if boost_terms and any(term in haystack for term in boost_terms):
            base += 0.35
        if query_terms:
            title_hits = sum(1 for term in query_terms if term in title)
            content_hits = sum(1 for term in query_terms if term in content)
            base += title_hits * 0.9
            base += min(content_hits, 4) * 0.2
        return base

_rag_service: Optional[RAGService] = None

def get_rag_service() -> RAGService:
    global _rag_service
    if _rag_service is None:
        _rag_service = RAGService()
    return _rag_service
