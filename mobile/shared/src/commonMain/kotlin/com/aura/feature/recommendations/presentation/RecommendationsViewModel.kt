package com.aura.feature.recommendations.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.core.data.api.model.RecommendationLine
import com.aura.feature.recommendations.domain.usecase.GenerateRecommendationUseCase
import com.aura.feature.recommendations.domain.usecase.GetSavedRecommendationUseCase
import com.aura.feature.recommendations.domain.usecase.SaveRecommendationFavoriteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecommendationsViewModel(
    private val generateRecommendation: GenerateRecommendationUseCase,
    private val saveRecommendationFavorite: SaveRecommendationFavoriteUseCase,
    private val getSavedRecommendation: GetSavedRecommendationUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RecommendationsUiState())
    val uiState: StateFlow<RecommendationsUiState> = _uiState.asStateFlow()

    fun loadGenerated() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, needsPassport = false, saved = false, saveError = null) }
            try {
                val response = generateRecommendation()
                _uiState.update {
                    it.copy(
                        recommendation = response,
                        selectedLineKey = response.lines.firstOrNull()?.key,
                        isLoading = false,
                        error = null,
                        needsPassport = false,
                    )
                }
            } catch (e: Exception) {
                val message = e.message ?: "Не удалось собрать рекомендацию"
                val needsPassport = message.contains("анкет", ignoreCase = true) || message.contains("passport", ignoreCase = true)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = if (needsPassport) "Сначала нужна анкета кожи" else message,
                        needsPassport = needsPassport,
                    )
                }
            }
        }
    }

    fun loadSaved(favoriteId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, needsPassport = false) }
            try {
                val response = getSavedRecommendation(favoriteId)
                _uiState.update {
                    it.copy(
                        recommendation = response,
                        selectedLineKey = response.lines.firstOrNull()?.key,
                        isLoading = false,
                        error = null,
                        needsPassport = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Не удалось загрузить рекомендацию") }
            }
        }
    }

    fun onLineSelected(line: RecommendationLine) {
        _uiState.update { it.copy(selectedLineKey = line.key) }
    }

    fun saveCurrentFavorite() {
        val recommendation = _uiState.value.recommendation ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saved = false, saveError = null) }
            try {
                saveRecommendationFavorite(recommendation)
                _uiState.update { it.copy(isSaving = false, saved = true, saveError = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, saveError = e.message ?: "Не удалось сохранить") }
            }
        }
    }
}
