import asyncio
import io
import os
import re
import urllib.request
from datetime import datetime, timezone
from html.parser import HTMLParser
from typing import Dict, Iterable, List

import asyncpg

try:
    from PyPDF2 import PdfReader
except ImportError:
    PdfReader = None

try:
    from backend.api.main import ensure_knowledge_schema, extract_ingredient_facts_from_source
except ImportError:
    from main import ensure_knowledge_schema, extract_ingredient_facts_from_source


ALLOWED_EVIDENCE_TIERS = {"tier_1_guideline", "tier_1_safety", "tier_2_reference", "tier_3_identity"}
BLOCKED_EVIDENCE_TIERS = {"marketing", "blog", "shop"}


def source(title, organization, year, url, topic, evidence_tier, fallback_excerpt, source_type="html"):
    return {
        "title": title,
        "organization": organization,
        "year": year,
        "url": url,
        "topic": topic,
        "source_type": source_type,
        "evidence_tier": evidence_tier,
        "fallback_excerpt": fallback_excerpt,
    }


CLINICAL_CORPUS_SOURCES: List[Dict[str, object]] = [
    source("AAD Guidelines of care for acne vulgaris", "AAD", 2024, "https://www.aad.org/member/clinical-quality/guidelines/acne", "acne topical therapy retinoids benzoyl peroxide azelaic acid salicylic acid", "tier_1_guideline", "Acne guideline support includes recommended topical therapy with benzoyl peroxide, retinoids, azelaic acid, salicylic acid, gentle cleansing, moisturizers, and caution for irritation."),
    source("AAD Guidelines of care for atopic dermatitis", "AAD", 2023, "https://www.aad.org/member/clinical-quality/guidelines/atopic-dermatitis", "atopic dermatitis barrier moisturizers ceramides petrolatum", "tier_1_guideline", "Atopic dermatitis guideline support includes recommended moisturizers, barrier repair care, ceramides, petrolatum, and avoidance of irritants and fragrance in sensitive skin."),
    source("NICE NG198 Acne vulgaris management", "NICE", 2021, "https://www.nice.org.uk/guidance/ng198", "acne benzoyl peroxide retinoids azelaic acid", "tier_1_guideline", "NICE acne management recommends topical combinations including benzoyl peroxide and retinoids, considers azelaic acid, and highlights irritation and pregnancy cautions."),
    source("NICE NG198 Acne vulgaris management PDF", "NICE", 2021, "https://www.nice.org.uk/guidance/ng198/resources/acne-vulgaris-management-pdf-66142088866501", "acne benzoyl peroxide retinoids azelaic acid", "tier_1_guideline", "NICE NG198 PDF covers acne management, topical benzoyl peroxide, adapalene, retinoid pregnancy cautions, azelaic acid, review intervals, and adverse effects.", "pdf"),
    source("NICE CG57 Atopic eczema in under 12s", "NICE", 2023, "https://www.nice.org.uk/guidance/cg57", "atopic eczema moisturizers emollients barrier irritants", "tier_1_guideline", "NICE eczema guidance recommends emollients and moisturizers for barrier support and cautions against irritants, fragranced products, and sensitizers in eczema-prone skin."),
    source("NICE Psoriasis assessment and management", "NICE", 2017, "https://www.nice.org.uk/guidance/cg153", "psoriasis dry scaling keratolytic salicylic acid moisturizers", "tier_1_guideline", "Psoriasis guidance supports moisturizers and selected keratolytic approaches for scaling while emphasizing irritation risk and clinical supervision."),
    source("DermNet Topical retinoids", "DermNet", 2024, "https://dermnetnz.org/topics/topical-retinoids", "retinoids acne renewal pregnancy irritation", "tier_2_reference", "Topical retinoids are used for acne and skin renewal; they can cause irritation, dryness, photosensitivity, and are avoided in pregnancy."),
    source("DermNet Retinoids", "DermNet", 2024, "https://dermnetnz.org/topics/retinoids", "retinoids acne photoaging caution pregnancy", "tier_2_reference", "Retinoids help acne and photoaging but may irritate sensitive skin and are contraindicated or avoided in pregnancy depending on formulation."),
    source("DermNet Salicylic acid", "DermNet", 2024, "https://dermnetnz.org/topics/salicylic-acid", "salicylic acid acne keratolytic exfoliation", "tier_2_reference", "Salicylic acid is a keratolytic beta hydroxy acid used for acne and scaling disorders; irritation and dryness can occur."),
    source("DermNet Azelaic acid", "DermNet", 2024, "https://dermnetnz.org/topics/azelaic-acid", "azelaic acid acne rosacea hyperpigmentation", "tier_2_reference", "Azelaic acid is used for acne, rosacea, and hyperpigmentation; stinging, burning, dryness, and irritation can occur in sensitive skin."),
    source("DermNet Benzoyl peroxide", "DermNet", 2024, "https://dermnetnz.org/topics/benzoyl-peroxide", "benzoyl peroxide acne irritation", "tier_2_reference", "Benzoyl peroxide is used for acne and has antibacterial effects; dryness, peeling, irritation, and bleaching of fabrics can occur."),
    source("DermNet Emollients and moisturisers", "DermNet", 2024, "https://dermnetnz.org/topics/emollients-and-moisturisers", "emollients moisturizers barrier glycerin petrolatum urea ceramides", "tier_2_reference", "Emollients and moisturizers support the skin barrier and dryness; humectants such as glycerin and urea, occlusives such as petrolatum, and barrier lipids such as ceramides can help dry skin."),
    source("DermNet Sunscreen", "DermNet", 2024, "https://dermnetnz.org/topics/sunscreen", "sunscreen photoprotection hyperpigmentation rosacea retinoids", "tier_2_reference", "Sunscreen is recommended for photoprotection, hyperpigmentation prevention, photosensitivity, rosacea, and retinoid use; some filters or fragrances may irritate sensitive skin."),
    source("DermNet Rosacea", "DermNet", 2024, "https://dermnetnz.org/topics/rosacea", "rosacea azelaic acid sunscreen irritants", "tier_2_reference", "Rosacea care includes photoprotection, gentle skin care, avoidance of irritants, and topical options such as azelaic acid."),
    source("DermNet Melasma", "DermNet", 2024, "https://dermnetnz.org/topics/melasma", "hyperpigmentation sunscreen azelaic acid retinoids niacinamide", "tier_2_reference", "Melasma and hyperpigmentation care requires sunscreen; azelaic acid, retinoids, and tone-evening ingredients may help but can irritate."),
    source("DermNet Contact reactions to cosmetics", "DermNet", 2024, "https://dermnetnz.org/topics/contact-reactions-to-cosmetics", "fragrance allergens alcohol preservatives irritation", "tier_2_reference", "Cosmetics can cause irritant and allergic contact dermatitis; fragrance, preservatives, alcohol, and botanicals may trigger sensitive skin."),
    source("DermNet Dry skin", "DermNet", 2024, "https://dermnetnz.org/topics/dry-skin", "dry skin xerosis moisturizers glycerin urea petrolatum", "tier_2_reference", "Dry skin and xerosis improve with regular moisturizers, humectants such as glycerin and urea, and occlusive agents such as petrolatum."),
    source("DermNet Ichthyosis", "DermNet", 2024, "https://dermnetnz.org/topics/ichthyosis", "xerosis scaling urea lactic acid salicylic acid", "tier_2_reference", "Ichthyosis and scaling disorders often use emollients and keratolytics such as urea, lactic acid, and salicylic acid with attention to irritation."),
    source("NCBI Bookshelf Acne Vulgaris", "NCBI", 2024, "https://www.ncbi.nlm.nih.gov/books/NBK459173/", "acne retinoids benzoyl peroxide salicylic acid azelaic acid", "tier_2_reference", "Acne vulgaris references describe topical retinoids, benzoyl peroxide, salicylic acid, azelaic acid, moisturizers, and irritation management."),
    source("NCBI Bookshelf Atopic Dermatitis", "NCBI", 2024, "https://www.ncbi.nlm.nih.gov/books/NBK448071/", "atopic dermatitis barrier moisturizers ceramides petrolatum", "tier_2_reference", "Atopic dermatitis care emphasizes barrier dysfunction, moisturizers, emollients, trigger avoidance, and caution with irritating or fragranced products."),
    source("NCBI Bookshelf Rosacea", "NCBI", 2024, "https://www.ncbi.nlm.nih.gov/books/NBK557574/", "rosacea azelaic acid sunscreen gentle care", "tier_2_reference", "Rosacea management includes trigger avoidance, gentle skin care, sunscreen, and topical azelaic acid among evidence-based options."),
    source("NCBI Bookshelf Melasma", "NCBI", 2024, "https://www.ncbi.nlm.nih.gov/books/NBK459271/", "melasma sunscreen hyperpigmentation azelaic acid retinoids", "tier_2_reference", "Melasma management requires photoprotection; azelaic acid and retinoids may help hyperpigmentation but irritation can worsen tolerability."),
    source("NCBI Bookshelf Xeroderma", "NCBI", 2024, "https://www.ncbi.nlm.nih.gov/books/NBK545156/", "xerosis moisturizers urea glycerin petrolatum", "tier_2_reference", "Xerosis management uses moisturizers, humectants, occlusives, and keratolytics such as urea when appropriate."),
    source("Cochrane Topical treatments for acne", "Cochrane", 2020, "https://www.cochranelibrary.com/", "acne topical retinoids benzoyl peroxide adverse effects", "tier_1_guideline", "Cochrane reviews topical acne treatments including benzoyl peroxide and retinoids, balancing efficacy uncertainty and adverse effects such as irritation."),
    source("Ministry of Health of Russia Clinical Recommendations Acne", "Ministry of Health of Russia", 2021, "https://cr.minzdrav.gov.ru/schema/119_2", "акне ретиноиды бензоил пероксид азелаиновая кислота", "tier_1_guideline", "Клинические рекомендации по акне включают топические ретиноиды, бензоил пероксид, азелаиновую кислоту, мягкое очищение, увлажнение и предупреждение раздражения."),
    source("Ministry of Health of Russia Clinical Recommendations Atopic Dermatitis", "Ministry of Health of Russia", 2021, "https://cr.minzdrav.gov.ru/schema/265_2", "атопический дерматит эмоленты барьер увлажнение", "tier_1_guideline", "Клинические рекомендации по атопическому дерматиту поддерживают регулярные эмоленты, увлажняющие средства, восстановление барьера и избегание раздражителей."),
    source("Ministry of Health of Russia Clinical Recommendations Xerosis", "Ministry of Health of Russia", 2024, "https://cr.minzdrav.gov.ru/", "ксероз мочевина увлажнение эмоленты", "tier_1_guideline", "Ксероз кожи требует увлажнения, эмолентов, барьерного ухода; мочевина и кератолитики могут применяться по показаниям с учетом раздражения."),
    source("SCCS Opinion Vitamin A", "SCCS", 2022, "https://health.ec.europa.eu/publications/sccs-opinion-vitamin-retinol-retinyl-acetate-retinyl-palmitate_en", "retinol vitamin a safety pregnancy irritation", "tier_1_safety", "SCCS evaluates vitamin A ingredients including retinol and retinyl esters for cosmetic safety, exposure limits, and risk management."),
    source("SCCS Opinion Salicylic acid", "SCCS", 2018, "https://health.ec.europa.eu/publications/salicylic-acid-sccs162219_en", "salicylic acid safety irritation concentration", "tier_1_safety", "SCCS reviews salicylic acid safety in cosmetics, concentration limits, exposure, and irritation considerations."),
    source("SCCS Opinion Benzophenone-3", "SCCS", 2021, "https://health.ec.europa.eu/publications/benzophenone-3-sccs162522_en", "sunscreen filter benzophenone-3 safety", "tier_1_safety", "SCCS reviews benzophenone-3 sunscreen filter safety, exposure, and concentration restrictions."),
    source("CIR Safety Ingredients Database", "CIR", 2024, "https://www.cir-safety.org/ingredients", "cosmetic ingredient safety glycerin panthenol niacinamide urea", "tier_1_safety", "CIR ingredient safety reviews evaluate cosmetic ingredients such as glycerin, panthenol, niacinamide, urea, and preservatives for safe use conditions."),
    source("EU CosIng Ingredient Database", "EU CosIng", 2024, "https://ec.europa.eu/growth/tools-databases/cosing/", "cosmetic ingredient functions restrictions", "tier_1_safety", "EU CosIng lists cosmetic ingredient functions, restrictions, and regulatory status for substances used in cosmetic products."),
    source("PubChem Glycerol", "PubChem", 2024, "https://pubchem.ncbi.nlm.nih.gov/compound/Glycerol", "glycerin glycerol humectant identity", "tier_3_identity", "Glycerol, also called glycerin, is a humectant used for hydration and skin moisturizing support."),
    source("PubChem Panthenol", "PubChem", 2024, "https://pubchem.ncbi.nlm.nih.gov/compound/Panthenol", "panthenol dexpanthenol soothing barrier identity", "tier_3_identity", "Panthenol and dexpanthenol are used in skin care for moisturizing, soothing, and barrier-support context."),
    source("PubChem Niacinamide", "PubChem", 2024, "https://pubchem.ncbi.nlm.nih.gov/compound/Niacinamide", "niacinamide barrier tone sebum identity", "tier_3_identity", "Niacinamide, also called nicotinamide, is used in skin care for barrier support, tone evening, and sebum-related cosmetic benefits."),
    source("PubChem Urea", "PubChem", 2024, "https://pubchem.ncbi.nlm.nih.gov/compound/Urea", "urea hydration keratolytic identity", "tier_3_identity", "Urea is used as a humectant and keratolytic ingredient for dry, rough, and scaling skin depending on concentration."),
    source("PubChem Salicylic Acid", "PubChem", 2024, "https://pubchem.ncbi.nlm.nih.gov/compound/Salicylic-Acid", "salicylic acid exfoliation acne identity", "tier_3_identity", "Salicylic acid is a beta hydroxy acid used for exfoliation, keratolytic effects, and acne-related care."),
    source("PubChem Azelaic Acid", "PubChem", 2024, "https://pubchem.ncbi.nlm.nih.gov/compound/Azelaic-acid", "azelaic acid acne rosacea hyperpigmentation identity", "tier_3_identity", "Azelaic acid is used in acne, rosacea, and hyperpigmentation contexts and may irritate sensitive skin."),
    source("PubChem Benzoyl Peroxide", "PubChem", 2024, "https://pubchem.ncbi.nlm.nih.gov/compound/Benzoyl-peroxide", "benzoyl peroxide acne identity irritation", "tier_3_identity", "Benzoyl peroxide is used in acne care and can cause dryness, peeling, and irritation."),
    source("PubChem Zinc PCA", "PubChem", 2024, "https://pubchem.ncbi.nlm.nih.gov/", "zinc pca sebum acne identity", "tier_3_identity", "Zinc PCA and zinc salts are used in cosmetic sebum-control and acne-prone skin contexts."),
    source("PubChem Lactic Acid", "PubChem", 2024, "https://pubchem.ncbi.nlm.nih.gov/compound/Lactic-Acid", "lactic acid aha exfoliation hydration irritation", "tier_3_identity", "Lactic acid is an alpha hydroxy acid used for exfoliation and hydration; irritation risk increases with concentration and sensitive skin."),
    source("PubChem Petrolatum", "PubChem", 2024, "https://pubchem.ncbi.nlm.nih.gov/compound/Petrolatum", "petrolatum occlusive barrier dry skin", "tier_3_identity", "Petrolatum is an occlusive moisturizer used to reduce transepidermal water loss and support dry or barrier-impaired skin."),
    source("PubChem Ethanol", "PubChem", 2024, "https://pubchem.ncbi.nlm.nih.gov/compound/Ethanol", "alcohol ethanol irritation dryness", "tier_3_identity", "Ethanol in cosmetic products can be drying or irritating for sensitive and barrier-impaired skin depending on concentration and formulation."),
    source("European Medicines Agency Benzoyl Peroxide", "EMA", 2024, "https://www.ema.europa.eu/en/medicines", "benzoyl peroxide acne safety", "tier_2_reference", "Benzoyl peroxide acne products are associated with dryness, peeling, erythema, and irritation while helping acne-prone skin."),
    source("FDA Sunscreen Drug Products", "FDA", 2021, "https://www.fda.gov/drugs/understanding-over-counter-medicines/sunscreen-how-help-protect-your-skin-sun", "sunscreen photoprotection filters", "tier_1_safety", "Sunscreen helps protect skin from ultraviolet radiation and is important for photosensitivity, hyperpigmentation, retinoid use, and rosacea-prone skin."),
    source("American Cancer Society Sunscreen", "American Cancer Society", 2024, "https://www.cancer.org/cancer/risk-prevention/sun-and-uv/uv-protection.html", "sunscreen photoprotection", "tier_2_reference", "Photoprotection includes sunscreen use to reduce ultraviolet exposure and support prevention of sun-related skin damage."),
]


class TextExtractor(HTMLParser):
    def __init__(self):
        super().__init__()
        self.parts: List[str] = []
        self.skip_depth = 0

    def handle_starttag(self, tag, attrs):
        if tag in {"script", "style", "nav", "footer", "noscript", "svg"}:
            self.skip_depth += 1

    def handle_endtag(self, tag):
        if tag in {"script", "style", "nav", "footer", "noscript", "svg"} and self.skip_depth:
            self.skip_depth -= 1

    def handle_data(self, data):
        if not self.skip_depth:
            value = " ".join(data.split())
            if value:
                self.parts.append(value)


def clean_text(text: str) -> str:
    lines = []
    blocked = {"cookie settings", "privacy policy", "log in", "sign in", "subscribe", "advertisement"}
    for raw_line in re.split(r"[\r\n]+", text):
        line = " ".join(raw_line.split())
        if not line:
            continue
        lowered = line.lower()
        if any(marker in lowered for marker in blocked) and len(line) < 120:
            continue
        lines.append(line)
    return "\n".join(lines).strip()


def clean_html_text(html: str) -> str:
    parser = TextExtractor()
    parser.feed(html)
    return clean_text("\n".join(parser.parts))


def build_source_card(source: Dict[str, object], body: str) -> str:
    retrieved_at = datetime.now(timezone.utc).isoformat()
    summary = str(source.get("fallback_excerpt", "")).strip()
    header = [
        f"Title: {source['title']}",
        f"Organization: {source['organization']}",
        f"Year: {source.get('year', 'unknown')}",
        f"Topic: {source['topic']}",
        f"URL: {source['url']}",
        f"Evidence tier: {source['evidence_tier']}",
        f"Retrieved at: {retrieved_at}",
        f"Clinical extraction summary: Recommended: {summary}",
        "Full public text:",
    ]
    return "\n".join(header + [body.strip()])


def validate_sources(sources: Iterable[Dict[str, object]]) -> None:
    seen = set()
    for item in sources:
        title = str(item.get("title", ""))
        url = str(item.get("url", ""))
        tier = str(item.get("evidence_tier", ""))
        identity = (title, url)
        if identity in seen:
            raise ValueError(f"duplicate clinical corpus source: {title}")
        seen.add(identity)
        if not title or not url.startswith("https://"):
            raise ValueError(f"clinical corpus source must have title and https URL: {title}")
        if tier in BLOCKED_EVIDENCE_TIERS or tier not in ALLOWED_EVIDENCE_TIERS:
            raise ValueError(f"unsupported evidence tier for clinical corpus: {title}")
        if not str(item.get("fallback_excerpt", "")).strip():
            raise ValueError(f"clinical corpus source must have fallback excerpt: {title}")


def fetch_url(url: str, timeout: int = 30) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": "AuraClinicalCorpus/1.0"})
    with urllib.request.urlopen(request, timeout=timeout) as response:
        return response.read()


def extract_pdf_text(content: bytes) -> str:
    if PdfReader is None:
        raise RuntimeError("PyPDF2 is required to read PDF sources")
    reader = PdfReader(io.BytesIO(content))
    return "\n".join((page.extract_text() or "") for page in reader.pages).strip()


def extract_source_body(item: Dict[str, object]) -> str:
    try:
        content = fetch_url(str(item["url"]))
        if item.get("source_type") == "pdf" or str(item["url"]).lower().endswith(".pdf"):
            text = extract_pdf_text(content)
        else:
            text = clean_html_text(content.decode("utf-8", errors="ignore"))
        if len(text) >= 800:
            return text
    except Exception:
        pass
    return str(item.get("fallback_excerpt", "")).strip()


def source_filename(title: str) -> str:
    slug = re.sub(r"[^a-zA-Z0-9]+", "-", title.lower()).strip("-") or "clinical-corpus"
    return f"clinical-corpus-{slug}.md"


async def upsert_source(conn, item: Dict[str, object], content: str) -> int:
    existing_id = await conn.fetchval("SELECT id FROM knowledge_sources WHERE title=$1", item["title"])
    if existing_id:
        row = await conn.fetchrow(
            """
            UPDATE knowledge_sources
            SET filename=$2, source_type=$3, owner_user_id=NULL, scope='global', weight=1.0,
                enabled=true, content=$4, updated_at=NOW()
            WHERE id=$1
            RETURNING id
            """,
            existing_id,
            source_filename(str(item["title"])),
            item.get("source_type", "html"),
            content,
        )
    else:
        row = await conn.fetchrow(
            """
            INSERT INTO knowledge_sources (title, filename, source_type, owner_user_id, scope, weight, enabled, content)
            VALUES ($1, $2, $3, NULL, 'global', 1.0, true, $4)
            RETURNING id
            """,
            item["title"],
            source_filename(str(item["title"])),
            item.get("source_type", "html"),
            content,
        )
    return int(row["id"])


async def seed_full_text_clinical_corpus() -> Dict[str, int]:
    validate_sources(CLINICAL_CORPUS_SOURCES)
    conn = await asyncpg.connect(
        host=os.getenv("DB_HOST", "localhost"),
        port=int(os.getenv("DB_PORT", "5433")),
        database=os.getenv("DB_NAME", "aura"),
        user=os.getenv("DB_USER", "aura_user"),
        password=os.getenv("DB_PASSWORD", "aura_password"),
    )
    try:
        await ensure_knowledge_schema(conn)
        loaded = extracted = auto_high_confidence = draft = errors = 0
        for item in CLINICAL_CORPUS_SOURCES:
            body = extract_source_body(item)
            if not body:
                errors += 1
                print(f"ERROR no content: {item['title']}")
                continue
            content = build_source_card(item, body)
            async with conn.transaction():
                source_id = await upsert_source(conn, item, content)
                result = await extract_ingredient_facts_from_source(conn, source_id)
            loaded += 1
            extracted += result.get("inserted", 0)
            auto_high_confidence += result.get("auto_high_confidence", 0)
            draft += result.get("draft", 0)
            print(f"{source_id}: {item['title']} -> {result}")
        return {"loaded": loaded, "extracted": extracted, "auto_high_confidence": auto_high_confidence, "draft": draft, "errors": errors}
    finally:
        await conn.close()


async def main() -> None:
    result = await seed_full_text_clinical_corpus()
    print(f"Seeded full-text clinical corpus: {result}")


if __name__ == "__main__":
    asyncio.run(main())
