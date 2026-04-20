package com.aura.feature.profile.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.feature.profile.domain.repository.ProfileSettingsRepository
import com.aura.feature.profile.logic.validatePasswordChange
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileSettingsViewModel(
    private val repository: ProfileSettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileSettingsUiState(user = repository.currentUser()))
    val uiState: StateFlow<ProfileSettingsUiState> = _uiState.asStateFlow()

    fun clearStatus() {
        _uiState.update { it.copy(error = null, success = null) }
    }

    fun setTab(tab: ProfileSettingsTab) {
        _uiState.update { it.copy(tab = tab, error = null, success = null) }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun updateNickname(nickname: String) {
        _uiState.update { it.copy(nickname = nickname) }
    }

    fun updateCurrentPassword(currentPassword: String) {
        _uiState.update { it.copy(currentPassword = currentPassword) }
    }

    fun updateNewPassword(newPassword: String) {
        _uiState.update { it.copy(newPassword = newPassword) }
    }

    fun updateConfirmPassword(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword) }
    }

    fun updateDeletePassword(deletePassword: String) {
        _uiState.update { it.copy(deletePassword = deletePassword) }
    }

    fun saveName() {
        updateAccount(successMessage = "Имя обновлено")
    }

    fun saveLogin() {
        updateAccount(successMessage = "Логин обновлен")
    }

    private fun updateAccount(successMessage: String) {
        val state = _uiState.value
        val trimmedName = state.name.trim()
        if (trimmedName.isEmpty()) {
            _uiState.update { it.copy(error = "Имя не может быть пустым", success = null) }
            return
        }
        launchSaving {
            val updated = repository.updateAccount(trimmedName, state.nickname.trim().ifBlank { null })
            _uiState.update {
                it.copy(
                    user = updated,
                    name = updated.name,
                    nickname = updated.nickname.orEmpty(),
                    success = successMessage,
                    error = null,
                )
            }
        }
    }

    fun updatePassword() {
        val state = _uiState.value
        val validationError = validatePasswordChange(state.currentPassword, state.newPassword, state.confirmPassword)
        if (validationError != null) {
            _uiState.update { it.copy(error = validationError, success = null) }
            return
        }
        launchSaving {
            repository.updatePassword(state.currentPassword, state.newPassword)
            _uiState.update {
                it.copy(
                    currentPassword = "",
                    newPassword = "",
                    confirmPassword = "",
                    success = "Пароль обновлен",
                    error = null,
                )
            }
        }
    }

    fun deleteAccount() {
        val currentPassword = _uiState.value.deletePassword
        if (currentPassword.isBlank()) {
            _uiState.update { it.copy(error = "Введите текущий пароль", success = null) }
            return
        }
        launchSaving {
            repository.deleteAccount(currentPassword)
            _uiState.update { it.copy(accountDeleted = true, error = null, success = null) }
        }
    }

    private fun launchSaving(block: suspend () -> Unit) {
        _uiState.update { it.copy(isSaving = true, error = null, success = null) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Не удалось сохранить", success = null) }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
