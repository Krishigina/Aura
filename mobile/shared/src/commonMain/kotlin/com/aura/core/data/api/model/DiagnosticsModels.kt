package com.aura.core.data.api.model

import kotlinx.serialization.Serializable

@Serializable
data class DiagnosticsMetrics(
    val hydration: String = "",
    val oiliness: String = "",
    val ph: String = "",
    val sensitivity: String = "",
)

@Serializable
data class DiagnosticsDevice(
    val name: String = "",
    val status: String = "",
    val battery: String = "",
)

@Serializable
data class DiagnosticsSummaryResponse(
    val metrics: DiagnosticsMetrics = DiagnosticsMetrics(),
    val device: DiagnosticsDevice = DiagnosticsDevice(),
)
