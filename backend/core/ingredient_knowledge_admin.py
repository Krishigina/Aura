import json
import re
from typing import Dict

from fastapi import HTTPException

from backend.ingredient_knowledge import extract_ingredient_facts, normalize_key, resolve_seed_alias


def row_value(row, key: str, default=None):
    try:
        return row[key]
    except (KeyError, IndexError):
        return default


def compute_product_function_profile_entries(evidence_rows):
    grouped = {}
    for row in evidence_rows:
        matching_effect = row_value(row, "matching_effect", "")
        effect_key = row_value(row, "effect_key", "")
        function_key = (
            "irritation_risk"
            if matching_effect in {"warning", "penalty", "block"} or effect_key in {"warning", "risk", "contraindication"}
            else effect_key
        )
        group = grouped.setdefault(
            function_key,
            {"weighted_score": 0.0, "statuses": set(), "source_ids": set(), "evidence_count": 0},
        )
        status = row["evidence_status"]
        multiplier = 1.0 if status == "confirmed" else 0.65
        raw_weight = row["matching_weight_delta"] or 0
        if function_key == "irritation_risk":
            raw_weight = abs(raw_weight) or 6
        group["weighted_score"] += max(raw_weight, 0) * multiplier * (row["confidence"] or 0)
        group["statuses"].add(status)
        if row["source_id"] is not None:
            group["source_ids"].add(row["source_id"])
        group["evidence_count"] += 1

    entries = []
    for function_key, group in grouped.items():
        statuses = group["statuses"]
        if statuses == {"confirmed"}:
            evidence_status = "confirmed"
        elif "confirmed" in statuses and "auto_high_confidence" in statuses:
            evidence_status = "mixed"
        else:
            evidence_status = "auto_only"
        entries.append(
            {
                "function_key": function_key,
                "score": round(min(group["weighted_score"] / 20, 1.0), 4),
                "evidence_status": evidence_status,
                "evidence_count": group["evidence_count"],
                "source_ids": sorted(group["source_ids"]),
            }
        )
    return entries


async def rebuild_product_function_profile(conn, product_id: int):
    evidence_rows = await conn.fetch(
        """
        SELECT
            ingredient_evidence.effect_key,
            ingredient_evidence.matching_weight_delta,
            ingredient_evidence.confidence,
            ingredient_evidence.evidence_status,
            ingredient_evidence.matching_effect,
            ingredient_evidence.source_id
        FROM product_ingredients
        JOIN ingredient_evidence ON ingredient_evidence.ingredient_id = product_ingredients.ingredient_id
        WHERE product_ingredients.product_id=$1
          AND ingredient_evidence.matching_effect IN ('boost', 'warning', 'penalty', 'block')
          AND ingredient_evidence.effect_key IS NOT NULL
          AND ingredient_evidence.effect_key <> ''
          AND ingredient_evidence.evidence_status IN ('confirmed', 'auto_high_confidence')
        """,
        product_id,
    )

    await conn.execute("DELETE FROM product_function_profiles WHERE product_id=$1", product_id)

    for entry in compute_product_function_profile_entries(evidence_rows):
        await conn.execute(
            """
            INSERT INTO product_function_profiles (product_id, function_key, score, evidence_status, evidence_count, source_ids, updated_at)
            VALUES ($1, $2, $3, $4, $5, $6::jsonb, NOW())
            ON CONFLICT (product_id, function_key)
            DO UPDATE SET
                score=EXCLUDED.score,
                evidence_status=EXCLUDED.evidence_status,
                evidence_count=EXCLUDED.evidence_count,
                source_ids=EXCLUDED.source_ids,
                updated_at=NOW()
            """,
            product_id,
            entry["function_key"],
            entry["score"],
            entry["evidence_status"],
            entry["evidence_count"],
            json.dumps(entry["source_ids"]),
        )


async def relink_product_ingredients_to_alias(conn, ingredient_id: int, aliases: set[str]) -> set[int]:
    normalized_aliases = sorted({normalize_key(alias) for alias in aliases if normalize_key(alias)})
    if not normalized_aliases:
        return set()
    rows = await conn.fetch(
        """
        UPDATE product_ingredients
        SET ingredient_id=$1
        WHERE ingredient_id<>$1
          AND LOWER(REGEXP_REPLACE(TRIM(raw_name), '\\s+', ' ', 'g')) = ANY($2::text[])
        RETURNING product_id
        """,
        ingredient_id,
        normalized_aliases,
    )
    return {row["product_id"] for row in rows}


async def extract_ingredient_facts_from_source(conn, source_id: int) -> Dict[str, int]:
    source = await conn.fetchrow(
        "SELECT id, title, content FROM knowledge_sources WHERE id=$1 AND enabled=true",
        source_id,
    )
    if not source:
        raise HTTPException(status_code=404, detail="Источник знаний не найден")

    facts = extract_ingredient_facts(source["content"] or "", source_id=source["id"], source_title=source["title"] or "")
    inserted = 0
    draft = 0
    auto_high_confidence = 0
    affected_ingredient_ids = set()
    affected_product_ids = set()

    for fact in facts:
        ingredient = await conn.fetchrow(
            """
            INSERT INTO ingredients (canonical_name, normalized_key, inci_name, verification_status, evidence_status)
            VALUES ($1, $2, $1, 'auto_created', $3)
            ON CONFLICT (normalized_key)
            DO UPDATE SET evidence_status=CASE
                WHEN ingredients.evidence_status = 'confirmed' THEN ingredients.evidence_status
                WHEN ingredients.evidence_status IN ('confirmed', 'auto_high_confidence') AND EXCLUDED.evidence_status = 'draft'
                    THEN ingredients.evidence_status
                ELSE EXCLUDED.evidence_status
            END
            RETURNING id
            """,
            fact.ingredient_key,
            normalize_key(fact.ingredient_key),
            fact.evidence_status,
        )
        affected_ingredient_ids.add(ingredient["id"])
        aliases = {fact.ingredient_key}
        if fact.matched_alias:
            aliases.add(fact.matched_alias)
        seed_ingredient = resolve_seed_alias(fact.ingredient_key)
        if seed_ingredient:
            aliases.update(seed_ingredient.aliases)
        for alias in aliases:
            await conn.execute(
                """
                INSERT INTO ingredient_aliases (ingredient_id, alias, normalized_key, language, source, confidence)
                VALUES ($1, $2, $3, $4, 'extracted', $5)
                ON CONFLICT (normalized_key) DO NOTHING
                """,
                ingredient["id"],
                alias,
                normalize_key(alias),
                "ru" if re.search(r"[а-яА-Я]", alias) else "en",
                fact.confidence,
            )
        affected_product_ids.update(await relink_product_ingredients_to_alias(conn, ingredient["id"], aliases))
        await conn.execute(
            """
            INSERT INTO ingredient_evidence (
                ingredient_id, ingredient_key, fact_type, effect_key, condition_type, condition_value,
                matching_effect, matching_weight_delta, confidence, evidence_status, evidence_quote, source_id
            ) VALUES ($1,$2,'benefit',$3,$4,$5,$6,$7,$8,$9,$10,$11)
            ON CONFLICT (ingredient_id, source_id, effect_key, condition_type, condition_value, evidence_quote) DO NOTHING
            """,
            ingredient["id"],
            fact.ingredient_key,
            fact.effect_key or "",
            fact.condition_type or "",
            fact.condition_value or "",
            fact.matching_effect,
            fact.matching_weight_delta,
            fact.confidence,
            fact.evidence_status,
            fact.evidence_quote or "",
            fact.source_id,
        )
        inserted += 1
        if fact.evidence_status == "auto_high_confidence":
            auto_high_confidence += 1
        else:
            draft += 1

    for ingredient_id in affected_ingredient_ids:
        product_rows = await conn.fetch(
            "SELECT DISTINCT product_id FROM product_ingredients WHERE ingredient_id=$1",
            ingredient_id,
        )
        affected_product_ids.update(row["product_id"] for row in product_rows)
    for product_id in affected_product_ids:
        await rebuild_product_function_profile(conn, product_id)

    return {"inserted": inserted, "auto_high_confidence": auto_high_confidence, "draft": draft}
