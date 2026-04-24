from typing import List, Optional, Union

from pydantic import BaseModel


class ProductCreate(BaseModel):
    name: str
    what_is_it: Optional[str] = None
    brand: Optional[str] = None
    product_type: Optional[str] = None
    for_whom: Optional[str] = None
    purpose: Optional[Union[List[str], str]] = None
    skin_type: Optional[Union[List[str], str]] = None
    application_time: Optional[str] = None
    area: Optional[str] = None
    active_ingredient: Optional[str] = None
    volume: Optional[str] = None
    segment: Optional[str] = None
    composition: Optional[str] = None
    application_info: Optional[str] = None
    country: Optional[str] = None
    category: Optional[str] = None
    description: Optional[str] = None
    images: Optional[List[str]] = None
    has_video: Optional[bool] = False
