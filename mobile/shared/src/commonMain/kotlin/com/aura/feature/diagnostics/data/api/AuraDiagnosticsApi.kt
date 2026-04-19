package com.aura.feature.diagnostics.data.api

import com.aura.core.data.api.client.GeneralContentNetworkClient
import com.aura.core.data.api.model.DiagnosticsSummaryResponse

internal class AuraDiagnosticsApi(
    private val apiClient: GeneralContentNetworkClient,
) : DiagnosticsApi {
    override suspend fun getSummary(token: String): DiagnosticsSummaryResponse {
        return apiClient.getDiagnosticsSummary(token)
    }
}
