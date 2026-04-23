from datetime import datetime
from pathlib import Path
from typing import Any, Dict

from backend.schemas.chat import ChatAttachmentResponse


async def create_chat_attachment_record(conn, user_id: int, session_id: int, filename: str, content_type: str):
    return await conn.fetchval(
        """
        INSERT INTO chat_attachments (user_id, session_id, filename, content_type, storage_path)
        VALUES ($1, $2, $3, $4, '')
        RETURNING id
        """,
        user_id,
        session_id,
        filename,
        content_type,
    )


async def save_chat_attachment_processing_result(
    conn,
    *,
    storage_path: str,
    summary: str,
    indexed_at,
    attachment_id: int,
    user_id: int,
):
    await conn.execute(
        """
        UPDATE chat_attachments
        SET storage_path=$1, extracted_text=$2, summary=$3, indexed_at=$4
        WHERE id=$5 AND user_id=$6
        """,
        storage_path,
        summary,
        summary,
        indexed_at,
        attachment_id,
        user_id,
    )


async def list_chat_session_attachments_for_user(conn, session_id: int, user_id: int):
    return await conn.fetch(
        """
        SELECT id, filename, content_type, summary, indexed_at
        FROM chat_attachments
        WHERE session_id=$1 AND user_id=$2
        ORDER BY created_at ASC, id ASC
        """,
        session_id,
        user_id,
    )


def persist_attachment_file(storage_path: str, contents: bytes) -> None:
    Path(storage_path).parent.mkdir(parents=True, exist_ok=True)
    with open(storage_path, "wb") as output:
        output.write(contents)


def build_chat_attachment_response(row, session_id: int):
    return ChatAttachmentResponse(
        attachment_id=int(row["id"]),
        session_id=session_id,
        filename=str(row["filename"] or ""),
        content_type=str(row["content_type"] or ""),
        status="ready" if row["indexed_at"] else "error",
        summary=str(row["summary"] or ""),
    )


def build_attachment_processing_state(ingest_result: Dict[str, Any]):
    summary = str(ingest_result.get("summary") or ingest_result.get("extracted_text") or "")
    indexed_at = datetime.utcnow()
    return summary, indexed_at
