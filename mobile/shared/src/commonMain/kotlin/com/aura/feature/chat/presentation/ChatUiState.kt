package com.aura.feature.chat.presentation

import com.aura.feature.chat.presentation.model.ChatAttachmentUi
import com.aura.feature.chat.presentation.model.ChatMessage

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val attachments: List<ChatAttachmentUi> = emptyList(),
    val draftMessage: String = "",
    val isLoading: Boolean = false,
    val isResponding: Boolean = false,
    val activeSessionId: Int? = null,
    val productContextActive: Boolean = false,
)
