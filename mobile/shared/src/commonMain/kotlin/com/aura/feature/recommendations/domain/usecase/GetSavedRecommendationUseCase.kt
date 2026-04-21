package com.aura.feature.recommendations.domain.usecase

import com.aura.feature.recommendations.domain.repository.RecommendationsRepository
class GetSavedRecommendationUseCase(private val repository: RecommendationsRepository) {
    suspend operator fun invoke(favoriteId: String) = repository.getFavorite(favoriteId)
}
