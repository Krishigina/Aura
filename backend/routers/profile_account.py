from fastapi import APIRouter, Depends

from backend.core.profile_account_service import (
    delete_profile_account_record,
    update_profile_account_record,
    update_profile_password_record,
)
from backend.core.security import get_current_user, get_password_hash, verify_password
from backend.core.users import ensure_unique_login, ensure_users_columns, is_valid_login, normalize_login
from backend.db.pool import get_db
from backend.schemas.auth import ProfileAccountUpdateRequest, ProfileDeleteRequest, ProfilePasswordUpdateRequest


router = APIRouter(tags=["Profile"])


@router.patch("/api/profile/account")
async def update_profile_account(
    request: ProfileAccountUpdateRequest,
    current_user: dict = Depends(get_current_user),
    db=Depends(get_db),
):
    async with db.acquire() as conn:
        return await update_profile_account_record(
            conn,
            request=request,
            current_user_id=current_user["id"],
            ensure_users_columns=ensure_users_columns,
            ensure_unique_login=ensure_unique_login,
            is_valid_login=is_valid_login,
            normalize_login=normalize_login,
        )


@router.patch("/api/profile/password")
async def update_profile_password(
    request: ProfilePasswordUpdateRequest,
    current_user: dict = Depends(get_current_user),
    db=Depends(get_db),
):
    async with db.acquire() as conn:
        return await update_profile_password_record(
            conn,
            request=request,
            current_user_id=current_user["id"],
            verify_password=verify_password,
            get_password_hash=get_password_hash,
        )


@router.delete("/api/profile/account")
async def delete_profile_account(
    request: ProfileDeleteRequest,
    current_user: dict = Depends(get_current_user),
    db=Depends(get_db),
):
    async with db.acquire() as conn:
        return await delete_profile_account_record(
            conn,
            request=request,
            current_user_id=current_user["id"],
            verify_password=verify_password,
        )
