from fastapi import APIRouter, Depends

from backend.core import ingredient_knowledge_admin as ingredient_knowledge_admin_core
from backend.core import passport_updates as passport_updates_core
from backend.core import skin_journal as skin_journal_core
from backend.core import skin_passport as skin_passport_core
from backend.core.matching import admin as matching_admin_core
from backend.core.matching import helpers as matching_helpers_core
from backend.core.matching import recommendations as matching_recommendations_core
from backend.core.matching import route_service as matching_route_service_core
from backend.core.matching import router_support as matching_router_support_core
from backend.core.security import get_current_user
from backend.db.pool import get_db
from backend.schemas import matching as matching_schemas
from backend.schemas.matching import ProductMatchingRequest


router = APIRouter(tags=["Matching"])


@router.post("/api/matching/products")
async def match_products_for_current_user(
    payload: ProductMatchingRequest = ProductMatchingRequest(),
    current_user: dict = Depends(get_current_user),
    db=Depends(get_db),
):
    async with db.acquire() as conn:
        # Contract markers: load_skin_passport_context, load_user_skin_journal,
        # load_product_function_signals, product_match_sort_key
        runtime_dependencies = await matching_route_service_core.build_product_matching_runtime_dependencies(
            conn,
            matching_router_support_core=matching_router_support_core,
            passport_updates_core=passport_updates_core,
            skin_passport_core=skin_passport_core,
            matching_helpers_core=matching_helpers_core,
            skin_journal_core=skin_journal_core,
        )
        return await matching_recommendations_core.match_products_for_user(conn, current_user["id"], payload, **runtime_dependencies)


@router.post("/api/recommendations/generate")
async def generate_recommendation_for_current_user(current_user: dict = Depends(get_current_user), db=Depends(get_db)):
    async with db.acquire() as conn:
        runtime_dependencies = await matching_route_service_core.build_recommendation_runtime_dependencies(
            conn,
            matching_router_support_core=matching_router_support_core,
            passport_updates_core=passport_updates_core,
            skin_passport_core=skin_passport_core,
            matching_helpers_core=matching_helpers_core,
            skin_journal_core=skin_journal_core,
        )
        return await matching_recommendations_core.generate_recommendation_for_user(conn, current_user["id"], **runtime_dependencies)


@router.post("/api/recommendations/{recommendation_id}/feedback")
async def track_recommendation_feedback(
    recommendation_id: str,
    payload: matching_schemas.RecommendationFeedbackCreate,
    current_user: dict = Depends(get_current_user),
    db=Depends(get_db),
):
    async with db.acquire() as conn:
        return await matching_recommendations_core.track_recommendation_feedback_record(
            conn,
            user_id=current_user["id"],
            recommendation_id=recommendation_id,
            payload=payload,
            validate_recommendation_feedback_action=matching_helpers_core.validate_recommendation_feedback_action,
        )


@router.post("/api/recommendations/favorites")
async def save_recommendation_favorite(
    payload: matching_schemas.RecommendationFavoriteCreate,
    current_user: dict = Depends(get_current_user),
    db=Depends(get_db),
):
    async with db.acquire() as conn:
        async with conn.transaction():
            return await matching_recommendations_core.save_recommendation_favorite_record(
                conn,
                user_id=current_user["id"],
                recommendation=dict(payload.recommendation),
                coerce_extra_data=skin_journal_core.coerce_extra_data,
            )


@router.get("/api/recommendations/favorites")
async def list_recommendation_favorites(current_user: dict = Depends(get_current_user), db=Depends(get_db)):
    async with db.acquire() as conn:
        return await matching_recommendations_core.list_recommendation_favorites_for_user(
            conn,
            user_id=current_user["id"],
            coerce_extra_data=skin_journal_core.coerce_extra_data,
        )


@router.get("/api/matching/rules")
async def list_matching_rules(status: str | None = None, current_user: dict = Depends(get_current_user), db=Depends(get_db)):
    matching_helpers_core.require_matching_rule_admin(current_user)
    if status is not None:
        status = matching_helpers_core.validate_matching_rule_status(status)
    async with db.acquire() as conn:
        return await matching_admin_core.list_matching_rules_for_admin(conn, status)


@router.post("/api/admin/ingredient-knowledge/extract/{source_id}")
async def extract_ingredient_knowledge_for_source(source_id: int, current_user: dict = Depends(get_current_user), db=Depends(get_db)):
    matching_helpers_core.require_matching_rule_admin(current_user)
    async with db.acquire() as conn:
        async with conn.transaction():
            return await ingredient_knowledge_admin_core.extract_ingredient_facts_from_source(conn, source_id)


@router.get("/api/admin/ingredient-knowledge/ingredients")
async def list_ingredient_knowledge(current_user: dict = Depends(get_current_user), db=Depends(get_db)):
    matching_helpers_core.require_matching_rule_admin(current_user)
    async with db.acquire() as conn:
        return await matching_admin_core.list_ingredient_knowledge_for_admin(conn)


@router.get("/api/admin/ingredient-knowledge/facts")
async def list_ingredient_fact_review_queue(status: str = "auto_high_confidence", current_user: dict = Depends(get_current_user), db=Depends(get_db)):
    matching_helpers_core.require_matching_rule_admin(current_user)
    status = matching_helpers_core.validate_ingredient_fact_evidence_status(status)
    async with db.acquire() as conn:
        return await matching_admin_core.list_ingredient_fact_review_queue_for_admin(conn, status)


@router.get("/api/admin/rules-ingredients/overview")
async def get_rules_ingredients_overview(current_user: dict = Depends(get_current_user), db=Depends(get_db)):
    matching_helpers_core.require_matching_rule_admin(current_user)
    async with db.acquire() as conn:
        return await matching_admin_core.get_rules_ingredients_overview_for_admin(conn)


@router.patch("/api/admin/ingredient-knowledge/facts/{fact_id}")
async def update_ingredient_fact_review(
    fact_id: int,
    payload: matching_schemas.IngredientFactReviewUpdate,
    current_user: dict = Depends(get_current_user),
    db=Depends(get_db),
):
    matching_helpers_core.require_matching_rule_admin(current_user)
    evidence_status = matching_helpers_core.validate_ingredient_fact_evidence_status(payload.evidence_status)
    if payload.matching_effect is not None:
        matching_helpers_core.validate_matching_rule_effect(payload.matching_effect)
    async with db.acquire() as conn:
        async with conn.transaction():
            return await matching_admin_core.update_ingredient_fact_review_for_admin(
                conn,
                fact_id,
                payload,
                evidence_status,
                ingredient_knowledge_admin_core.rebuild_product_function_profile,
            )


@router.post("/api/matching/rules")
async def create_matching_rule(
    payload: matching_schemas.MatchingRuleCreate,
    current_user: dict = Depends(get_current_user),
    db=Depends(get_db),
):
    matching_helpers_core.require_matching_rule_admin(current_user)
    status = matching_helpers_core.validate_matching_rule_status(payload.status)
    effect = matching_helpers_core.validate_matching_rule_effect(payload.effect)
    async with db.acquire() as conn:
        await matching_helpers_core.ensure_matching_rule_source_exists(conn, payload.source_id)
        return await matching_admin_core.create_matching_rule_for_admin(conn, payload, status, effect, current_user["id"])


@router.patch("/api/matching/rules/{rule_id}")
async def update_matching_rule(
    rule_id: int,
    payload: matching_schemas.MatchingRuleUpdate,
    current_user: dict = Depends(get_current_user),
    db=Depends(get_db),
):
    matching_helpers_core.require_matching_rule_admin(current_user)
    updates = matching_router_support_core.build_matching_rule_updates(
        payload,
        allowed_fields=matching_helpers_core.MATCHING_RULE_UPDATE_FIELDS,
        validate_matching_rule_status=matching_helpers_core.validate_matching_rule_status,
        validate_matching_rule_effect=matching_helpers_core.validate_matching_rule_effect,
    )
    if "effect" in updates:
        updates["effect"] = matching_helpers_core.validate_matching_rule_effect(updates["effect"])
    async with db.acquire() as conn:
        if "source_id" in updates:
            await matching_helpers_core.ensure_matching_rule_source_exists(conn, updates.get("source_id"))
        return await matching_admin_core.update_matching_rule_for_admin(conn, rule_id, updates, current_user["id"])
