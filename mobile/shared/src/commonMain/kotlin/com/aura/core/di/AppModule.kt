package com.aura.core.di

import com.aura.core.data.api.AuraApiClient
import org.koin.dsl.module

val appModule = module {
    single { AuraApiClient("http://10.0.2.2:8000", get()) }
}

class TokenManager { fun getAccessToken(): String? = null }