package com.aura.feature.survey.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.core.data.api.model.SurveyQuestionSchema
import com.aura.feature.survey.domain.repository.SkinSurveyRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SkinSurveyViewModel(
    private val repository: SkinSurveyRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SkinSurveyUiState())
    val uiState: StateFlow<SkinSurveyUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            try {
                val data = repository.loadInitialData()
                _uiState.update { state ->
                    val sectionIndex = state.sectionIndex.coerceIn(0, data.sections.lastIndex.coerceAtLeast(0))
                    state.copy(
                        sections = data.sections,
                        initialAnswers = data.answers,
                        selectedAnswers = data.answers.mapValues { it.value.toSet() },
                        sectionIndex = sectionIndex,
                        isSchemaLoading = false,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(isSchemaLoading = false) }
            }
        }
    }

    fun savePassport(answers: Map<String, List<String>>) {
        if (answers.isEmpty()) {
            _uiState.update { it.copy(saveError = "Заполните анкету перед сохранением") }
            return
        }
        _uiState.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            try {
                repository.savePassport(answers)
                _uiState.update { it.copy(isSaving = false, showSensorPrompt = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, saveError = "Не удалось сохранить анкету: ${e.message ?: "неизвестная ошибка"}") }
            }
        }
    }

    fun selectAnswer(question: SurveyQuestionSchema, option: String) {
        _uiState.update { state ->
            val current = state.selectedAnswers[question.id].orEmpty().toMutableSet()
            if (question.multi_choice) {
                if (current.contains(option)) current.remove(option) else current.add(option)
            } else {
                current.clear()
                current.add(option)
            }
            state.copy(selectedAnswers = state.selectedAnswers + (question.id to current))
        }
    }

    fun previousSection() {
        _uiState.update { it.copy(sectionIndex = (it.sectionIndex - 1).coerceAtLeast(0)) }
    }

    fun nextSection() {
        _uiState.update { state ->
            state.copy(sectionIndex = (state.sectionIndex + 1).coerceAtMost(state.sections.lastIndex.coerceAtLeast(0)))
        }
    }

    fun saveSensorOwnership(hasSensor: Boolean) {
        _uiState.update { it.copy(showSensorPrompt = false) }
        viewModelScope.launch {
            runCatching { repository.saveSensorOwnership(hasSensor) }
            _uiState.update { it.copy(sensorOwnershipSaved = true) }
        }
    }

    fun clearSaveError() {
        _uiState.update { it.copy(saveError = null) }
    }
}
