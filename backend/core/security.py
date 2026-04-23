from datetime import datetime, timedelta
from typing import Optional

from fastapi import Depends, HTTPException
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jose import JWTError, jwt
from passlib.context import CryptContext

from backend.core.config import JWT_ALGORITHM, JWT_EXPIRATION_HOURS, JWT_SECRET
from backend.core.entity_dictionary_refs import user_select_sql
from backend.db.pool import get_db


pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
security = HTTPBearer()


def create_access_token(data: dict, expires_delta: Optional[timedelta] = None):
    to_encode = data.copy()
    subject = to_encode.get("sub")
    if subject is not None and not isinstance(subject, str):
        to_encode["sub"] = str(subject)
    if expires_delta:
        expire = datetime.utcnow() + expires_delta
    else:
        expire = datetime.utcnow() + timedelta(hours=JWT_EXPIRATION_HOURS)
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, JWT_SECRET, algorithm=JWT_ALGORITHM)


def verify_password(plain_password: str, hashed_password: str) -> bool:
    try:
        return pwd_context.verify(plain_password, hashed_password)
    except Exception:
        return False


def get_password_hash(password: str) -> str:
    return pwd_context.hash(password)


async def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(security)):
    try:
        token = credentials.credentials
        payload = jwt.decode(token, JWT_SECRET, algorithms=[JWT_ALGORITHM])
        raw_user_id = payload.get("sub")
        if raw_user_id is None:
            raise HTTPException(status_code=401, detail="Invalid token")
        user_id = int(raw_user_id)
        if user_id <= 0:
            raise HTTPException(status_code=401, detail="Invalid token")
    except (TypeError, ValueError):
        raise HTTPException(status_code=401, detail="Invalid token")
    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid token")

    db = await get_db()
    async with db.acquire() as conn:
        row = await conn.fetchrow(f"SELECT * FROM ({user_select_sql('u')}) AS hydrated_users WHERE id=$1", user_id)
        if not row:
            raise HTTPException(status_code=401, detail="User not found")
        return dict(row)


async def get_current_user_optional(credentials: Optional[HTTPAuthorizationCredentials] = Depends(None)):
    if not credentials:
        return None
    try:
        return await get_current_user(credentials)
    except HTTPException:
        return None
