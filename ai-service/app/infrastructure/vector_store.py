import weaviate
from typing import List, Dict, Any, Optional
import logging
from app.infrastructure.embedder import get_embedder

logger = logging.getLogger(__name__)

class VectorStore:
    def __init__(self, url: str):
        self.client = weaviate.Client(url)
        self.embedder = get_embedder()
    
    def create_collection(self, name: str, dimension: int):
        if self.client.collections.exists(name):
            logger.info(f"Collection {name} already exists")
            return
        
        self.client.collections.create(
            name=name,
            vectorizer_config=weaviate.config.Vectorizer(module_name="text2vec-transformers", vectorize_collection_name=False),
            properties=[
                {"name": "text", "dataType": ["text"]},
                {"name": "name", "dataType": ["text"]},
                {"name": "description", "dataType": ["text"]},
                {"name": "category", "dataType": ["text"]},
                {"name": "metadata", "dataType": ["object"]}
            ]
        )
        logger.info(f"Created collection: {name}")
    
    def add_documents(self, collection_name: str, documents: List[Dict[str, Any]], texts: List[str]) -> int:
        collection = self.client.collections.get(collection_name)
        vectors = self.embedder.embed_batch(texts)
        indexed_count = 0

        for doc, vector in zip(documents, vectors):
            title = str(doc.get("title") or doc.get("name") or "Untitled")
            content = str(doc.get("content") or doc.get("text") or "")
            description = str(doc.get("description") or "")
            category = str(doc.get("category") or "")

            metadata = {
                k: v
                for k, v in doc.items()
                if k not in {"title", "name", "content", "text", "description", "category"}
            }

            properties = {
                "text": content or description or title,
                "name": title,
                "description": description,
                "category": category,
                "metadata": metadata,
            }

            collection.data.insert(properties=properties, vector=vector)
            indexed_count += 1

        logger.info(f"Added {indexed_count} documents to {collection_name}")
        return indexed_count
    
    def search(self, collection_name: str, query: str, limit: int = 5) -> List[Dict[str, Any]]:
        collection = self.client.collections.get(collection_name)
        query_vector = self.embedder.embed(query)
        search = collection.query.hybrid(query=query, vector=query_vector, limit=limit)
        results = []
        for obj in search.objects:
            results.append({"id": obj.uuid, "score": obj.metadata.score, "properties": obj.properties})
        return results

    def delete_collection(self, collection_name: str) -> bool:
        if not self.client.collections.exists(collection_name):
            return False
        self.client.collections.delete(collection_name)
        logger.info(f"Deleted collection: {collection_name}")
        return True

_vector_store: Optional[VectorStore] = None

def get_vector_store() -> VectorStore:
    global _vector_store
    if _vector_store is None:
        from app.core.config import settings
        _vector_store = VectorStore(settings.weaviate_url)
    return _vector_store
