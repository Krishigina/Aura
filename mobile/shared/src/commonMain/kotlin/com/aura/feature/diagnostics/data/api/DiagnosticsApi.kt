package com.aura.feature.diagnostics.data.api

import com.aura.core.data.api.model.DiagnosticsSummaryResponse

interface DiagnosticsApi {
    suspend fun getSummary(token: String): DiagnosticsSummaryResponse
}
