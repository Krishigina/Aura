from typing import List, Dict, Any, Optional
import logging
from app.infrastructure.vector_store import get_vector_store
from app.infrastructure.embedder import get_embedder
from app.infrastructure.llm_client import get_llm_client

logger = logging.getLogger(__name__)

class RAGPipeline:
    """
    RAG (Retrieval-Augmented Generation) Pipeline
    1. Ingest documents
    2. Create embeddings  
    3. Store in vector DB
    4. Query with user input
    5. Generate response
    """
    
    def __init__(self):
        self.vector_store = get_vector_store()
        self.embedder = get_embedder()
        self.llm_client = get_llm_client()

    def _user_collection_name(self, owner_user_id: str) -> str:
        from app.core.config import settings
        safe_user_id = str(owner_user_id).strip().replace("/", "_").replace(" ", "_")
        return f"{settings.collection_knowledge_user_prefix}{safe_user_id}"
    
    def ingest_documents(self, documents: List[Dict[str, Any]]) -> Dict[str, Any]:
        """
        Ingest documents into the knowledge base
        """
        from app.core.config import settings

        logger.info(f"Ingesting {len(documents)} documents")
        if not documents:
            return {"status": "success", "indexed_count": 0}

        global_docs: List[Dict[str, Any]] = []
        user_docs_by_collection: Dict[str, List[Dict[str, Any]]] = {}

        for doc in documents:
            scope = str(doc.get("source_scope") or "global").lower()
            owner_user_id = doc.get("owner_user_id")
            if scope == "user" and owner_user_id:
                collection = self._user_collection_name(str(owner_user_id))
                user_docs_by_collection.setdefault(collection, []).append(doc)
            else:
                global_docs.append(doc)

        indexed_count = 0
        indexed_collections: List[str] = []

        if global_docs:
            global_texts = [
                f"{doc.get('title', '')} {doc.get('content', '')} {doc.get('description', '')}".strip() or str(doc)
                for doc in global_docs
            ]
            self.vector_store.create_collection(settings.collection_knowledge, self.embedder.dimension)
            indexed_count += self.vector_store.add_documents(settings.collection_knowledge, global_docs, global_texts)
            indexed_collections.append(settings.collection_knowledge)

        for collection_name, docs_chunk in user_docs_by_collection.items():
            chunk_texts = [
                f"{doc.get('title', '')} {doc.get('content', '')} {doc.get('description', '')}".strip() or str(doc)
                for doc in docs_chunk
            ]
            self.vector_store.create_collection(collection_name, self.embedder.dimension)
            indexed_count += self.vector_store.add_documents(collection_name, docs_chunk, chunk_texts)
            indexed_collections.append(collection_name)

        return {
            "status": "success",
            "indexed_count": indexed_count,
            "collections": indexed_collections,
        }
    
    def query(self, user_query: str, user_context: Optional[Dict[str, Any]] = None, max_results: int = 5) -> Dict[str, Any]:
        """
        Query the RAG system
        """
        from app.core.config import settings

        logger.info(f"Query: {user_query}")

        retrieval_source = "vector"
        results: List[Dict[str, Any]] = []

        user_id = None
        if isinstance(user_context, dict):
            user_id = user_context.get("user_id")

        # 1) Search global collection
        try:
            raw_global = self.vector_store.search(
                collection_name=settings.collection_knowledge,
                query=user_query,
                limit=max_results,
            )
            results.extend(self._normalize_vector_results(raw_global, scope="global", owner_user_id=None))
        except Exception as exc:
            logger.warning(f"Global vector retrieval failed: {exc}")

        # 2) Search personal collection for current user (if present)
        if user_id is not None:
            user_collection = self._user_collection_name(str(user_id))
            try:
                raw_personal = self.vector_store.search(
                    collection_name=user_collection,
                    query=user_query,
                    limit=max_results,
                )
                results.extend(
                    self._normalize_vector_results(raw_personal, scope="user", owner_user_id=str(user_id))
                )
            except Exception as exc:
                logger.info(f"Personal retrieval skipped ({user_collection}): {exc}")

        if results:
            results.sort(key=lambda x: x.get("score", 0), reverse=True)
            results = results[:max_results]

        # 2) Safe fallback for local/dev mode
        if not results:
            retrieval_source = "simulated"
            results = self._simulated_search(user_query, max_results)
        
        if not results:
            return {
                "answer": "Извините, я не нашёл релевантной информации по вашему вопросу. Попробуйте переформулировать вопрос.",
                "sources": [],
                "query": user_query,
                "retrieval_source": retrieval_source,
            }
        
        # 3. Generate context from results
        context = self._build_context(results)
        
        # 4. Generate answer (in production, would use LLM)
        answer = self._generate_answer(user_query, context, user_context)
        
        # 5. Format sources
        sources = [
            {
                "id": r.get("id"),
                "title": r.get("title"),
                "content": r.get("content", "")[:200],
                "relevance": r.get("score", 0)
            }
            for r in results
        ]
        
        return {
            "answer": answer,
            "sources": sources,
            "query": user_query,
            "retrieval_source": retrieval_source,
        }

    def _normalize_vector_results(
        self,
        raw_results: List[Dict[str, Any]],
        scope: str,
        owner_user_id: Optional[str],
    ) -> List[Dict[str, Any]]:
        normalized: List[Dict[str, Any]] = []

        for item in raw_results:
            if not isinstance(item, dict):
                continue

            props = item.get("properties") or {}
            if not isinstance(props, dict):
                props = {}

            title = str(props.get("name") or props.get("title") or "Без названия")
            content = str(props.get("text") or props.get("content") or props.get("description") or "")

            if not content:
                continue

            score = item.get("score")
            try:
                score_value = float(score) if score is not None else 0.0
            except (TypeError, ValueError):
                score_value = 0.0

            normalized.append(
                {
                    "id": str(item.get("id") or ""),
                    "title": title,
                    "content": content,
                    "category": str(props.get("category") or ""),
                    "score": score_value,
                    "scope": scope,
                    "owner_user_id": owner_user_id,
                }
            )

        return normalized
    
    def _simulated_search(self, query: str, limit: int) -> List[Dict[str, Any]]:
        """Simulated search for demo"""
        # Knowledge base for cosmetics
        knowledge_base = [
            {
                "id": "kb_1",
                "title": "Как подобрать увлажняющий крем для сухой кожи",
                "content": "Для сухой кожи выбирайте кремы с увлажняющими ингредиентами: гиалуроновая кислота, глицерин, керамиды. Избегайте спирта и агрессивных отшелушивающих средств.",
                "category": "Уход за кожей",
                "score": 0.95
            },
            {
                "id": "kb_2", 
                "title": "Ретинол: как использовать",
                "content": "Ретинол - мощный антивозрастной ингредиент. Начинайте с низкой концентрации (0.25%), используйте на ночь, обязательно используйте SPF днём. Не комбинируйте с AHA и витамином C.",
                "category": "Активные ингредиенты",
                "score": 0.90
            },
            {
                "id": "kb_3",
                "title": "Уход за жирной кожей",
                "content": "Для жирной кожи используйте средства с салициловой кислотой, ниацинамидом. Выбирайте лёгкие текстуры - гели, флюиды. Увлажнение обязательно!",
                "category": "Типы кожи",
                "score": 0.88
            },
            {
                "id": "kb_4",
                "title": "Аллергия на косметику",
                "content": "При аллергии избегайте: парабенов, формальдегида, синтетических отдушек и красителей. Делайте патч-тест перед использованием новых средств.",
                "category": "Безопасность",
                "score": 0.85
            },
            {
                "id": "kb_5",
                "title": "Утренний и вечерний уход",
                "content": "Утро: очищение + увлажнение + SPF. Вечер: демакияж + очищение + активные ингредиенты (ретинол, кислоты) + увлажнение. Не забывайте про область вокруг глаз.",
                "category": "Рутины",
                "score": 0.82
            }
        ]
        
        # Simple keyword matching for demo
        query_lower = query.lower()
        scored = []
        
        for kb in knowledge_base:
            score = 0
            content = (kb["title"] + " " + kb["content"]).lower()
            
            keywords = ["увлажнение", "сухой", "жирный", "ретинол", "аллергия", "крем", "кожа", "spf", "уход"]
            for kw in keywords:
                if kw in query_lower and kw in content:
                    score += 0.2
            
            if score > 0:
                scored.append({**kb, "score": min(score, 1.0)})
        
        scored.sort(key=lambda x: x["score"], reverse=True)
        return scored[:limit]
    
    def _build_context(self, results: List[Dict[str, Any]]) -> str:
        """Build context string from search results"""
        context_parts = []
        
        for i, result in enumerate(results, 1):
            title = result.get("title", "")
            content = result.get("content", "")
            context_parts.append(f"[{i}] {title}\n{content}\n")
        
        return "\n".join(context_parts)
    
    def _generate_answer(self, query: str, context: str, user_context: Optional[Dict[str, Any]]) -> str:
        """Generate answer based on context"""

        llm_answer = self.llm_client.generate_with_context(
            query=query,
            context=context,
            user_context=user_context,
        )
        if llm_answer:
            return llm_answer
        
        # Fallback rule-based generation when LLM is unavailable
        
        query_lower = query.lower()
        
        if "увлажн" in query_lower or "сух" in query_lower:
            return """Для увлажнения кожи используйте:

1. **Гиалуроновая кислота** - притягивает влагу
2. **Глицерин** - смягчает
3. **Керамиды** - восстанавливают барьер

Для сухой кожи выбирайте плотные текстуры, наносите на влажную кожу."""
        
        if "жирн" in query_lower or "проблемн" in query_lower:
            return """Для жирной кожи:

1. **Салициловая кислота** - очищает поры
2. **Ниацинамид** - контролирует себум
3. **Лёгкие текстуры** - гели, флюиды

Не забывайте увлажнять - это важно!"""
        
        if "ретинол" in query_lower or "анти-эйдж" in query_lower:
            return """Ретинол - сильный ингредиент:

• Начинайте с 0.25%
• Используйте на ночь
• Обязательно SPF днём
• Не используйте с AHA и витамином C
• Беременным - противопоказан"""
        
        if "аллерг" in query_lower or "аллерги" in query_lower:
            return """При аллергии на косметику:

• Избегайте: парабены, формальдегид, отдушки
• Тестируйте на маленьком участке кожи
• Выбирайте гипоаллергенные средства
• Консультируйтесь с дерматологом"""
        
        if "spf" in query_lower or "защита" in query_lower or "солнц" in query_lower:
            return """Защита от солнца:

• SPF 30+ для ежедневного использования
• Наносите за 15 минут до выхода
• Обновляйте каждые 2 часа
• Даже в облачную погоду!

Это лучшая профилактика старения."""
        
        # Default response using context
        return f"""Я нашла информацию по вашему вопросу:

{context[:500]}...

Если нужна более подробная информация, задайте конкретный вопрос!"""
    
    def delete_knowledge_base(self) -> Dict[str, Any]:
        """Delete all documents from knowledge base"""
        from app.core.config import settings

        logger.info("Deleting global knowledge base collection")
        deleted = self.vector_store.delete_collection(settings.collection_knowledge)
        return {"status": "success", "deleted": bool(deleted), "collection": settings.collection_knowledge}


# Singleton
_rag_pipeline: Optional[RAGPipeline] = None

def get_rag_pipeline() -> RAGPipeline:
    global _rag_pipeline
    if _rag_pipeline is None:
        _rag_pipeline = RAGPipeline()
    return _rag_pipeline
