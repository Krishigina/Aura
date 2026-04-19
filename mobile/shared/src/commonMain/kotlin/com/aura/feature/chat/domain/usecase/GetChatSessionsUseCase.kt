package com.aura.feature.chat.domain.usecase

import com.aura.feature.chat.domain.repository.ChatSessionsRepository
class GetChatSessionsUseCase(private val repository: ChatSessionsRepository) {
    suspend operator fun invoke() = repository.getSessions()
}
