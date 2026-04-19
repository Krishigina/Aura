package com.aura.feature.diagnostics.domain.usecase

import com.aura.feature.diagnostics.domain.repository.DiagnosticsRepository
class GetDiagnosticsSummaryUseCase(private val repository: DiagnosticsRepository) {
    suspend operator fun invoke() = repository.getSummary()
}
