import logging
from typing import Any, Dict, List, Optional
from urllib.parse import urlparse

import weaviate
from weaviate.classes.config import Configure, DataType, Property
from weaviate.classes.data import DataObject

from app.infrastructure.embedder import get_embedder

logger = logging.getLogger(__name__)


class VectorStore:
    def __init__(self, url: str, embedder=None):
        parsed_url = urlparse(url)
        scheme = parsed_url.scheme or "http"
        host = parsed_url.hostname or url
        port = parsed_url.port or (443 if scheme == "https" else 80)
        secure = scheme == "https"

        self.client = weaviate.connect_to_custom(
            http_host=host,
            http_port=port,
            http_secure=secure,
            grpc_host=host,
            grpc_port=50051,
            grpc_secure=secure,
        )
        self.embedder = embedder or get_embedder()

    def create_collection(self, name: str, dimension: Optional[int] = None):
        if self.client.collections.exists(name):
            logger.info(f"Collection {name} already exists")
            return

        self.client.collections.create(
            name=name,
            vectorizer_config=Configure.Vectorizer.none(),
            properties=[
                Property(name="title", data_type=DataType.TEXT),
                Property(name="name", data_type=DataType.TEXT),
                Property(name="content", data_type=DataType.TEXT),
                Property(name="text", data_type=DataType.TEXT),
                Property(name="description", data_type=DataType.TEXT),
                Property(name="category", data_type=DataType.TEXT),
            ],
        )
        logger.info(f"Created collection: {name}")

    def add_documents(self, collection_name: str, documents: List[Dict[str, Any]], texts: List[str]) -> int:
        self.create_collection(collection_name)
        collection = self.client.collections.get(collection_name)
        vectors = self.embedder.embed_batch(texts)
        objects: List[DataObject] = []

        for doc, text, vector in zip(documents, texts, vectors):
            title = str(doc.get("title") or doc.get("name") or "Untitled")
            content = str(doc.get("content") or doc.get("text") or "")
            description = str(doc.get("description") or "")
            category = str(doc.get("category") or "")

            objects.append(
                DataObject(
                    properties={
                        "title": title,
                        "name": title,
                        "content": content,
                        "text": text or content or description or title,
                        "description": description,
                        "category": category,
                    },
                    vector=vector,
                )
            )

        if objects:
            collection.data.insert_many(objects)
        logger.info(f"Added {len(objects)} documents to {collection_name}")
        return len(objects)

    def search(self, collection_name: str, query: str, limit: int = 5) -> List[Dict[str, Any]]:
        self.create_collection(collection_name)
        collection = self.client.collections.get(collection_name)
        query_vector = self.embedder.embed(query)
        search = collection.query.hybrid(query=query, vector=query_vector, limit=limit)
        results = []
        for obj in search.objects:
            results.append(
                {
                    "id": str(obj.uuid),
                    "score": getattr(obj.metadata, "score", None),
                    "properties": obj.properties,
                }
            )
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
