import pytest

from backend.api.full_text_clinical_corpus import (
    CLINICAL_CORPUS_SOURCES,
    build_source_card,
    clean_html_text,
    validate_sources,
)


def test_manifest_has_authoritative_full_text_first_sources():
    validate_sources(CLINICAL_CORPUS_SOURCES)
    assert len(CLINICAL_CORPUS_SOURCES) >= 40
    assert any(source["organization"] == "DermNet" for source in CLINICAL_CORPUS_SOURCES)
    assert any(source["organization"] == "NICE" for source in CLINICAL_CORPUS_SOURCES)
    assert any(source["organization"] == "AAD" for source in CLINICAL_CORPUS_SOURCES)
    assert any(source["organization"] == "SCCS" for source in CLINICAL_CORPUS_SOURCES)


def test_manifest_rejects_non_https_and_marketing_sources():
    bad_sources = [
        {
            "title": "Shop Blog Acne Tips",
            "organization": "Shop",
            "url": "http://example.com/blog/acne",
            "topic": "acne",
            "source_type": "html",
            "evidence_tier": "marketing",
            "fallback_excerpt": "Buy this product for acne.",
        }
    ]

    with pytest.raises(ValueError):
        validate_sources(bad_sources)


def test_clean_html_text_removes_obvious_boilerplate():
    html = """
    <html><body><nav>Menu Login</nav><main><h1>Azelaic acid</h1>
    <p>Azelaic acid is used for acne and rosacea.</p>
    <script>alert('x')</script><footer>Cookie settings</footer></main></body></html>
    """

    text = clean_html_text(html)

    assert "Azelaic acid is used for acne and rosacea." in text
    assert "alert" not in text
    assert "Cookie settings" not in text


def test_build_source_card_keeps_traceability():
    source = {
        "title": "DermNet Salicylic acid",
        "organization": "DermNet",
        "year": 2024,
        "url": "https://dermnetnz.org/topics/salicylic-acid",
        "topic": "salicylic acid acne keratolytic",
        "source_type": "html",
        "evidence_tier": "tier_2_reference",
        "fallback_excerpt": "Salicylic acid is a keratolytic used in acne.",
    }

    card = build_source_card(source, "Salicylic acid is a keratolytic used in acne.")

    assert "Title: DermNet Salicylic acid" in card
    assert "Organization: DermNet" in card
    assert "URL: https://dermnetnz.org/topics/salicylic-acid" in card
    assert "Evidence tier: tier_2_reference" in card
    assert "Salicylic acid is a keratolytic used in acne." in card
