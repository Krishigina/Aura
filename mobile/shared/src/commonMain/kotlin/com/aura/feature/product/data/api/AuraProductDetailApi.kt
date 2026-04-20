package com.aura.feature.product.data.api

import com.aura.core.data.api.client.ProductNetworkClient
import com.aura.core.domain.model.Product
import com.aura.core.domain.model.ProductDetailResponse
import com.aura.core.domain.model.ProductPhoto

internal class AuraProductDetailApi(
    private val apiClient: ProductNetworkClient,
) : ProductDetailApi {
    override suspend fun getProductDetail(token: String, productId: Int): ProductDetailResponse {
        return apiClient.getProductDetail(token, productId)
    }

    override suspend fun getProducts(token: String, hydratePhotos: Boolean): List<Product> {
        return apiClient.getProducts(token, hydratePhotos)
    }

    override suspend fun getProductPhotos(productId: Int): List<ProductPhoto> {
        return apiClient.getProductPhotos(productId)
    }
}
