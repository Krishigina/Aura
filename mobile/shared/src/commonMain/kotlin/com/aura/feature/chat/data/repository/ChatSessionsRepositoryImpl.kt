package com.aura.feature.chat.data.repository

import com.aura.core.data.api.model.ChatSessionSummary
import com.aura.core.domain.repository.SessionRepository
import com.aura.feature.chat.data.api.ChatApi
import com.aura.feature.chat.domain.repository.ChatSessionsRepository

class ChatSessionsRepositoryImpl(
    private val chatApi: ChatApi,
    private val sessionRepository: SessionRepository,
) : ChatSessionsRepository {
    override suspend fun getSessions(): List<ChatSessionSummary> {
        val token = sessionRepository.token().takeUnless { it.isNullOrBlank() }
            ?: throw IllegalStateException("Войдите в аккаунт, чтобы увидеть историю чатов")
        return chatApi.getSessions(token).sessions
    }
}
