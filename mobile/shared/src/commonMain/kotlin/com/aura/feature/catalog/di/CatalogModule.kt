package com.aura.feature.catalog.di

import com.aura.feature.catalog.data.api.AuraCatalogApi
import com.aura.feature.catalog.data.api.CatalogApi
import com.aura.feature.catalog.data.repository.CatalogRepositoryImpl
import com.aura.feature.catalog.domain.repository.CatalogRepository
import com.aura.feature.catalog.domain.usecase.GetCatalogProductsUseCase
import com.aura.feature.catalog.domain.usecase.GetCatalogThumbnailUseCase
import com.aura.feature.catalog.presentation.CatalogFiltersViewModel
import com.aura.feature.catalog.presentation.CatalogViewModel
import org.koin.dsl.module

val catalogModule = module {
    single<CatalogApi> { AuraCatalogApi(get()) }
    single<CatalogRepository> { CatalogRepositoryImpl(get(), get()) }
    factory { GetCatalogProductsUseCase(get()) }
    factory { GetCatalogThumbnailUseCase(get()) }
    factory { CatalogViewModel(get(), get()) }
    factory { CatalogFiltersViewModel(get()) }
}
