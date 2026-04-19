package com.aura.feature.chat.domain.repository

import com.aura.feature.chat.domain.model.ChatAttachmentPayload
import com.aura.feature.chat.domain.model.ChatConversation
import com.aura.feature.chat.domain.model.ChatSendResult
import kotlinx.serialization.json.JsonObject

interface ChatRepository {
    suspend fun loadConversation(sessionId: Int?): ChatConversation
    suspend fun sendMessage(message: String, sessionId: Int?, productContext: JsonObject?): ChatSendResult
    suspend fun uploadAttachment(sessionId: Int?, attachment: ChatAttachmentPayload): Pair<Int, String>
}
