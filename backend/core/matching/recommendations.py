import json
import uuid
from datetime import datetime
from typing import Any, Awaitable, Callable, Dict, List, Optional

from backend.core.entity_dictionary_refs import sync_recommendation_feedback_product_ref
from backend.core.product_matching import build_product_matching_response
from backend.core.recommendations.generation import build_recommendation_response


async def match_products_for_user(
    conn,
    user_id: int,
    payload,
    *,
    load_skin_passport_context: Callable[[int], Awaitable[Optional[Dict[str, Any]]]],
    sanitize_skin_passport_answers: Callable[[Any], Dict[str, List[str]]],
    load_accepted_passport_insights: Callable[[Any, int], Awaitable[List[str]]],
    load_user_skin_journal: Callable[[Any, int], Awaitable[Dict[str, Any]]],
    load_product_function_signals: Callable[[Any, List[int]], Awaitable[Dict[int, List[Any]]]],
    product_match_sort_key: Callable[[Dict[str, Any]], Any],
    filter_product_match_results: Callable[[List[Dict[str, Any]], Any], List[Dict[str, Any]]],
):
    return await build_product_matching_response(
        conn,
        user_id,
        payload,
        load_skin_passport_context=load_skin_passport_context,
        sanitize_skin_passport_answers=sanitize_skin_passport_answers,
        load_accepted_passport_insights=load_accepted_passport_insights,
        load_user_skin_journal=load_user_skin_journal,
        load_product_function_signals=load_product_function_signals,
        product_match_sort_key=product_match_sort_key,
        filter_product_match_results=filter_product_match_results,
    )


async def generate_recommendation_for_user(
    conn,
    user_id: int,
    *,
    load_skin_passport_context: Callable[[int], Awaitable[Optional[Dict[str, Any]]]],
    sanitize_skin_passport_answers: Callable[[Any], Dict[str, List[str]]],
    load_accepted_passport_insights: Callable[[Any, int], Awaitable[List[str]]],
    load_user_skin_journal: Callable[[Any, int], Awaitable[Dict[str, Any]]],
    load_product_function_signals: Callable[[Any, List[int]], Awaitable[Dict[int, List[Any]]]],
    product_row_to_recommendation_input: Callable[[Any, Optional[List[Any]]], Any],
):
    return await build_recommendation_response(
        conn,
        user_id,
        load_skin_passport_context=load_skin_passport_context,
        sanitize_skin_passport_answers=sanitize_skin_passport_answers,
        load_accepted_passport_insights=load_accepted_passport_insights,
        load_user_skin_journal=load_user_skin_journal,
        load_product_function_signals=load_product_function_signals,
        product_row_to_recommendation_input=product_row_to_recommendation_input,
    )


async def track_recommendation_feedback_record(
    conn,
    *,
    user_id: int,
    recommendation_id: str,
    payload,
    validate_recommendation_feedback_action: Callable[[str], str],
) -> Dict[str, Any]:
    action = validate_recommendation_feedback_action(payload.action)
    row = await conn.fetchrow(
        """
        INSERT INTO recommendation_feedback (
            user_id, recommendation_id, product_id, rank, action, algorithm_version, metadata
        )
        VALUES ($1, $2, $3, $4, $5, $6, $7::jsonb)
        RETURNING id, created_at
        """,
        user_id,
        recommendation_id,
        payload.product_id,
        payload.rank,
        action,
        payload.algorithm_version,
        json.dumps(payload.metadata or {}, ensure_ascii=False),
    )
    await sync_recommendation_feedback_product_ref(conn, row)
    return {"success": True, "feedback_id": row["id"], "created_at": row["created_at"]}


async def save_recommendation_favorite_record(
    conn,
    *,
    user_id: int,
    recommendation: Dict[str, Any],
    coerce_extra_data: Callable[[Any], dict],
) -> Dict[str, Any]:
    favorite = dict(recommendation)
    favorite["favorite_id"] = str(uuid.uuid4())
    favorite["saved_at"] = datetime.utcnow().isoformat() + "Z"

    await conn.execute(
        """
        INSERT INTO user_profiles (user_id, extra_data, updated_at)
        VALUES ($1, '{}'::jsonb, NOW())
        ON CONFLICT (user_id) DO NOTHING
        """,
        user_id,
    )
    row = await conn.fetchrow("SELECT extra_data FROM user_profiles WHERE user_id=$1 FOR UPDATE", user_id)
    extra_data = coerce_extra_data(row["extra_data"]) if row else {}
    favorites = extra_data.get("recommendation_favorites")
    if not isinstance(favorites, list):
        favorites = []
    favorites.insert(0, favorite)
    extra_data["recommendation_favorites"] = favorites[:20]
    await conn.execute(
        """
        INSERT INTO user_profiles (user_id, extra_data, updated_at)
        VALUES ($1, $2::jsonb, NOW())
        ON CONFLICT (user_id) DO UPDATE SET extra_data=$2::jsonb, updated_at=NOW()
        """,
        user_id,
        json.dumps(extra_data, ensure_ascii=False),
    )
    return {"success": True, "favorite": favorite}


async def list_recommendation_favorites_for_user(
    conn,
    *,
    user_id: int,
    coerce_extra_data: Callable[[Any], dict],
) -> Dict[str, Any]:
    row = await conn.fetchrow("SELECT extra_data FROM user_profiles WHERE user_id=$1", user_id)
    extra_data = coerce_extra_data(row["extra_data"]) if row else {}
    favorites = extra_data.get("recommendation_favorites")
    if not isinstance(favorites, list):
        favorites = []
    return {"items": favorites[:20]}
