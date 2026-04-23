import os
from pathlib import Path


JWT_SECRET = os.getenv("JWT_SECRET", "aura-super-secret-key-change-in-production")
JWT_ALGORITHM = "HS256"
JWT_EXPIRATION_HOURS = 24

DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "postgresql://aura_user:aura_password@localhost:5433/aura",
)

DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = int(os.getenv("DB_PORT", "5433"))
DB_NAME = os.getenv("DB_NAME", "aura")
DB_USER = os.getenv("DB_USER", "aura_user")
DB_PASSWORD = os.getenv("DB_PASSWORD", "aura_password")

AI_SERVICE_URL = os.getenv("AI_SERVICE_URL", "http://localhost:9002")
CHAT_ATTACHMENTS_DIR = Path(os.getenv("CHAT_ATTACHMENTS_DIR", "backend/chat_attachments"))
