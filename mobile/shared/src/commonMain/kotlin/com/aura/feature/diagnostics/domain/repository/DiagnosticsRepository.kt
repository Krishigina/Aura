package com.aura.feature.diagnostics.domain.repository

import com.aura.core.data.api.model.DiagnosticsSummaryResponse
interface DiagnosticsRepository {
    suspend fun getSummary(): DiagnosticsSummaryResponse
}
