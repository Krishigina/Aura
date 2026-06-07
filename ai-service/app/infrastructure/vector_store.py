import logging
from typing import Any, Dict, List, Optional
from urllib.parse import urlparse

import weaviate
from weaviate.classes.config import Configure, DataType, Property
from weaviate.classes.data import DataObject
from weaviate.classes.query import Filter

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
                Property(name="source_type", data_type=DataType.TEXT),
                Property(name="attachment_id", data_type=DataType.TEXT),
                Property(name="user_id", data_type=DataType.TEXT),
                Property(name="session_id", data_type=DataType.TEXT),
                Property(name="filename", data_type=DataType.TEXT),
                Property(name="content_type", data_type=DataType.TEXT),
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
                        "source_type": doc.get("source_type"),
                        "attachment_id": doc.get("attachment_id"),
                        "user_id": doc.get("user_id"),
                        "session_id": doc.get("session_id"),
                        "filename": doc.get("filename"),
                        "content_type": doc.get("content_type"),
                    },
                    vector=vector,
                )
            )

        if objects:
            collection.data.insert_many(objects)
        logger.info(f"Added {len(objects)} documents to {collection_name}")
        return len(objects)

    def search(self, collection_name: str, query: str, limit: int = 5, filters: Optional[Dict[str, Any]] = None) -> List[Dict[str, Any]]:
        self.create_collection(collection_name)
        collection = self.client.collections.get(collection_name)
        embed_query = getattr(self.embedder, "embed", None) or getattr(self.embedder, "embed_query")
        query_vector = embed_query(query)
        metadata_filter = None
        if filters:
            for key, value in filters.items():
                if value is None:
                    continue
                condition = Filter.by_property(key).equal(str(value))
                metadata_filter = condition if metadata_filter is None else metadata_filter & condition
        search = collection.query.hybrid(query=query, vector=query_vector, limit=limit, filters=metadata_filter)
        results = []
        for obj in search.objects:
            properties = obj.properties or {}
            if filters:
                skip = False
                for key, value in filters.items():
                    if value is None:
                        continue
                    prop_value = properties.get(key)
                    if prop_value not in (None, "", str(value)):
                        skip = True
                        break
                if skip:
                    continue

            results.append(
                {
                    "id": str(obj.uuid),
                    "score": getattr(obj.metadata, "score", None),
                    "properties": properties,
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
