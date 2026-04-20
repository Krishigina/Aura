package com.aura.feature.profile.data.repository

import com.aura.core.data.api.model.RecommendationFavorite
import com.aura.core.domain.repository.SessionRepository
import com.aura.feature.profile.data.api.ProfileApi
import com.aura.feature.profile.domain.repository.ProfileFavoritesRepository

class ProfileFavoritesRepositoryImpl(
    private val profileApi: ProfileApi,
    private val sessionRepository: SessionRepository,
) : ProfileFavoritesRepository {
    override suspend fun loadFavorites(): List<RecommendationFavorite> {
        val token = sessionRepository.token().takeUnless { it.isNullOrBlank() } ?: return emptyList()
        return profileApi.getRecommendationFavorites(token).items
    }
}
