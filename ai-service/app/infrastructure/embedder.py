from typing import List
import logging
import httpx

from app.core.config import settings

logger = logging.getLogger(__name__)


class EmbeddingError(RuntimeError):
    pass

class Embedder:
    def __init__(self, model_url: str = "http://t2v-transformers:8080", dimension: int = 384):
        self.model_url = model_url
        self.dimension = dimension

    def embed_query(self, text: str) -> List[float]:
        return self._embed(text)

    def embed_doc(self, text: str) -> List[float]:
        return self._embed(text)

    def embed(self, text: str) -> List[float]:
        return self._embed(text)

    def _embed(self, text: str) -> List[float]:
        try:
            with httpx.Client() as client:
                response = client.post(
                    f"{self.model_url}/vectors",
                    json={"text": text},
                    timeout=30.0
                )
                if response.status_code == 404:
                    response = client.post(
                        f"{self.model_url}/embed",
                        json={"inputs": text},
                        timeout=30.0,
                    )
                if response.status_code != 200:
                    raise EmbeddingError(f"Embedding service returned {response.status_code}")
                vector = self._extract_vector(response.json())
                if not isinstance(vector, list):
                    raise EmbeddingError("Embedding service response missing vector")
                return vector
        except EmbeddingError:
            raise
        except Exception as e:
            logger.error(f"Failed to get embedding: {e}")
            raise EmbeddingError("Failed to get embedding") from e

    def _extract_vector(self, result) -> List[float]:
        if isinstance(result, dict):
            return result.get("vector")
        if isinstance(result, list) and result and isinstance(result[0], list):
            return result[0]
        if isinstance(result, list):
            return result
        raise EmbeddingError("Embedding service response missing vector")

    def embed_batch(self, texts: List[str]) -> List[List[float]]:
        return [self._embed(text) for text in texts]

_embedder: Embedder = None

def get_embedder() -> Embedder:
    global _embedder
    if _embedder is None:
        _embedder = Embedder(settings.embedding_service_url, settings.embedding_dimension)
    return _embedder
