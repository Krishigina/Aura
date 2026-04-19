package com.aura.feature.chat.data.api

import com.aura.core.data.api.model.ChatAttachment
import com.aura.core.data.api.model.ChatBootstrapResponse
import com.aura.core.data.api.model.ChatSessionCreateResponse
import com.aura.core.data.api.model.ChatSessionDetailResponse
import com.aura.core.data.api.model.ChatSessionsResponse
import com.aura.core.data.api.model.RagChatResponse
import kotlinx.serialization.json.JsonObject

interface ChatApi {
    suspend fun getBootstrap(token: String): ChatBootstrapResponse
    suspend fun getSession(token: String, sessionId: Int): ChatSessionDetailResponse
    suspend fun queryRagChat(token: String, message: String, sessionId: Int?, productContext: JsonObject?): RagChatResponse
    suspend fun createSession(token: String): ChatSessionCreateResponse
    suspend fun uploadAttachment(token: String, sessionId: Int, filename: String, contentType: String, bytes: ByteArray): ChatAttachment
    suspend fun getSessions(token: String): ChatSessionsResponse
}
