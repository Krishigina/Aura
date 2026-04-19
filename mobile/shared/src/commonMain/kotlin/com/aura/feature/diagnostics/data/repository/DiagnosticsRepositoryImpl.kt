package com.aura.feature.diagnostics.data.repository

import com.aura.core.data.api.model.DiagnosticsSummaryResponse
import com.aura.core.domain.repository.SessionRepository
import com.aura.feature.diagnostics.data.api.DiagnosticsApi
import com.aura.feature.diagnostics.domain.repository.DiagnosticsRepository

class DiagnosticsRepositoryImpl(
    private val diagnosticsApi: DiagnosticsApi,
    private val sessionRepository: SessionRepository,
) : DiagnosticsRepository {
    override suspend fun getSummary(): DiagnosticsSummaryResponse {
        val token = sessionRepository.token().takeUnless { it.isNullOrBlank() }
            ?: throw IllegalStateException("Нужна авторизация")
        return diagnosticsApi.getSummary(token)
    }
}
