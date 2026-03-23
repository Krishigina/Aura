from typing import List, Dict, Any, Optional
import logging
from app.infrastructure.vector_store import get_vector_store
from app.infrastructure.embedder import get_embedder

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
    
    def ingest_documents(self, documents: List[Dict[str, Any]]) -> Dict[str, Any]:
        """
        Ingest documents into the knowledge base
        """
        logger.info(f"Ingesting {len(documents)} documents")
        
        # Extract text for embedding
        texts = []
        for doc in documents:
            text = f"{doc.get('title', '')} {doc.get('content', '')} {doc.get('description', '')}"
            texts.append(text)
        
        # Create embeddings
        embeddings = self.embedder.embed_batch(texts)
        
        # Store in vector DB
        for i, (doc, embedding) in enumerate(zip(documents, embeddings)):
            doc_with_vector = {
                **doc,
                "_vector": embedding
            }
            # In production, would use vector_store.add_documents()
            logger.info(f"Indexed document: {doc.get('title', 'Untitled')}")
        
        return {
            "status": "success",
            "indexed_count": len(documents)
        }
    
    def query(self, user_query: str, user_context: Optional[Dict[str, Any]] = None, max_results: int = 5) -> Dict[str, Any]:
        """
        Query the RAG system
        """
        logger.info(f"Query: {user_query}")
        
        # 1. Create embedding for query
        query_embedding = self.embedder.embed(user_query)
        
        # 2. Retrieve relevant documents (simulated)
        # In production: results = self.vector_store.search(collection="knowledge", vector=query_embedding, limit=max_results)
        results = self._simulated_search(user_query, max_results)
        
        if not results:
            return {
                "answer": "Извините, я не нашёл релевантной информации по вашему вопросу. Попробуйте переформулировать вопрос.",
                "sources": [],
                "query": user_query
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
            "query": user_query
        }
    
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
        
        # Simple rule-based generation for demo
        # In production: would use LLM (OpenAI, Claude)
        
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
        logger.info("Deleting knowledge base")
        return {"status": "success", "deleted_count": 0}


# Singleton
_rag_pipeline: Optional[RAGPipeline] = None

def get_rag_pipeline() -> RAGPipeline:
    global _rag_pipeline
    if _rag_pipeline is None:
        _rag_pipeline = RAGPipeline()
    return _rag_pipeline
