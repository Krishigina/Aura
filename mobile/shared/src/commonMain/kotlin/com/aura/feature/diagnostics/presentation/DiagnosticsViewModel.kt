package com.aura.feature.diagnostics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.core.data.api.model.DiagnosticsDevice
import com.aura.core.data.api.model.DiagnosticsMetrics
import com.aura.feature.diagnostics.domain.usecase.GetDiagnosticsSummaryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DiagnosticsViewModel(
    private val getDiagnosticsSummary: GetDiagnosticsSummaryUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { getDiagnosticsSummary() }
                .onSuccess { summary ->
                    _uiState.update { it.copy(metrics = summary.metrics, device = summary.device, isLoading = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(metrics = DiagnosticsMetrics(), device = DiagnosticsDevice(), isLoading = false) }
                }
        }
    }
}
