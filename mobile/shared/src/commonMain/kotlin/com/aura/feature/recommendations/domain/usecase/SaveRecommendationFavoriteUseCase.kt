package com.aura.feature.recommendations.domain.usecase

import com.aura.core.data.api.model.RecommendationResponse
import com.aura.feature.recommendations.domain.repository.RecommendationsRepository
class SaveRecommendationFavoriteUseCase(private val repository: RecommendationsRepository) {
    suspend operator fun invoke(recommendation: RecommendationResponse) = repository.saveFavorite(recommendation)
}
