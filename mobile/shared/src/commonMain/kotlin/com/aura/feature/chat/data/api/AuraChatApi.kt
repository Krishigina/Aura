package com.aura.feature.chat.data.api

import com.aura.core.data.api.client.ChatNetworkClient
import com.aura.core.data.api.model.ChatAttachment
import com.aura.core.data.api.model.ChatBootstrapResponse
import com.aura.core.data.api.model.ChatSessionCreateResponse
import com.aura.core.data.api.model.ChatSessionDetailResponse
import com.aura.core.data.api.model.ChatSessionsResponse
import com.aura.core.data.api.model.RagChatResponse
import kotlinx.serialization.json.JsonObject

internal class AuraChatApi(
    private val apiClient: ChatNetworkClient,
) : ChatApi {
    override suspend fun getBootstrap(token: String): ChatBootstrapResponse {
        return apiClient.getBootstrap(token)
    }

    override suspend fun getSession(token: String, sessionId: Int): ChatSessionDetailResponse {
        return apiClient.getSession(token, sessionId)
    }

    override suspend fun queryRagChat(token: String, message: String, sessionId: Int?, productContext: JsonObject?): RagChatResponse {
        return apiClient.queryRagChat(token, message, sessionId, productContext)
    }

    override suspend fun createSession(token: String): ChatSessionCreateResponse {
        return apiClient.createSession(token)
    }

    override suspend fun uploadAttachment(token: String, sessionId: Int, filename: String, contentType: String, bytes: ByteArray): ChatAttachment {
        return apiClient.uploadAttachment(token, sessionId, filename, contentType, bytes)
    }

    override suspend fun getSessions(token: String): ChatSessionsResponse {
        return apiClient.getSessions(token)
    }
}
