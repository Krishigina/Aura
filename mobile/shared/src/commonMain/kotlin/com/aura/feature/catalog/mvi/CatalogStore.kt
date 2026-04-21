package com.aura.feature.catalog.mvi

import com.aura.core.data.api.AuraApiClient
import com.aura.core.presentation.mvi.MviStore

class CatalogStore(private val apiClient: AuraApiClient) : MviStore<CatalogUiState, CatalogIntent>(CatalogUiState()) {
    override fun reduce(state: CatalogUiState, intent: CatalogIntent): CatalogUiState = catalogReduce(state, intent)

    suspend fun loadProducts(): CatalogUiState {
        dispatch(CatalogIntent.RetryLoad)

        return runCatching {
            val products = apiClient.getProducts()
            dispatch(CatalogIntent.ProductsLoaded(products))
        }.getOrElse { e ->
            dispatch(CatalogIntent.ProductsLoadFailed(e.message ?: "Ошибка загрузки"))
        }
    }
}
