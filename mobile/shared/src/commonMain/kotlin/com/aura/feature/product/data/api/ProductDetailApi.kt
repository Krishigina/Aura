package com.aura.feature.product.data.api

import com.aura.core.domain.model.Product
import com.aura.core.domain.model.ProductDetailResponse
import com.aura.core.domain.model.ProductPhoto

interface ProductDetailApi {
    suspend fun getProductDetail(token: String, productId: Int): ProductDetailResponse
    suspend fun getProducts(token: String, hydratePhotos: Boolean): List<Product>
    suspend fun getProductPhotos(productId: Int): List<ProductPhoto>
}
