package com.aura.feature.product.di

import com.aura.feature.product.data.api.AuraProductDetailApi
import com.aura.feature.product.data.api.ProductDetailApi
import com.aura.feature.product.data.repository.ProductDetailRepositoryImpl
import com.aura.feature.product.domain.repository.ProductDetailRepository
import com.aura.feature.product.domain.usecase.GetProductDetailUseCase
import com.aura.feature.product.presentation.ProductDetailViewModel
import org.koin.dsl.module

val productModule = module {
    single<ProductDetailApi> { AuraProductDetailApi(get()) }
    single<ProductDetailRepository> { ProductDetailRepositoryImpl(get(), get()) }
    factory { GetProductDetailUseCase(get()) }
    factory { ProductDetailViewModel(get()) }
}
