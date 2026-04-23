from typing import Any, Dict, List, Optional

from pydantic import BaseModel


class ChatMessageCreateRequest(BaseModel):
    text: str
    is_from_user: bool = True
    timestamp: Optional[str] = None


class ChatSessionSummary(BaseModel):
    id: int
    title: str
    last_message: str = ""
    updated_at: str = ""
    message_count: int = 0


class ChatSessionsResponse(BaseModel):
    sessions: List[ChatSessionSummary] = []


class ChatSessionCreateResponse(BaseModel):
    session_id: int
    title: str


class ChatSessionMessage(BaseModel):
    role: str
    content: str
    timestamp: str = ""


class ChatSessionDetailResponse(BaseModel):
    session: ChatSessionSummary
    messages: List[ChatSessionMessage] = []


class ChatAttachmentResponse(BaseModel):
    attachment_id: int
    session_id: int
    filename: str
    content_type: str
    status: str = "ready"
    summary: str = ""


class ChatAttachmentsResponse(BaseModel):
    attachments: List[ChatAttachmentResponse] = []


class RagChatRequest(BaseModel):
    message: str
    session_id: int | None = None
    product_context: Optional[Dict[str, Any]] = None


class RagChatSource(BaseModel):
    id: str = ""
    title: str = ""
    content: str = ""
    score: Optional[float] = None


class RagChatResponse(BaseModel):
    session_id: int
    answer: str = ""
    sources: List[RagChatSource] = []
    conversation_id: Optional[str] = None
