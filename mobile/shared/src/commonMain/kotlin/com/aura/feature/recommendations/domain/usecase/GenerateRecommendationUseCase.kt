package com.aura.feature.recommendations.domain.usecase

import com.aura.feature.recommendations.domain.repository.RecommendationsRepository
class GenerateRecommendationUseCase(private val repository: RecommendationsRepository) {
    suspend operator fun invoke() = repository.generateRecommendation()
}
