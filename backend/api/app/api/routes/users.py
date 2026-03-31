from fastapi import APIRouter, HTTPException
from app.models.user import UserCreate
from app.services import UserService

router = APIRouter()


@router.get("")
def get_users():
    return UserService.get_all()


@router.post("")
def create_user(user: UserCreate):
    return UserService.create(user)


@router.put("/{user_id}")
def update_user(user_id: int, user: UserCreate):
    result = UserService.update(user_id, user)
    if not result:
        raise HTTPException(status_code=404, detail="User not found")
    return result


@router.delete("/{user_id}")
def delete_user(user_id: int):
    UserService.delete(user_id)
    return {"success": True}
