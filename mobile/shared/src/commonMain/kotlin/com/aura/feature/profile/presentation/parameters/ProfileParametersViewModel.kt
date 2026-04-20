package com.aura.feature.profile.presentation.parameters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.feature.profile.domain.repository.ProfileParametersRepository
import com.aura.feature.profile.domain.usecase.RefreshSkinPassportUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileParametersViewModel(
    private val refreshSkinPassport: RefreshSkinPassportUseCase,
    private val repository: ProfileParametersRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileParametersUiState(repository.skinPassportAnswers()))
    val uiState: StateFlow<ProfileParametersUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            refreshSkinPassport()
            _uiState.update { it.copy(skinPassportAnswers = repository.skinPassportAnswers()) }
        }
    }
}
