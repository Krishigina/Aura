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


def test_chat_attachment_models_match_backend_contract():
    source = (COMMON / "core" / "domain" / "model" / "Models.kt").read_text(encoding="utf-8")

    assert "data class ChatAttachment" in source
    assert '@SerialName("attachment_id") val attachmentId' in source
    assert '@SerialName("session_id") val sessionId' in source
    assert '@SerialName("content_type") val contentType' in source
    assert "data class ChatAttachmentsResponse" in source
    assert "val attachments: List<ChatAttachment>" in source


def test_chat_client_exposes_attachment_upload_and_list_methods():
    source = (COMMON / "core" / "data" / "api" / "AuraApiClient.kt").read_text(encoding="utf-8")

    assert "MultiPartFormDataContent" in source
    assert "formData" in source
    assert "suspend fun uploadChatAttachment" in source
    assert "suspend fun getChatAttachments" in source
    assert '"$baseUrl/api/chat/sessions/$sessionId/attachments"' in source
    assert 'filename=\\"$filename\\"' in source
    assert "ContentType.parse(contentType)" in source


def test_chat_ui_has_soft_user_bubble_and_attachment_chips():
    source = (COMMON / "feature" / "chat" / "ChatScreen.kt").read_text(encoding="utf-8")

    assert "ChatAttachmentChip" in source
    assert "selectedAttachments" in source
    assert "onAttachClick" in source
    assert "Color(0xFFE9FFF6)" in source or "UserBubbleMint" in source
    assert ".background(CoolGrey)" not in source
    assert "color = TextGray800" in source


def test_chat_attach_button_uses_picker_bridge_not_hardcoded_chip():
    source = (COMMON / "feature" / "chat" / "ChatScreen.kt").read_text(encoding="utf-8")

    assert "data class PickedChatAttachment" in source
    assert "object ChatAttachmentPickerBridge" in source
    assert "ChatAttachmentPickerBridge.openPicker()" in source
    assert "ChatAttachmentPickerBridge.onPicked" in source
    assert "apiClient.uploadChatAttachment" in source
    assert "apiClient.createChatSession" in source
    assert "Файл будет добавлен" not in source


def test_android_main_activity_uses_native_source_chooser():
    source = (SHARED_ROOT.parent / "app" / "src" / "androidMain" / "kotlin" / "com" / "aura" / "app" / "MainActivity.kt").read_text(encoding="utf-8")

    assert "rememberLauncherForActivityResult" in source
    assert "ActivityResultContracts.StartActivityForResult" in source
    assert "Intent.createChooser" in source
    assert "Intent.ACTION_OPEN_DOCUMENT" in source
    assert "Intent.ACTION_PICK" in source
    assert "ActivityResultContracts.OpenDocument" not in source
    assert "application/pdf" in source
    assert "application/vnd.openxmlformats-officedocument.wordprocessingml.document" in source
    assert "image/jpeg" in source
    assert "image/png" in source
    assert "ChatAttachmentPickerBridge.deliver" in source
    assert "OpenableColumns.DISPLAY_NAME" in source
