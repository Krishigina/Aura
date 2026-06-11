import asyncio
from typing import List, Optional
import logging

logger = logging.getLogger(__name__)

MODEL_NAME = "BAAI/bge-reranker-v2-m3"

class RerankerClient:
    def __init__(self):
        self._model = None
        self._tokenizer = None
        self._ready = False
        self._load_task = None

    def _start_load(self):
        if self._load_task is not None:
            return
        self._load_task = asyncio.create_task(self._load())
        logger.info(f"Started background loading of reranker: {MODEL_NAME}")

    def warm_up(self):
        self._start_load()

    async def _load(self):
        try:
            await asyncio.to_thread(self._load_sync)
        except Exception as e:
            logger.warning(f"Reranker load failed: {e}")

    def _load_sync(self):
        import torch
        from transformers import AutoModelForSequenceClassification, AutoTokenizer
        logger.info(f"Loading reranker: {MODEL_NAME}")
        device = "cuda" if torch.cuda.is_available() else "cpu"
        self._model = AutoModelForSequenceClassification.from_pretrained(MODEL_NAME).to(device).eval()
        self._tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)
        self._ready = True
        logger.info(f"Reranker loaded on {device}")

    async def rerank(self, query: str, documents: List[str], top_k: Optional[int] = None) -> Optional[List[int]]:
        self._start_load()
        if not self._ready:
            return None
        try:
            import torch
            pairs = [[query, doc] for doc in documents]
            inputs = self._tokenizer(
                pairs, padding=True, truncation=True, return_tensors="pt", max_length=512,
            )
            device = next(self._model.parameters()).device
            inputs = {k: v.to(device) for k, v in inputs.items()}
            with torch.no_grad():
                scores = self._model(**inputs).logits.squeeze(-1).cpu().tolist()
            if isinstance(scores, float):
                scores = [scores]
            indexed = list(enumerate(scores))
            indexed.sort(key=lambda x: x[1], reverse=True)
            top_k = top_k or len(documents)
            return [idx for idx, _ in indexed[:top_k]]
        except Exception as e:
            logger.warning(f"Reranker inference failed: {e}")
            return None

_reranker: Optional[RerankerClient] = None

def get_reranker() -> RerankerClient:
    global _reranker
    if _reranker is None:
        _reranker = RerankerClient()
    return _reranker
