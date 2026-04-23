from typing import Any, Dict

from fastapi import APIRouter, Body, Depends

from backend.core.profile_preferences import (
    load_user_profile_notifications,
    load_user_profile_routine,
    save_user_profile_notifications,
    save_user_profile_routine,
)
from backend.core.security import get_current_user
from backend.db.pool import get_db


router = APIRouter(tags=["Profile"])


@router.get("/api/profile/routine")
async def get_profile_routine(current_user: dict = Depends(get_current_user), db=Depends(get_db)):
    async with db.acquire() as conn:
        return await load_user_profile_routine(conn, current_user["id"])


@router.put("/api/profile/routine")
async def save_profile_routine(
    payload: Dict[str, Any] = Body(default_factory=dict),
    current_user: dict = Depends(get_current_user),
    db=Depends(get_db),
):
    async with db.acquire() as conn:
        async with conn.transaction():
            return await save_user_profile_routine(conn, current_user["id"], payload)


@router.get("/api/profile/notifications")
async def get_profile_notifications(current_user: dict = Depends(get_current_user), db=Depends(get_db)):
    async with db.acquire() as conn:
        return await load_user_profile_notifications(conn, current_user["id"])


@router.put("/api/profile/notifications")
async def save_profile_notifications(
    payload: Dict[str, Any] = Body(default_factory=dict),
    current_user: dict = Depends(get_current_user),
    db=Depends(get_db),
):
    async with db.acquire() as conn:
        async with conn.transaction():
            return await save_user_profile_notifications(conn, current_user["id"], payload)
