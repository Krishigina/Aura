package com.aura.feature.recommendations.data.repository

import com.aura.core.data.api.model.RecommendationResponse
import com.aura.core.domain.repository.SessionRepository
import com.aura.feature.recommendations.data.api.RecommendationsApi
import com.aura.feature.recommendations.domain.repository.RecommendationsRepository

class RecommendationsRepositoryImpl(
    private val recommendationsApi: RecommendationsApi,
    private val sessionRepository: SessionRepository,
) : RecommendationsRepository {
    override suspend fun generateRecommendation(): RecommendationResponse {
        return recommendationsApi.generateRecommendation(requireToken())
    }

    override suspend fun saveFavorite(recommendation: RecommendationResponse) {
        recommendationsApi.saveFavorite(requireToken(), recommendation)
    }

    override suspend fun getFavorite(favoriteId: String): RecommendationResponse {
        val favorite = recommendationsApi.getFavorites(requireToken())
            .items
            .firstOrNull { it.favoriteId == favoriteId }
            ?: throw IllegalStateException("Рекомендация не найдена")
        return favorite.asRecommendationResponse()
    }

    private fun requireToken(): String {
        return sessionRepository.token().takeUnless { it.isNullOrBlank() }
            ?: throw IllegalStateException("Нужна авторизация")
    }
}
