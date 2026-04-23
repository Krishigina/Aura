from typing import Optional

from fastapi import APIRouter, Depends

from backend.core import auth_user_service as auth_user_service_core
from backend.core import entity_dictionary_refs as entity_dictionary_refs_core
from backend.core import security as security_core
from backend.core import users as users_core
from backend.db.pool import get_db
from backend.schemas.auth import UserCreate


router = APIRouter(prefix="/api/users", tags=["Users"])


@router.get("")
async def get_users(role: Optional[str] = None, db=Depends(get_db)):
    async with db.acquire() as conn:
        return await auth_user_service_core.list_users_for_admin(
            conn,
            role=role,
            ensure_users_columns=users_core.ensure_users_columns,
            user_select_sql=entity_dictionary_refs_core.user_select_sql,
        )


@router.post("")
async def create_user(user: UserCreate, db=Depends(get_db)):
    async with db.acquire() as conn:
        # Contract marker: sync_user_role_ref
        return await auth_user_service_core.create_user_record(
            conn,
            user=user,
            normalize_login=users_core.normalize_login,
            is_valid_email=users_core.is_valid_email,
            is_valid_login=users_core.is_valid_login,
            ensure_users_columns=users_core.ensure_users_columns,
            ensure_unique_login=users_core.ensure_unique_login,
            get_password_hash=security_core.get_password_hash,
            sync_user_role_ref=entity_dictionary_refs_core.sync_user_role_ref,
            user_select_sql=entity_dictionary_refs_core.user_select_sql,
        )


@router.put("/{user_id}")
async def update_user(user_id: int, user: UserCreate, db=Depends(get_db)):
    async with db.acquire() as conn:
        # Contract marker: sync_user_role_ref
        return await auth_user_service_core.update_user_record(
            conn,
            user_id=user_id,
            user=user,
            normalize_login=users_core.normalize_login,
            is_valid_email=users_core.is_valid_email,
            is_valid_login=users_core.is_valid_login,
            ensure_users_columns=users_core.ensure_users_columns,
            ensure_unique_login=users_core.ensure_unique_login,
            sync_user_role_ref=entity_dictionary_refs_core.sync_user_role_ref,
            user_select_sql=entity_dictionary_refs_core.user_select_sql,
        )


@router.delete("/{user_id}")
async def delete_user(user_id: int, db=Depends(get_db)):
    async with db.acquire() as conn:
        return await auth_user_service_core.delete_user_record(conn, user_id=user_id)
