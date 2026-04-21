package com.aura.feature.catalog.mvi

import com.aura.core.presentation.mvi.Intent
import com.aura.core.presentation.mvi.UiState
import com.aura.core.domain.model.Product

data class CatalogUiState(
    val products: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val filterChips: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedFilter: String? = null
): UiState

sealed interface CatalogIntent : Intent {
    data class ProductsLoaded(val products: List<Product>) : CatalogIntent
    data class ProductsLoadFailed(val message: String) : CatalogIntent
    data class SearchChanged(val query: String) : CatalogIntent
    data class FilterToggled(val filter: String) : CatalogIntent
    data object RetryLoad : CatalogIntent
}
