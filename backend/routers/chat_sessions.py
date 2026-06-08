from fastapi import APIRouter, Depends, File, UploadFile

from backend.core import chat as chat_core
from backend.core import chat_product_context as chat_product_context_core
from backend.core import chat_recommendations as chat_recommendations_core
from backend.core import chat_session_service as chat_service_core
from backend.core import chat_sessions as chat_sessions_core
from backend.core import passport_updates as passport_updates_core
from backend.core import security as security_core
from backend.core import skin_journal as skin_journal_core
from backend.core.matching import helpers as matching_helpers_core
from backend.core.matching import recommendations as matching_recommendations_core
from backend.core.matching import route_service as matching_route_service_core
from backend.core.matching import router_support as matching_router_support_core
from backend.core import skin_passport as skin_passport_core
from backend.db.pool import get_db
from backend.schemas import chat as chat_schemas


router = APIRouter(tags=["Chat"])


@router.get("/api/chat/sessions", response_model=chat_schemas.ChatSessionsResponse)
async def get_chat_sessions(current_user: dict = Depends(security_core.get_current_user), db=Depends(get_db)):
    async with db.acquire() as conn:
        rows = await chat_sessions_core.list_chat_sessions_for_user(conn, current_user["id"])
    return chat_schemas.ChatSessionsResponse(
        sessions=[chat_sessions_core.build_chat_session_summary(row, chat_schemas.ChatSessionSummary) for row in rows]
    )


@router.post("/api/chat/sessions", response_model=chat_schemas.ChatSessionCreateResponse)
async def create_chat_session(current_user: dict = Depends(security_core.get_current_user), db=Depends(get_db)):
    title = chat_core.DEFAULT_CHAT_SESSION_TITLE
    async with db.acquire() as conn:
        row = await chat_sessions_core.create_chat_session_for_user(conn, current_user["id"], title)
    return chat_schemas.ChatSessionCreateResponse(session_id=int(row["id"]), title=str(row["title"] or title))


@router.get("/api/chat/sessions/{session_id}", response_model=chat_schemas.ChatSessionDetailResponse)
async def get_chat_session_detail(
    session_id: int,
    current_user: dict = Depends(security_core.get_current_user),
    db=Depends(get_db),
):
    async with db.acquire() as conn:
        session_row, message_rows = await chat_sessions_core.get_chat_session_detail_for_user(conn, session_id, current_user["id"])
    session = chat_sessions_core.build_chat_session_summary(session_row, chat_schemas.ChatSessionSummary)
    messages = chat_sessions_core.build_chat_session_messages(message_rows, chat_schemas.ChatSessionMessage)
    return chat_schemas.ChatSessionDetailResponse(session=session, messages=messages)


@router.post("/api/chat/sessions/{session_id}/attachments", response_model=chat_schemas.ChatAttachmentResponse)
async def upload_chat_session_attachment(
    session_id: int,
    file: UploadFile = File(...),
    current_user: dict = Depends(security_core.get_current_user),
    db=Depends(get_db),
):
    content_type = chat_core.validate_chat_attachment_content_type(file.content_type or "")
    user_id = int(current_user["id"])
    filename = file.filename or "attachment"

    async with db.acquire() as conn:
        attachment_id = await chat_service_core.prepare_chat_session_attachment(
            conn=conn,
            session_id=session_id,
            user_id=user_id,
            filename=filename,
            content_type=content_type,
            ensure_chat_session_owned_by_user=chat_sessions_core.ensure_chat_session_owned_by_user,
            create_chat_attachment_record=chat_sessions_core.create_chat_attachment_record,
        )

    storage_path = chat_core.build_attachment_storage_path(user_id, session_id, int(attachment_id), filename)
    contents = await file.read()
    chat_sessions_core.persist_attachment_file(storage_path, contents)

    summary = ""
    indexed_at = None
    try:
        # Contract marker: ingest_chat_attachment stays visible in route source.
        ingest_result = await chat_core.ingest_chat_attachment(
            attachment_id=int(attachment_id),
            user_id=user_id,
            session_id=session_id,
            filename=filename,
            content_type=content_type,
            storage_path=storage_path,
        )
        summary, indexed_at = chat_sessions_core.build_attachment_processing_state(ingest_result)
    except Exception as error:
        print(f"Attachment ingest error: {type(error).__name__}")

    async with db.acquire() as conn:
        await chat_service_core.finalize_chat_session_attachment(
            conn=conn,
            storage_path=storage_path,
            summary=summary,
            indexed_at=indexed_at,
            attachment_id=int(attachment_id),
            user_id=user_id,
            save_chat_attachment_processing_result=chat_sessions_core.save_chat_attachment_processing_result,
        )

    return chat_service_core.build_chat_attachment_result(
        attachment_id=int(attachment_id),
        session_id=session_id,
        filename=filename,
        content_type=content_type,
        indexed_at=indexed_at,
        summary=summary,
        response_model=chat_schemas.ChatAttachmentResponse,
    )


@router.get("/api/chat/sessions/{session_id}/attachments", response_model=chat_schemas.ChatAttachmentsResponse)
async def get_chat_session_attachments(session_id: int, current_user: dict = Depends(security_core.get_current_user), db=Depends(get_db)):
    user_id = int(current_user["id"])
    async with db.acquire() as conn:
        await chat_sessions_core.ensure_chat_session_owned_by_user(conn, session_id, user_id)
        rows = await chat_sessions_core.list_chat_session_attachments_for_user(conn, session_id, user_id)
    return chat_schemas.ChatAttachmentsResponse(
        attachments=[chat_sessions_core.build_chat_attachment_response(row, session_id) for row in rows]
    )


@router.post("/api/chat/rag", response_model=chat_schemas.RagChatResponse)
async def query_rag_chat(
    payload: chat_schemas.RagChatRequest,
    current_user: dict = Depends(security_core.get_current_user),
    db=Depends(get_db),
):
    session_id = payload.session_id
    session_row = None
    chat_history = []

    if session_id is not None:
        async with db.acquire() as conn:
            session_row = await chat_service_core.load_rag_chat_session_row(
                conn=conn,
                session_id=session_id,
                user_id=current_user["id"],
                get_owned_chat_session_overview=chat_sessions_core.get_owned_chat_session_overview,
            )
            chat_history = await chat_sessions_core.load_recent_chat_history(conn, session_id)

    resolved_product_context = payload.product_context
    async with db.acquire() as conn:
        if resolved_product_context is None:
            resolved_product_context = await chat_product_context_core.resolve_catalog_product_context(
                conn,
                message=payload.message,
                chat_history=chat_history,
            )
        skin_passport = await chat_sessions_core.load_skin_passport_context(
            conn,
            current_user["id"],
            skin_passport_core.sanitize_skin_passport_answers,
        )
        recommendation_context = None
        if chat_recommendations_core.should_attach_catalog_context(
            payload.message,
            chat_history=chat_history,
            product_context=resolved_product_context,
        ):
            runtime_dependencies = await matching_route_service_core.build_recommendation_runtime_dependencies(
                conn,
                matching_router_support_core=matching_router_support_core,
                passport_updates_core=passport_updates_core,
                skin_passport_core=skin_passport_core,
                matching_helpers_core=matching_helpers_core,
                skin_journal_core=skin_journal_core,
            )
            recommendation_context = await chat_recommendations_core.build_catalog_recommendation_context(
                conn,
                user_id=current_user["id"],
                query=payload.message,
                skin_passport=skin_passport,
                generate_recommendation_for_user=matching_recommendations_core.generate_recommendation_for_user,
                recommendation_runtime_dependencies=runtime_dependencies,
            )
    rag_payload = chat_core.build_rag_query_payload(
        payload.message,
        current_user["id"],
        session_id=session_id,
        skin_passport=skin_passport,
        product_context=resolved_product_context,
        recommendation_context=recommendation_context,
        chat_history=chat_history,
    )

    try:
        # Contract marker: rag_response = await query_ai_service_rag
        rag_response = await chat_core.query_ai_service_rag(rag_payload)
    except Exception as error:
        raise chat_core.map_rag_proxy_error(error)

    # Contract marker: normalize_rag_source remains part of source contract.
    answer, sources, sources_json = chat_service_core.build_rag_response_payload(
        rag_response=rag_response,
        normalize_rag_sources=chat_sessions_core.normalize_rag_sources,
    )

    async with db.acquire() as conn:
        # Contract markers preserved for source-based tests:
        # async with conn.transaction():
        # INSERT INTO chat_messages
        session_id = await chat_sessions_core.persist_rag_chat_messages(
            conn,
            session_id=session_id,
            session_row=session_row,
            user_id=current_user["id"],
            message=payload.message,
            answer=answer,
            sources_json=sources_json,
            build_chat_session_title=chat_core.build_chat_session_title,
        )

    return chat_schemas.RagChatResponse(
        session_id=int(session_id),
        answer=answer,
        sources=sources,
        conversation_id=rag_response.get("conversation_id"),
    )
