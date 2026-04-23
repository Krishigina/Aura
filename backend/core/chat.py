import base64
import json
import re
from typing import Any, Dict, Optional

import aiohttp
from fastapi import HTTPException

from backend.core.config import AI_SERVICE_URL, CHAT_ATTACHMENTS_DIR
from backend.schemas.chat import RagChatSource


SUPPORTED_CHAT_ATTACHMENT_TYPES = {
    "application/pdf",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "image/jpeg",
    "image/png",
}
DEFAULT_CHAT_SESSION_TITLE = "Новый чат"


def build_chat_session_title(text: str) -> str:
    title = re.sub(r"\s+", " ", text or "").strip()
    if not title:
        return DEFAULT_CHAT_SESSION_TITLE
    return title[:80]


def format_chat_timestamp(value: Any) -> str:
    if value is None:
        return ""
    text = str(value)
    match = re.search(r"(?<!\d)(\d{2}:\d{2})(?::\d{2})?(?!\d)", text)
    if match:
        return match.group(1)
    return text


def validate_chat_attachment_content_type(content_type: str) -> str:
    normalized = (content_type or "").split(";")[0].strip().lower()
    if normalized not in SUPPORTED_CHAT_ATTACHMENT_TYPES:
        raise HTTPException(status_code=400, detail="Поддерживаются только PDF, DOCX, JPG и PNG")
    return normalized


def build_attachment_storage_path(user_id: int, session_id: int, attachment_id: int, filename: str) -> str:
    safe_filename = re.sub(r"[^A-Za-z0-9А-Яа-яЁё._-]+", "_", filename or "attachment").strip("._") or "attachment"
    return str(CHAT_ATTACHMENTS_DIR / f"user_{user_id}" / f"session_{session_id}" / f"{attachment_id}_{safe_filename}")


def compact_product_context(value: Any) -> Any:
    bulky_keys = {
        "photo",
        "photos",
        "video",
        "videos",
        "media",
        "image",
        "images",
        "data",
        "file_data",
        "image_data",
        "video_data",
        "content_base64",
    }
    if isinstance(value, dict):
        return {
            key: compact_product_context(item)
            for key, item in value.items()
            if str(key).lower() not in bulky_keys
        }
    if isinstance(value, list):
        return [compact_product_context(item) for item in value[:20]]
    if isinstance(value, str) and len(value) > 2000:
        return value[:2000]
    return value


def build_rag_query_payload(
    query: str,
    user_id: int,
    max_results: int = 5,
    session_id: Optional[int] = None,
    skin_passport: Optional[Dict[str, Any]] = None,
    product_context: Optional[Dict[str, Any]] = None,
) -> Dict:
    cleaned_query = (query or "").strip()
    if not cleaned_query:
        raise HTTPException(status_code=400, detail="Пустой вопрос")

    payload = {
        "query": cleaned_query,
        "user_id": str(user_id),
        "max_results": max_results,
    }
    if session_id is not None:
        payload["session_id"] = str(session_id)
    context = {}
    if skin_passport:
        context["skin_passport"] = skin_passport
    if product_context:
        context["product_context"] = compact_product_context(product_context)
    if context:
        payload["context"] = context
    return payload


def map_rag_proxy_error(error: Exception) -> HTTPException:
    print(f"RAG proxy error: {type(error).__name__}")
    return HTTPException(status_code=502, detail="AI сервис временно недоступен")


def normalize_rag_source(source: Dict) -> RagChatSource:
    score = source.get("score")
    if score is None:
        score = source.get("relevance")
    return RagChatSource(
        id=str(source.get("id") or ""),
        title=str(source.get("title") or ""),
        content=str(source.get("content") or ""),
        score=score,
    )


async def ingest_chat_attachment(
    attachment_id: int,
    user_id: int,
    session_id: int,
    filename: str,
    content_type: str,
    storage_path: str,
) -> Dict[str, Any]:
    timeout = aiohttp.ClientTimeout(total=120)
    with open(storage_path, "rb") as file_handle:
        content_base64 = base64.b64encode(file_handle.read()).decode("ascii")
    payload = {
        "attachment_id": str(attachment_id),
        "user_id": str(user_id),
        "session_id": str(session_id),
        "filename": filename,
        "content_type": content_type,
        "content_base64": content_base64,
    }
    async with aiohttp.ClientSession(timeout=timeout) as session:
        async with session.post(f"{AI_SERVICE_URL}/api/v1/rag/attachments/ingest", json=payload) as response:
            response_text = await response.text()
            if response.status >= 400:
                raise RuntimeError(f"AI attachment ingest status {response.status}")
            return json.loads(response_text) if response_text.strip() else {}


async def query_ai_service_rag(payload: Dict) -> Dict:
    timeout = aiohttp.ClientTimeout(total=60)
    async with aiohttp.ClientSession(timeout=timeout) as session:
        async with session.post(f"{AI_SERVICE_URL}/api/v1/rag/query", json=payload) as response:
            response_text = await response.text()
            if response.status >= 400:
                raise RuntimeError(f"AI service status {response.status}")
            if not response_text.strip():
                return {}
            return json.loads(response_text)
