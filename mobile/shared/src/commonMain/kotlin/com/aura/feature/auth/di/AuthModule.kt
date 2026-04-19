package com.aura.feature.auth.di

import com.aura.feature.auth.data.api.AuraAuthApi
import com.aura.feature.auth.data.api.AuthApi
import com.aura.feature.auth.data.repository.AuthRepositoryImpl
import com.aura.feature.auth.domain.repository.AuthRepository
import com.aura.feature.auth.domain.usecase.ClearSessionUseCase
import com.aura.feature.auth.domain.usecase.LoadSessionUseCase
import com.aura.feature.auth.domain.usecase.LoginUseCase
import com.aura.feature.auth.domain.usecase.RegisterUseCase
import com.aura.feature.auth.domain.usecase.ValidateSessionUseCase
import com.aura.feature.auth.presentation.AuthSessionViewModel
import com.aura.feature.auth.presentation.AuthViewModel
import org.koin.dsl.module

val authModule = module {
    single<AuthApi> { AuraAuthApi(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    factory { ClearSessionUseCase(get()) }
    factory { LoadSessionUseCase(get()) }
    factory { LoginUseCase(get()) }
    factory { RegisterUseCase(get()) }
    factory { ValidateSessionUseCase(get()) }
    factory { AuthSessionViewModel(get(), get(), get(), get()) }
    factory { AuthViewModel(get(), get()) }
}
