from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def test_matching_rules_defaults_to_all_statuses_and_has_actionable_empty_state():
    source = (ROOT / "src/pages/MatchingRules.jsx").read_text(encoding="utf-8")

    assert "const ALL_STATUSES_VALUE = 'all'" in source
    assert "useState(ALL_STATUSES_VALUE)" in source
    assert "matchingApi.listRules(status === ALL_STATUSES_VALUE ? null : status)" in source
    assert "Правила подбора пока не созданы" in source
    assert "Загрузите доказательные источники" in source


def test_matching_rules_shows_status_and_disables_current_status_action():
    source = (ROOT / "src/pages/MatchingRules.jsx").read_text(encoding="utf-8")

    assert "<th>Статус</th>" in source
    assert "getStatusLabel(rule.status)" in source
    assert "disabled={rule.status === 'confirmed'}" in source
