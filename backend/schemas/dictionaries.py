from typing import Optional

from pydantic import BaseModel


class DictionaryValue(BaseModel):
    value: str


class DictionaryUpdate(BaseModel):
    oldValue: str
    newValue: str


class BrandUpdate(BaseModel):
    value: str
    description: Optional[str] = None
    country: Optional[str] = None
    country_origin: Optional[str] = None
    manufacturer: Optional[str] = None
