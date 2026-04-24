from typing import List, Optional

from pydantic import BaseModel


class ProductParseRequest(BaseModel):
    url: str


class ProductParseResponse(BaseModel):
    name: Optional[str] = None
    brand: Optional[str] = None
    category: Optional[str] = None
    description: Optional[str] = None
    images: Optional[List[str]] = None
    volume: Optional[str] = None
