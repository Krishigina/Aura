package com.aura.feature.catalog.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.feature.catalog.CatalogProductFilters
import com.aura.feature.catalog.domain.model.CatalogProduct
import com.aura.feature.catalog.domain.usecase.GetCatalogProductsUseCase
import com.aura.feature.catalog.domain.usecase.GetCatalogThumbnailUseCase
import com.aura.feature.catalog.filterCatalogProducts
import com.aura.feature.catalog.maxCatalogThumbnailPrefetchCount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CatalogViewModel(
    private val getCatalogProducts: GetCatalogProductsUseCase,
    private val getCatalogThumbnail: GetCatalogThumbnailUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state -> state.copy(searchQuery = query).withFilteredProducts() }
    }

    fun onFiltersChange(filters: CatalogProductFilters) {
        _uiState.update { state -> state.copy(filters = filters).withFilteredProducts() }
    }

    fun retry() {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val products = getCatalogProducts()
                _uiState.update {
                    it.copy(products = products, isLoading = false, error = null).withFilteredProducts()
                }
                loadThumbnails(products)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Ошибка загрузки") }
            }
        }
    }

    private suspend fun loadThumbnails(products: List<CatalogProduct>) {
        _uiState.update { it.copy(thumbnailDataByProductId = emptyMap()) }
        products
            .asSequence()
            .filter { product -> product.photos.isEmpty() && !product.thumbnailUrl.isNullOrBlank() }
            .take(maxCatalogThumbnailPrefetchCount())
            .forEach { product ->
                val url = product.thumbnailUrl ?: return@forEach
                val data = getCatalogThumbnail(url)
                if (!data.isNullOrBlank()) {
                    _uiState.update { state ->
                        state.copy(thumbnailDataByProductId = state.thumbnailDataByProductId + (product.id to data))
                    }
                }
            }
    }

    private fun CatalogUiState.withFilteredProducts(): CatalogUiState {
        return copy(filteredProducts = filterCatalogProducts(products, searchQuery, filters))
    }
}
