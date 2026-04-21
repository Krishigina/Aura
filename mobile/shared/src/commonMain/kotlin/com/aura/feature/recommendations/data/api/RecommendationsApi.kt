package com.aura.feature.recommendations.data.api

import com.aura.core.data.api.model.RecommendationFavoritesResponse
import com.aura.core.data.api.model.RecommendationResponse

interface RecommendationsApi {
    suspend fun generateRecommendation(token: String): RecommendationResponse
    suspend fun saveFavorite(token: String, recommendation: RecommendationResponse)
    suspend fun getFavorites(token: String): RecommendationFavoritesResponse
}
