package com.aura.feature.product.presentation

import com.aura.core.domain.model.ProductDetailResponse

data class ProductDetailUiState(
    val detail: ProductDetailResponse? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isAssistantLoading: Boolean = false,
    val isRoutineLoading: Boolean = false,
    val isInFavorites: Boolean = false,
    val routineFeedback: String? = null,
)
