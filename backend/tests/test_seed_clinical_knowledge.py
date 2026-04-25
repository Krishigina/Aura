from backend.seed_clinical_knowledge import (
    CLINICAL_SOURCES,
    EVIDENCE_ORGANIZATIONS,
    build_source_content,
    source_identity,
)


def test_clinical_sources_are_strict_evidence_based_records():
    assert len(CLINICAL_SOURCES) >= 12
    for source in CLINICAL_SOURCES:
        assert source["title"]
        assert source["organization"] in EVIDENCE_ORGANIZATIONS
        assert isinstance(source["year"], int)
        assert source["year"] >= 2015
        assert source["url"].startswith("https://")
        assert source["topic"]
        assert source["content"]
        assert source["weight"] == 1.0
        assert source["enabled"] is True
        assert source["source_type"] == "clinical_guideline"
        assert "blog" not in source["url"].lower()
        assert "brand" not in source["topic"].lower()


def test_build_source_content_preserves_provenance_and_summary():
    source = CLINICAL_SOURCES[0]

    content = build_source_content(source)

    assert source["organization"] in content
    assert str(source["year"]) in content
    assert source["url"] in content
    assert source["content"] in content


def test_source_identity_uses_title_for_idempotent_upsert():
    assert source_identity({"title": "AAD Acne Guideline"}) == "AAD Acne Guideline"
