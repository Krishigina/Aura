import pytest

from app import main


@pytest.fixture
def anyio_backend():
    return "asyncio"


@pytest.mark.anyio
async def test_lifespan_starts_reranker_warmup(monkeypatch):
    class FakeReranker:
        def __init__(self):
            self.started = False

        def warm_up(self):
            self.started = True

    fake_reranker = FakeReranker()
    monkeypatch.setattr(main, "get_reranker", lambda: fake_reranker, raising=False)

    async with main.lifespan(main.app):
        pass

    assert fake_reranker.started is True
