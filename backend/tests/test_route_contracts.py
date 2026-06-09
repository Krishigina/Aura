import inspect
import json
from pathlib import Path

import pytest
from fastapi import HTTPException

from backend.core.chat import (
    build_attachment_storage_path,
    build_chat_session_title,
    build_rag_query_payload,
    format_chat_timestamp,
    ingest_chat_attachment,
    map_rag_proxy_error,
    normalize_rag_source,
    validate_chat_attachment_content_type,
)
from backend.main import app
from backend.routers.chat_sessions import (
    get_chat_session_attachments,
    get_chat_session_detail,
    query_rag_chat,
    upload_chat_session_attachment,
)
from backend.schemas.chat import (
    ChatAttachmentResponse,
    ChatSessionCreateResponse,
    ChatSessionMessage,
    ChatSessionSummary,
    RagChatRequest,
    RagChatResponse,
)


@pytest.fixture
def anyio_backend():
    return "asyncio"


def _route_pairs():
    pairs = set()
    for route in app.routes:
        methods = getattr(route, "methods", set())
        path = getattr(route, "path", None)
        if not path:
            continue
        for method in methods:
            if method in {"HEAD", "OPTIONS"}:
                continue
            pairs.add((method, path))
    return pairs


def test_core_entity_routes_exist():
    pairs = _route_pairs()

    expected = {
        ("GET", "/api/health"),
        ("POST", "/api/auth/login"),
        ("POST", "/api/auth/register"),
        ("GET", "/api/auth/me"),
        ("GET", "/api/profile/skin-passport"),
        ("PUT", "/api/profile/skin-passport"),
        ("GET", "/api/profile/skin-passport/suggestions"),
        ("POST", "/api/profile/skin-passport/suggestions"),
        ("PATCH", "/api/profile/skin-passport/suggestions/{suggestion_id}"),
        ("GET", "/api/profile/skin-journal"),
        ("PUT", "/api/profile/skin-journal/settings"),
        ("POST", "/api/profile/skin-journal/procedures"),
        ("POST", "/api/profile/skin-journal/sensor-readings"),
        ("PATCH", "/api/profile/skin-journal/reminders/{reminder_id}"),
        ("GET", "/api/home/feed"),
        ("GET", "/api/home/status"),
        ("GET", "/api/diagnostics/summary"),
        ("GET", "/api/survey/schema"),
        ("GET", "/api/chat/bootstrap"),
        ("POST", "/api/chat/messages"),
        ("GET", "/api/chat/sessions"),
        ("POST", "/api/chat/sessions"),
        ("GET", "/api/chat/sessions/{session_id}"),
        ("POST", "/api/chat/sessions/{session_id}/attachments"),
        ("GET", "/api/chat/sessions/{session_id}/attachments"),
        ("POST", "/api/chat/rag"),
        ("GET", "/api/analytics/dashboard"),
        ("GET", "/api/reports/summary"),
        ("GET", "/api/products"),
        ("POST", "/api/products"),
        ("GET", "/api/matching/rules"),
        ("POST", "/api/matching/rules"),
        ("PATCH", "/api/matching/rules/{rule_id}"),
        ("POST", "/api/matching/products"),
        ("GET", "/api/dictionaries/{key}"),
        ("POST", "/api/dictionaries/{key}"),
        ("PUT", "/api/dictionaries/{key}"),
        ("DELETE", "/api/dictionaries/{key}/{value}"),
        ("PUT", "/api/dictionaries/brands"),
        ("GET", "/api/procedures"),
        ("POST", "/api/procedures"),
        ("GET", "/api/content"),
        ("POST", "/api/content"),
    }

    missing = expected - pairs
    assert not missing, f"Missing route contracts: {sorted(missing)}"


def test_single_health_route_in_active_backend():
    pairs = _route_pairs()
    health_count = sum(1 for pair in pairs if pair == ("GET", "/api/health"))
    assert health_count == 1, f"Expected 1 /api/health route, got {health_count}"


def test_build_skin_journal_default_shape():
    module = __import__("backend.core.skin_journal", fromlist=["build_empty_skin_journal"])

    journal = module.build_empty_skin_journal()

    assert journal == {
        "settings": {
            "has_sensor": None,
            "push_enabled": False,
            "sensor_reminder_schedule": None,
        },
        "procedures": [],
        "sensor_readings": [],
        "reminders": [],
    }


def test_skin_journal_routes_use_authenticated_user_profile():
    router_module = __import__(
        "backend.routers.profile_skin",
        fromlist=[
            "get_skin_journal",
            "save_skin_journal_settings",
        ],
    )
    core_module = __import__(
        "backend.core.skin_journal",
        fromlist=["load_user_skin_journal", "save_user_skin_journal"],
    )

    get_source = inspect.getsource(router_module.get_skin_journal)
    settings_source = inspect.getsource(router_module.save_skin_journal_settings)
    load_source = inspect.getsource(core_module.load_user_skin_journal)
    save_source = inspect.getsource(core_module.save_user_skin_journal)

    assert "Depends(get_current_user)" in get_source
    assert "load_user_skin_journal" in get_source
    assert "user_profiles" not in get_source
    assert "SKIN_JOURNAL_KEY" not in get_source
    assert "Depends(get_current_user)" in settings_source
    assert "save_skin_journal_settings_for_user" in settings_source
    assert "user_profiles" not in settings_source
    assert "SKIN_JOURNAL_KEY" not in settings_source
    assert "user_profiles" in load_source
    assert "user_profiles" in save_source
    assert "SKIN_JOURNAL_KEY" in load_source
    assert "SKIN_JOURNAL_KEY" in save_source


def test_create_procedure_entry_generates_repeat_reminder():
    source = inspect.getsource(
        __import__("backend.core.skin_journal", fromlist=["create_skin_journal_procedure_for_user"]).create_skin_journal_procedure_for_user
    )

    assert "procedure_repeat" in source
    assert "payload.repeat_due_at" in source
    assert "zone_amounts" in source


def test_skin_journal_mutations_lock_profile_row_before_save():
    router_module = __import__(
        "backend.routers.profile_skin",
        fromlist=[
            "save_skin_journal_settings",
            "create_skin_journal_procedure",
            "create_skin_journal_sensor_reading",
            "update_skin_journal_reminder",
        ],
    )
    core_module = __import__(
        "backend.core.skin_journal",
        fromlist=[
            "load_user_skin_journal",
            "save_skin_journal_settings_for_user",
            "create_skin_journal_procedure_for_user",
            "create_skin_journal_sensor_reading_for_user",
            "update_skin_journal_reminder_for_user",
        ],
    )
    schemas_module = __import__("backend.schemas.profile", fromlist=["SkinJournalProcedureCreate"])

    load_source = inspect.getsource(core_module.load_user_skin_journal)
    procedure_model_source = inspect.getsource(schemas_module.SkinJournalProcedureCreate)

    assert "for_update" in load_source
    assert "FOR UPDATE" in load_source
    assert "ON CONFLICT (user_id) DO NOTHING" in load_source
    assert load_source.index("ON CONFLICT (user_id) DO NOTHING") < load_source.index("FOR UPDATE")
    assert "Field(default_factory=list)" in procedure_model_source

    for name in [
        "save_skin_journal_settings",
        "create_skin_journal_procedure",
        "create_skin_journal_sensor_reading",
        "update_skin_journal_reminder",
    ]:
        source = inspect.getsource(getattr(router_module, name))
        assert "conn.transaction()" in source

    for name in [
        "save_skin_journal_settings_for_user",
        "create_skin_journal_procedure_for_user",
        "create_skin_journal_sensor_reading_for_user",
        "update_skin_journal_reminder_for_user",
    ]:
        source = inspect.getsource(getattr(core_module, name))
        assert "for_update=True" in source


def test_skin_journal_sensor_reading_validates_metric_ranges():
    module = __import__("backend.schemas.profile", fromlist=["SkinJournalSensorReadingCreate"])

    valid = module.SkinJournalSensorReadingCreate(
        measured_at="2026-04-29T12:00:00Z",
        zone="forehead",
        percent_value=75,
        hydration=3,
        oiliness=2,
        softness=4,
    )
    assert valid.percent_value == 75

    with pytest.raises(Exception):
        module.SkinJournalSensorReadingCreate(
            measured_at="2026-04-29T12:00:00Z",
            zone="forehead",
            percent_value=101,
            hydration=3,
            oiliness=2,
            softness=4,
        )

    with pytest.raises(Exception):
        module.SkinJournalSensorReadingCreate(
            measured_at="2026-04-29T12:00:00Z",
            zone="forehead",
            percent_value=75,
            hydration=0,
            oiliness=2,
            softness=4,
        )


def test_skin_journal_procedure_preserves_optional_photos():
    module = __import__("backend.schemas.profile", fromlist=["SkinJournalProcedureCreate"])

    payload = module.SkinJournalProcedureCreate(
        catalog_procedure_id=1,
        procedure_name="Релатокс",
        performed_at="2026-04-29T12:00:00Z",
        zones=["forehead"],
        photos=["photo-1"],
    )

    with pytest.warns(DeprecationWarning):
        entry = payload.dict()

    assert entry["photos"] == ["photo-1"]


def test_apply_reminder_action_updates_status_and_reschedule_time():
    module = __import__("backend.core.skin_journal", fromlist=["apply_reminder_action"])
    reminder = {
        "id": "reminder-1",
        "title": "Замер кожи",
        "status": "planned",
        "due_at": "2026-04-29T10:00:00Z",
    }

    updated = module.apply_reminder_action(
        reminder,
        action="reschedule",
        rescheduled_due_at="2026-04-30T10:00:00Z",
    )

    assert updated["status"] == "rescheduled"
    assert updated["due_at"] == "2026-04-30T10:00:00Z"


def test_matching_rule_update_allows_source_id_edits():
    source = inspect.getsource(__import__("backend.schemas.matching", fromlist=["MatchingRuleUpdate"]).MatchingRuleUpdate)

    assert "source_id" in source


def test_matching_rule_list_validates_status_filter():
    source = inspect.getsource(__import__("backend.routers.matching_admin", fromlist=["list_matching_rules"]).list_matching_rules)

    assert "validate_matching_rule_status(status)" in source


def test_matching_rule_api_validates_effect_and_roles():
    helper_module = __import__(
        "backend.core.matching.helpers",
        fromlist=["validate_matching_rule_effect", "require_matching_rule_admin"],
    )
    router_module = __import__(
        "backend.routers.matching_admin",
        fromlist=["create_matching_rule", "update_matching_rule", "list_matching_rules"],
    )

    assert helper_module.validate_matching_rule_effect("block") == "block"
    with pytest.raises(HTTPException):
        helper_module.validate_matching_rule_effect("unknown")

    create_source = inspect.getsource(router_module.create_matching_rule)
    update_source = inspect.getsource(router_module.update_matching_rule)
    list_source = inspect.getsource(router_module.list_matching_rules)

    assert "require_matching_rule_admin(current_user)" in create_source
    assert "require_matching_rule_admin(current_user)" in update_source
    assert "require_matching_rule_admin(current_user)" in list_source
    assert "validate_matching_rule_effect(payload.effect)" in create_source
    assert "validate_matching_rule_effect(updates[\"effect\"])" in update_source
    assert "MATCHING_RULE_UPDATE_FIELDS" in update_source


def test_rules_ingredients_overview_endpoint_combines_manual_and_ingredient_rules():
    router_module = __import__("backend.routers.matching_admin", fromlist=["get_rules_ingredients_overview"])
    core_module = __import__("backend.core.matching.admin", fromlist=["get_rules_ingredients_overview_for_admin"])
    route_source = inspect.getsource(router_module.get_rules_ingredients_overview)
    source = inspect.getsource(core_module.get_rules_ingredients_overview_for_admin)

    assert '@router.get("/api/admin/rules-ingredients/overview")' in route_source
    assert "FROM matching_rules mr" in source
    assert "FROM ingredient_evidence ie" in source
    assert "ie.evidence_status IN ('confirmed', 'auto_high_confidence')" in source
    assert "ie.matching_effect IN ('boost', 'warning', 'penalty', 'block')" in source
    assert "ie.effect_key IS NOT NULL" in source
    assert "ie.effect_key <> ''" in source
    assert "manual_rule" in source
    assert "ingredient_fact" in source


def test_rules_ingredients_overview_endpoint_returns_summary_counts():
    source = inspect.getsource(
        __import__("backend.core.matching.admin", fromlist=["get_rules_ingredients_overview_for_admin"]).get_rules_ingredients_overview_for_admin
    )

    assert "manual_rules_count" in source
    assert "active_ingredient_facts_count" in source
    assert "draft_ingredient_facts_count" in source
    assert "ingredients_count" in source
    assert "knowledge_sources_count" in source
    active_count_query = source[source.index("active_ingredient_facts_count = await conn.fetchval"):source.index("draft_ingredient_facts_count = await conn.fetchval")]
    ingredient_rows_query = source[source.index("ingredient_rows = await conn.fetch"):source.index("return {")]
    for query in [active_count_query, ingredient_rows_query]:
        assert "matching_effect IN ('boost', 'warning', 'penalty', 'block')" in query
        assert "effect_key IS NOT NULL" in query
        assert "effect_key <> ''" in query
        assert "evidence_status IN ('confirmed', 'auto_high_confidence')" in query
    assert 'conn.fetchval("SELECT COUNT(*) FROM knowledge_sources")' in source
    assert "owner_user_id IS NULL AND enabled=true" not in source


def test_matching_rules_schema_depends_on_existing_knowledge_sources():
    source = __import__("backend.db.schema", fromlist=["MAIN_SCHEMA_SQL"]).MAIN_SCHEMA_SQL

    assert source.index("CREATE TABLE IF NOT EXISTS knowledge_sources") < source.index("CREATE TABLE IF NOT EXISTS matching_rules")


def test_init_db_creates_ingredient_evidence_schema_after_knowledge_sources():
    source = inspect.getsource(__import__("backend.db.schema", fromlist=["ensure_knowledge_schema"]).ensure_knowledge_schema)

    assert "CREATE TABLE IF NOT EXISTS knowledge_sources" in source
    assert "CREATE TABLE IF NOT EXISTS ingredient_evidence" in source
    assert source.index("CREATE TABLE IF NOT EXISTS knowledge_sources") < source.index("CREATE TABLE IF NOT EXISTS ingredient_evidence")
    assert "source_id INTEGER REFERENCES knowledge_sources(id) ON DELETE SET NULL" in source
    assert "ingredient_key VARCHAR(255) NOT NULL" in source
    assert "effect_key VARCHAR(100) NOT NULL" in source
    assert "skin_condition_key VARCHAR(100)" in source
    assert "direction VARCHAR(50)" in source
    assert "strength VARCHAR(50)" in source
    assert "status VARCHAR(50) DEFAULT 'draft'" in source


def test_knowledge_schema_creates_ingredient_evidence_and_compatibility_columns():
    schema_specs = __import__("backend.db.schema_specs", fromlist=["KNOWLEDGE_SCHEMA_ALTERS"])
    source = inspect.getsource(__import__("backend.db.schema", fromlist=["ensure_knowledge_schema"]).ensure_knowledge_schema)

    assert "CREATE TABLE IF NOT EXISTS ingredient_evidence" in source
    assert "ALTER TABLE ingredients ADD COLUMN IF NOT EXISTS ingredient_class VARCHAR(100)" in schema_specs.KNOWLEDGE_SCHEMA_ALTERS
    assert "ALTER TABLE ingredients ADD COLUMN IF NOT EXISTS evidence_status VARCHAR(50) DEFAULT 'auto_created'" in schema_specs.KNOWLEDGE_SCHEMA_ALTERS
    assert "ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS matching_effect VARCHAR(50)" in schema_specs.KNOWLEDGE_SCHEMA_ALTERS
    assert "ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS matching_weight_delta DOUBLE PRECISION" in schema_specs.KNOWLEDGE_SCHEMA_ALTERS
    assert "ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS matching_condition_type VARCHAR(100)" in schema_specs.KNOWLEDGE_SCHEMA_ALTERS


def test_ingredient_evidence_schema_core_columns_match_startup_and_knowledge_runtime():
    schema_module = __import__("backend.db.schema", fromlist=["initialize_database_schema", "ensure_knowledge_schema"])
    schema_specs = __import__(
        "backend.db.schema_specs",
        fromlist=["INGREDIENT_EVIDENCE_MIGRATION_STATEMENTS", "KNOWLEDGE_SCHEMA_ALTERS"],
    )
    init_source = schema_module.MAIN_SCHEMA_SQL
    knowledge_source = inspect.getsource(schema_module.ensure_knowledge_schema)

    for column in [
        "ingredient_key VARCHAR(255) NOT NULL",
        "effect_key VARCHAR(100) NOT NULL",
        "skin_condition_key VARCHAR(100)",
        "direction VARCHAR(50)",
        "strength VARCHAR(50)",
        "summary TEXT",
        "evidence_quote TEXT DEFAULT ''",
        "source_id INTEGER REFERENCES knowledge_sources(id) ON DELETE SET NULL",
        "matching_effect VARCHAR(50)",
        "matching_weight_delta DOUBLE PRECISION",
        "matching_condition_type VARCHAR(100)",
        "status VARCHAR(50) DEFAULT 'draft'",
        "created_at TIMESTAMP DEFAULT NOW()",
        "updated_at TIMESTAMP DEFAULT NOW()",
    ]:
        assert column in init_source
        assert column in knowledge_source

    for migration in [
        "ALTER TABLE ingredient_evidence ALTER COLUMN direction DROP NOT NULL",
        "ALTER TABLE ingredient_evidence ALTER COLUMN strength DROP NOT NULL",
        "ALTER TABLE ingredient_evidence ALTER COLUMN summary DROP NOT NULL",
    ]:
        assert migration in schema_specs.INGREDIENT_EVIDENCE_MIGRATION_STATEMENTS
        assert migration in schema_specs.KNOWLEDGE_SCHEMA_ALTERS


def test_demo_entity_seed_requires_explicit_env_flag():
    source = inspect.getsource(__import__("backend.main", fromlist=["init_db"]).init_db)

    assert 'os.getenv("SEED_DEMO_DATA", "false").lower() == "true"' in source
    assert source.index('os.getenv("SEED_DEMO_DATA", "false").lower() == "true"') < source.index("Demo users created")


def test_product_matching_route_uses_confirmed_rules_only():
    router_module = __import__("backend.routers.matching_admin", fromlist=["match_products_for_current_user"])
    core_module = __import__("backend.core.product_matching", fromlist=["build_product_matching_response"])
    route_source = inspect.getsource(router_module.match_products_for_current_user)
    source = inspect.getsource(core_module.build_product_matching_response)

    assert "mr.status = 'confirmed'" in source
    assert "load_skin_passport_context" in route_source
    assert "match_product" in source


def test_product_matching_request_defaults_to_catalog_sized_limit():
    request = __import__("backend.schemas.matching", fromlist=["ProductMatchingRequest"]).ProductMatchingRequest()

    assert request.limit >= 100


@pytest.mark.anyio
async def test_load_accepted_passport_insights_extracts_jsonb_values():
    class FakeConn:
        async def fetch(self, query, user_id):
            assert "status='accepted'" in query
            assert "suggestion_type='append_insight'" in query
            assert user_id == 42
            return [
                {"proposed_value": {"normalized_value": "damaged_barrier"}},
                {"proposed_value": json.dumps({"value": "acid_sensitivity"})},
                {"proposed_value": json.dumps("scalar_sensitivity")},
                {"proposed_value": {"ignored": "missing value"}},
            ]

    module = __import__("backend.core.matching.helpers", fromlist=["load_accepted_passport_insights"])

    assert await module.load_accepted_passport_insights(FakeConn(), 42) == ["damaged_barrier", "acid_sensitivity", "scalar_sensitivity"]


def test_product_matching_endpoint_accepts_default_request_body():
    source = inspect.getsource(__import__("backend.routers.matching_admin", fromlist=["match_products_for_current_user"]).match_products_for_current_user)

    assert "payload: ProductMatchingRequest = ProductMatchingRequest()" in source


def test_product_match_sort_key_prioritizes_decision_before_score():
    main = __import__("backend.core.matching.helpers", fromlist=["product_match_sort_key"])
    items = [
        {"decision": "caution", "final_score": 99},
        {"decision": "recommend", "final_score": 1},
        {"decision": "exclude", "final_score": 100},
    ]

    assert [item["decision"] for item in sorted(items, key=main.product_match_sort_key)] == ["recommend", "caution", "exclude"]


def test_init_db_relaxes_legacy_ingredient_evidence_columns():
    source = __import__("backend.db.schema_specs", fromlist=["INGREDIENT_EVIDENCE_MIGRATION_STATEMENTS"]).INGREDIENT_EVIDENCE_MIGRATION_STATEMENTS

    assert "ALTER TABLE ingredient_evidence ALTER COLUMN direction DROP NOT NULL" in source
    assert "ALTER TABLE ingredient_evidence ALTER COLUMN strength DROP NOT NULL" in source
    assert "ALTER TABLE ingredient_evidence ALTER COLUMN summary DROP NOT NULL" in source


def test_product_matching_route_uses_decision_priority_sort():
    source = inspect.getsource(__import__("backend.routers.matching_admin", fromlist=["match_products_for_current_user"]).match_products_for_current_user)

    assert "product_match_sort_key" in source


def test_recommendation_feedback_schema_is_created_at_startup():
    source = __import__("backend.db.schema", fromlist=["MAIN_SCHEMA_SQL"]).MAIN_SCHEMA_SQL

    assert "CREATE TABLE IF NOT EXISTS recommendation_feedback" in source
    assert "recommendation_id VARCHAR(100) NOT NULL" in source
    assert "action VARCHAR(50) NOT NULL" in source


def test_recommendation_feedback_endpoint_persists_telemetry():
    schemas_module = __import__("backend.schemas.matching", fromlist=["RecommendationFeedbackCreate"])
    router_module = __import__("backend.routers.matching_admin", fromlist=["track_recommendation_feedback"])
    core_module = __import__("backend.core.matching.recommendations", fromlist=["track_recommendation_feedback_record"])

    payload = schemas_module.RecommendationFeedbackCreate(product_id="product-1", action="clicked", rank=2)
    route_source = inspect.getsource(router_module.track_recommendation_feedback)
    source = inspect.getsource(core_module.track_recommendation_feedback_record)

    assert payload.product_id == "product-1"
    assert payload.action == "clicked"
    assert "INSERT INTO recommendation_feedback" in source
    assert "current_user[\"id\"]" in route_source
    assert "validate_recommendation_feedback_action" in route_source


def test_matching_rule_api_validates_source_id_before_writes():
    router_module = __import__("backend.routers.matching_admin", fromlist=["create_matching_rule", "update_matching_rule"])
    create_source = inspect.getsource(router_module.create_matching_rule)
    update_source = inspect.getsource(router_module.update_matching_rule)

    assert "ensure_matching_rule_source_exists" in create_source
    assert "ensure_matching_rule_source_exists" in update_source


def test_passport_suggestion_create_validates_payload_shape():
    router_module = __import__("backend.routers.profile_skin", fromlist=["PassportSuggestionCreate"])
    validation_module = __import__("backend.core.passport_suggestion_validation", fromlist=["validate_passport_suggestion_payload"])

    valid = router_module.PassportSuggestionCreate(
        suggestion_type="append_insight",
        proposed_value={"normalized_value": "damaged_barrier"},
        source_type="chat_text",
    )
    validation_module.validate_passport_suggestion_payload(valid)

    invalid = router_module.PassportSuggestionCreate(
        suggestion_type="append_insight",
        proposed_value={"unexpected": "value"},
        source_type="chat_text",
    )
    with pytest.raises(ValueError):
        validation_module.validate_passport_suggestion_payload(invalid)


def test_passport_suggestion_update_is_single_decision_and_applies_field_updates():
    source = inspect.getsource(
        __import__("backend.core.passport_updates", fromlist=["update_passport_suggestion_for_user"]).update_passport_suggestion_for_user
    )

    assert "existing[\"status\"] != \"proposed\"" in source
    assert "\u041f\u0440\u0435\u0434\u043b\u043e\u0436\u0435\u043d\u0438\u0435 \u0443\u0436\u0435 \u043e\u0431\u0440\u0430\u0431\u043e\u0442\u0430\u043d\u043e" in source
    assert "apply_passport_field_update" in source
    route_source = inspect.getsource(__import__("backend.routers.profile_skin", fromlist=["update_passport_suggestion"]).update_passport_suggestion)
    assert "async with conn.transaction():" in route_source


def test_passport_suggestion_update_rejects_any_processed_suggestion_and_locks_row():
    source = inspect.getsource(
        __import__("backend.core.passport_updates", fromlist=["update_passport_suggestion_for_user"]).update_passport_suggestion_for_user
    )

    assert "FOR UPDATE" in source
    assert "existing[\"status\"] != \"proposed\"" in source
    assert "WHERE id=$2 AND user_id=$3" in source


def test_passport_suggestion_patch_requires_terminal_decision():
    validation_module = __import__("backend.core.passport_suggestion_validation", fromlist=["validate_passport_suggestion_decision_status"])
    router_module = __import__("backend.routers.profile_skin", fromlist=["update_passport_suggestion"])

    assert validation_module.validate_passport_suggestion_decision_status("accepted") == "accepted"
    with pytest.raises(ValueError):
        validation_module.validate_passport_suggestion_decision_status("proposed")

    source = inspect.getsource(router_module.update_passport_suggestion)
    assert "validate_passport_suggestion_decision_status" in source


def test_passport_field_update_locks_profile_row():
    source = inspect.getsource(__import__("backend.core.passport_updates", fromlist=["apply_passport_field_update"]).apply_passport_field_update)

    assert "FOR UPDATE" in source


def test_passport_field_update_locks_user_before_profile_upsert():
    source = inspect.getsource(__import__("backend.core.passport_updates", fromlist=["apply_passport_field_update"]).apply_passport_field_update)

    assert "SELECT id FROM users WHERE id=$1 FOR UPDATE" in source
    assert source.index("SELECT id FROM users WHERE id=$1 FOR UPDATE") < source.index("SELECT extra_data FROM user_profiles")


def test_passport_update_field_suggestion_requires_values_at_create_time():
    router_module = __import__("backend.routers.profile_skin", fromlist=["PassportSuggestionCreate"])
    validation_module = __import__("backend.core.passport_suggestion_validation", fromlist=["validate_passport_suggestion_payload"])
    invalid = router_module.PassportSuggestionCreate(
        suggestion_type="update_field",
        target_field="concerns",
        proposed_value={},
        source_type="chat_text",
    )

    with pytest.raises(ValueError):
        validation_module.validate_passport_suggestion_payload(invalid)


def test_passport_update_field_suggestion_rejects_blank_scalar_value():
    main = __import__("backend.routers.profile_skin", fromlist=["PassportSuggestionCreate", "validate_passport_suggestion_payload"])
    invalid = main.PassportSuggestionCreate(
        suggestion_type="update_field",
        target_field="concerns",
        proposed_value={"value": "   "},
        source_type="chat_text",
    )

    with pytest.raises(ValueError):
        main.validate_passport_suggestion_payload(invalid)


def test_passport_suggestion_response_decodes_jsonb_values():
    main = __import__("backend.core.passport_updates", fromlist=["passport_suggestion_response"])
    row = {
        "id": 1,
        "old_value": json.dumps({"value": "old"}),
        "proposed_value": json.dumps({"value": "new"}),
    }

    assert main.passport_suggestion_response(row)["proposed_value"] == {"value": "new"}


@pytest.mark.anyio
async def test_matching_rule_source_validation_rejects_missing_source():
    class FakeConn:
        async def fetchval(self, query, source_id):
            return False

    main = __import__("backend.core.matching.helpers", fromlist=["ensure_matching_rule_source_exists"])
    with pytest.raises(HTTPException) as exc_info:
        await main.ensure_matching_rule_source_exists(FakeConn(), 999)

    assert exc_info.value.status_code == 400


def test_build_rag_query_payload_uses_authenticated_user_id():
    payload = build_rag_query_payload(query="  Как использовать ретинол?  ", user_id=42, session_id=7)

    assert payload == {
        "query": "Как использовать ретинол?",
        "user_id": "42",
        "session_id": "7",
        "max_results": 5,
    }


def test_build_rag_query_payload_includes_skin_passport_context():
    payload = build_rag_query_payload(
        query="Что мне использовать?",
        user_id=42,
        session_id=7,
        skin_passport={"answers": {"skin_type": ["сухая"]}},
    )

    assert payload["context"] == {"skin_passport": {"answers": {"skin_type": ["сухая"]}}}


def test_build_rag_query_payload_includes_recent_chat_history():
    payload = build_rag_query_payload(
        query="А как часто использовать?",
        user_id=42,
        session_id=7,
        chat_history=[
            {"role": "user", "content": "Расскажи про сыворотку с ретинолом"},
            {"role": "assistant", "content": "Это средство лучше вводить постепенно."},
        ],
    )

    assert payload["context"]["chat_history"] == [
        {"role": "user", "content": "Расскажи про сыворотку с ретинолом"},
        {"role": "assistant", "content": "Это средство лучше вводить постепенно."},
    ]


def test_build_rag_query_payload_includes_recommendation_context():
    payload = build_rag_query_payload(
        query="Какие продукты мне подходят?",
        user_id=42,
        session_id=7,
        recommendation_context={
            "status": "available",
            "products": [
                {"product_id": 75, "product_name": "Vinoclean Moisturizing Toner", "brand": "Caudalie"},
            ],
        },
    )

    assert payload["context"]["recommendation_context"] == {
        "status": "available",
        "products": [
            {"product_id": 75, "product_name": "Vinoclean Moisturizing Toner", "brand": "Caudalie"},
        ],
    }


def test_assistant_knowledge_reindex_route_triggers_real_reindex():
    source = inspect.getsource(__import__("backend.routers.assistant_knowledge", fromlist=["reindex_knowledge_sources"]).reindex_knowledge_sources)

    assert "reindex_knowledge_sources_to_ai" in source
    assert "count_reindexable_knowledge_sources" not in source


def test_build_rag_query_payload_compacts_product_context_media():
    payload = build_rag_query_payload(
        query="Подходит ли мне продукт?",
        user_id=42,
        product_context={
            "product": {
                "id": 75,
                "name": "Vinoclean Moisturizing Toner",
                "composition": "Aqua, Glycerin",
                "photos": [{"filename": "front.jpg", "data": "x" * 100_000}],
                "videos": [{"filename": "demo.mp4", "data": "y" * 100_000}],
            },
            "matching": {"compatibility_percent": 88, "explanations": ["увлажнение"]},
            "user_context": {"skin_passport": {"answers": {"skin_type": ["сухая"]}}},
        },
    )

    compact = payload["context"]["product_context"]
    assert compact["product"] == {
        "id": 75,
        "name": "Vinoclean Moisturizing Toner",
        "composition": "Aqua, Glycerin",
    }
    assert compact["matching"] == {"compatibility_percent": 88, "explanations": ["увлажнение"]}
    assert "photos" not in json.dumps(compact, ensure_ascii=False)
    assert "videos" not in json.dumps(compact, ensure_ascii=False)
    assert len(json.dumps(payload, ensure_ascii=False)) < 20_000


def test_build_rag_query_payload_rejects_blank_query():
    with pytest.raises(HTTPException) as exc_info:
        build_rag_query_payload(query="   ", user_id=42)

    assert exc_info.value.status_code == 400
    assert exc_info.value.detail == "\u041f\u0443\u0441\u0442\u043e\u0439 \u0432\u043e\u043f\u0440\u043e\u0441"


def test_map_rag_proxy_error_hides_upstream_details():
    error = map_rag_proxy_error(RuntimeError("openrouter secret failure"))

    assert isinstance(error, HTTPException)
    assert error.status_code == 502
    assert error.detail == "AI \u0441\u0435\u0440\u0432\u0438\u0441 \u0432\u0440\u0435\u043c\u0435\u043d\u043d\u043e \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d"


def test_normalize_rag_source_accepts_relevance_as_score():
    source = normalize_rag_source(
        {
            "id": "doc-1",
            "title": "Retinol",
            "content": "Use at night",
            "relevance": 0.87,
        }
    )

    assert source.score == 0.87


def test_chat_session_summary_uses_integer_id():
    summary = ChatSessionSummary(id=7, title="Уход")

    assert summary.id == 7


def test_chat_session_create_response_uses_top_level_session_id_and_title():
    response = ChatSessionCreateResponse(session_id=7, title="Новый чат")

    assert response.model_dump() == {"session_id": 7, "title": "Новый чат"}


def test_chat_session_message_exposes_role_content_timestamp():
    message = ChatSessionMessage(role="assistant", content="Наносите вечером", timestamp="09:07")

    assert message.model_dump() == {
        "role": "assistant",
        "content": "Наносите вечером",
        "timestamp": "09:07",
    }


def test_chat_session_detail_route_uses_integer_session_id():
    assert get_chat_session_detail.__annotations__["session_id"] is int


def test_chat_session_detail_supports_legacy_message_columns():
    source = inspect.getsource(
        __import__("backend.core.chat_sessions", fromlist=["get_chat_session_detail_for_user"]).get_chat_session_detail_for_user
    )

    assert "COALESCE(content, text, '') AS content" in source
    assert "CASE WHEN is_from_user THEN 'user' ELSE 'assistant' END" in source


def test_rag_chat_request_accepts_message_and_session_id():
    request = RagChatRequest(message="Как использовать ретинол?", session_id=7)

    assert request.message == "Как использовать ретинол?"
    assert request.session_id == 7


def test_rag_chat_response_exposes_session_id():
    response = RagChatResponse(answer="Наносите вечером", session_id=7)

    assert response.session_id == 7


def test_chat_attachment_response_contract():
    response = ChatAttachmentResponse(
        attachment_id=3,
        session_id=7,
        filename="routine.pdf",
        content_type="application/pdf",
        status="ready",
        summary="описание",
    )

    assert response.model_dump() == {
        "attachment_id": 3,
        "session_id": 7,
        "filename": "routine.pdf",
        "content_type": "application/pdf",
        "status": "ready",
        "summary": "описание",
    }


def test_chat_attachment_content_type_validation():
    assert validate_chat_attachment_content_type("application/pdf") == "application/pdf"
    assert validate_chat_attachment_content_type("application/vnd.openxmlformats-officedocument.wordprocessingml.document") == "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    assert validate_chat_attachment_content_type("image/jpeg") == "image/jpeg"
    assert validate_chat_attachment_content_type("image/png") == "image/png"
    with pytest.raises(HTTPException) as exc_info:
        validate_chat_attachment_content_type("text/plain")
    assert exc_info.value.status_code == 400


def test_chat_attachment_storage_path_is_user_scoped():
    path = build_attachment_storage_path(user_id=42, session_id=7, attachment_id=3, filename="routine.pdf")

    assert "chat_attachments" in path
    assert "user_42" in path
    assert "session_7" in path
    assert path.endswith("3_routine.pdf")


def test_chat_attachment_schema_created_at_startup():
    source = inspect.getsource(__import__("backend.main", fromlist=["init_db"]).init_db)

    assert "CREATE TABLE IF NOT EXISTS chat_attachments" in source
    assert "indexed_at TIMESTAMP" in source
    assert "session_id INTEGER NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE" in source


def test_product_matching_schema_created_at_startup():
    source = inspect.getsource(__import__("backend.main", fromlist=["init_db"]).init_db)

    assert "CREATE TABLE IF NOT EXISTS ingredients" in source
    assert "CREATE TABLE IF NOT EXISTS product_ingredients" in source
    assert "CREATE TABLE IF NOT EXISTS matching_rules" in source
    assert "CREATE TABLE IF NOT EXISTS passport_update_suggestions" in source
    assert "verification_status VARCHAR(50) DEFAULT 'auto_created'" in source
    assert "status VARCHAR(50) DEFAULT 'draft'" in source


def test_chat_attachment_routes_validate_session_ownership():
    upload_source = inspect.getsource(upload_chat_session_attachment)
    list_source = inspect.getsource(get_chat_session_attachments)
    core_source = inspect.getsource(
        __import__("backend.core.chat_sessions", fromlist=["ensure_chat_session_owned_by_user"]).ensure_chat_session_owned_by_user
    )

    assert "ensure_chat_session_owned_by_user" in upload_source
    assert "ensure_chat_session_owned_by_user" in list_source
    assert "WHERE id=$1 AND user_id=$2" in core_source
    assert "ingest_chat_attachment" in upload_source


def test_chat_attachment_ingest_uses_json_base64_payload():
    source = inspect.getsource(ingest_chat_attachment)

    assert "base64.b64encode" in source
    assert "content_base64" in source
    assert "json=payload" in source
    assert "aiohttp.FormData" not in source



def test_rag_chat_persists_after_upstream_call_in_transaction():
    source = inspect.getsource(query_rag_chat)

    assert source.index("rag_response = await query_ai_service_rag") < source.index("INSERT INTO chat_messages")
    assert "async with conn.transaction():" in source
    assert source.count("except Exception as error") == 1
    assert source.index("except Exception as error") < source.index("async with conn.transaction():")


def test_rag_chat_payload_includes_skin_passport_before_ai_call():
    source = inspect.getsource(query_rag_chat)

    assert "load_skin_passport_context" in source
    assert source.index("skin_passport") < source.index("rag_response = await query_ai_service_rag")


def test_rag_chat_route_resolves_catalog_product_context_before_ai_call():
    source = inspect.getsource(query_rag_chat)

    assert "resolve_catalog_product_context" in source
    assert source.index("resolved_product_context") < source.index("rag_response = await query_ai_service_rag")


def test_build_chat_session_title_trims_whitespace():
    assert build_chat_session_title("  Как использовать ретинол вечером?  ") == "Как использовать ретинол вечером?"


def test_build_chat_session_title_falls_back_for_blank_text():
    assert build_chat_session_title("   ") == "\u041d\u043e\u0432\u044b\u0439 \u0447\u0430\u0442"


def test_build_chat_session_title_truncates_to_80_characters():
    assert len(build_chat_session_title("а" * 120)) == 80


def test_format_chat_timestamp_extracts_hour_and_minute():
    assert format_chat_timestamp("2026-04-28T09:07:33") == "09:07"


def test_product_ingredient_sync_helper_uses_composition_field():
    core_module = __import__("backend.core.product_admin", fromlist=["sync_product_ingredients"])
    router_module = __import__("backend.routers.product_catalog", fromlist=["create_product", "update_product"])
    source = inspect.getsource(core_module.sync_product_ingredients)

    assert "parse_inci_ingredients" in source
    assert "composition" in inspect.getsource(router_module.create_product)
    assert "sync_product_ingredients" in inspect.getsource(router_module.create_product)
    assert "sync_product_ingredients" in inspect.getsource(router_module.update_product)


def test_product_ingredient_sync_updates_function_profiles():
    core_product_module = __import__("backend.core.product_admin", fromlist=["sync_product_ingredients"])
    ingredient_module = __import__("backend.core.ingredient_knowledge_admin", fromlist=["rebuild_product_function_profile"])
    sync_source = inspect.getsource(core_product_module.sync_product_ingredients)
    profile_source = inspect.getsource(ingredient_module.rebuild_product_function_profile)

    assert "rebuild_product_function_profile" in sync_source
    assert "product_function_profiles" in profile_source
    assert "ingredient_evidence" in profile_source
    assert "auto_high_confidence" in profile_source
    assert "confirmed" in profile_source


def test_compute_product_function_profile_entries_scores_positive_weight():
    main = __import__("backend.core.ingredient_knowledge_admin", fromlist=["compute_product_function_profile_entries"])

    entries = main.compute_product_function_profile_entries([
        {
            "effect_key": "hydration",
            "matching_weight_delta": 8,
            "confidence": 0.75,
            "evidence_status": "auto_high_confidence",
            "source_id": 7,
        }
    ])

    assert entries == [{
        "function_key": "hydration",
        "score": 0.195,
        "evidence_status": "auto_only",
        "evidence_count": 1,
        "source_ids": [7],
    }]
    assert entries[0]["score"] > 0


def test_product_admin_uses_package_matching_import():
    source = inspect.getsource(__import__("backend.core.product_admin", fromlist=["sync_product_ingredients"]))

    assert "from backend.core.matching.domain import parse_inci_ingredients" in source


def test_product_ingredient_sync_runs_in_product_transactions():
    main = __import__("backend.routers.product_catalog", fromlist=["create_product", "update_product"])
    create_source = inspect.getsource(main.create_product)
    update_source = inspect.getsource(main.update_product)

    assert "async with conn.transaction():" in create_source
    assert create_source.index("insert_product_row") < create_source.index("sync_product_ingredients")
    assert "async with conn.transaction():" in update_source
    assert update_source.index("update_product_row") < update_source.index("sync_product_ingredients")


def test_recommendation_routes_exist():
    pairs = _route_pairs()

    assert ("POST", "/api/recommendations/generate") in pairs
    assert ("POST", "/api/recommendations/favorites") in pairs
    assert ("GET", "/api/recommendations/favorites") in pairs


def test_generate_recommendation_uses_authenticated_current_user_only():
    router_module = __import__("backend.routers.matching_admin", fromlist=["generate_recommendation_for_current_user"])
    core_module = __import__("backend.core.recommendations.generation", fromlist=["build_recommendation_response"])
    route_source = inspect.getsource(router_module.generate_recommendation_for_current_user)
    source = inspect.getsource(core_module.build_recommendation_response)

    assert "Depends(get_current_user)" in route_source
    assert "current_user[\"id\"]" in route_source
    assert "payload.user_id" not in source
    assert "build_recommendation" in source


def test_generate_recommendation_loads_function_profiles_for_products():
    source = inspect.getsource(
        __import__("backend.core.recommendations.generation", fromlist=["build_recommendation_response"]).build_recommendation_response
    )

    assert "load_product_function_signals" in source
    assert "function_signals_by_product" in source


def test_matching_products_route_uses_shared_function_signal_loader():
    source = inspect.getsource(__import__("backend.routers.matching_admin", fromlist=["match_products_for_current_user"]).match_products_for_current_user)

    assert "load_product_function_signals" in source
    assert "product_function_profiles" not in source


def test_recommendation_favorites_are_stored_in_user_profile_extra_data():
    router_module = __import__("backend.routers.matching_admin", fromlist=["save_recommendation_favorite"])
    core_module = __import__("backend.core.matching.recommendations", fromlist=["save_recommendation_favorite_record"])
    route_source = inspect.getsource(router_module.save_recommendation_favorite)
    source = inspect.getsource(core_module.save_recommendation_favorite_record)

    assert "Depends(get_current_user)" in route_source
    assert "recommendation_favorites" in source
    assert "user_profiles" in source
    assert "ON CONFLICT (user_id)" in source


def test_recommendation_favorites_are_loaded_from_current_user_profile():
    router_module = __import__("backend.routers.matching_admin", fromlist=["list_recommendation_favorites"])
    core_module = __import__("backend.core.matching.recommendations", fromlist=["list_recommendation_favorites_for_user"])
    route_source = inspect.getsource(router_module.list_recommendation_favorites)
    source = inspect.getsource(core_module.list_recommendation_favorites_for_user)

    assert "Depends(get_current_user)" in route_source
    assert "current_user[\"id\"]" in route_source
    assert "recommendation_favorites" in source
    assert "user_profiles" in source
    assert "items" in source


def test_product_detail_route_exists_and_uses_matching_context():
    pairs = _route_pairs()
    assert ("GET", "/api/products/{product_id:int}") in pairs
    route_source = inspect.getsource(__import__("backend.routers.product_catalog", fromlist=["get_product"]).get_product)
    source = inspect.getsource(__import__("backend.core.product_detail", fromlist=["build_product_detail_response"]).build_product_detail_response)
    assert "build_product_detail_response" in route_source
    assert "match_product" in source
    assert "score_breakdown" in source
    assert "extended_skin_profile" in source


def test_product_detail_route_loads_function_profiles_for_matching():
    source = inspect.getsource(__import__("backend.core.product_detail", fromlist=["build_product_detail_response"]).build_product_detail_response)

    assert "load_product_function_signals" in source
    assert "function_signals=function_signals_by_product.get(row[\"id\"], [])" in source


def test_product_detail_assistant_context_contains_product_matching_and_user_context():
    source = inspect.getsource(__import__("backend.core.product_detail", fromlist=["build_product_detail_response"]).build_product_detail_response)

    assert '"product": product_response' in source
    assert '"matching": matching_payload' in source
    assert '"user_context"' in source
    assert '"skin_passport": skin_passport' in source
    assert '"extended_skin_profile": extended_skin_profile' in source
    assert '"accepted_insights": accepted_insights' in source


def test_product_matching_route_uses_extended_skin_profile_context():
    route_source = inspect.getsource(__import__("backend.routers.matching_admin", fromlist=["match_products_for_current_user"]).match_products_for_current_user)
    source = inspect.getsource(__import__("backend.core.product_matching", fromlist=["build_product_matching_response"]).build_product_matching_response)

    assert "load_user_skin_journal" in route_source
    assert "build_extended_skin_profile" in source
    assert "skin_state=extended_skin_profile" in source


def test_product_matching_route_normalizes_product_list_fields():
    source = inspect.getsource(__import__("backend.core.product_matching", fromlist=["build_product_matching_response"]).build_product_matching_response)

    assert '", ".join(coerce_list_field(row["purpose"]))' in source
    assert '", ".join(coerce_list_field(row["skin_type"]))' in source


def test_product_matching_route_filters_low_compatibility_items():
    main = __import__("backend.core.matching.helpers", fromlist=["filter_product_match_results"])
    schema_module = __import__("backend.schemas.matching", fromlist=["ProductMatchingRequest"])

    payload = schema_module.ProductMatchingRequest(limit=10)
    results = [
        {"product_id": 1, "compatibility_percent": 5, "decision": "caution"},
        {"product_id": 2, "compatibility_percent": 55, "decision": "recommend"},
        {"product_id": 3, "compatibility_percent": 0, "decision": "exclude"},
    ]

    assert main.filter_product_match_results(results, payload) == [results[1]]


def test_rag_chat_request_accepts_product_context():
    fields = RagChatRequest.model_fields
    assert "product_context" in fields
    source = inspect.getsource(build_rag_query_payload)
    assert "product_context" in source


def test_product_response_normalizes_json_string_list_fields():
    main = __import__("backend.core.products", fromlist=["normalize_product_response"])

    response = main.normalize_product_response({
        "id": 1,
        "name": "Cream",
        "purpose": '["Восстановление", "Увлажнение"]',
        "skin_type": '["Сухая"]',
    })

    assert response["purpose"] == ["Восстановление", "Увлажнение"]
    assert response["skin_type"] == ["Сухая"]


def test_product_list_route_omits_heavy_media_columns():
    main = __import__("backend.routers.product_catalog", fromlist=["get_products"])
    source = inspect.getsource(main.get_products)

    assert "SELECT * FROM products" not in source
    assert "images" not in source
    assert " video" not in source


def test_product_list_route_exposes_lightweight_thumbnail_url():
    main = __import__("backend.routers.product_catalog", fromlist=["get_products"])
    source = inspect.getsource(main.get_products)

    assert "product_photos" in source
    assert "thumbnail_url" in source
    assert '"photos"' not in source
    assert '"data"' not in source


def test_product_photo_payload_has_inline_size_guard():
    main = __import__("backend.core.products", fromlist=["load_product_photos_payload", "MAX_INLINE_PRODUCT_PHOTO_BYTES"])
    media_module = __import__("backend.routers.product_media", fromlist=["get_product_photos"])

    assert main.MAX_INLINE_PRODUCT_PHOTO_BYTES <= 5 * 1024 * 1024
    assert "MAX_INLINE_PRODUCT_PHOTO_BYTES" in inspect.getsource(main.load_product_photos_payload)
    assert "MAX_INLINE_PRODUCT_PHOTO_BYTES" in inspect.getsource(media_module.get_product_photos)


def test_product_photo_payload_includes_stream_url_for_non_inline_photos():
    main = __import__("backend.core.products", fromlist=["load_product_photos_payload"])
    media_module = __import__("backend.routers.product_media", fromlist=["get_product_photos"])

    payload_source = inspect.getsource(main.load_product_photos_payload)
    endpoint_source = inspect.getsource(media_module.get_product_photos)

    assert '"url"' in payload_source
    assert '"url"' in endpoint_source
    assert '"data": ""' in payload_source
    assert '"data": ""' in endpoint_source


def test_product_detail_route_embeds_gallery_media():
    source = inspect.getsource(__import__("backend.core.product_detail", fromlist=["build_product_detail_response"]).build_product_detail_response)

    assert "load_product_photos_payload(photo_rows, include_data=False)" in source
    assert "load_product_video_payload(row[\"id\"], row[\"video\"], include_data=False)" in source
    assert 'product_response["photos"]' in source
    assert 'product_response["video"]' in source


def test_init_db_creates_ingredient_aliases_and_function_profiles():
    source = __import__("backend.db.schema", fromlist=["MAIN_SCHEMA_SQL"]).MAIN_SCHEMA_SQL

    assert "CREATE TABLE IF NOT EXISTS ingredient_aliases" in source
    assert "ingredient_id INTEGER NOT NULL REFERENCES ingredients(id) ON DELETE CASCADE" in source
    assert "normalized_key VARCHAR(255) NOT NULL" in source
    assert "CREATE UNIQUE INDEX IF NOT EXISTS idx_ingredient_aliases_normalized_key" in source
    assert "CREATE TABLE IF NOT EXISTS product_function_profiles" in source
    assert "function_key VARCHAR(100) NOT NULL" in source
    assert "evidence_status VARCHAR(50) DEFAULT 'auto_only'" in source
    assert "PRIMARY KEY (product_id, function_key)" in source


def test_ingredient_evidence_has_fact_columns_for_matching():
    source = __import__("backend.db.schema_specs", fromlist=["INGREDIENT_EVIDENCE_MIGRATION_STATEMENTS"]).INGREDIENT_EVIDENCE_MIGRATION_STATEMENTS

    required_columns = [
        "ALTER TABLE ingredients ADD COLUMN IF NOT EXISTS evidence_status VARCHAR(50) DEFAULT 'draft'",
        "ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS ingredient_id INTEGER",
        "ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS ingredient_key VARCHAR(255)",
        "ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS fact_type VARCHAR(50)",
        "ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS effect_key VARCHAR(100)",
        "ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS condition_type VARCHAR(100)",
        "ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS condition_value VARCHAR(255)",
        "ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS matching_effect VARCHAR(50)",
        "ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS matching_weight_delta DOUBLE PRECISION DEFAULT 0",
        "ALTER TABLE ingredient_evidence ADD COLUMN IF NOT EXISTS evidence_status VARCHAR(50) DEFAULT 'draft'",
    ]
    for column_sql in required_columns:
        assert column_sql in source


def test_ingredient_evidence_has_unique_identity_index():
    source = "\n".join(__import__("backend.db.schema_specs", fromlist=["INGREDIENT_EVIDENCE_CLEANUP_STATEMENTS"]).INGREDIENT_EVIDENCE_CLEANUP_STATEMENTS)

    assert "UPDATE ingredient_evidence" in source
    assert "condition_type=COALESCE(condition_type, '')" in source
    assert "condition_value=COALESCE(condition_value, '')" in source
    assert "evidence_quote=COALESCE(evidence_quote, '')" in source
    assert "ROW_NUMBER() OVER" in source
    assert "DELETE FROM ingredient_evidence" in source
    assert "CREATE UNIQUE INDEX IF NOT EXISTS idx_ingredient_evidence_extracted_identity" in source
    assert "ON ingredient_evidence(ingredient_id, source_id, effect_key, condition_type, condition_value, evidence_quote)" in source


def test_ingredient_knowledge_admin_imports_extraction_helpers():
    source = inspect.getsource(__import__("backend.core.ingredient_knowledge_admin", fromlist=["extract_ingredient_facts_from_source"]))
    assert "extract_ingredient_facts" in source
    assert "resolve_seed_alias" in source


def test_extract_ingredient_facts_from_source_persists_aliases_and_evidence():
    main = __import__("backend.core.ingredient_knowledge_admin", fromlist=["extract_ingredient_facts_from_source"])
    source = inspect.getsource(main.extract_ingredient_facts_from_source)

    assert "INSERT INTO ingredients" in source
    assert "INSERT INTO ingredient_aliases" in source
    assert "INSERT INTO ingredient_evidence" in source
    assert "ON CONFLICT" in source
    assert "CASE" in source
    assert "ingredients.evidence_status IN ('confirmed', 'auto_high_confidence')" in source
    assert "ON CONFLICT (ingredient_id, source_id, effect_key, condition_type, condition_value, evidence_quote) DO NOTHING" in source
    assert "fact.condition_type or \"\"" in source
    assert "fact.condition_value or \"\"" in source
    assert "fact.evidence_quote or \"\"" in source
    assert "raw_ingredient_name" not in source
    assert "fact.matching_weight_delta" in source
    assert "matched_alias" in source
    assert "resolve_seed_alias" in source
    assert "SELECT DISTINCT product_id" in source
    assert "rebuild_product_function_profile" in source
    assert "auto_high_confidence" in source


@pytest.mark.anyio
async def test_extract_ingredient_facts_from_source_uses_actual_fact_fields(monkeypatch):
    from backend.ingredient_knowledge import ExtractedIngredientFact

    main = __import__("backend.core.ingredient_knowledge_admin", fromlist=["extract_ingredient_facts_from_source"])

    fact = ExtractedIngredientFact(
        ingredient_key="glycerin",
        effect_key="hydration",
        condition_type=None,
        condition_value=None,
        matching_effect="boost",
        confidence=0.7,
        evidence_status="draft",
        source_id=7,
        source_title="source.txt",
        evidence_quote="",
        matching_weight_delta=8,
        matched_alias="\u0433\u043b\u0438\u0446\u0435\u0440\u0438\u043d",
    )
    monkeypatch.setattr(main, "extract_ingredient_facts", lambda *args, **kwargs: [fact])
    rebuilt_product_ids = []

    async def fake_rebuild(conn, product_id):
        rebuilt_product_ids.append(product_id)

    monkeypatch.setattr(main, "rebuild_product_function_profile", fake_rebuild)

    class FakeConn:
        def __init__(self):
            self.aliases = []
            self.weight_delta = None

        async def fetchrow(self, query, *args):
            if "SELECT id, title, content FROM knowledge_sources" in query:
                return {"id": 7, "title": "source.txt", "content": "content"}
            if "INSERT INTO ingredients" in query:
                return {"id": 3}
            raise AssertionError(query)

        async def fetch(self, query, *args):
            if "UPDATE product_ingredients" in query and "RETURNING product_id" in query:
                return []
            if "SELECT DISTINCT product_id" in query:
                assert args == (3,)
                return [{"product_id": 42}]
            raise AssertionError(query)

        async def execute(self, query, *args):
            if "INSERT INTO ingredient_aliases" in query:
                self.aliases.append(args[1])
            if "INSERT INTO ingredient_evidence" in query:
                assert args[3] == ""
                assert args[4] == ""
                self.weight_delta = args[6]
                assert args[9] == ""
            return "INSERT 0 1"

    conn = FakeConn()
    assert await main.extract_ingredient_facts_from_source(conn, 7) == {
        "inserted": 1,
        "auto_high_confidence": 0,
        "draft": 1,
    }
    assert conn.weight_delta == 8
    assert "glycerin" in conn.aliases
    assert "glycerol" in conn.aliases
    assert "\u0433\u043b\u0438\u0446\u0435\u0440\u0438\u043d" in conn.aliases
    assert "\u0433\u043b\u0438\u0446\u0435\u0440\u0438\u043d\u0430" in conn.aliases
    assert rebuilt_product_ids == [42]


def test_ensure_knowledge_schema_relaxes_legacy_ingredient_evidence_columns():
    source = __import__("backend.db.schema_specs", fromlist=["KNOWLEDGE_SCHEMA_ALTERS"]).KNOWLEDGE_SCHEMA_ALTERS

    assert "ALTER TABLE ingredient_evidence ALTER COLUMN direction DROP NOT NULL" in source
    assert "ALTER TABLE ingredient_evidence ALTER COLUMN strength DROP NOT NULL" in source
    assert "ALTER TABLE ingredient_evidence ALTER COLUMN summary DROP NOT NULL" in source


def test_product_admin_update_keeps_category_in_sync():
    module = __import__("backend.core.product_admin", fromlist=["update_product_row"])
    source = inspect.getsource(module.update_product_row)

    assert "sync_product_dictionary_refs" in source
    assert '"category": product.category if product.category is not None else existing["category"]' in source
    assert 'SELECT * FROM ({product_select_sql(\'p\')}) AS hydrated_products WHERE id=$1' in source


def test_import_procedures_review_syncs_dictionary_refs():
    module = __import__("backend.import_procedures_review", fromlist=["import_records"])
    source = inspect.getsource(module.import_records)

    assert "sync_procedure_dictionary_refs" in source
    assert 'row = await conn.fetchrow("SELECT * FROM procedures WHERE id=$1", existing_id)' in source
    assert "await sync_procedure_dictionary_refs(conn, row)" in source


def test_initialize_database_schema_adds_fk_support_indexes():
    source = __import__("backend.db.schema_specs", fromlist=["FK_SUPPORT_INDEX_STATEMENTS"]).FK_SUPPORT_INDEX_STATEMENTS

    assert "CREATE INDEX IF NOT EXISTS idx_users_role_id ON users(role_id)" in source
    assert "CREATE INDEX IF NOT EXISTS idx_content_category_id ON content(category_id)" in source
    assert "CREATE INDEX IF NOT EXISTS idx_recommendation_feedback_product_ref_id ON recommendation_feedback(product_ref_id)" in source
    assert "CREATE INDEX IF NOT EXISTS idx_products_brand_id ON products(brand_id)" in source
    assert "CREATE INDEX IF NOT EXISTS idx_products_category_id ON products(category_id)" in source
    assert "CREATE INDEX IF NOT EXISTS idx_procedures_direction_id ON procedures(direction_id)" in source
    assert "CREATE INDEX IF NOT EXISTS idx_product_purpose_links_purpose_id ON product_purpose_links(purpose_id)" in source
    assert "CREATE INDEX IF NOT EXISTS idx_procedure_problem_links_problem_id ON procedure_problem_links(problem_id)" in source


def test_initialize_database_schema_adds_reference_fks_for_users_content_and_feedback():
    specs = __import__(
        "backend.db.schema_specs",
        fromlist=["USER_CONTENT_REFERENCE_CONSTRAINTS", "EXTRA_REFERENCE_CONSTRAINT_STATEMENTS"],
    )
    source = "\n".join(specs.EXTRA_REFERENCE_CONSTRAINT_STATEMENTS)

    assert ("users", "users_role_id_fkey", "role_id", "user_roles") in specs.USER_CONTENT_REFERENCE_CONSTRAINTS
    assert ("content", "content_category_id_fkey", "category_id", "content_categories") in specs.USER_CONTENT_REFERENCE_CONSTRAINTS
    assert "recommendation_feedback_product_ref_id_fkey" in source
    assert "FOREIGN KEY (product_ref_id) REFERENCES products(id) ON DELETE SET NULL" in source


def test_auth_and_users_sync_role_dictionary_refs():
    auth_module = __import__("backend.routers.auth", fromlist=["register"])
    users_module = __import__("backend.routers.users", fromlist=["create_user", "update_user"])

    assert "sync_user_role_ref" in inspect.getsource(auth_module.register)
    assert "sync_user_role_ref" in inspect.getsource(users_module.create_user)
    assert "sync_user_role_ref" in inspect.getsource(users_module.update_user)


def test_content_router_syncs_category_dictionary_refs():
    module = __import__("backend.routers.content", fromlist=["create_content", "update_content"])

    assert "sync_content_category_ref" in inspect.getsource(module.create_content)
    assert "sync_content_category_ref" in inspect.getsource(module.update_content)
    assert "sync_content_tag_refs" in inspect.getsource(module.create_content)
    assert "sync_content_tag_refs" in inspect.getsource(module.update_content)


def test_content_tags_schema_is_created_and_indexed():
    schema = __import__("backend.db.schema_sql", fromlist=["MAIN_SCHEMA_SQL"])
    specs = __import__("backend.db.schema_specs", fromlist=["FK_SUPPORT_INDEX_STATEMENTS", "OBSOLETE_NORMALIZED_DROP_STATEMENTS"])
    refs = __import__("backend.core.entity_dictionary_refs", fromlist=["content_select_sql"])

    assert "CREATE TABLE IF NOT EXISTS content_tags" in schema.MAIN_SCHEMA_SQL
    assert "CREATE TABLE IF NOT EXISTS content_tag_links" in schema.MAIN_SCHEMA_SQL
    assert "CREATE INDEX IF NOT EXISTS idx_content_tag_links_tag_id ON content_tag_links(tag_id)" in specs.FK_SUPPORT_INDEX_STATEMENTS
    assert "ALTER TABLE content DROP COLUMN IF EXISTS tags" in specs.OBSOLETE_NORMALIZED_DROP_STATEMENTS
    assert "FROM content_tag_links content_tag_link" in refs.content_select_sql()


def test_matching_recommendations_syncs_feedback_product_fk():
    module = __import__("backend.core.matching.recommendations", fromlist=["track_recommendation_feedback_record"])
    source = inspect.getsource(module.track_recommendation_feedback_record)

    assert "sync_recommendation_feedback_product_ref" in source


def test_entity_dictionary_refs_parse_product_reference_id():
    module = __import__("backend.core.entity_dictionary_refs", fromlist=["parse_product_reference_id"])

    assert module.parse_product_reference_id("product-42") == 42
    assert module.parse_product_reference_id("42") == 42
    assert module.parse_product_reference_id(7) == 7
    assert module.parse_product_reference_id("other-id") is None


@pytest.mark.anyio
async def test_extract_ingredient_facts_from_source_relinks_products_matching_new_alias(monkeypatch):
    from backend.ingredient_knowledge import ExtractedIngredientFact

    main = __import__("backend.core.ingredient_knowledge_admin", fromlist=["extract_ingredient_facts_from_source"])

    fact = ExtractedIngredientFact(
        ingredient_key="glycerin",
        effect_key="hydration",
        condition_type=None,
        condition_value=None,
        matching_effect="boost",
        confidence=0.8,
        evidence_status="auto_high_confidence",
        source_id=8,
        source_title="source.txt",
        evidence_quote="Glycerin hydrates",
        matching_weight_delta=9,
        matched_alias="\u0433\u043b\u0438\u0446\u0435\u0440\u0438\u043d",
    )
    monkeypatch.setattr(main, "extract_ingredient_facts", lambda *args, **kwargs: [fact])
    rebuilt_product_ids = []

    async def fake_rebuild(conn, product_id):
        rebuilt_product_ids.append(product_id)

    monkeypatch.setattr(main, "rebuild_product_function_profile", fake_rebuild)

    class FakeConn:
        def __init__(self):
            self.relinked_args = None

        async def fetchrow(self, query, *args):
            if "SELECT id, title, content FROM knowledge_sources" in query:
                return {"id": 8, "title": "source.txt", "content": "content"}
            if "INSERT INTO ingredients" in query:
                return {"id": 3}
            raise AssertionError(query)

        async def fetch(self, query, *args):
            if "UPDATE product_ingredients" in query and "RETURNING product_id" in query:
                self.relinked_args = args
                return [{"product_id": 42}]
            if "SELECT DISTINCT product_id" in query:
                return []
            raise AssertionError(query)

        async def execute(self, query, *args):
            if "INSERT INTO ingredient_aliases" in query or "INSERT INTO ingredient_evidence" in query:
                return "INSERT 0 1"
            raise AssertionError(query)

    conn = FakeConn()

    await main.extract_ingredient_facts_from_source(conn, 8)

    assert conn.relinked_args is not None
    assert conn.relinked_args[0] == 3
    assert "glycerin" in conn.relinked_args[1]
    assert "\u0433\u043b\u0438\u0446\u0435\u0440\u0438\u043d" in conn.relinked_args[1]
    assert rebuilt_product_ids == [42]


def test_admin_extraction_endpoint_exists():
    source = inspect.getsource(__import__("backend.routers.matching_admin", fromlist=["extract_ingredient_knowledge_for_source"]).extract_ingredient_knowledge_for_source)
    assert '@router.post("/api/admin/ingredient-knowledge/extract/{source_id}")' in source
    assert "extract_ingredient_facts_from_source" in source


def test_ingredient_knowledge_admin_routes_exist():
    source = inspect.getsource(__import__("backend.routers.matching_admin", fromlist=["list_ingredient_knowledge"]))
    assert '@router.get("/api/admin/ingredient-knowledge/ingredients")' in source
    source = inspect.getsource(__import__("backend.routers.matching_admin", fromlist=["list_ingredient_fact_review_queue"]))
    assert '@router.get("/api/admin/ingredient-knowledge/facts")' in source
    source = inspect.getsource(__import__("backend.routers.matching_admin", fromlist=["update_ingredient_fact_review"]))
    assert '@router.patch("/api/admin/ingredient-knowledge/facts/{fact_id}")' in source
    assert "require_matching_rule_admin" in source


def test_unified_rules_ingredients_nav_uses_manager_visible_content_permission():
    layout_source = Path("web-admin/src/components/Layout/Layout.jsx").read_text(encoding="utf-8")
    auth_source = Path("web-admin/src/context/AuthContext.jsx").read_text(encoding="utf-8")

    assert "path: '/rules-ingredients'" in layout_source
    assert "path: '/rules-ingredients', icon: BookA, label: '\u041f\u0440\u0430\u0432\u0438\u043b\u0430 \u0438 \u0438\u043d\u0433\u0440\u0435\u0434\u0438\u0435\u043d\u0442\u044b', permission: 'content'" in layout_source
    assert "path: '/ingredient-knowledge'" not in layout_source
    assert "'manager': ['dashboard', 'products', 'procedures', 'content'" in auth_source


def test_ingredient_knowledge_list_uses_existing_category_schema_column():
    schema_module = __import__("backend.db.schema", fromlist=["initialize_database_schema"])
    admin_module = __import__("backend.core.matching.admin", fromlist=["list_ingredient_knowledge_for_admin"])
    init_source = schema_module.MAIN_SCHEMA_SQL
    list_source = inspect.getsource(admin_module.list_ingredient_knowledge_for_admin)

    assert "category VARCHAR(100)" in init_source
    assert "i.category AS ingredient_class" in list_source
    assert "i.ingredient_class" not in list_source


def test_ingredient_fact_status_validation_rejects_unknown_status():
    main = __import__("backend.core.matching.helpers", fromlist=["validate_ingredient_fact_evidence_status"])

    assert main.validate_ingredient_fact_evidence_status("confirmed") == "confirmed"
    with pytest.raises(HTTPException) as exc_info:
        main.validate_ingredient_fact_evidence_status("unknown")
    assert exc_info.value.status_code == 400


def test_ingredient_fact_list_validates_status_filter():
    main = __import__("backend.routers.matching_admin", fromlist=["list_ingredient_fact_review_queue"])
    source = inspect.getsource(main.list_ingredient_fact_review_queue)

    assert "validate_ingredient_fact_evidence_status(status)" in source


def test_ingredient_fact_update_rebuilds_profiles_for_affected_products():
    router_module = __import__("backend.routers.matching_admin", fromlist=["update_ingredient_fact_review"])
    core_module = __import__("backend.core.matching.admin", fromlist=["update_ingredient_fact_review_for_admin"])
    route_source = inspect.getsource(router_module.update_ingredient_fact_review)
    source = inspect.getsource(core_module.update_ingredient_fact_review_for_admin)

    assert "async with conn.transaction():" in route_source
    assert "SELECT DISTINCT product_id" in source
    assert "FROM product_ingredients" in source
    assert "row[\"ingredient_id\"]" in source
    assert "rebuild_product_function_profile" in source
    assert source.index("UPDATE ingredient_evidence") < source.index("SELECT DISTINCT product_id")


def test_admin_extraction_endpoint_wraps_persistence_in_transaction():
    source = inspect.getsource(__import__("backend.routers.matching_admin", fromlist=["extract_ingredient_knowledge_for_source"]).extract_ingredient_knowledge_for_source)

    assert "async with conn.transaction():" in source
    assert source.index("async with conn.transaction():") < source.index("extract_ingredient_facts_from_source")


def test_build_fallback_home_weather_uses_existing_shape():
    module = __import__("backend.core.home_status", fromlist=["build_fallback_home_status"])

    status = module.build_fallback_home_status(answers_count=3)

    assert status == {
        "weather": {
            "temperature": "22\u00b0C",
            "uv_index": "UV 3.2",
        },
        "top_widget": {
            "humidity_value": "50%",
            "humidity_subtitle": "\u041e\u043f\u0440\u0435\u0434\u0435\u043b\u0435\u043d\u043e \u043f\u043e \u043f\u0440\u043e\u0444\u0438\u043b\u044e \u043a\u043e\u0436\u0438",
            "air_quality": "\u0425\u043e\u0440\u043e\u0448\u0435\u0435",
            "air_status": "\u0410\u043a\u0442\u0443\u0430\u043b\u044c\u043d\u043e",
            "weather_advice": "\u0412\u043b\u0430\u0436\u043d\u043e\u0441\u0442\u044c \u043a\u043e\u043c\u0444\u043e\u0440\u0442\u043d\u0430\u044f: \u0432\u044b\u0431\u0438\u0440\u0430\u0439\u0442\u0435 \u043b\u0435\u0433\u043a\u043e\u0435 \u0443\u0432\u043b\u0430\u0436\u043d\u0435\u043d\u0438\u0435.",
        },
    }


def test_build_home_status_from_open_meteo_formats_current_weather():
    module = __import__("backend.core.home_status", fromlist=["build_home_status_from_open_meteo"])

    status = module.build_home_status_from_open_meteo(
        {
            "current": {
                "temperature_2m": 21.7,
                "relative_humidity_2m": 63,
            },
            "daily": {
                "uv_index_max": [4.36],
            },
        },
        fallback_air_quality="\u0425\u043e\u0440\u043e\u0448\u0435\u0435",
    )

    assert status["weather"] == {
        "temperature": "22\u00b0C",
        "uv_index": "UV 4.4",
    }
    assert status["top_widget"]["humidity_value"] == "63%"
    assert status["top_widget"]["humidity_subtitle"] == "\u041f\u043e \u0434\u0430\u043d\u043d\u044b\u043c \u043f\u043e\u0433\u043e\u0434\u044b \u0440\u044f\u0434\u043e\u043c \u0441 \u0432\u0430\u043c\u0438"
    assert status["top_widget"]["air_quality"] == "\u0425\u043e\u0440\u043e\u0448\u0435\u0435"
    assert status["top_widget"]["air_status"] == "\u0410\u043a\u0442\u0443\u0430\u043b\u044c\u043d\u043e"
    assert status["top_widget"]["weather_advice"] == "\u0412\u043b\u0430\u0436\u043d\u043e\u0441\u0442\u044c \u043a\u043e\u043c\u0444\u043e\u0440\u0442\u043d\u0430\u044f: \u0432\u044b\u0431\u0438\u0440\u0430\u0439\u0442\u0435 \u043b\u0435\u0433\u043a\u043e\u0435 \u0443\u0432\u043b\u0430\u0436\u043d\u0435\u043d\u0438\u0435."


def test_build_home_status_from_open_meteo_rejects_incomplete_payload():
    module = __import__("backend.core.home_status", fromlist=["build_home_status_from_open_meteo"])

    assert module.build_home_status_from_open_meteo({"current": {}}, fallback_air_quality="\u0425\u043e\u0440\u043e\u0448\u0435\u0435") is None


def test_build_home_weather_advice_prioritizes_high_uv():
    module = __import__("backend.core.home_status", fromlist=["build_home_weather_advice"])

    assert module.build_home_weather_advice(35, 6.1) == "\u0412\u044b\u0441\u043e\u043a\u0438\u0439 UV: \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u0443\u0439\u0442\u0435 SPF-\u0441\u0440\u0435\u0434\u0441\u0442\u0432\u0430 \u0441\u0435\u0433\u043e\u0434\u043d\u044f."


def test_build_home_weather_advice_recommends_hydration_on_low_humidity():
    module = __import__("backend.core.home_status", fromlist=["build_home_weather_advice"])

    assert module.build_home_weather_advice(34, 2.0) == "\u041d\u0438\u0437\u043a\u0430\u044f \u0432\u043b\u0430\u0436\u043d\u043e\u0441\u0442\u044c: \u0434\u043e\u0431\u0430\u0432\u044c\u0442\u0435 \u0431\u043e\u043b\u0435\u0435 \u0438\u043d\u0442\u0435\u043d\u0441\u0438\u0432\u043d\u043e\u0435 \u0443\u0432\u043b\u0430\u0436\u043d\u0435\u043d\u0438\u0435."


def test_build_home_weather_advice_uses_light_hydration_by_default():
    module = __import__("backend.core.home_status", fromlist=["build_home_weather_advice"])

    assert module.build_home_weather_advice(60, 2.0) == "\u0412\u043b\u0430\u0436\u043d\u043e\u0441\u0442\u044c \u043a\u043e\u043c\u0444\u043e\u0440\u0442\u043d\u0430\u044f: \u0432\u044b\u0431\u0438\u0440\u0430\u0439\u0442\u0435 \u043b\u0435\u0433\u043a\u043e\u0435 \u0443\u0432\u043b\u0430\u0436\u043d\u0435\u043d\u0438\u0435."


@pytest.mark.anyio
async def test_fetch_open_meteo_home_status_maps_success(monkeypatch):
    module = __import__("backend.core.home_status", fromlist=["fetch_open_meteo_home_status"])

    class FakeResponse:
        status = 200

        async def __aenter__(self):
            return self

        async def __aexit__(self, exc_type, exc, traceback):
            return False

        async def json(self):
            return {
                "current": {
                    "temperature_2m": 18.2,
                    "relative_humidity_2m": 72,
                },
                "daily": {
                    "uv_index_max": [2.81],
                },
            }

    class FakeSession:
        def __init__(self, timeout):
            self.timeout = timeout

        async def __aenter__(self):
            return self

        async def __aexit__(self, exc_type, exc, traceback):
            return False

        def get(self, url, params):
            assert url == "https://api.open-meteo.com/v1/forecast"
            assert params == {
                "latitude": 55.75,
                "longitude": 37.62,
                "current": "temperature_2m,relative_humidity_2m",
                "daily": "uv_index_max",
                "timezone": "auto",
            }
            return FakeResponse()

    monkeypatch.setattr(module.aiohttp, "ClientSession", FakeSession)

    status = await module.fetch_open_meteo_home_status(
        latitude=55.75,
        longitude=37.62,
        fallback_air_quality="Хорошее",
    )

    assert status["weather"] == {
        "temperature": "18°C",
        "uv_index": "UV 2.8",
    }
    assert status["top_widget"]["humidity_value"] == "72%"


@pytest.mark.anyio
async def test_fetch_open_meteo_home_status_returns_none_on_http_error(monkeypatch):
    module = __import__("backend.core.home_status", fromlist=["fetch_open_meteo_home_status"])

    class FakeResponse:
        status = 500

        async def __aenter__(self):
            return self

        async def __aexit__(self, exc_type, exc, traceback):
            return False

        async def json(self):
            raise AssertionError("HTTP errors should not parse JSON")

    class FakeSession:
        def __init__(self, timeout):
            self.timeout = timeout

        async def __aenter__(self):
            return self

        async def __aexit__(self, exc_type, exc, traceback):
            return False

        def get(self, url, params):
            return FakeResponse()

    monkeypatch.setattr(module.aiohttp, "ClientSession", FakeSession)

    status = await module.fetch_open_meteo_home_status(
        latitude=55.75,
        longitude=37.62,
        fallback_air_quality="Хорошее",
    )

    assert status is None


def test_home_status_accepts_optional_coordinates_and_falls_back():
    module = __import__("backend.routers.home", fromlist=["get_home_status"])
    source = inspect.getsource(module.get_home_status)

    assert "latitude: Optional[float] = Query(None" in source
    assert "longitude: Optional[float] = Query(None" in source
    assert "shared_fetch_open_meteo_home_status" in source
    assert "shared_build_fallback_home_status" in source

