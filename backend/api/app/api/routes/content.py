from fastapi import APIRouter, HTTPException
from app.models.content import ContentCreate
from app.services import ContentService

router = APIRouter()


@router.get("")
async def get_content():
    return await ContentService.get_all()


@router.post("")
async def create_content(content: ContentCreate):
    return await ContentService.create(content)


@router.put("/{content_id}")
async def update_content(content_id: int, content: ContentCreate):
    result = await ContentService.update(content_id, content)
    if not result:
        raise HTTPException(status_code=404, detail="Content not found")
    return result


@router.delete("/{content_id}")
async def delete_content(content_id: int):
    await ContentService.delete(content_id)
    return {"success": True}
