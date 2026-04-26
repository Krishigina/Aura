from typing import List, Optional, Union

from pydantic import BaseModel


class ContentCreate(BaseModel):
    title: str
    category: Optional[str] = None
    tags: Optional[Union[List[str], str]] = None
    author_id: Optional[int] = None
    author_name: Optional[str] = None
    body: Optional[str] = None
    image_url: Optional[str] = None
    published: Optional[bool] = False
