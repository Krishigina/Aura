from fastapi import APIRouter, HTTPException
from app.models.content import ContentCreate
from app.services import ContentService

router = APIRouter()


@router.get("")
def get_content():
    return ContentService.get_all()


@router.post("")
def create_content(content: ContentCreate):
    return ContentService.create(content)


@router.put("/{content_id}")
def update_content(content_id: int, content: ContentCreate):
    result = ContentService.update(content_id, content)
    if not result:
        raise HTTPException(status_code=404, detail="Content not found")
    return result


@router.delete("/{content_id}")
def delete_content(content_id: int):
    ContentService.delete(content_id)
    return {"success": True}
