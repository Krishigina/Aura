package com.aura.feature.chat.presentation

import com.aura.core.data.api.model.ChatSessionSummary
data class ChatSessionsUiState(
    val sessions: List<ChatSessionSummary> = emptyList(),
    val isLoading: Boolean = false,
    val errorText: String? = null,
)
