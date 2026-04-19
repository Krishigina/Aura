package com.aura.feature.catalog.data.api

import com.aura.core.domain.model.Product

interface CatalogApi {
    suspend fun getProducts(token: String?, hydratePhotos: Boolean): List<Product>
    suspend fun loadImageDataFromUrl(url: String): String?
}
