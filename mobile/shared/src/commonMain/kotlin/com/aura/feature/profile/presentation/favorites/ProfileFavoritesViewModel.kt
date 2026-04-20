package com.aura.feature.profile.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.feature.profile.domain.repository.ProfileFavoritesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileFavoritesViewModel(
    private val repository: ProfileFavoritesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileFavoritesUiState())
    val uiState: StateFlow<ProfileFavoritesUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.update { it.copy(loading = true) }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(favorites = repository.loadFavorites(), loading = false, error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message ?: "Не удалось загрузить избранные рекомендации") }
            }
        }
    }
}
