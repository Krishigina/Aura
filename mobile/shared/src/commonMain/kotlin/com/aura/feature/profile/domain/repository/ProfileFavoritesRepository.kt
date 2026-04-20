package com.aura.feature.profile.domain.repository

import com.aura.core.data.api.model.RecommendationFavorite

interface ProfileFavoritesRepository {
    suspend fun loadFavorites(): List<RecommendationFavorite>
}
