import base64
import inspect
import pytest

from app.api.routes import rag
from app.models.schemas import AttachmentIngestResponse, RAGAttachmentIngestRequest, RAGRequest
from app.infrastructure.llm_client import OpenRouterClient
from app.infrastructure.vector_store import VectorStore
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


def test_delete_knowledge_route_uses_rag_service_not_legacy_pipeline():
    source = inspect.getsource(rag.delete_knowledge)

    assert "get_rag_service" in source
    assert "get_rag_pipeline" not in source


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


def test_rag_v1_defaults_use_aura_knowledge_and_available_embeddings():
    from app.core.config import Settings

    settings = Settings(_env_file=None)

    assert settings.collection_knowledge == "AuraKnowledge"
    assert settings.embedding_model == "sentence-transformers/all-MiniLM-L6-v2"
    assert settings.embedding_dimension == 384


def test_embedder_raises_when_embedding_service_returns_error(monkeypatch):
    from app.infrastructure.embedder import Embedder, EmbeddingError

    class FakeClient:
        def __enter__(self):
            return self

        def __exit__(self, exc_type, exc, tb):
            return False

        def post(self, *args, **kwargs):
            return type("Response", (), {"status_code": 500, "text": "bad gateway"})()

    monkeypatch.setattr("app.infrastructure.embedder.httpx.Client", lambda: FakeClient())

    with pytest.raises(EmbeddingError):
        Embedder("http://embedder", 1024).embed_query("ретинол")


def test_embedder_supports_huggingface_tei_embed_endpoint(monkeypatch):
    from app.infrastructure.embedder import Embedder

    class FakeResponse:
        def __init__(self, status_code, payload=None):
            self.status_code = status_code
            self._payload = payload

        def json(self):
            return self._payload

    class FakeClient:
        def __enter__(self):
            return self

        def __exit__(self, exc_type, exc, tb):
            return False

        def post(self, url, *args, **kwargs):
            if url.endswith("/vectors"):
                return FakeResponse(404, {"error": "not found"})
            return FakeResponse(200, [[0.1, 0.2, 0.3]])

    monkeypatch.setattr("app.infrastructure.embedder.httpx.Client", lambda: FakeClient())

    assert Embedder("http://embedder", 3).embed_query("ретинол") == [0.1, 0.2, 0.3]


class BrokenVectorStore:
    def search(self, collection_name: str, query: str, limit: int = 5):
        raise RuntimeError("leader not found")


class FallbackLlm:
    async def generate_with_context(self, query: str, context: list[dict], system_prompt: str) -> str:
        return f"LLM fallback for {query}"

    async def summarize_image(self, image_bytes: bytes, content_type: str, prompt: str) -> str:
        return "image summary"


class RecordingFallbackLlm(FallbackLlm):
    def __init__(self):
        self.context = None

    async def generate_with_context(self, query: str, context: list[dict], system_prompt: str) -> str:
        self.context = context
        return await super().generate_with_context(query, context, system_prompt)


class StructuredNoneRecoveringLlm(RecordingFallbackLlm):
    async def generate_structured(self, query: str, context: list[dict], system_prompt: str, json_schema: dict) -> dict:
        return {
            "answer": "Недостаточно данных.",
            "confidence": "none",
            "sources_used": [],
        }

    async def generate_with_context(self, query: str, context: list[dict], system_prompt: str) -> str:
        self.context = context
        return "Можно начать с 1-2 раз в неделю вечером и следить за реакцией кожи."


class StructuredNoneKnowledgeRecoveringLlm(RecordingFallbackLlm):
    async def generate_structured(self, query: str, context: list[dict], system_prompt: str, json_schema: dict) -> dict:
        return {
            "answer": "Недостаточно данных.",
            "confidence": "none",
            "sources_used": [],
        }

    async def generate_with_context(self, query: str, context: list[dict], system_prompt: str) -> str:
        self.context = context
        return "Начинайте ретинол 2-3 раза в неделю вечером и обязательно используйте SPF днем."


class StructuredLowInsufficientLlm(RecordingFallbackLlm):
    async def generate_structured(self, query: str, context: list[dict], system_prompt: str, json_schema: dict) -> dict:
        return {
            "answer": "К сожалению, информации недостаточно для ответа на ваш вопрос. Пожалуйста, уточните его.",
            "confidence": "low",
            "sources_used": [0],
        }


class RecordingVectorStore:
    def __init__(self):
        self.documents = None

    def search(self, collection_name: str, query: str, limit: int = 5, filters=None):
        return []

    def add_documents(self, collection_name: str, documents: list[dict], texts: list[str]):
        self.documents = documents


class SplitResultsVectorStore:
    def __init__(self):
        self.calls = []

    def search(self, collection_name: str, query: str, limit: int = 5, filters=None):
        self.calls.append({"collection_name": collection_name, "query": query, "limit": limit, "filters": filters})
        if filters and filters.get("session_id"):
            return [
                {
                    "id": "session-doc-1",
                    "score": 0.93,
                    "properties": {"title": "Сессионный документ", "content": "Локальная заметка по продукту"},
                }
            ]
        return [
            {
                "id": "global-doc-1",
                "score": 0.81,
                "properties": {"title": "Глобальная база", "content": "Характеристики продукта из базы знаний"},
            }
        ]


class ProductBoostVectorStore:
    def search(self, collection_name: str, query: str, limit: int = 5, filters=None):
        if filters and filters.get("session_id"):
            return [
                {
                    "id": "session-product-doc",
                    "score": 0.60,
                    "properties": {"title": "Ultra Cream", "content": "Состав: ниацинамид, пантенол"},
                }
            ]
        return [
            {
                "id": "global-generic-doc",
                "score": 0.90,
                "properties": {"title": "Общий гид", "content": "Общие рекомендации по уходу"},
            }
        ]


class QueryBoostVectorStore:
    def search(self, collection_name: str, query: str, limit: int = 5, filters=None):
        return [
            {
                "id": "generic-doc",
                "score": 0.91,
                "properties": {"title": "Общие рекомендации", "content": "Базовый уход за кожей и общие советы.", "category": "skincare_knowledge"},
            },
            {
                "id": "retinol-doc",
                "score": 0.20,
                "properties": {"title": "Ретинол (Vitamin A)", "content": "Начинать 2-3 раза в неделю вечером, использовать SPF.", "category": "active_ingredient"},
            },
        ]


class RecordingSearchQueryVectorStore:
    def __init__(self):
        self.queries = []

    def search(self, collection_name: str, query: str, limit: int = 5, filters=None):
        self.queries.append(query)
        return [
            {
                "id": "doc-1",
                "score": 0.88,
                "properties": {"title": "Retinol serum", "content": "Используйте 2 раза в неделю вечером"},
            }
        ]


class RecordingAddVectorStore:
    def __init__(self):
        self.calls = []

    def add_documents(self, collection_name: str, documents: list[dict], texts: list[str]):
        self.calls.append({"collection_name": collection_name, "documents": documents, "texts": texts})


class FakeWeaviateCollections:
    def __init__(self, collection):
        self.collection = collection

    def exists(self, name: str) -> bool:
        return True

    def get(self, name: str):
        return self.collection


class FakeWeaviateClient:
    def __init__(self, collection):
        self.collections = FakeWeaviateCollections(collection)


class FakeQuery:
    def __init__(self):
        self.hybrid_kwargs = None

    def hybrid(self, **kwargs):
        self.hybrid_kwargs = kwargs
        return type("SearchResult", (), {"objects": []})()


class FakeCollection:
    def __init__(self):
        self.query = FakeQuery()


class FakeEmbedder:
    def embed_query(self, text: str) -> list[float]:
        return [0.1, 0.2, 0.3]


class NoopReranker:
    async def rerank(self, query: str, documents: list[str], top_k: int):
        return None


class ReverseReranker:
    async def rerank(self, query: str, documents: list[str], top_k: int):
        return list(reversed(range(min(len(documents), top_k))))


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
async def test_rag_service_indexes_attachment_chunks_with_session_metadata():
    vector_store = RecordingVectorStore()
    service = RAGService(vector_store=vector_store, llm=FallbackLlm(), reranker=NoopReranker())
    content = " ".join(["ниацинамид"] * 250)

    await service.ingest_attachment(
        RAGAttachmentIngestRequest(
            attachment_id="3",
            user_id="42",
            session_id="7",
            filename="routine.txt",
            content_type="text/plain",
            content_base64=base64.b64encode(content.encode("utf-8")).decode("ascii"),
        )
    )

    assert len(vector_store.documents) > 1
    assert vector_store.documents[0]["source_type"] == "user_attachment"
    assert vector_store.documents[0]["user_id"] == "42"
    assert vector_store.documents[0]["session_id"] == "7"
    assert vector_store.documents[0]["chunk_index"] == 0


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
async def test_rag_service_prompt_includes_catalog_recommendation_context():
    service = RAGService(vector_store=BrokenVectorStore(), llm=FallbackLlm())
    request = RAGRequest(
        query="какие продукты мне подходят",
        user_id="42",
        session_id="7",
        context={
            "recommendation_context": {
                "status": "available",
                "products": [{"product_id": 75, "product_name": "Vinoclean Moisturizing Toner", "brand": "Caudalie"}],
            }
        },
    )

    source = inspect.getsource(service._build_system_prompt)
    assert "recommendation_context" in source
    response = await service.query(request)
    assert response.answer == "LLM fallback for какие продукты мне подходят"


@pytest.mark.anyio
async def test_rag_service_initializes_when_default_vector_store_is_unavailable(monkeypatch):
    import app.services.rag_service as rag_service

    def fail_get_vector_store():
        raise RuntimeError("weaviate unavailable")

    monkeypatch.setattr(rag_service, "get_vector_store", fail_get_vector_store)
    service = RAGService(llm=FallbackLlm())

    response = await service.query(RAGRequest(query="ретинол", user_id="debug", max_results=1))

    assert response.answer == "LLM fallback for ретинол"


@pytest.mark.anyio
async def test_rag_service_combines_session_and_global_knowledge_when_session_exists():
    vector_store = SplitResultsVectorStore()
    service = RAGService(vector_store=vector_store, llm=FallbackLlm(), reranker=NoopReranker())

    response = await service.query(
        RAGRequest(
            query="что важно про этот продукт",
            user_id="42",
            session_id="7",
            max_results=5,
        )
    )

    assert len(vector_store.calls) == 2
    assert vector_store.calls[0]["filters"] == {"user_id": "42", "session_id": "7"}
    assert vector_store.calls[1]["filters"] is None
    assert len(response.sources) == 2
    assert {source["id"] for source in response.sources} == {"session-doc-1", "global-doc-1"}


@pytest.mark.anyio
async def test_rag_service_boosts_product_related_sources_only_when_product_context_present():
    service = RAGService(vector_store=ProductBoostVectorStore(), llm=FallbackLlm(), reranker=NoopReranker())

    with_product_context = await service.query(
        RAGRequest(
            query="подходит ли мне этот продукт",
            user_id="42",
            session_id="7",
            context={"product_context": {"product": {"name": "Ultra Cream"}}},
            max_results=5,
        )
    )
    without_product_context = await service.query(
        RAGRequest(
            query="подходит ли мне этот продукт",
            user_id="42",
            session_id="7",
            max_results=5,
        )
    )

    assert with_product_context.sources[0]["id"] == "session-product-doc"
    assert without_product_context.sources[0]["id"] == "global-generic-doc"


@pytest.mark.anyio
async def test_rag_service_boosts_query_keyword_matches_above_generic_results():
    service = RAGService(vector_store=QueryBoostVectorStore(), llm=FallbackLlm(), reranker=NoopReranker())

    response = await service.query(
        RAGRequest(
            query="как использовать ретинол",
            user_id="42",
            max_results=5,
        )
    )

    assert response.sources[0]["id"] == "retinol-doc"


@pytest.mark.anyio
async def test_rag_service_keeps_query_keyword_match_first_after_reranker():
    service = RAGService(vector_store=QueryBoostVectorStore(), llm=FallbackLlm(), reranker=ReverseReranker())

    response = await service.query(
        RAGRequest(
            query="как использовать ретинол",
            user_id="42",
            max_results=5,
        )
    )

    assert response.sources[0]["id"] == "retinol-doc"


@pytest.mark.anyio
async def test_rag_service_passes_product_context_to_llm_when_search_has_no_results():
    llm = RecordingFallbackLlm()
    service = RAGService(vector_store=RecordingVectorStore(), llm=llm)

    await service.query(
        RAGRequest(
            query="подходит ли мне этот продукт",
            user_id="42",
            context={"product_context": {"product": {"name": "Ultra Cream", "composition": "Aqua, Panthenol"}}},
            max_results=5,
        )
    )

    assert llm.context == [
        {
            "title": "Контекст текущего продукта",
            "content": '{"product": {"name": "Ultra Cream", "composition": "Aqua, Panthenol"}}',
            "category": "product_context",
        }
    ]


@pytest.mark.anyio
async def test_rag_service_passes_chat_history_to_llm_when_search_has_no_results():
    llm = RecordingFallbackLlm()
    service = RAGService(vector_store=RecordingVectorStore(), llm=llm)

    response = await service.query(
        RAGRequest(
            query="А как часто использовать?",
            user_id="42",
            session_id="7",
            context={
                "chat_history": [
                    {"role": "user", "content": "Подойдет ли сыворотка Retinol Serum для новичка?"},
                    {"role": "assistant", "content": "Да, но начинайте постепенно и наносите вечером."},
                ]
            },
            max_results=5,
        )
    )

    assert response.answer == "LLM fallback for А как часто использовать?"
    assert llm.context == [
        {
            "title": "История диалога",
            "content": "user: Подойдет ли сыворотка Retinol Serum для новичка?\nassistant: Да, но начинайте постепенно и наносите вечером.",
            "category": "chat_history",
        }
    ]


@pytest.mark.anyio
async def test_rag_service_expands_follow_up_query_with_chat_history():
    vector_store = RecordingSearchQueryVectorStore()
    service = RAGService(vector_store=vector_store, llm=FallbackLlm(), reranker=NoopReranker())

    await service.query(
        RAGRequest(
            query="А как часто использовать?",
            user_id="42",
            session_id="7",
            context={
                "chat_history": [
                    {"role": "user", "content": "Подойдет ли сыворотка Retinol Serum для новичка?"},
                    {"role": "assistant", "content": "Да, но начинайте постепенно и наносите вечером."},
                ]
            },
            max_results=5,
        )
    )

    assert len(vector_store.queries) == 2
    assert vector_store.queries[0].startswith("А как часто использовать?")
    assert "Retinol Serum" in vector_store.queries[0]
    assert "начинайте постепенно" in vector_store.queries[0]

@pytest.mark.anyio
async def test_rag_service_recovers_answer_when_structured_output_returns_none_with_context():
    llm = StructuredNoneRecoveringLlm()
    service = RAGService(vector_store=RecordingSearchQueryVectorStore(), llm=llm, reranker=NoopReranker())

    response = await service.query(
        RAGRequest(
            query="А как часто использовать?",
            user_id="42",
            session_id="7",
            context={
                "chat_history": [
                    {"role": "user", "content": "Подойдет ли сыворотка Retinol Serum для новичка?"},
                    {"role": "assistant", "content": "Да, но начинайте постепенно и наносите вечером."},
                ]
            },
            max_results=5,
        )
    )

    assert response.answer == "Можно начать с 1-2 раз в неделю вечером и следить за реакцией кожи."


@pytest.mark.anyio
async def test_rag_service_recovers_answer_when_structured_output_returns_none_with_knowledge_sources():
    llm = StructuredNoneKnowledgeRecoveringLlm()
    service = RAGService(vector_store=QueryBoostVectorStore(), llm=llm, reranker=NoopReranker())

    response = await service.query(
        RAGRequest(
            query="как использовать ретинол",
            user_id="42",
            max_results=5,
        )
    )

    assert response.answer == "Начинайте ретинол 2-3 раза в неделю вечером и обязательно используйте SPF днем."


def test_rag_service_builds_direct_answer_from_matching_knowledge_source():
    service = RAGService()
    results = [
        {
            "id": "generic-doc",
            "properties": {"title": "Керамиды", "content": "Поддерживают барьер кожи.", "category": "active_ingredient"},
        },
        {
            "id": "retinol-doc",
            "properties": {
                "title": "Ретинол (Vitamin A)",
                "content": "Ретинол — это форма витамина A. Начинать с низкой концентрации 2-3 раза в неделю. Обязательно использовать SPF днем.",
                "category": "active_ingredient",
            },
        },
    ]

    answer = service._build_direct_source_answer("как использовать ретинол", results)

    assert "2-3 раза в неделю" in answer


@pytest.mark.anyio
async def test_rag_service_replaces_insufficient_structured_answer_with_direct_source_summary():
    llm = StructuredLowInsufficientLlm()
    service = RAGService(vector_store=QueryBoostVectorStore(), llm=llm, reranker=NoopReranker())

    response = await service.query(
        RAGRequest(
            query="как использовать ретинол",
            user_id="42",
            max_results=5,
        )
    )

    assert "2-3 раза в неделю" in response.answer


def test_rag_service_add_knowledge_uses_default_vector_store_when_not_injected(monkeypatch):
    import app.services.rag_service as rag_service

    recorder = RecordingAddVectorStore()
    monkeypatch.setattr(rag_service, "get_vector_store", lambda: recorder)
    service = RAGService(llm=FallbackLlm())

    result = service.add_knowledge([
        {"title": "A", "content": "B", "category": "ingredients"},
    ])

    assert result["status"] == "success"
    assert result["indexed_count"] == 1
    assert len(recorder.calls) == 1


def test_rag_service_chunks_long_text_with_overlap():
    service = RAGService(vector_store=RecordingAddVectorStore(), llm=FallbackLlm(), reranker=NoopReranker())
    text = " ".join(["ретинол"] * 300)

    chunks = service._chunk_text(text, chunk_size=120, overlap=20)

    assert len(chunks) > 1
    assert all(chunk.strip() for chunk in chunks)
    assert chunks[0][-20:] in chunks[1]


def test_rag_service_add_knowledge_indexes_chunks_with_metadata():
    recorder = RecordingAddVectorStore()
    service = RAGService(vector_store=recorder, llm=FallbackLlm(), reranker=NoopReranker())
    long_content = " ".join(["гиалуроновая кислота"] * 250)

    result = service.add_knowledge([{"title": "Hydration", "content": long_content, "category": "care"}])

    assert result["indexed_count"] > 1
    first = recorder.calls[0]["documents"][0]
    assert first["document_id"]
    assert first["chunk_index"] == 0
    assert first["chunk_count"] == result["indexed_count"]
    assert first["embedding_model"] == "sentence-transformers/all-MiniLM-L6-v2"
    assert first["schema_version"] == "rag_v1"


def test_rag_service_builds_retrieval_debug_snapshot():
    service = RAGService(vector_store=RecordingVectorStore(), llm=FallbackLlm(), reranker=NoopReranker())

    debug = service._build_retrieval_debug(
        filters={"user_id": "42", "session_id": "7"},
        session_results=[{"id": "s1", "score": 0.9}],
        global_results=[{"id": "g1", "score": 0.8}],
        reranker_used=False,
    )

    assert debug["collection"] == "AuraKnowledge"
    assert debug["filters"] == {"user_id": "42", "session_id": "7"}
    assert debug["session_result_count"] == 1
    assert debug["global_result_count"] == 1
    assert debug["source_ids"] == ["s1", "g1"]
    assert debug["reranker_used"] is False


def test_vector_store_search_passes_metadata_filters_to_weaviate():
    collection = FakeCollection()
    store = VectorStore.__new__(VectorStore)
    store.client = FakeWeaviateClient(collection)
    store.embedder = FakeEmbedder()

    store.search(
        collection_name="KnowledgeBase",
        query="ретинол",
        filters={"user_id": "42", "session_id": "7"},
    )

    assert collection.query.hybrid_kwargs["filters"] is not None
