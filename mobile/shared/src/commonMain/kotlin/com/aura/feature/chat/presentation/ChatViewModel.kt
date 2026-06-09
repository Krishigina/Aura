package com.aura.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.feature.chat.domain.model.ChatAttachmentPayload
import com.aura.feature.chat.domain.usecase.LoadChatConversationUseCase
import com.aura.feature.chat.domain.usecase.SendChatMessageUseCase
import com.aura.feature.chat.domain.usecase.UploadChatAttachmentUseCase
import com.aura.feature.chat.presentation.model.ChatAttachmentUi
import com.aura.feature.chat.presentation.model.ChatMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

class ChatViewModel(
    private val loadChatConversation: LoadChatConversationUseCase,
    private val sendChatMessage: SendChatMessageUseCase,
    private val uploadChatAttachment: UploadChatAttachmentUseCase,
) : ViewModel() {
    companion object {
        private const val SHORT_REPLY_STEP = 2
        private const val MEDIUM_REPLY_STEP = 4
        private const val LONG_REPLY_STEP = 8
        private const val REPLY_FRAME_DELAY_MILLIS = 16L
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    private var productContext: JsonObject? = null
    private var lastLoadSessionId: Int? = null
    private var lastLoadRequestKey: Int? = null

    fun load(sessionId: Int?, initialProductContext: JsonObject?, requestKey: Int) {
        val sameRequest = lastLoadSessionId == sessionId && lastLoadRequestKey == requestKey
        if (sameRequest && (_uiState.value.messages.isNotEmpty() || _uiState.value.isResponding)) {
            return
        }

        if (initialProductContext != null) {
            productContext = initialProductContext
        } else if (!sameRequest) {
            productContext = null
        }

        lastLoadSessionId = sessionId
        lastLoadRequestKey = requestKey
        _uiState.update {
            it.copy(
                messages = if (sameRequest) it.messages else emptyList(),
                activeSessionId = sessionId,
                productContextActive = productContext != null,
                draftMessage = if (sameRequest) it.draftMessage else "",
                attachments = if (sameRequest) it.attachments else emptyList(),
                isResponding = false,
            )
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isResponding = false) }
            try {
                val conversation = loadChatConversation(sessionId)
                _uiState.update {
                    it.copy(
                        messages = conversation.messages.map { message ->
                            ChatMessage(message.text, message.isFromUser, message.timestamp)
                        },
                        activeSessionId = conversation.sessionId ?: sessionId,
                        isLoading = false,
                        isResponding = false,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, isResponding = false) }
            }
        }
    }

    fun reset() {
        productContext = null
        lastLoadSessionId = null
        lastLoadRequestKey = null
        _uiState.value = ChatUiState()
    }

    fun send(message: String) {
        val payload = message.trim()
        if (payload.isEmpty() || _uiState.value.isLoading || _uiState.value.isResponding) return

        val fallbackMessage = "\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u043e\u0442\u0432\u0435\u0442. \u041f\u043e\u043f\u0440\u043e\u0431\u0443\u0439\u0442\u0435 \u0435\u0449\u0435 \u0440\u0430\u0437."
        _uiState.update {
            it.copy(
                messages = it.messages + ChatMessage(payload, true, currentTimeLabel()),
                isLoading = true,
                isResponding = false,
            )
        }

        viewModelScope.launch {
            try {
                val outgoingProductContext = productContext
                val result = sendChatMessage(payload, _uiState.value.activeSessionId, outgoingProductContext)
                val answer = result.answer.trim().takeIf { it.isNotBlank() } ?: fallbackMessage
                animateAssistantReply(answer = answer, resolvedSessionId = result.sessionId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                animateAssistantReply(answer = fallbackMessage, resolvedSessionId = null)
            }
        }
    }

    fun updateDraftMessage(message: String) {
        _uiState.update { it.copy(draftMessage = message) }
    }

    fun sendDraftMessage() {
        val payload = _uiState.value.draftMessage.trim()
        if (payload.isEmpty() || _uiState.value.isLoading || _uiState.value.isResponding) return
        _uiState.update { it.copy(draftMessage = "") }
        send(payload)
    }

    fun uploadAttachmentChip(attachment: ChatAttachmentPayload) {
        val chipId = System.currentTimeMillis().toString()
        _uiState.update {
            it.copy(attachments = it.attachments + ChatAttachmentUi(chipId, attachment.filename, "\u0417\u0430\u0433\u0440\u0443\u0436\u0430\u0435\u0442\u0441\u044f..."))
        }
        viewModelScope.launch {
            val status = try {
                val (sessionId, uploadedStatus) = uploadChatAttachment(_uiState.value.activeSessionId, attachment)
                _uiState.update { it.copy(activeSessionId = sessionId) }
                uploadedStatus
            } catch (_: Exception) {
                "\u041e\u0448\u0438\u0431\u043a\u0430 \u0437\u0430\u0433\u0440\u0443\u0437\u043a\u0438"
            }
            _uiState.update { state ->
                state.copy(
                    attachments = state.attachments.map { chip ->
                        if (chip.id == chipId) chip.copy(status = status) else chip
                    },
                )
            }
        }
    }

    private fun currentTimeLabel(): String {
        val totalMinutes = ((System.currentTimeMillis() / 60000) % (24 * 60)).toInt()
        val hours = (totalMinutes / 60).toString().padStart(2, '0')
        val minutes = (totalMinutes % 60).toString().padStart(2, '0')
        return "$hours:$minutes"
    }

    private suspend fun animateAssistantReply(answer: String, resolvedSessionId: Int?) {
        val reply = answer.trim()
        val timestamp = currentTimeLabel()
        _uiState.update {
            it.copy(
                messages = it.messages + ChatMessage("", false, timestamp),
                activeSessionId = resolvedSessionId ?: it.activeSessionId,
                productContextActive = productContext != null,
                isLoading = false,
                isResponding = true,
            )
        }

        val revealStep = when {
            reply.length <= 120 -> SHORT_REPLY_STEP
            reply.length <= 320 -> MEDIUM_REPLY_STEP
            else -> LONG_REPLY_STEP
        }

        var revealedLength = 0
        while (revealedLength < reply.length) {
            revealedLength = minOf(reply.length, revealedLength + revealStep)
            val partialReply = reply.substring(0, revealedLength)
            _uiState.update { state ->
                val updatedMessages = state.messages.toMutableList()
                if (updatedMessages.isNotEmpty()) {
                    updatedMessages[updatedMessages.lastIndex] = updatedMessages.last().copy(text = partialReply)
                }
                state.copy(
                    messages = updatedMessages,
                    activeSessionId = resolvedSessionId ?: state.activeSessionId,
                    productContextActive = productContext != null,
                    isLoading = false,
                    isResponding = revealedLength < reply.length,
                )
            }
            if (revealedLength < reply.length) {
                delay(REPLY_FRAME_DELAY_MILLIS)
            }
        }
    }
}
