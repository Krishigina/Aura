import inspect
import pytest

from app.api.routes import rag
from app.models.schemas import AttachmentIngestResponse, RAGAttachmentIngestRequest, RAGRequest
from app.infrastructure.llm_client import OpenRouterClient
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


def test_attachment_ingest_route_exists_and_uses_service():
    source = inspect.getsource(rag.ingest_attachment)

    assert "@router.post(\"/attachments/ingest\"" in inspect.getsource(rag)
    assert "ingest_attachment" in source
    assert "get_rag_service" in source


def test_attachment_ingest_schema_carries_user_session_metadata():
    request = RAGAttachmentIngestRequest(
        attachment_id="3",
        user_id="42",
        session_id="7",
        filename="photo.png",
        content_type="image/png",
        content_base64="abc",
    )

    assert request.attachment_id == "3"
    assert request.user_id == "42"
    assert request.session_id == "7"


def test_attachment_ingest_response_contract():
    response = AttachmentIngestResponse(attachment_id="3", summary="описание", extracted_text="текст")

    assert response.model_dump() == {
        "attachment_id": "3",
        "summary": "описание",
        "extracted_text": "текст",
        "indexed": True,
    }


def test_llm_client_exposes_vision_summary_helper():
    assert hasattr(OpenRouterClient, "summarize_image")


class BrokenVectorStore:
    def search(self, collection_name: str, query: str, limit: int = 5):
        raise RuntimeError("leader not found")


class FallbackLlm:
    async def generate_with_context(self, query: str, context: list[dict], system_prompt: str) -> str:
        return f"LLM fallback for {query}"

    async def summarize_image(self, image_bytes: bytes, content_type: str, prompt: str) -> str:
        return "image summary"


class RecordingVectorStore:
    def __init__(self):
        self.documents = None

    def search(self, collection_name: str, query: str, limit: int = 5, filters=None):
        return []

    def add_documents(self, collection_name: str, documents: list[dict], texts: list[str]):
        self.documents = documents


@pytest.mark.anyio
async def test_rag_service_returns_answer_when_vector_store_is_unavailable():
    service = RAGService(vector_store=BrokenVectorStore(), llm=FallbackLlm())

    response = await service.query(RAGRequest(query="ретинол", user_id="debug", max_results=1))

    assert response.answer == "LLM fallback for ретинол"
    assert response.sources == []


@pytest.mark.anyio
async def test_rag_service_indexes_attachment_with_user_session_metadata():
    vector_store = RecordingVectorStore()
    service = RAGService(vector_store=vector_store, llm=FallbackLlm())

    response = await service.ingest_attachment(
        RAGAttachmentIngestRequest(
            attachment_id="3",
            user_id="42",
            session_id="7",
            filename="photo.png",
            content_type="image/png",
            content_base64="aW1hZ2U=",
        )
    )

    assert response.summary == "image summary"
    assert vector_store.documents[0]["source_type"] == "user_attachment"
    assert vector_store.documents[0]["user_id"] == "42"
    assert vector_store.documents[0]["session_id"] == "7"


@pytest.mark.anyio
async def test_rag_service_prompt_includes_skin_passport_context():
    service = RAGService(vector_store=BrokenVectorStore(), llm=FallbackLlm())
    request = RAGRequest(
        query="что мне использовать",
        user_id="42",
        session_id="7",
        context={"skin_passport": {"answers": {"skin_type": ["сухая"]}}},
    )

    source = inspect.getsource(service._build_system_prompt)
    assert "Паспорт кожи пользователя" in source
    assert "skin_passport" in source
    response = await service.query(request)
    assert response.answer == "LLM fallback for что мне использовать"


@pytest.mark.anyio
async def test_rag_service_initializes_when_default_vector_store_is_unavailable(monkeypatch):
    import app.services.rag_service as rag_service

    def fail_get_vector_store():
        raise RuntimeError("weaviate unavailable")

    monkeypatch.setattr(rag_service, "get_vector_store", fail_get_vector_store)
    service = RAGService(llm=FallbackLlm())

    response = await service.query(RAGRequest(query="ретинол", user_id="debug", max_results=1))

    assert response.answer == "LLM fallback for ретинол"
