import os
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.routes import products, procedures, content, users, dictionaries
from app.database import init_db, get_db_pool


@asynccontextmanager
async def lifespan(app: FastAPI):
    init_db()
    yield
    pool = get_db_pool()
    if pool:
        pool.close()


app = FastAPI(
    title="Aura Admin API",
    description="Aura Cosmetics Admin Panel",
    version="1.0.0",
    lifespan=lifespan
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
    max_age=86400,
)

app.include_router(products.router, prefix="/api/products", tags=["Products"])
app.include_router(procedures.router, prefix="/api/procedures", tags=["Procedures"])
app.include_router(content.router, prefix="/api/content", tags=["Content"])
app.include_router(users.router, prefix="/api/users", tags=["Users"])
app.include_router(dictionaries.router, prefix="/api/dictionaries", tags=["Dictionaries"])


@app.get("/api/health")
async def health_check():
    return {"status": "ok"}
