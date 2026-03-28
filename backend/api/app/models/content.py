from pydantic import BaseModel
from typing import Optional


class ContentBase(BaseModel):
    title: str
    type: Optional[str] = None
    body: Optional[str] = None
    image_url: Optional[str] = None
    published: Optional[bool] = False


class ContentCreate(ContentBase):
    pass


class Content(ContentBase):
    id: int
    created_at: Optional[str] = None

    class Config:
        from_attributes = True
