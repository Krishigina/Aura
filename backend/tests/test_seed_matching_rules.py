import inspect

from backend import seed_matching_rules
from backend.seed_matching_rules import build_seed_rules, resolve_source_ids


def test_resolve_source_ids_requires_known_titles():
    sources = {"Акне вульгарные.docx": 8}

    try:
        resolve_source_ids(["Акне вульгарные.docx", "Ксероз кожи.pdf"], sources)
    except ValueError as exc:
        assert "Ксероз кожи.pdf" in str(exc)
    else:
        raise AssertionError("expected missing source title")


def test_build_seed_rules_creates_confirmed_rules_with_sources_and_quotes():
    source_ids = {
        "Акне вульгарные.docx": 8,
        "Атопический дерматит.docx": 10,
        "КР_Ксероз кожи_общественное обсуждение.pdf": 19,
    }

    rules = build_seed_rules(source_ids)

    assert len(rules) >= 5
    assert all(rule["status"] == "confirmed" for rule in rules)
    assert all(rule["source_id"] for rule in rules)
    assert all(rule["evidence_quote"] for rule in rules)
    assert any(rule["target_key"] == "benzoyl peroxide" and rule["effect"] == "boost" for rule in rules)
    assert any(rule["condition_value"] == "dryness" and rule["effect"] == "boost" for rule in rules)


def test_seed_rules_update_query_uses_contiguous_parameter_numbers():
    source = inspect.getsource(seed_matching_rules.seed_rules)

    assert "UPDATE matching_rules" in source
    assert "rule_type=$1" in source
    assert "target_type=$2" in source
    assert "target_id=$3" in source
    assert "target_key=$4" in source
