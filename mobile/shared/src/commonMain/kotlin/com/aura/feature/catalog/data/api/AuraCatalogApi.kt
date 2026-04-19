package com.aura.feature.catalog.data.api

import com.aura.core.data.api.client.ProductNetworkClient
import com.aura.core.domain.model.Product

internal class AuraCatalogApi(
    private val apiClient: ProductNetworkClient,
) : CatalogApi {
    override suspend fun getProducts(token: String?, hydratePhotos: Boolean): List<Product> {
        return apiClient.getProducts(token, hydratePhotos)
    }

    override suspend fun loadImageDataFromUrl(url: String): String? {
        return apiClient.loadImageDataFromUrl(url)
    }
}
