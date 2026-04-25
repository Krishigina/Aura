import json
from pathlib import Path

from backend.import_procedures_review import (
    build_procedure_dictionary_values,
    build_procedure_records,
    serialize_field,
)


def test_build_procedure_records_expands_compact_review_rows():
    payload = {
        "metadata": {"count": 1},
        "field_order": ["name", "zones", "description"],
        "procedures": [["Test", ["Лицо", "Шея"], "Description"]],
    }

    records = build_procedure_records(payload)

    assert records == [{"name": "Test", "zones": ["Лицо", "Шея"], "description": "Description"}]


def test_build_procedure_records_rejects_count_mismatch():
    payload = {"metadata": {"count": 2}, "field_order": ["name"], "procedures": [["Only one"]]}

    try:
        build_procedure_records(payload)
    except ValueError as exc:
        assert "count" in str(exc)
    else:
        raise AssertionError("expected count mismatch")


def test_serialize_field_preserves_list_values_as_json():
    assert serialize_field(["Лицо", "Шея"]) == '["Лицо", "Шея"]'
    assert serialize_field("Лицо") == "Лицо"
    assert serialize_field(None) is None


def test_review_file_matches_declared_shape():
    payload = json.loads(Path("backend/data/procedures_seed_review.json").read_text(encoding="utf-8"))

    records = build_procedure_records(payload)

    assert len(records) == payload["metadata"]["count"]
    assert all(record["name"] for record in records)


def test_botulinum_review_zones_include_detailed_muscle_targets():
    payload = json.loads(Path("backend/data/procedures_seed_review.json").read_text(encoding="utf-8"))
    records = build_procedure_records(payload)

    botulinum = next(record for record in records if record["name"] == "Ботулинотерапия мимических морщин")

    assert "Жевательные мышцы" in botulinum["zones"]
    assert "Круговая мышца рта" in botulinum["zones"]


def test_build_procedure_dictionary_values_collects_zones_from_records():
    records = [
        {"zones": ["Лицо", "Жевательные мышцы"]},
        {"zones": ["Круговая мышца рта", "Лицо"]},
    ]

    values = build_procedure_dictionary_values(records)["procedure_zones"]

    assert values == ["Жевательные мышцы", "Круговая мышца рта", "Лицо"]
