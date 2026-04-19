package com.aura.feature.chat.domain.usecase

import com.aura.feature.chat.domain.repository.ChatRepository

class LoadChatConversationUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(sessionId: Int?) = repository.loadConversation(sessionId)
}
