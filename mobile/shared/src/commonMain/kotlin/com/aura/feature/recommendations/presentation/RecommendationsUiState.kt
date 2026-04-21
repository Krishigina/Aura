package com.aura.feature.recommendations.presentation

import com.aura.core.data.api.model.RecommendationResponse

data class RecommendationsUiState(
    val recommendation: RecommendationResponse? = null,
    val selectedLineKey: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val needsPassport: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val saveError: String? = null,
)
