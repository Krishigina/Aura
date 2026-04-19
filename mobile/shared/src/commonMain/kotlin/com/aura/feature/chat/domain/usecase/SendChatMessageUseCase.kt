package com.aura.feature.chat.domain.usecase

import com.aura.feature.chat.domain.repository.ChatRepository
import kotlinx.serialization.json.JsonObject

class SendChatMessageUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(message: String, sessionId: Int?, productContext: JsonObject?) =
        repository.sendMessage(message, sessionId, productContext)
}
