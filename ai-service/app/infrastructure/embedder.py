import numpy as np
from typing import List
import logging
import httpx

from app.core.config import settings

logger = logging.getLogger(__name__)

class Embedder:
    def __init__(self, model_url: str = "http://t2v-transformers:8080"):
        self.model_url = model_url
        self.dimension = 384
    
    def embed(self, text: str) -> List[float]:
        try:
            with httpx.Client() as client:
                response = client.post(
                    f"{self.model_url}/vectors",
                    json={"text": text},
                    timeout=30.0
                )
                if response.status_code == 200:
                    result = response.json()
                    return result["vector"]
        except Exception as e:
            logger.error(f"Failed to get embedding from t2v service: {e}")
        return [0.0] * self.dimension
    
    def embed_batch(self, texts: List[str]) -> List[List[float]]:
        return [self.embed(text) for text in texts]

_embedder: Embedder = None

def get_embedder() -> Embedder:
    global _embedder
    if _embedder is None:
        _embedder = Embedder(settings.embedding_service_url)
    return _embedder
