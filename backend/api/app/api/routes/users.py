from typing import Optional

from fastapi import APIRouter, HTTPException
from app.models.user import UserCreate
from app.services import UserService

router = APIRouter()


@router.get("")
def get_users(role: Optional[str] = None):
    return UserService.get_all(role)


@router.post("")
def create_user(user: UserCreate):
    try:
        return UserService.create(user)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.put("/{user_id}")
def update_user(user_id: int, user: UserCreate):
    try:
        result = UserService.update(user_id, user)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    if not result:
        raise HTTPException(status_code=404, detail="User not found")
    return result


@router.delete("/{user_id}")
def delete_user(user_id: int):
    UserService.delete(user_id)
    return {"success": True}
