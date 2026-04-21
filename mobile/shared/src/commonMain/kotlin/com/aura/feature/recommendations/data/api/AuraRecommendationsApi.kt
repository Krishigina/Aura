package com.aura.feature.recommendations.data.api

import com.aura.core.data.api.client.RecommendationsNetworkClient
import com.aura.core.data.api.model.RecommendationFavoritesResponse
import com.aura.core.data.api.model.RecommendationResponse

internal class AuraRecommendationsApi(
    private val apiClient: RecommendationsNetworkClient,
) : RecommendationsApi {
    override suspend fun generateRecommendation(token: String): RecommendationResponse {
        return apiClient.generateRecommendation(token)
    }

    override suspend fun saveFavorite(token: String, recommendation: RecommendationResponse) {
        apiClient.saveFavorite(token, recommendation)
    }

    override suspend fun getFavorites(token: String): RecommendationFavoritesResponse {
        return apiClient.getFavorites(token)
    }
}
