from typing import Dict

from fastapi import APIRouter, Depends, File, UploadFile

from backend.core.assistant_knowledge_admin import (
    count_reindexable_knowledge_sources,
    create_admin_knowledge_document,
    create_user_knowledge_document,
    list_knowledge_sources_for_admin,
    reindex_knowledge_sources_to_ai,
    update_knowledge_source_for_admin,
)
from backend.core.knowledge import (
    extract_knowledge_text,
    get_knowledge_source_type,
    knowledge_source_response,
    normalize_knowledge_scope,
)
from backend.core.security import get_current_user
from backend.db.pool import get_db


router = APIRouter(tags=["Assistant Knowledge"])


@router.get("/api/assistant/knowledge/sources")
async def list_knowledge_sources(current_user: dict = Depends(get_current_user), db=Depends(get_db)):
    async with db.acquire() as conn:
        return await list_knowledge_sources_for_admin(conn, knowledge_source_response)


@router.patch("/api/assistant/knowledge/sources/{source_id}")
async def update_knowledge_source(source_id: int, payload: Dict, current_user: dict = Depends(get_current_user), db=Depends(get_db)):
    async with db.acquire() as conn:
        return await update_knowledge_source_for_admin(
            conn,
            source_id,
            payload,
            normalize_knowledge_scope=normalize_knowledge_scope,
            knowledge_source_response=knowledge_source_response,
        )


@router.post("/api/assistant/knowledge/admin-documents")
async def upload_admin_knowledge_document(
    file: UploadFile = File(...),
    scope: str = "both",
    weight: float = 1.0,
    current_user: dict = Depends(get_current_user),
    db=Depends(get_db),
):
    filename = file.filename or "document"
    content = await file.read()
    async with db.acquire() as conn:
        return await create_admin_knowledge_document(
            conn,
            filename=filename,
            content=content,
            scope=scope,
            weight=weight,
            normalize_knowledge_scope=normalize_knowledge_scope,
            get_knowledge_source_type=get_knowledge_source_type,
            extract_knowledge_text=extract_knowledge_text,
            knowledge_source_response=knowledge_source_response,
        )


@router.post("/api/assistant/knowledge/user-documents")
async def upload_user_knowledge_document(file: UploadFile = File(...), current_user: dict = Depends(get_current_user), db=Depends(get_db)):
    filename = file.filename or "document"
    content = await file.read()
    async with db.acquire() as conn:
        return await create_user_knowledge_document(
            conn,
            filename=filename,
            content=content,
            user_id=current_user["id"],
            get_knowledge_source_type=get_knowledge_source_type,
            extract_knowledge_text=extract_knowledge_text,
            knowledge_source_response=knowledge_source_response,
        )


@router.post("/api/assistant/knowledge/reindex")
async def reindex_knowledge_sources(current_user: dict = Depends(get_current_user), db=Depends(get_db)):
    async with db.acquire() as conn:
        return await reindex_knowledge_sources_to_ai(conn)
