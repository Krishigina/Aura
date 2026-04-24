from typing import Dict, List, Optional

from fastapi import APIRouter, Depends, Query

from backend.core.entity_dictionary_refs import content_select_sql
from backend.core.home_diagnostics import build_diagnostics_summary
from backend.core.home_feed import build_home_feed_insights, build_home_feed_ritual_items
from backend.core.home_status import (
    build_fallback_home_status as shared_build_fallback_home_status,
    fetch_open_meteo_home_status as shared_fetch_open_meteo_home_status,
)
from backend.core.products import product_select_sql
from backend.core.security import get_current_user
from backend.core.skin_passport import sanitize_skin_passport_answers
from backend.db.pool import get_db


router = APIRouter(tags=["Home"])


def extract_skin_passport_answers(profile_row) -> Dict[str, List[str]]:
    answers: Dict[str, List[str]] = {}
    if profile_row and isinstance(profile_row.get("extra_data"), dict):
        skin_passport = profile_row["extra_data"].get("skin_passport")
        if isinstance(skin_passport, dict):
            answers = sanitize_skin_passport_answers(skin_passport.get("answers"))
    return answers


@router.get("/api/home/feed")
async def get_home_feed(current_user: dict = Depends(get_current_user), db=Depends(get_db)):
    async with db.acquire() as conn:
        ritual_rows = await conn.fetch(
            f"""
            SELECT id, name, application_time, category
            FROM ({product_select_sql('p')}) AS hydrated_products
            ORDER BY created_at DESC, id DESC
            LIMIT 4
            """
        )
        insights_rows = await conn.fetch(
            f"""
            SELECT id, title, category, created_at
            FROM ({content_select_sql('c')}) AS hydrated_content
            WHERE published = true
            ORDER BY created_at DESC, id DESC
            LIMIT 3
            """
        )

    return {
        "ritual_items": build_home_feed_ritual_items(ritual_rows),
        "insights": build_home_feed_insights(insights_rows),
    }


@router.get("/api/diagnostics/summary")
async def get_diagnostics_summary(current_user: dict = Depends(get_current_user), db=Depends(get_db)):
    user_id = current_user.get("id")
    async with db.acquire() as conn:
        profile_row = await conn.fetchrow("SELECT extra_data FROM user_profiles WHERE user_id=$1", user_id)
    return build_diagnostics_summary(extract_skin_passport_answers(profile_row))


@router.get("/api/home/status")
async def get_home_status(
    latitude: Optional[float] = Query(None, ge=-90, le=90),
    longitude: Optional[float] = Query(None, ge=-180, le=180),
    current_user: dict = Depends(get_current_user),
    db=Depends(get_db),
):
    user_id = current_user.get("id")
    async with db.acquire() as conn:
        profile_row = await conn.fetchrow("SELECT extra_data FROM user_profiles WHERE user_id=$1", user_id)

    answers = extract_skin_passport_answers(profile_row)
    fallback = shared_build_fallback_home_status(sum(len(value) for value in answers.values()))
    if latitude is not None and longitude is not None:
        live_status = await shared_fetch_open_meteo_home_status(
            latitude=latitude,
            longitude=longitude,
            fallback_air_quality=fallback["top_widget"]["air_quality"],
        )
        if live_status is not None:
            return live_status
    return fallback
