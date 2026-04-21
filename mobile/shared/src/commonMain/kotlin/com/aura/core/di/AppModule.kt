package com.aura.core.di

import com.aura.core.data.api.AuraApiClient
import org.koin.dsl.module

val appModule = module {
    val configuredApiUrl =
        System.getenv("AURA_API_URL")?.takeIf { it.isNotBlank() }
            ?: System.getProperty("aura.api.url")?.takeIf { it.isNotBlank() }
            ?: "http://localhost:3002"

    single { AuraApiClient(configuredApiUrl) }
}
