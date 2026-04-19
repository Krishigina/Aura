package com.aura.feature.chat.domain.model

data class ChatConversationMessage(
    val text: String,
    val isFromUser: Boolean,
    val timestamp: String = "",
)

data class ChatConversation(
    val sessionId: Int?,
    val messages: List<ChatConversationMessage>,
)

data class ChatAttachmentPayload(
    val filename: String,
    val contentType: String,
    val bytes: ByteArray,
)

data class ChatSendResult(
    val sessionId: Int?,
    val answer: String,
)
