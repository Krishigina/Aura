from typing import Optional

from pydantic import BaseModel


class UserCreate(BaseModel):
    name: str
    email: str
    password: Optional[str] = None
    role: Optional[str] = "user"
    nickname: Optional[str] = None
    avatar: Optional[str] = None


class LoginRequest(BaseModel):
    email: str
    password: str


class ProfileAccountUpdateRequest(BaseModel):
    name: str
    nickname: Optional[str] = None


class ProfilePasswordUpdateRequest(BaseModel):
    current_password: str
    new_password: str


class ProfileDeleteRequest(BaseModel):
    current_password: str


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: dict
