from fastapi import HTTPException


async def prepare_chat_session_attachment(
    *,
    conn,
    session_id: int,
    user_id: int,
    filename: str,
    content_type: str,
    ensure_chat_session_owned_by_user,
    create_chat_attachment_record,
):
    await ensure_chat_session_owned_by_user(conn, session_id, user_id)
    attachment_id = await create_chat_attachment_record(
        conn,
        user_id,
        session_id,
        filename,
        content_type,
    )
    return int(attachment_id)


async def finalize_chat_session_attachment(
    *,
    conn,
    storage_path: str,
    summary: str,
    indexed_at,
    attachment_id: int,
    user_id: int,
    save_chat_attachment_processing_result,
):
    await save_chat_attachment_processing_result(
        conn,
        storage_path=storage_path,
        summary=summary,
        indexed_at=indexed_at,
        attachment_id=attachment_id,
        user_id=user_id,
    )


def build_chat_attachment_result(
    *,
    attachment_id: int,
    session_id: int,
    filename: str,
    content_type: str,
    indexed_at,
    summary: str,
    response_model,
):
    return response_model(
        attachment_id=attachment_id,
        session_id=session_id,
        filename=filename,
        content_type=content_type,
        status="ready" if indexed_at else "error",
        summary=summary,
    )


async def load_rag_chat_session_row(
    *,
    conn,
    session_id: int,
    user_id: int,
    get_owned_chat_session_overview,
):
    session_row = await get_owned_chat_session_overview(conn, session_id, user_id)
    if not session_row:
        raise HTTPException(status_code=404, detail="Чат не найден")
    return session_row


def build_rag_response_payload(*, rag_response, normalize_rag_sources):
    answer = str(rag_response.get("answer") or "").strip()
    raw_sources = rag_response.get("sources") or []
    sources, sources_json = normalize_rag_sources(raw_sources)
    return answer, sources, sources_json
