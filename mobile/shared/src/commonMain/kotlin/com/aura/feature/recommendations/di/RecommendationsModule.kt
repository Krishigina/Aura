package com.aura.feature.recommendations.di

import com.aura.feature.recommendations.data.api.AuraRecommendationsApi
import com.aura.feature.recommendations.data.api.RecommendationsApi
import com.aura.feature.recommendations.data.repository.RecommendationsRepositoryImpl
import com.aura.feature.recommendations.domain.repository.RecommendationsRepository
import com.aura.feature.recommendations.domain.usecase.GenerateRecommendationUseCase
import com.aura.feature.recommendations.domain.usecase.GetSavedRecommendationUseCase
import com.aura.feature.recommendations.domain.usecase.SaveRecommendationFavoriteUseCase
import com.aura.feature.recommendations.presentation.RecommendationsViewModel
import org.koin.dsl.module

val recommendationsModule = module {
    single<RecommendationsApi> { AuraRecommendationsApi(get()) }
    single<RecommendationsRepository> { RecommendationsRepositoryImpl(get(), get()) }
    factory { GenerateRecommendationUseCase(get()) }
    factory { SaveRecommendationFavoriteUseCase(get()) }
    factory { GetSavedRecommendationUseCase(get()) }
    factory { RecommendationsViewModel(get(), get(), get()) }
}
