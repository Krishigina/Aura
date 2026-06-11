import asyncio
import sys
import time
import types

import pytest

from app.infrastructure.reranker_client import RerankerClient


class _FakeModel:
    def to(self, _device):
        return self

    def eval(self):
        return self


def test_reranker_background_load_does_not_block_event_loop(monkeypatch):
    def slow_from_pretrained(_model_name):
        time.sleep(0.2)
        return _FakeModel()

    fake_torch = types.SimpleNamespace(
        cuda=types.SimpleNamespace(is_available=lambda: False),
    )
    fake_transformers = types.SimpleNamespace(
        AutoModelForSequenceClassification=types.SimpleNamespace(from_pretrained=slow_from_pretrained),
        AutoTokenizer=types.SimpleNamespace(from_pretrained=slow_from_pretrained),
    )
    monkeypatch.setitem(sys.modules, "torch", fake_torch)
    monkeypatch.setitem(sys.modules, "transformers", fake_transformers)

    async def run_check():
        reranker = RerankerClient()
        result = await reranker.rerank("query", ["document"], 1)

        assert result is None
        start = time.perf_counter()
        await asyncio.sleep(0.01)
        elapsed = time.perf_counter() - start
        assert elapsed < 0.1

    asyncio.run(run_check())
