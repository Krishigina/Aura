package com.aura.feature.profile.presentation.favorites

import com.aura.core.data.api.model.RecommendationFavorite

data class ProfileFavoritesUiState(
    val favorites: List<RecommendationFavorite> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)
