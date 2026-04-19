package com.aura.feature.chat.domain.usecase

import com.aura.feature.chat.domain.model.ChatAttachmentPayload
import com.aura.feature.chat.domain.repository.ChatRepository

class UploadChatAttachmentUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(sessionId: Int?, attachment: ChatAttachmentPayload) = repository.uploadAttachment(sessionId, attachment)
}
