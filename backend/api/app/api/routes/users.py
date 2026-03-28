from fastapi import APIRouter, HTTPException
from app.models.user import UserCreate
from app.services import UserService

router = APIRouter()


@router.get("")
async def get_users():
    return await UserService.get_all()


@router.post("")
async def create_user(user: UserCreate):
    return await UserService.create(user)


@router.put("/{user_id}")
async def update_user(user_id: int, user: UserCreate):
    result = await UserService.update(user_id, user)
    if not result:
        raise HTTPException(status_code=404, detail="User not found")
    return result


@router.delete("/{user_id}")
async def delete_user(user_id: int):
    await UserService.delete(user_id)
    return {"success": True}
