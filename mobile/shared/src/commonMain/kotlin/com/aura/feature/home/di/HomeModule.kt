package com.aura.feature.home.di

import com.aura.feature.home.data.api.AuraHomeApi
import com.aura.feature.home.data.api.HomeApi
import com.aura.feature.home.data.repository.HomeRepositoryImpl
import com.aura.feature.home.domain.repository.HomeRepository
import com.aura.feature.home.domain.usecase.GetHomeDashboardUseCase
import com.aura.feature.home.domain.usecase.GetHomeStatusUseCase
import com.aura.feature.home.domain.usecase.GetHomeUserNameUseCase
import com.aura.feature.home.presentation.HomeViewModel
import org.koin.dsl.module

val homeModule = module {
    single<HomeApi> { AuraHomeApi(get(), get(), get()) }
    single<HomeRepository> { HomeRepositoryImpl(get(), get()) }
    factory { GetHomeDashboardUseCase(get()) }
    factory { GetHomeStatusUseCase(get()) }
    factory { GetHomeUserNameUseCase(get()) }
    factory { HomeViewModel(get(), get(), get()) }
}
