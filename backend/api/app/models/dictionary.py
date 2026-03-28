from pydantic import BaseModel


class DictionaryValue(BaseModel):
    value: str


class DictionaryUpdate(BaseModel):
    oldValue: str
    newValue: str
