package com.aura.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.feature.chat.domain.usecase.GetChatSessionsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatSessionsViewModel(
    private val getChatSessions: GetChatSessionsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatSessionsUiState())
    val uiState: StateFlow<ChatSessionsUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorText = null) }
            try {
                _uiState.update { it.copy(sessions = getChatSessions(), isLoading = false, errorText = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorText = e.message ?: "Не удалось загрузить историю чатов") }
            }
        }
    }
}
