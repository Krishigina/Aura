from fastapi import APIRouter, Depends

from backend.core.analytics_reports import (
    build_dashboard_analytics_payload,
    build_reports_summary_payload,
)
from backend.core.products import product_select_sql
from backend.core.security import get_current_user
from backend.db.pool import get_db


router = APIRouter(tags=["Analytics"])


@router.get("/api/analytics/dashboard")
async def get_dashboard_analytics(current_user: dict = Depends(get_current_user), db=Depends(get_db)):
    async with db.acquire() as conn:
        return await build_dashboard_analytics_payload(conn)


@router.get("/api/reports/summary")
async def get_reports_summary(current_user: dict = Depends(get_current_user), db=Depends(get_db)):
    async with db.acquire() as conn:
        return await build_reports_summary_payload(conn, product_select_sql=product_select_sql)
