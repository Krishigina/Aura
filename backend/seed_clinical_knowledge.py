import asyncio
import os
import re
from typing import Dict, List

import asyncpg


EVIDENCE_ORGANIZATIONS = {
    "AAD",
    "NICE",
    "BAD",
    "EDF/EADV",
    "Cochrane",
    "Ministry of Health of Russia",
    "American Society for Dermatologic Surgery",
    "International Consensus Group",
}


CLINICAL_SOURCES: List[Dict[str, object]] = [
    {
        "title": "AAD Guidelines of care for acne vulgaris",
        "organization": "AAD",
        "year": 2024,
        "url": "https://www.aad.org/member/clinical-quality/guidelines/acne",
        "topic": "acne vulgaris",
        "content": "Official American Academy of Dermatology guideline for acne vulgaris. Supports evidence-based topical acne therapy including benzoyl peroxide, topical retinoids, combination therapy, antibiotic stewardship, and patient-centered adverse effect counseling.",
        "source_type": "clinical_guideline",
        "weight": 1.0,
        "enabled": True,
    },
    {
        "title": "NICE NG198 Acne vulgaris management",
        "organization": "NICE",
        "year": 2021,
        "url": "https://www.nice.org.uk/guidance/ng198",
        "topic": "acne vulgaris",
        "content": "NICE guideline NG198 covers management of acne vulgaris, treatment choice by severity, topical combination treatments, oral antibiotic stewardship, isotretinoin referral criteria, pregnancy considerations, and review intervals.",
        "source_type": "clinical_guideline",
        "weight": 1.0,
        "enabled": True,
    },
    {
        "title": "NICE CG153 Psoriasis assessment and management",
        "organization": "NICE",
        "year": 2017,
        "url": "https://www.nice.org.uk/guidance/cg153",
        "topic": "psoriasis",
        "content": "NICE psoriasis guideline covers assessment, topical therapy, phototherapy, systemic therapy escalation, safety monitoring, and quality-of-life impact. Useful for RAG safety context and referral boundaries.",
        "source_type": "clinical_guideline",
        "weight": 1.0,
        "enabled": True,
    },
    {
        "title": "NICE CG57 Atopic eczema in under 12s",
        "organization": "NICE",
        "year": 2023,
        "url": "https://www.nice.org.uk/guidance/cg57",
        "topic": "atopic dermatitis eczema",
        "content": "NICE atopic eczema guidance covers stepped care, emollient use, topical corticosteroid potency, trigger avoidance, infection recognition, and when to refer. Useful for barrier support and caution rules.",
        "source_type": "clinical_guideline",
        "weight": 1.0,
        "enabled": True,
    },
    {
        "title": "AAD Guidelines of care for atopic dermatitis",
        "organization": "AAD",
        "year": 2023,
        "url": "https://www.aad.org/member/clinical-quality/guidelines/atopic-dermatitis",
        "topic": "atopic dermatitis",
        "content": "AAD atopic dermatitis guidelines cover topical therapy, moisturizers, bathing practices, topical anti-inflammatory therapy, systemic therapy, and safety counseling for chronic inflammatory skin disease.",
        "source_type": "clinical_guideline",
        "weight": 1.0,
        "enabled": True,
    },
    {
        "title": "BAD Guidelines for biologic therapy for psoriasis",
        "organization": "BAD",
        "year": 2020,
        "url": "https://academic.oup.com/bjd/article/183/4/628/6600570",
        "topic": "psoriasis biologic therapy",
        "content": "British Association of Dermatologists guideline on biologic therapy for psoriasis. Provides evidence-based escalation, screening, contraindication, monitoring, and safety considerations.",
        "source_type": "clinical_guideline",
        "weight": 1.0,
        "enabled": True,
    },
    {
        "title": "EDF/EADV European evidence-based guideline for rosacea",
        "organization": "EDF/EADV",
        "year": 2017,
        "url": "https://onlinelibrary.wiley.com/doi/10.1111/jdv.14349",
        "topic": "rosacea",
        "content": "European Dermatology Forum and EADV evidence-based rosacea guideline. Covers phenotype-based management, topical and systemic treatment options, trigger counseling, ocular rosacea, and laser/light considerations.",
        "source_type": "clinical_guideline",
        "weight": 1.0,
        "enabled": True,
    },
    {
        "title": "BAD Guidelines for vitiligo management",
        "organization": "BAD",
        "year": 2021,
        "url": "https://academic.oup.com/bjd/article/186/1/18/6599823",
        "topic": "vitiligo",
        "content": "British Association of Dermatologists guideline for vitiligo management. Covers diagnosis, photoprotection, topical therapies, phototherapy, psychological impact, camouflage, and referral considerations.",
        "source_type": "clinical_guideline",
        "weight": 1.0,
        "enabled": True,
    },
    {
        "title": "Ministry of Health of Russia Clinical Recommendations Acne",
        "organization": "Ministry of Health of Russia",
        "year": 2021,
        "url": "https://cr.minzdrav.gov.ru/schema/119_2",
        "topic": "acne vulgaris Russian clinical recommendations",
        "content": "Russian clinical recommendations for acne vulgaris. Include gentle cleansing and moisturizing care, avoidance of irritating components in high concentrations, benzoyl peroxide with antibiotic stewardship, topical retinoids, azelaic acid, and severity-based therapy.",
        "source_type": "clinical_guideline",
        "weight": 1.0,
        "enabled": True,
    },
    {
        "title": "Ministry of Health of Russia Clinical Recommendations Atopic Dermatitis",
        "organization": "Ministry of Health of Russia",
        "year": 2021,
        "url": "https://cr.minzdrav.gov.ru/schema/265_2",
        "topic": "atopic dermatitis Russian clinical recommendations",
        "content": "Russian clinical recommendations for atopic dermatitis. Emphasize regular emollient and moisturizer use, topical anti-inflammatory therapy, infection management, trigger avoidance, severity assessment, and referral indications.",
        "source_type": "clinical_guideline",
        "weight": 1.0,
        "enabled": True,
    },
    {
        "title": "Ministry of Health of Russia Clinical Recommendations Xerosis",
        "organization": "Ministry of Health of Russia",
        "year": 2024,
        "url": "https://cr.minzdrav.gov.ru/",
        "topic": "xerosis dry skin Russian clinical recommendations",
        "content": "Russian clinical recommendations for xerosis of the skin. Support moisturizers and emollients for dry skin, barrier-restoring care, urea/lactic/salicylic acid keratolytic considerations by indication, and evaluation of secondary causes of xerosis.",
        "source_type": "clinical_guideline",
        "weight": 1.0,
        "enabled": True,
    },
    {
        "title": "Cochrane Topical treatments for acne",
        "organization": "Cochrane",
        "year": 2020,
        "url": "https://www.cochranelibrary.com/",
        "topic": "acne evidence review",
        "content": "Cochrane evidence reviews provide systematic review context for topical acne interventions, comparative effectiveness, uncertainty, and adverse effects. Use as supporting evidence rather than standalone prescribing guidance.",
        "source_type": "clinical_guideline",
        "weight": 1.0,
        "enabled": True,
    },
    {
        "title": "ASDS Guidelines of care for injectable fillers",
        "organization": "American Society for Dermatologic Surgery",
        "year": 2015,
        "url": "https://www.asds.net/skin-experts/news-room/press-releases/asds-guidelines-of-care-for-soft-tissue-fillers",
        "topic": "soft tissue fillers safety",
        "content": "American Society for Dermatologic Surgery guidance on soft-tissue fillers. Covers patient selection, informed consent, aseptic technique, adverse events, vascular compromise risk, and practitioner training considerations.",
        "source_type": "clinical_guideline",
        "weight": 1.0,
        "enabled": True,
    },
    {
        "title": "International Consensus Recommendations on Botulinum Toxin Aesthetic Use",
        "organization": "International Consensus Group",
        "year": 2020,
        "url": "https://academic.oup.com/asj/article/40/1/NP1/5612563",
        "topic": "botulinum toxin aesthetic safety",
        "content": "International consensus recommendations for aesthetic botulinum toxin use. Covers anatomical assessment, dosing principles, patient selection, contraindications, adverse event prevention, and safe injection planning.",
        "source_type": "clinical_guideline",
        "weight": 1.0,
        "enabled": True,
    },
    {
        "title": "International Consensus Recommendations on Laser and Energy-Based Device Safety",
        "organization": "International Consensus Group",
        "year": 2018,
        "url": "https://academic.oup.com/asj/article/38/1/86/4096429",
        "topic": "laser energy based device safety",
        "content": "Consensus recommendations for laser and energy-based device safety. Covers patient selection, contraindications, eye protection, device parameters, pigmentary risk, burns, infection prevention, and post-procedure care.",
        "source_type": "clinical_guideline",
        "weight": 1.0,
        "enabled": True,
    },
]


def slugify(value: str) -> str:
    normalized = re.sub(r"[^a-zA-Z0-9а-яА-Я]+", "-", value.strip()).strip("-").lower()
    return normalized or "clinical-source"


def source_identity(source: Dict[str, object]) -> str:
    return str(source["title"])


def build_source_content(source: Dict[str, object]) -> str:
    return "\n".join([
        f"Title: {source['title']}",
        f"Organization: {source['organization']}",
        f"Year: {source['year']}",
        f"Topic: {source['topic']}",
        f"URL: {source['url']}",
        "Evidence level: official clinical guideline, clinical recommendation, consensus statement, or systematic review support source.",
        "Summary:",
        str(source["content"]),
    ])


def validate_sources(sources: List[Dict[str, object]]) -> None:
    titles = set()
    for source in sources:
        title = source_identity(source)
        if title in titles:
            raise ValueError(f"duplicate clinical source title: {title}")
        titles.add(title)
        if source["organization"] not in EVIDENCE_ORGANIZATIONS:
            raise ValueError(f"unsupported evidence organization: {source['organization']}")
        if not str(source["url"]).startswith("https://"):
            raise ValueError(f"clinical source must use https URL: {title}")
        if source.get("weight") != 1.0 or source.get("enabled") is not True:
            raise ValueError(f"clinical source must be enabled with weight=1.0: {title}")


async def seed_clinical_knowledge() -> Dict[str, int]:
    validate_sources(CLINICAL_SOURCES)
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
        async with conn.transaction():
            for source in CLINICAL_SOURCES:
                title = source_identity(source)
                filename = f"{slugify(title)}.md"
                content = build_source_content(source)
                existing_id = await conn.fetchval("SELECT id FROM knowledge_sources WHERE title=$1", title)
                if existing_id:
                    await conn.execute(
                        """
                        UPDATE knowledge_sources
                        SET filename=$2, source_type=$3, owner_user_id=NULL, scope='global',
                            weight=$4, enabled=$5, content=$6, updated_at=NOW()
                        WHERE id=$1
                        """,
                        existing_id,
                        filename,
                        source["source_type"],
                        float(source["weight"]),
                        bool(source["enabled"]),
                        content,
                    )
                    updated += 1
                else:
                    await conn.execute(
                        """
                        INSERT INTO knowledge_sources (title, filename, source_type, owner_user_id, scope, weight, enabled, content)
                        VALUES ($1, $2, $3, NULL, 'global', $4, $5, $6)
                        """,
                        title,
                        filename,
                        source["source_type"],
                        float(source["weight"]),
                        bool(source["enabled"]),
                        content,
                    )
                    inserted += 1
    finally:
        await conn.close()
    return {"inserted": inserted, "updated": updated, "total": len(CLINICAL_SOURCES)}


async def main() -> None:
    result = await seed_clinical_knowledge()
    print(
        f"Seeded clinical knowledge: inserted={result['inserted']} "
        f"updated={result['updated']} total={result['total']}"
    )


if __name__ == "__main__":
    asyncio.run(main())
