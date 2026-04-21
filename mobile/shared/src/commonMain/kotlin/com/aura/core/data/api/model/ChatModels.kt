package com.aura.core.data.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable data class ChatMessage(val id: String, val content: String, val isFromUser: Boolean, val timestamp: Long = System.currentTimeMillis())

@Serializable data class RagChatRequest(
    val message: String,
    @SerialName("session_id") val sessionId: Int? = null,
    @SerialName("product_context") val productContext: JsonObject? = null,
)

@Serializable data class RagChatSource(val id: String = "", val title: String = "", val content: String = "", val score: Double? = null)

@Serializable data class RagChatResponse(
    val answer: String = "",
    val sources: List<RagChatSource> = emptyList(),
    @SerialName("session_id") val sessionId: Int = 0,
    @SerialName("conversation_id") val conversationId: String? = null,
)

@Serializable
data class ChatSessionSummary(
    val id: Int,
    val title: String,
    @SerialName("last_message") val lastMessage: String? = null,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("message_count") val messageCount: Int,
)

@Serializable data class ChatSessionsResponse(val sessions: List<ChatSessionSummary> = emptyList())

@Serializable
data class ChatSessionCreateResponse(
    @SerialName("session_id") val sessionId: Int,
    val title: String,
)

@Serializable data class ChatSessionMessage(val role: String, val content: String, val timestamp: String)

@Serializable
data class ChatSessionDetailResponse(
    val session: ChatSessionSummary,
    val messages: List<ChatSessionMessage> = emptyList(),
)

@Serializable
data class ChatAttachment(
    @SerialName("attachment_id") val attachmentId: Int,
    @SerialName("session_id") val sessionId: Int,
    val filename: String,
    @SerialName("content_type") val contentType: String,
    val status: String = "pending",
    val summary: String? = null,
)

@Serializable
data class ChatAttachmentsResponse(val attachments: List<ChatAttachment> = emptyList())

@Serializable
data class ChatBootstrapMessage(
    val text: String = "",
    val is_from_user: Boolean = false,
    val timestamp: String = "",
)

@Serializable
data class ChatBootstrapResponse(
    val assistant_name: String = "",
    val assistant_context: String = "",
    val messages: List<ChatBootstrapMessage> = emptyList(),
)

@Serializable
data class ChatMessageCreateRequest(
    val text: String,
    val is_from_user: Boolean = true,
    val timestamp: String? = null,
)

@Serializable
data class ChatMessageAppendResponse(
    val success: Boolean = false,
    val message: ChatBootstrapMessage = ChatBootstrapMessage(),
)
