from fastapi import APIRouter, Depends

from backend.core import passport_updates as passport_updates_core
from backend.core import skin_journal as skin_journal_core
from backend.core import skin_passport as skin_passport_core
from backend.core.passport_suggestion_validation import (
    validate_passport_suggestion_decision_status,
    validate_passport_suggestion_payload,
)
from backend.core.security import get_current_user
from backend.db.pool import get_db
from backend.schemas.profile import (
    PassportSuggestionCreate,
    PassportSuggestionUpdate,
    SkinJournalProcedureCreate,
    SkinJournalReminderAction,
    SkinJournalSensorReadingCreate,
    SkinJournalSettingsUpdate,
    SkinPassportResponse,
    SkinPassportUpdateRequest,
)


router = APIRouter(tags=["Profile"])


@router.get("/api/profile/skin-journal")
async def get_skin_journal(current_user: dict = Depends(get_current_user), db=Depends(get_db)):
    async with db.acquire() as conn:
        return await skin_journal_core.load_user_skin_journal(conn, current_user["id"])


@router.put("/api/profile/skin-journal/settings")
async def save_skin_journal_settings(
    payload: SkinJournalSettingsUpdate,
    current_user: dict = Depends(get_current_user),
    db=Depends(get_db),
):
    async with db.acquire() as conn:
        async with conn.transaction():
            return await skin_journal_core.save_skin_journal_settings_for_user(conn, current_user["id"], payload)


@router.post("/api/profile/skin-journal/procedures")
async def create_skin_journal_procedure(
    payload: SkinJournalProcedureCreate,
    current_user: dict = Depends(get_current_user),
    db=Depends(get_db),
):
    async with db.acquire() as conn:
        async with conn.transaction():
            return await skin_journal_core.create_skin_journal_procedure_for_user(conn, current_user["id"], payload)


@router.post("/api/profile/skin-journal/sensor-readings")
async def create_skin_journal_sensor_reading(
    payload: SkinJournalSensorReadingCreate,
    current_user: dict = Depends(get_current_user),
    db=Depends(get_db),
):
    async with db.acquire() as conn:
        async with conn.transaction():
            return await skin_journal_core.create_skin_journal_sensor_reading_for_user(conn, current_user["id"], payload)


@router.patch("/api/profile/skin-journal/reminders/{reminder_id}")
async def update_skin_journal_reminder(
    reminder_id: str,
    payload: SkinJournalReminderAction,
    current_user: dict = Depends(get_current_user),
    db=Depends(get_db),
):
    async with db.acquire() as conn:
        async with conn.transaction():
            return await skin_journal_core.update_skin_journal_reminder_for_user(conn, current_user["id"], reminder_id, payload)


@router.get("/api/profile/skin-passport", response_model=SkinPassportResponse)
async def get_skin_passport(current_user: dict = Depends(get_current_user), db=Depends(get_db)):
    async with db.acquire() as conn:
        return await passport_updates_core.get_skin_passport_for_user(
            conn,
            current_user["id"],
            skin_passport_core.sanitize_skin_passport_answers,
            SkinPassportResponse,
        )


@router.put("/api/profile/skin-passport", response_model=SkinPassportResponse)
async def save_skin_passport(
    payload: SkinPassportUpdateRequest,
    current_user: dict = Depends(get_current_user),
    db=Depends(get_db),
):
    async with db.acquire() as conn:
        return await passport_updates_core.save_skin_passport_for_user(
            conn,
            current_user["id"],
            payload,
            skin_passport_core.sanitize_skin_passport_answers,
            SkinPassportResponse,
        )


@router.get("/api/profile/skin-passport/suggestions")
async def list_passport_suggestions(current_user: dict = Depends(get_current_user), db=Depends(get_db)):
    async with db.acquire() as conn:
        return await passport_updates_core.list_passport_suggestions_for_user(conn, current_user["id"])


@router.post("/api/profile/skin-passport/suggestions")
async def create_passport_suggestion(
    payload: PassportSuggestionCreate,
    current_user: dict = Depends(get_current_user),
    db=Depends(get_db),
):
    try:
        validate_passport_suggestion_payload(payload)
    except ValueError as exc:
        from fastapi import HTTPException

        raise HTTPException(status_code=400, detail=str(exc)) from exc

    async with db.acquire() as conn:
        return await passport_updates_core.create_passport_suggestion_for_user(conn, current_user["id"], payload)


@router.patch("/api/profile/skin-passport/suggestions/{suggestion_id}")
async def update_passport_suggestion(
    suggestion_id: int,
    payload: PassportSuggestionUpdate,
    current_user: dict = Depends(get_current_user),
    db=Depends(get_db),
):
    try:
        status = validate_passport_suggestion_decision_status(payload.status)
    except ValueError as exc:
        from fastapi import HTTPException

        raise HTTPException(status_code=400, detail=str(exc)) from exc

    async with db.acquire() as conn:
        async with conn.transaction():
            return await passport_updates_core.update_passport_suggestion_for_user(
                conn,
                current_user["id"],
                suggestion_id,
                status,
                skin_passport_core.sanitize_skin_passport_answers,
            )
