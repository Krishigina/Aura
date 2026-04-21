package com.aura.feature.recommendations.domain.repository

import com.aura.core.data.api.model.RecommendationResponse

interface RecommendationsRepository {
    suspend fun generateRecommendation(): RecommendationResponse
    suspend fun saveFavorite(recommendation: RecommendationResponse)
    suspend fun getFavorite(favoriteId: String): RecommendationResponse
}
