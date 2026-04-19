package com.aura.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.core.domain.repository.SessionRepository
import com.aura.feature.auth.domain.usecase.ClearSessionUseCase
import com.aura.feature.auth.domain.usecase.LoadSessionUseCase
import com.aura.feature.auth.domain.usecase.ValidateSessionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthSessionUiState(
    val isReady: Boolean = false,
    val isAuthenticated: Boolean = false,
)

class AuthSessionViewModel(
    private val loadSession: LoadSessionUseCase,
    private val validateSession: ValidateSessionUseCase,
    private val clearSession: ClearSessionUseCase,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthSessionUiState())
    val uiState: StateFlow<AuthSessionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.authState().collect { isAuthenticated ->
                _uiState.update { current ->
                    if (!current.isReady) current else current.copy(isAuthenticated = isAuthenticated)
                }
            }
        }
    }

    fun load() {
        _uiState.update { it.copy(isReady = true, isAuthenticated = loadSession()) }
    }

    fun validate() {
        viewModelScope.launch {
            if (!validateSession()) {
                _uiState.update { it.copy(isAuthenticated = false) }
            }
        }
    }

    fun markAuthenticated() {
        _uiState.update { it.copy(isReady = true, isAuthenticated = true) }
    }

    fun clear() {
        clearSession()
        _uiState.update { it.copy(isReady = true, isAuthenticated = false) }
    }
}
