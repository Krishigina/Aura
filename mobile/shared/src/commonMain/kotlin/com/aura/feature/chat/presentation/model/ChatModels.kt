package com.aura.feature.chat.presentation.model

data class ChatMessage(
    val text: String,
    val isFromUser: Boolean,
    val timestamp: String = "12:30",
)

data class ChatAttachmentUi(
    val id: String,
    val filename: String,
    val status: String,
)

data class PickedChatAttachment(
    val filename: String,
    val contentType: String,
    val bytes: ByteArray,
)
