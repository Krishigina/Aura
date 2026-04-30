from pathlib import Path


SHARED_ROOT = Path(__file__).resolve().parents[3]
COMMON = SHARED_ROOT / "src" / "commonMain" / "kotlin" / "com" / "aura"


def test_sensor_onboarding_navigates_to_skin_journal_when_sensor_selected():
    survey = (COMMON / "feature" / "survey" / "SkinSurveyScreen.kt").read_text(encoding="utf-8")
    navigation = (COMMON / "core" / "navigation" / "Navigation.kt").read_text(encoding="utf-8")

    assert "onSensorConnected: () -> Unit" in survey
    assert "if (hasSensor) onSensorConnected() else onComplete()" in survey
    assert "onSensorConnected = {" in navigation
    assert "navController.navigate(Routes.SKIN_JOURNAL)" in navigation


def test_sensor_onboarding_uses_aura_styled_prompt_not_system_dialog():
    source = (COMMON / "feature" / "survey" / "SkinSurveyScreen.kt").read_text(encoding="utf-8")

    assert "AlertDialog" not in source
    assert "SensorOnboardingPrompt(" in source
    assert "Подключить журнал замеров" in source
