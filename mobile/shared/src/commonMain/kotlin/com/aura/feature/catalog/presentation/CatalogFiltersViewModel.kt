package com.aura.feature.catalog.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.feature.catalog.CatalogProductFilters
import com.aura.feature.catalog.buildCatalogFilterOptions
import com.aura.feature.catalog.domain.usecase.GetCatalogProductsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CatalogFiltersViewModel(
    private val getCatalogProducts: GetCatalogProductsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CatalogFiltersUiState())
    val uiState: StateFlow<CatalogFiltersUiState> = _uiState.asStateFlow()

    fun load(initialFilters: CatalogProductFilters) {
        _uiState.update { it.copy(filters = initialFilters, isLoading = true) }
        viewModelScope.launch {
            val products = runCatching { getCatalogProducts() }
                .getOrDefault(emptyList())
            _uiState.update { it.copy(options = buildCatalogFilterOptions(products), isLoading = false) }
        }
    }

    fun updateFilters(filters: CatalogProductFilters) {
        _uiState.update { it.copy(filters = filters) }
    }

    fun clearFilters() {
        _uiState.update { it.copy(filters = CatalogProductFilters()) }
    }
}
