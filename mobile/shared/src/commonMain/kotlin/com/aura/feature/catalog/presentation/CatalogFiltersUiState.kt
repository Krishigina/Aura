package com.aura.feature.catalog.presentation

import com.aura.feature.catalog.CatalogFilterOptions
import com.aura.feature.catalog.CatalogProductFilters

data class CatalogFiltersUiState(
    val filters: CatalogProductFilters = CatalogProductFilters(),
    val options: CatalogFilterOptions = CatalogFilterOptions(),
    val isLoading: Boolean = true,
)
