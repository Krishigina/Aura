package com.aura.feature.chat.domain.repository

import com.aura.core.data.api.model.ChatSessionSummary
interface ChatSessionsRepository {
    suspend fun getSessions(): List<ChatSessionSummary>
}
