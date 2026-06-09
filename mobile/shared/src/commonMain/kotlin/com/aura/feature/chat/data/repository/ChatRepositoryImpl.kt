package com.aura.feature.chat.data.repository

import com.aura.core.domain.repository.SessionRepository
import com.aura.feature.chat.data.api.ChatApi
import com.aura.feature.chat.domain.model.ChatAttachmentPayload
import com.aura.feature.chat.domain.model.ChatConversation
import com.aura.feature.chat.domain.model.ChatConversationMessage
import com.aura.feature.chat.domain.model.ChatSendResult
import com.aura.feature.chat.domain.repository.ChatRepository
import kotlinx.serialization.json.JsonObject

class ChatRepositoryImpl(
    private val chatApi: ChatApi,
    private val sessionRepository: SessionRepository,
) : ChatRepository {
    override suspend fun loadConversation(sessionId: Int?): ChatConversation {
        val token = requireToken()
        return if (sessionId == null) {
            val bootstrap = chatApi.getBootstrap(token)
            ChatConversation(
                sessionId = null,
                messages = bootstrap.messages
                    .filter { it.text.isNotBlank() }
                    .map {
                        ChatConversationMessage(
                            text = if (it.is_from_user) it.text else normalizeAssistantMessage(it.text),
                            isFromUser = it.is_from_user,
                            timestamp = it.timestamp.takeIf { ts -> ts.isNotBlank() } ?: "",
                        )
                    },
            )
        } else {
            val session = chatApi.getSession(token, sessionId)
            ChatConversation(
                sessionId = session.session.id,
                messages = session.messages
                    .filter { it.content.isNotBlank() }
                    .map {
                        val isUser = it.role.equals("user", ignoreCase = true)
                        ChatConversationMessage(
                            text = if (isUser) it.content else normalizeAssistantMessage(it.content),
                            isFromUser = isUser,
                            timestamp = it.timestamp.takeIf { ts -> ts.isNotBlank() } ?: "",
                        )
                    },
            )
        }
    }

    override suspend fun sendMessage(message: String, sessionId: Int?, productContext: JsonObject?): ChatSendResult {
        val token = requireToken()
        val rag = chatApi.queryRagChat(token, message, sessionId, productContext)
        return ChatSendResult(
            sessionId = rag.sessionId.takeIf { it > 0 },
            answer = normalizeAssistantMessage(rag.answer),
        )
    }

    override suspend fun uploadAttachment(sessionId: Int?, attachment: ChatAttachmentPayload): Pair<Int, String> {
        val token = requireToken()
        val resolvedSessionId = sessionId ?: chatApi.createSession(token).sessionId
        val uploaded = chatApi.uploadAttachment(
            token = token,
            sessionId = resolvedSessionId,
            filename = attachment.filename,
            contentType = attachment.contentType,
            bytes = attachment.bytes,
        )
        return resolvedSessionId to uploaded.status.ifBlank { "\u0413\u043e\u0442\u043e\u0432\u043e" }
    }

    private fun requireToken(): String = sessionRepository.token().takeUnless { it.isNullOrBlank() }
        ?: throw IllegalStateException("\u041d\u0443\u0436\u043d\u0430 \u0430\u0432\u0442\u043e\u0440\u0438\u0437\u0430\u0446\u0438\u044f")
}

internal fun normalizeAssistantMessage(text: String): String {
    return text
        .replace("\r\n", "\n")
        .replace("\\n", "\n")
        .replace("\\t", "\t")
        .replace(Regex("""\\+(["'])"""), "$1")
        .trim()
}
