package com.aura.feature.catalog.mvi

import com.aura.core.domain.model.Product

private fun rebuild(state: CatalogUiState, products: List<Product> = state.products): CatalogUiState {
    val filterChips = products
        .mapNotNull { it.product_type }
        .distinct()
        .filter { it.isNotBlank() }
        .take(6)

    val filtered = products.filter { product ->
        val matchesSearch = state.searchQuery.isEmpty() ||
            product.name?.contains(state.searchQuery, ignoreCase = true) == true ||
            product.brand?.contains(state.searchQuery, ignoreCase = true) == true

        val matchesFilter = state.selectedFilter == null ||
            product.product_type == state.selectedFilter ||
            (product.skin_type?.any { it == state.selectedFilter } == true) ||
            (product.purpose?.any { it == state.selectedFilter } == true)

        matchesSearch && matchesFilter
    }

    return state.copy(
        products = products,
        filteredProducts = filtered,
        filterChips = filterChips
    )
}

fun catalogReduce(state: CatalogUiState, intent: CatalogIntent): CatalogUiState {
    return when (intent) {
        is CatalogIntent.ProductsLoaded -> {
            rebuild(
                state.copy(
                    isLoading = false,
                    error = null,
                    products = intent.products
                ),
                intent.products
            )
        }

        is CatalogIntent.ProductsLoadFailed -> {
            state.copy(isLoading = false, error = intent.message)
        }

        is CatalogIntent.SearchChanged -> {
            rebuild(state.copy(searchQuery = intent.query))
        }

        is CatalogIntent.FilterToggled -> {
            val nextFilter = if (state.selectedFilter == intent.filter) null else intent.filter
            rebuild(state.copy(selectedFilter = nextFilter))
        }

        CatalogIntent.RetryLoad -> {
            state.copy(isLoading = true, error = null)
        }
    }
}
