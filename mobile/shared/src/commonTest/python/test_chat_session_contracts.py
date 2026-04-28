from pathlib import Path


SHARED_ROOT = Path(__file__).resolve().parents[3]
COMMON = SHARED_ROOT / "src" / "commonMain" / "kotlin" / "com" / "aura"


def test_rag_client_timeout_allows_slow_ai_response():
    source = (COMMON / "core" / "data" / "api" / "AuraApiClient.kt").read_text(encoding="utf-8")

    assert "HttpTimeout" in source
    assert "CHAT_RAG_TIMEOUT_MILLIS" in source
    assert "120_000" in source


def test_chat_screen_persists_active_session_between_tab_changes():
    source = (COMMON / "feature" / "chat" / "ChatScreen.kt").read_text(encoding="utf-8")

    assert "object ChatSessionMemory" in source
    assert "ChatSessionMemory.activeSessionId" in source
    assert "ChatSessionMemory.activeSessionId = rag.sessionId" in source


def test_chat_history_exposes_new_chat_action():
    source = (COMMON / "feature" / "chat" / "ChatSessionsScreen.kt").read_text(encoding="utf-8")
    navigation = (COMMON / "core" / "navigation" / "Navigation.kt").read_text(encoding="utf-8")

    assert "onNewChat" in source
    assert "Новый чат" in source
    assert "onNewChat = {" in navigation
    assert "ChatSessionMemory.clear()" in navigation
    assert "navController.navigate(Routes.CHAT)" in navigation


def test_chat_messages_scroll_to_input_without_manual_height_reservation():
    source = (COMMON / "feature" / "chat" / "ChatScreen.kt").read_text(encoding="utf-8")

    assert ".padding(top = 16.dp, bottom = 164.dp)" not in source
    assert ".padding(bottom = 116.dp)" not in source
    assert ".navigationBarsPadding()" in source
    assert ".imePadding()" in source


def test_chat_history_new_chat_action_is_bottom_button_without_colored_icon():
    source = (COMMON / "feature" / "chat" / "ChatSessionsScreen.kt").read_text(encoding="utf-8")

    assert "Icons.Rounded.ChatBubbleOutline" not in source
    assert "ChatSessionsHeader(" in source
    assert "onBack = onBack" in source
    assert "ChatSessionsNewChatButton(" in source
    assert "onNewChat = onNewChat" in source
    assert ".align(Alignment.BottomCenter)" in source


def test_chat_and_profile_use_translucent_toolbars():
    chat = (COMMON / "feature" / "chat" / "ChatScreen.kt").read_text(encoding="utf-8")
    sessions = (COMMON / "feature" / "chat" / "ChatSessionsScreen.kt").read_text(encoding="utf-8")
    profile = (COMMON / "feature" / "profile" / "ProfileScreen.kt").read_text(encoding="utf-8")
    navigation = (COMMON / "core" / "navigation" / "Navigation.kt").read_text(encoding="utf-8")
    glass = (COMMON / "core" / "ui" / "components" / "GlassBars.kt").read_text(encoding="utf-8")

    assert "const val AuraGlassBarAlpha = 0.24f" in glass
    assert "fun GlassSurface(" in glass
    assert "Color.White.copy(alpha = AuraGlassBarAlpha)" in glass

    assert "GlassSurface(" in navigation
    assert "Color.White.copy(alpha = glassAlpha)" not in navigation
    assert ".fillMaxWidth()" in navigation
    assert ".padding(start = 16.dp, end = 16.dp, bottom = 24.dp)" in navigation
    assert "modifier = Modifier\n                        .fillMaxWidth()\n                        .padding(horizontal = 8.dp, vertical = 8.dp)" in navigation
    assert ".weight(1f)" in navigation
    assert ".fillMaxWidth(0.9f)" not in navigation

    assert "ChatInputDock" in chat
    assert "modifier = Modifier.align(Alignment.TopCenter)" in chat
    assert ".align(Alignment.BottomCenter)" in chat
    assert "GlassSurface(" in chat
    assert "Color.White.copy(alpha = 0.95f)" in chat
    assert "Column(modifier = Modifier.fillMaxSize())" not in chat

    assert "modifier = Modifier.align(Alignment.TopCenter)" in sessions
    assert "GlassSurface(" in sessions
    assert "color = SessionsPrimaryMint" in sessions
    assert "color = SessionsText900" in sessions
    assert "color = SessionsText900,\n        shape = RoundedCornerShape(24.dp)" not in sessions

    assert "modifier = Modifier.align(Alignment.TopCenter)" in profile
    assert "GlassSurface(" in profile
