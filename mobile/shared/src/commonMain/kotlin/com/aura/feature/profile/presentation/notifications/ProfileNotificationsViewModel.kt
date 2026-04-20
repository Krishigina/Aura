package com.aura.feature.profile.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.core.domain.model.ProfileNotificationSettings
import com.aura.core.notifications.scheduleProfileNotifications
import com.aura.feature.profile.domain.usecase.LoadProfileNotificationsUseCase
import com.aura.feature.profile.domain.usecase.SaveProfileNotificationsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileNotificationsViewModel(
    private val loadNotifications: LoadProfileNotificationsUseCase,
    private val saveNotifications: SaveProfileNotificationsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileNotificationsUiState())
    val uiState: StateFlow<ProfileNotificationsUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.update { it.copy(loading = true) }
        viewModelScope.launch {
            try {
                val data = loadNotifications()
                _uiState.update { it.copy(routineStepsCount = data.routineStepsCount, settings = data.settings, loading = false, error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = null) }
            }
        }
    }

    fun updateSettings(settings: ProfileNotificationSettings) {
        _uiState.update { it.copy(settings = settings, error = null, success = null) }
    }

    fun save() {
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                val saved = saveNotifications(_uiState.value.settings)
                scheduleProfileNotifications(saved)
                _uiState.update { it.copy(settings = saved, saving = false, success = "Настройки уведомлений сохранены", error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(saving = false, error = null) }
            }
        }
    }
}
