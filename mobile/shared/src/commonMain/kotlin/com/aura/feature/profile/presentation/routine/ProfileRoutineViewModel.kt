package com.aura.feature.profile.presentation.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.core.domain.model.ProfileRoutineStep
import com.aura.feature.profile.domain.usecase.LoadProfileRoutineUseCase
import com.aura.feature.profile.domain.usecase.SaveProfileRoutineUseCase
import com.aura.feature.profile.domain.usecase.SearchRoutineProductsUseCase
import com.aura.feature.profile.logic.normalizeRoutineStepOrder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileRoutineViewModel(
    private val loadRoutine: LoadProfileRoutineUseCase,
    private val searchProducts: SearchRoutineProductsUseCase,
    private val saveRoutine: SaveProfileRoutineUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileRoutineUiState())
    val uiState: StateFlow<ProfileRoutineUiState> = _uiState.asStateFlow()
    private val searchJobs = mutableMapOf<String, Job>()
    private var nextLocalStepId = 0

    fun newLocalStepId(): String = "local-step-${nextLocalStepId++}"

    fun withStableStepIds(steps: List<ProfileRoutineStep>): List<ProfileRoutineStep> {
        return steps.map { step -> if (step.id.isNotBlank()) step else step.copy(id = newLocalStepId()) }
    }

    fun load(withStableIds: (List<ProfileRoutineStep>) -> List<ProfileRoutineStep>) {
        _uiState.update { it.copy(loading = true) }
        viewModelScope.launch {
            try {
                val steps = normalizeRoutineStepOrder(withStableIds(loadRoutine()))
                _uiState.update { it.copy(routineSteps = steps, loading = false, error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = null) }
            }
        }
    }

    fun updateSteps(steps: List<ProfileRoutineStep>) {
        _uiState.update { it.copy(routineSteps = steps, error = null, success = null) }
    }

    fun clearSearchForStep(stepId: String) {
        searchJobs.remove(stepId)?.cancel()
        _uiState.update { it.copy(searchResults = it.searchResults - stepId, searchQueries = it.searchQueries - stepId) }
    }

    fun setPendingRemoveStepId(stepId: String?) {
        _uiState.update { it.copy(pendingRemoveStepId = stepId) }
    }

    fun search(stepId: String, query: String) {
        searchJobs.remove(stepId)?.cancel()
        _uiState.update { it.copy(searchQueries = it.searchQueries + (stepId to query)) }
        if (query.trim().length < 2) {
            _uiState.update { it.copy(searchResults = it.searchResults + (stepId to emptyList())) }
            return
        }
        searchJobs[stepId] = viewModelScope.launch {
            delay(220)
            val options = runCatching { searchProducts(query) }.getOrDefault(emptyList())
            _uiState.update { it.copy(searchResults = it.searchResults + (stepId to options)) }
        }
    }

    fun save(steps: List<ProfileRoutineStep>, withStableIds: (List<ProfileRoutineStep>) -> List<ProfileRoutineStep>) {
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                val saved = normalizeRoutineStepOrder(withStableIds(saveRoutine(steps)))
                _uiState.update { it.copy(routineSteps = saved, saving = false, success = "Рутина сохранена", error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(saving = false, error = null) }
            }
        }
    }
}
