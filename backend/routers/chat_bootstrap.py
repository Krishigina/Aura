from fastapi import APIRouter, Depends

from backend.core.chat_bootstrap_service import append_chat_message, load_chat_bootstrap_payload
from backend.core.security import get_current_user
from backend.db.pool import get_db
from backend.schemas.chat import ChatMessageCreateRequest


router = APIRouter(tags=["Chat"])


@router.get("/api/chat/bootstrap")
async def get_chat_bootstrap(current_user: dict = Depends(get_current_user), db=Depends(get_db)):
    user_id = current_user.get("id")
    async with db.acquire() as conn:
        return await load_chat_bootstrap_payload(conn, user_id=user_id)


@router.post("/api/chat/messages")
async def append_bootstrap_chat_message(
    payload: ChatMessageCreateRequest,
    current_user: dict = Depends(get_current_user),
    db=Depends(get_db),
):
    user_id = current_user.get("id")
    async with db.acquire() as conn:
        return await append_chat_message(conn, user_id=user_id, payload=payload)
