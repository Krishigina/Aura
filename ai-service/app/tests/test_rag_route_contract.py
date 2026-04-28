import inspect
import pytest

from app.api.routes import rag
from app.models.schemas import RAGRequest
from app.services.rag_service import RAGService


@pytest.fixture
def anyio_backend():
    return "asyncio"


def test_rag_query_route_uses_rag_service_not_legacy_pipeline():
    source = inspect.getsource(rag.query_rag)

    assert "get_rag_service" in source
    assert "get_rag_pipeline" not in source


def test_rag_query_route_hides_internal_errors():
    source = inspect.getsource(rag.query_rag)

    assert "RAG service unavailable" in source
    assert "detail=str(e)" not in source


class BrokenVectorStore:
    def search(self, collection_name: str, query: str, limit: int = 5):
        raise RuntimeError("leader not found")


class FallbackLlm:
    async def generate_with_context(self, query: str, context: list[dict], system_prompt: str) -> str:
        return f"LLM fallback for {query}"


@pytest.mark.anyio
async def test_rag_service_returns_answer_when_vector_store_is_unavailable():
    service = RAGService(vector_store=BrokenVectorStore(), llm=FallbackLlm())

    response = await service.query(RAGRequest(query="ретинол", user_id="debug", max_results=1))

    assert response.answer == "LLM fallback for ретинол"
    assert response.sources == []


@pytest.mark.anyio
async def test_rag_service_initializes_when_default_vector_store_is_unavailable(monkeypatch):
    import app.services.rag_service as rag_service

    def fail_get_vector_store():
        raise RuntimeError("weaviate unavailable")

    monkeypatch.setattr(rag_service, "get_vector_store", fail_get_vector_store)
    service = RAGService(llm=FallbackLlm())

    response = await service.query(RAGRequest(query="ретинол", user_id="debug", max_results=1))

    assert response.answer == "LLM fallback for ретинол"
