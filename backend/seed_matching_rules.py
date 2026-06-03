import asyncio
import os
from typing import Dict, Iterable, List

import asyncpg


REQUIRED_SOURCE_TITLES = [
    "Акне вульгарные.docx",
    "Атопический дерматит.docx",
    "КР_Ксероз кожи_общественное обсуждение.pdf",
]


def resolve_source_ids(required_titles: Iterable[str], sources_by_title: Dict[str, int]) -> Dict[str, int]:
    missing = [title for title in required_titles if title not in sources_by_title]
    if missing:
        raise ValueError(f"missing knowledge sources: {', '.join(missing)}")
    return {title: sources_by_title[title] for title in required_titles}


def build_seed_rules(source_ids: Dict[str, int]) -> List[Dict[str, object]]:
    acne = source_ids["Акне вульгарные.docx"]
    atopic = source_ids["Атопический дерматит.docx"]
    xerosis = source_ids["КР_Ксероз кожи_общественное обсуждение.pdf"]
    return [
        {
            "rule_type": "ingredient_profile",
            "target_type": "ingredient",
            "target_key": "benzoyl peroxide",
            "condition_type": "concern",
            "condition_value": "acne",
            "effect": "boost",
            "weight_delta": 18,
            "severity": "info",
            "source_id": acne,
            "evidence_quote": "Не рекомендуется применять системные и топические антибактериальные препараты в качестве монотерапии; необходимо избегать комбинаций без дополнительного назначения бензоила пероксида.",
            "confidence": 0.9,
            "status": "confirmed",
        },
        {
            "rule_type": "ingredient_profile",
            "target_type": "ingredient",
            "target_key": "adapalene",
            "condition_type": "concern",
            "condition_value": "acne",
            "effect": "boost",
            "weight_delta": 14,
            "severity": "info",
            "source_id": acne,
            "evidence_quote": "Для лечения акне у взрослых женщин в качестве базисной терапии рекомендованы ретиноиды при комедональных и воспалительных акне легкой и средней степени тяжести.",
            "confidence": 0.85,
            "status": "confirmed",
        },
        {
            "rule_type": "profile_support",
            "target_type": "ingredient",
            "target_key": "emollient",
            "condition_type": "concern",
            "condition_value": "dryness",
            "effect": "boost",
            "weight_delta": 18,
            "severity": "info",
            "source_id": xerosis,
            "evidence_quote": "Рекомендуется всем пациентам с ксерозом независимо от степени тяжести: увлажняющие и смягчающие средства (эмоленты) наружно.",
            "confidence": 0.9,
            "status": "confirmed",
        },
        {
            "rule_type": "ingredient_profile",
            "target_type": "ingredient",
            "target_key": "urea",
            "condition_type": "concern",
            "condition_value": "dryness",
            "effect": "boost",
            "weight_delta": 12,
            "severity": "info",
            "source_id": xerosis,
            "evidence_quote": "В рекомендациях по ксерозу указано применение средств с 2-5-10% мочевины при сухости кожи.",
            "confidence": 0.75,
            "status": "confirmed",
        },
        {
            "rule_type": "profile_support",
            "target_type": "ingredient",
            "target_key": "emollient",
            "condition_type": "accepted_insight",
            "condition_value": "damaged_barrier",
            "effect": "boost",
            "weight_delta": 16,
            "severity": "info",
            "source_id": atopic,
            "evidence_quote": "Всем пациентам с атопическим дерматитом показано использование увлажняющих и смягчающих средств (эмолентов).",
            "confidence": 0.9,
            "status": "confirmed",
        },
        {
            "rule_type": "ingredient_profile",
            "target_type": "ingredient",
            "target_key": "salicylic acid",
            "condition_type": "accepted_insight",
            "condition_value": "damaged_barrier",
            "effect": "warning",
            "weight_delta": -8,
            "severity": "caution",
            "source_id": acne,
            "evidence_quote": "При акне рекомендован бережный уход без раздражающих кожу компонентов, включая кератолитические средства в высоких концентрациях.",
            "confidence": 0.7,
            "status": "confirmed",
        },
    ]


def rule_identity(rule: Dict[str, object]) -> tuple:
    return (
        rule["rule_type"],
        rule["target_type"],
        rule.get("target_key"),
        rule["condition_type"],
        rule["condition_value"],
        rule["effect"],
    )


async def seed_rules() -> Dict[str, int]:
    conn = await asyncpg.connect(
        host=os.getenv("DB_HOST", "localhost"),
        port=int(os.getenv("DB_PORT", "5433")),
        database=os.getenv("DB_NAME", "aura"),
        user=os.getenv("DB_USER", "aura_user"),
        password=os.getenv("DB_PASSWORD", "aura_password"),
    )
    inserted = 0
    updated = 0
    try:
        rows = await conn.fetch("SELECT id, title FROM knowledge_sources WHERE enabled = true")
        sources = {row["title"]: row["id"] for row in rows}
        source_ids = resolve_source_ids(REQUIRED_SOURCE_TITLES, sources)
        rules = build_seed_rules(source_ids)
        async with conn.transaction():
            for rule in rules:
                existing_id = await conn.fetchval(
                    """
                    SELECT id
                    FROM matching_rules
                    WHERE rule_type=$1 AND target_type=$2 AND COALESCE(target_key, '')=COALESCE($3, '')
                      AND condition_type=$4 AND condition_value=$5 AND effect=$6
                    """,
                    *rule_identity(rule),
                )
                values = [
                    rule["rule_type"], rule["target_type"], None, rule.get("target_key"),
                    rule["condition_type"], rule["condition_value"], rule["effect"],
                    rule["weight_delta"], rule["severity"], rule["source_id"],
                    rule["evidence_quote"], rule["confidence"], rule["status"],
                ]
                if existing_id:
                    await conn.execute(
                        """
                        UPDATE matching_rules
                        SET rule_type=$1, target_type=$2, target_id=$3, target_key=$4,
                            condition_type=$5, condition_value=$6, effect=$7, weight_delta=$8,
                            severity=$9, source_id=$10, evidence_quote=$11, confidence=$12,
                            status=$13, updated_at=NOW()
                        WHERE id=$14
                        """,
                        *values,
                        existing_id,
                    )
                    updated += 1
                else:
                    await conn.execute(
                        """
                        INSERT INTO matching_rules (
                            rule_type, target_type, target_id, target_key, condition_type, condition_value,
                            effect, weight_delta, severity, source_id, evidence_quote, confidence, status,
                            reviewed_at
                        ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,NOW())
                        """,
                        *values,
                    )
                    inserted += 1
    finally:
        await conn.close()
    return {"inserted": inserted, "updated": updated, "total": inserted + updated}


async def main() -> None:
    result = await seed_rules()
    print(f"Seeded matching rules: inserted={result['inserted']} updated={result['updated']} total={result['total']}")


if __name__ == "__main__":
    asyncio.run(main())
