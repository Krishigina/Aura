package com.aura.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.feature.auth.domain.usecase.LoginUseCase
import com.aura.feature.auth.domain.usecase.RegisterUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val EMAIL_REGEX = Regex("^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$", RegexOption.IGNORE_CASE)
private val NICKNAME_REGEX = Regex("^[a-zA-Z0-9_]+$")

private fun normalizeNicknameInput(input: String): String {
    val clean = input.removePrefix("@").filter { it.isLetterOrDigit() || it == '_' }.take(32)
    return "@$clean"
}

class AuthViewModel(
    private val login: LoginUseCase,
    private val register: RegisterUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun setValidationError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun toggleMode() {
        _uiState.update { state ->
            state.copy(
                isLogin = !state.isLogin,
                errorMessage = null,
                nickname = state.nickname.ifBlank { "@" },
            )
        }
    }

    fun updateName(value: String) {
        _uiState.update { it.copy(name = value) }
    }

    fun updateNickname(value: String) {
        _uiState.update { it.copy(nickname = normalizeNicknameInput(value)) }
    }

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    fun updateConfirmPassword(value: String) {
        _uiState.update { it.copy(confirmPassword = value) }
    }

    fun submitCurrentForm() {
        val state = _uiState.value
        val validationError = validate(state)
        if (validationError != null) {
            setValidationError(validationError)
            return
        }
        if (state.isLogin) {
            submitLogin(state.email, state.password)
        } else {
            submitRegister(state.name, state.email, state.password, state.nickname.removePrefix("@").trim())
        }
    }

    private fun validate(state: AuthUiState): String? {
        if (state.isLogin) {
            if (state.email.isBlank()) return "Введите email"
            if (!EMAIL_REGEX.matches(state.email)) return "Некорректный email"
            if (state.password.length < 6) return "Пароль минимум 6 символов"
        } else {
            if (state.name.isBlank()) return "Введите имя"
            if (state.name.length < 2) return "Имя минимум 2 символа"
            if (state.nickname.isBlank() || state.nickname == "@") return "Введите логин"
            val cleanNickname = state.nickname.removePrefix("@")
            if (!NICKNAME_REGEX.matches(cleanNickname)) return "Логин: только латиница, цифры, _"
            if (state.email.isBlank()) return "Введите email"
            if (!EMAIL_REGEX.matches(state.email)) return "Некорректный email"
            if (state.password.length < 6) return "Пароль минимум 6 символов"
            if (state.password != state.confirmPassword) return "Пароли не совпадают"
        }
        return null
    }

    fun submitLogin(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, isAuthenticated = false, isNewUser = false) }
            try {
                login(email, password)
                _uiState.update { it.copy(isLoading = false, isAuthenticated = true, isNewUser = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Unknown error") }
            }
        }
    }

    fun submitRegister(name: String, email: String, password: String, nickname: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, isAuthenticated = false, isNewUser = false) }
            try {
                register(name, email, password, nickname)
                _uiState.update { it.copy(isLoading = false, isAuthenticated = true, isNewUser = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Unknown error") }
            }
        }
    }
}
