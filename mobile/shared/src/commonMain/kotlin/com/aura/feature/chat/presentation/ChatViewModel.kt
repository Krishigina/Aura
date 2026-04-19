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
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    private var productContext: JsonObject? = null

    fun load(sessionId: Int?, initialProductContext: JsonObject?) {
        productContext = initialProductContext
        _uiState.update { it.copy(activeSessionId = sessionId, productContextActive = productContext != null) }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val conversation = loadChatConversation(sessionId)
                _uiState.update {
                    it.copy(
                        messages = conversation.messages.map { message -> ChatMessage(message.text, message.isFromUser, message.timestamp) },
                        activeSessionId = conversation.sessionId ?: sessionId,
                        isLoading = false,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun send(message: String) {
        val payload = message.trim()
        if (payload.isEmpty() || _uiState.value.isLoading) return

        val fallbackMessage = "Не удалось получить ответ. Попробуйте еще раз."
        _uiState.update {
            it.copy(
                messages = it.messages + ChatMessage(payload, true, currentTimeLabel()),
                isLoading = true,
            )
        }

        viewModelScope.launch {
            try {
                val outgoingProductContext = productContext
                val result = sendChatMessage(payload, _uiState.value.activeSessionId, outgoingProductContext)
                val answer = result.answer.trim().takeIf { it.isNotBlank() } ?: fallbackMessage
                _uiState.update {
                    it.copy(
                        messages = it.messages + ChatMessage(answer, false, currentTimeLabel()),
                        activeSessionId = result.sessionId ?: it.activeSessionId,
                        productContextActive = productContext != null,
                        isLoading = false,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        messages = it.messages + ChatMessage(fallbackMessage, false, currentTimeLabel()),
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun updateDraftMessage(message: String) {
        _uiState.update { it.copy(draftMessage = message) }
    }

    fun sendDraftMessage() {
        val payload = _uiState.value.draftMessage.trim()
        if (payload.isEmpty() || _uiState.value.isLoading) return
        _uiState.update { it.copy(draftMessage = "") }
        send(payload)
    }

    fun uploadAttachmentChip(attachment: ChatAttachmentPayload) {
        val chipId = System.currentTimeMillis().toString()
        _uiState.update {
            it.copy(attachments = it.attachments + ChatAttachmentUi(chipId, attachment.filename, "Загружается..."))
        }
        viewModelScope.launch {
            val status = try {
                val (sessionId, uploadedStatus) = uploadChatAttachment(_uiState.value.activeSessionId, attachment)
                _uiState.update { it.copy(activeSessionId = sessionId) }
                uploadedStatus
            } catch (_: Exception) {
                "Ошибка загрузки"
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
}
