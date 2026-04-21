package com.aura.core.data.api.client

import com.aura.core.data.api.model.ChatAttachment
import com.aura.core.data.api.model.ChatAttachmentsResponse
import com.aura.core.data.api.model.ChatBootstrapResponse
import com.aura.core.data.api.model.ChatMessageAppendResponse
import com.aura.core.data.api.model.ChatMessageCreateRequest
import com.aura.core.data.api.model.ChatSessionCreateResponse
import com.aura.core.data.api.model.ChatSessionDetailResponse
import com.aura.core.data.api.model.ChatSessionsResponse
import com.aura.core.data.api.model.RagChatRequest
import com.aura.core.data.api.model.RagChatResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

internal class ChatNetworkClient(
    private val baseUrl: String,
    private val httpClient: HttpClient,
    private val json: Json,
    private val extractErrorMessage: (String, HttpStatusCode) -> String,
) {
    suspend fun getBootstrap(token: String): ChatBootstrapResponse {
        val response = httpClient.get("$baseUrl/api/chat/bootstrap") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        return json.decodeOrDefault(response.bodyTextOrThrow(extractErrorMessage), ::ChatBootstrapResponse)
    }

    suspend fun appendMessage(token: String, text: String, isFromUser: Boolean, timestamp: String?): ChatMessageAppendResponse {
        val response = httpClient.post("$baseUrl/api/chat/messages") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(ChatMessageCreateRequest(text = text, is_from_user = isFromUser, timestamp = timestamp))
        }

        return json.decodeOrDefault(response.bodyTextOrThrow(extractErrorMessage), ::ChatMessageAppendResponse)
    }

    suspend fun getSessions(token: String): ChatSessionsResponse {
        val response = httpClient.get("$baseUrl/api/chat/sessions") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        return json.decodeOrDefault(response.bodyTextOrThrow(extractErrorMessage), ::ChatSessionsResponse)
    }

    suspend fun createSession(token: String): ChatSessionCreateResponse {
        val response = httpClient.post("$baseUrl/api/chat/sessions") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { })
        }

        return json.decodeFromString(response.bodyTextOrThrow(extractErrorMessage))
    }

    suspend fun getSession(token: String, sessionId: Int): ChatSessionDetailResponse {
        val response = httpClient.get("$baseUrl/api/chat/sessions/$sessionId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        return json.decodeFromString(response.bodyTextOrThrow(extractErrorMessage))
    }

    suspend fun uploadAttachment(token: String, sessionId: Int, filename: String, contentType: String, bytes: ByteArray): ChatAttachment {
        val response = httpClient.post("$baseUrl/api/chat/sessions/$sessionId/attachments") {
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            "file",
                            bytes,
                            Headers.build {
                                append(HttpHeaders.ContentType, ContentType.parse(contentType).toString())
                                append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"$filename\"")
                            },
                        )
                    },
                ),
            )
        }

        return json.decodeFromString(response.bodyTextOrThrow(extractErrorMessage))
    }

    suspend fun getAttachments(token: String, sessionId: Int): ChatAttachmentsResponse {
        val response = httpClient.get("$baseUrl/api/chat/sessions/$sessionId/attachments") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        return json.decodeOrDefault(response.bodyTextOrThrow(extractErrorMessage), ::ChatAttachmentsResponse)
    }

    suspend fun queryRagChat(token: String, message: String, sessionId: Int? = null, productContext: JsonObject? = null): RagChatResponse {
        val response = httpClient.post("$baseUrl/api/chat/rag") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(RagChatRequest(message = message, sessionId = sessionId, productContext = productContext))
        }

        return json.decodeOrDefault(response.bodyTextOrThrow(extractErrorMessage), ::RagChatResponse)
    }
}
