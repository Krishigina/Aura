package com.aura.feature.catalog.presentation

import com.aura.feature.catalog.CatalogProductFilters
import com.aura.feature.catalog.domain.model.CatalogProduct

data class CatalogUiState(
    val products: List<CatalogProduct> = emptyList(),
    val filteredProducts: List<CatalogProduct> = emptyList(),
    val filters: CatalogProductFilters = CatalogProductFilters(),
    val searchQuery: String = "",
    val thumbnailDataByProductId: Map<Int, String> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    val activeFiltersCount: Int get() = filters.activeCount
}
