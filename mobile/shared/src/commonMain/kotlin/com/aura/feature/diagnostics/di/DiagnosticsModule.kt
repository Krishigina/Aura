package com.aura.feature.diagnostics.di

import com.aura.feature.diagnostics.data.api.AuraDiagnosticsApi
import com.aura.feature.diagnostics.data.api.DiagnosticsApi
import com.aura.feature.diagnostics.data.repository.DiagnosticsRepositoryImpl
import com.aura.feature.diagnostics.domain.repository.DiagnosticsRepository
import com.aura.feature.diagnostics.domain.usecase.GetDiagnosticsSummaryUseCase
import com.aura.feature.diagnostics.presentation.DiagnosticsViewModel
import org.koin.dsl.module

val diagnosticsModule = module {
    single<DiagnosticsApi> { AuraDiagnosticsApi(get()) }
    single<DiagnosticsRepository> { DiagnosticsRepositoryImpl(get(), get()) }
    factory { GetDiagnosticsSummaryUseCase(get()) }
    factory { DiagnosticsViewModel(get()) }
}
