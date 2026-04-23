from fastapi import APIRouter, Depends

from backend.core import auth_user_service as auth_user_service_core
from backend.core import entity_dictionary_refs as entity_dictionary_refs_core
from backend.core import security as security_core
from backend.core import users as users_core
from backend.db.pool import get_db
from backend.schemas.auth import LoginRequest, TokenResponse, UserCreate


router = APIRouter(prefix="/api/auth", tags=["Auth"])


@router.post("/login", response_model=TokenResponse)
async def login(request: LoginRequest, db=Depends(get_db)):
    async with db.acquire() as conn:
        return await auth_user_service_core.login_user_account(
            conn,
            request=request,
            user_select_sql=entity_dictionary_refs_core.user_select_sql,
            verify_password=security_core.verify_password,
            create_access_token=security_core.create_access_token,
            token_response_model=TokenResponse,
        )


@router.post("/register", response_model=TokenResponse)
async def register(user: UserCreate, db=Depends(get_db)):
    async with db.acquire() as conn:
        # Contract marker: sync_user_role_ref
        return await auth_user_service_core.register_user_account(
            conn,
            user=user,
            ensure_users_columns=users_core.ensure_users_columns,
            is_valid_email=users_core.is_valid_email,
            normalize_login=users_core.normalize_login,
            is_valid_login=users_core.is_valid_login,
            ensure_unique_login=users_core.ensure_unique_login,
            get_password_hash=security_core.get_password_hash,
            sync_user_role_ref=entity_dictionary_refs_core.sync_user_role_ref,
            user_select_sql=entity_dictionary_refs_core.user_select_sql,
            create_access_token=security_core.create_access_token,
            token_response_model=TokenResponse,
        )


@router.get("/me")
async def get_me(current_user: dict = Depends(security_core.get_current_user)):
    return auth_user_service_core.sanitize_current_user_payload(current_user)
