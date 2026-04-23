from backend.core.config import DB_HOST, DB_NAME, DB_PASSWORD, DB_PORT, DB_USER


pool = None


def get_pool_settings() -> dict:
    return {
        "host": DB_HOST,
        "port": DB_PORT,
        "database": DB_NAME,
        "user": DB_USER,
        "password": DB_PASSWORD,
    }


def set_pool(value) -> None:
    global pool
    pool = value


async def get_db():
    return pool
