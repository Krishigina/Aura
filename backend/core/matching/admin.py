from typing import Any, Callable, Dict, Optional

from fastapi import HTTPException


async def list_matching_rules_for_admin(conn, status: Optional[str]) -> Dict[str, Any]:
    where = "WHERE ($1::text IS NULL OR mr.status=$1)"
    rows = await conn.fetch(
        f"""
        SELECT mr.*, ks.title AS source_title
        FROM matching_rules mr
        LEFT JOIN knowledge_sources ks ON ks.id = mr.source_id
        {where}
        ORDER BY mr.created_at DESC, mr.id DESC
        """,
        status,
    )
    return {"items": [dict(row) for row in rows]}


async def list_ingredient_knowledge_for_admin(conn) -> Dict[str, Any]:
    rows = await conn.fetch(
        """
        SELECT i.id, i.canonical_name, i.inci_name, i.category AS ingredient_class, i.verification_status, i.evidence_status,
               COUNT(DISTINCT ia.id)::int AS alias_count,
               COUNT(DISTINCT ie.id)::int AS fact_count,
               COUNT(DISTINCT pi.product_id)::int AS product_count
        FROM ingredients i
        LEFT JOIN ingredient_aliases ia ON ia.ingredient_id=i.id
        LEFT JOIN ingredient_evidence ie ON ie.ingredient_id=i.id
        LEFT JOIN product_ingredients pi ON pi.ingredient_id=i.id
        GROUP BY i.id
        ORDER BY i.canonical_name
        """
    )
    return {"items": [dict(row) for row in rows]}


async def list_ingredient_fact_review_queue_for_admin(conn, status: str) -> Dict[str, Any]:
    rows = await conn.fetch(
        """
        SELECT ie.id, ie.ingredient_id, i.canonical_name, ie.effect_key, ie.condition_type, ie.condition_value,
               ie.matching_effect, ie.matching_weight_delta, ie.confidence, ie.evidence_status,
               ie.evidence_quote, ie.source_id, ks.title AS source_title
        FROM ingredient_evidence ie
        LEFT JOIN ingredients i ON i.id=ie.ingredient_id
        LEFT JOIN knowledge_sources ks ON ks.id=ie.source_id
        WHERE ie.evidence_status=$1
        ORDER BY ie.confidence DESC, ie.id DESC
        LIMIT 500
        """,
        status,
    )
    return {"items": [dict(row) for row in rows]}


async def get_rules_ingredients_overview_for_admin(conn) -> Dict[str, Any]:
    manual_rules_count = await conn.fetchval("SELECT COUNT(*) FROM matching_rules")
    active_ingredient_facts_count = await conn.fetchval(
        """
        SELECT COUNT(*)
        FROM ingredient_evidence ie
        WHERE ie.evidence_status IN ('confirmed', 'auto_high_confidence')
          AND ie.matching_effect IN ('boost', 'warning', 'penalty', 'block')
          AND ie.effect_key IS NOT NULL
          AND ie.effect_key <> ''
        """
    )
    draft_ingredient_facts_count = await conn.fetchval(
        """
        SELECT COUNT(*)
        FROM ingredient_evidence ie
        WHERE ie.evidence_status='draft'
        """
    )
    ingredients_count = await conn.fetchval("SELECT COUNT(*) FROM ingredients")
    knowledge_sources_count = await conn.fetchval("SELECT COUNT(*) FROM knowledge_sources")
    manual_rows = await conn.fetch(
        """
        SELECT 'manual_rule' AS source_type, mr.id, mr.rule_type, mr.target_type, mr.target_key,
               mr.condition_type, mr.condition_value, mr.effect, mr.weight_delta, mr.status,
               NULL::varchar AS evidence_status, NULL::double precision AS confidence,
               NULL::text AS evidence_quote, mr.source_id, ks.title AS source_title
        FROM matching_rules mr
        LEFT JOIN knowledge_sources ks ON ks.id=mr.source_id
        WHERE mr.status='confirmed'
        ORDER BY mr.id DESC
        LIMIT 200
        """
    )
    ingredient_rows = await conn.fetch(
        """
        SELECT 'ingredient_fact' AS source_type, ie.id, 'ingredient' AS rule_type,
               'ingredient' AS target_type, i.canonical_name AS target_key,
               ie.condition_type, ie.condition_value, ie.matching_effect AS effect,
               ie.matching_weight_delta AS weight_delta, ie.evidence_status AS status,
               ie.evidence_status, ie.confidence, ie.evidence_quote, ie.source_id,
               ks.title AS source_title
        FROM ingredient_evidence ie
        LEFT JOIN ingredients i ON i.id=ie.ingredient_id
        LEFT JOIN knowledge_sources ks ON ks.id=ie.source_id
        WHERE ie.evidence_status IN ('confirmed', 'auto_high_confidence')
          AND ie.matching_effect IN ('boost', 'warning', 'penalty', 'block')
          AND ie.effect_key IS NOT NULL
          AND ie.effect_key <> ''
        ORDER BY ie.confidence DESC, ie.id DESC
        LIMIT 300
        """
    )
    return {
        "summary": {
            "manual_rules_count": manual_rules_count or 0,
            "active_ingredient_facts_count": active_ingredient_facts_count or 0,
            "draft_ingredient_facts_count": draft_ingredient_facts_count or 0,
            "ingredients_count": ingredients_count or 0,
            "knowledge_sources_count": knowledge_sources_count or 0,
        },
        "active_rules": [dict(row) for row in manual_rows] + [dict(row) for row in ingredient_rows],
    }


async def update_ingredient_fact_review_for_admin(
    conn,
    fact_id: int,
    payload,
    evidence_status: str,
    rebuild_product_function_profile: Callable[[Any, int], Any],
) -> Dict[str, Any]:
    row = await conn.fetchrow(
        """
        UPDATE ingredient_evidence
        SET evidence_status=$1,
            effect_key=COALESCE($2, effect_key),
            condition_type=COALESCE($3, condition_type),
            condition_value=COALESCE($4, condition_value),
            matching_effect=COALESCE($5, matching_effect),
            matching_weight_delta=COALESCE($6, matching_weight_delta)
        WHERE id=$7
        RETURNING *
        """,
        evidence_status,
        payload.effect_key,
        payload.condition_type,
        payload.condition_value,
        payload.matching_effect,
        payload.matching_weight_delta,
        fact_id,
    )
    if not row:
        raise HTTPException(status_code=404, detail="Факт ингредиента не найден")
    product_rows = await conn.fetch(
        "SELECT DISTINCT product_id FROM product_ingredients WHERE ingredient_id=$1",
        row["ingredient_id"],
    )
    for product_row in product_rows:
        await rebuild_product_function_profile(conn, product_row["product_id"])
    return dict(row)


async def create_matching_rule_for_admin(conn, payload, status: str, effect: str, current_user_id: int) -> Dict[str, Any]:
    row = await conn.fetchrow(
        """
        INSERT INTO matching_rules (
            rule_type, target_type, target_id, target_key, condition_type, condition_value,
            effect, weight_delta, severity, source_id, evidence_quote, confidence, status,
            reviewed_by, reviewed_at
        )
        VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,
                CASE WHEN $13='confirmed' THEN $14 ELSE NULL END,
                CASE WHEN $13='confirmed' THEN NOW() ELSE NULL END)
        RETURNING *
        """,
        payload.rule_type,
        payload.target_type,
        payload.target_id,
        payload.target_key,
        payload.condition_type,
        payload.condition_value,
        effect,
        payload.weight_delta,
        payload.severity,
        payload.source_id,
        payload.evidence_quote,
        payload.confidence,
        status,
        current_user_id,
    )
    return dict(row)


async def update_matching_rule_for_admin(
    conn,
    rule_id: int,
    updates: Dict[str, Any],
    current_user_id: int,
) -> Dict[str, Any]:
    set_parts = []
    values = []
    for index, (key, value) in enumerate(updates.items(), start=1):
        set_parts.append(f"{key}=${index}")
        values.append(value)
    if updates.get("status") == "confirmed":
        set_parts.append(f"reviewed_by=${len(values) + 1}")
        values.append(current_user_id)
        set_parts.append("reviewed_at=NOW()")
    set_parts.append("updated_at=NOW()")
    values.append(rule_id)

    row = await conn.fetchrow(
        f"UPDATE matching_rules SET {', '.join(set_parts)} WHERE id=${len(values)} RETURNING *",
        *values,
    )
    if not row:
        raise HTTPException(status_code=404, detail="Правило не найдено")
    return dict(row)
