package com.aura.feature.diagnostics.presentation

import com.aura.core.data.api.model.DiagnosticsDevice
import com.aura.core.data.api.model.DiagnosticsMetrics

data class DiagnosticsUiState(
    val metrics: DiagnosticsMetrics = DiagnosticsMetrics(),
    val device: DiagnosticsDevice = DiagnosticsDevice(),
    val isLoading: Boolean = false,
)
