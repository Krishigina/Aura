package com.aura.feature.product.domain.repository

import com.aura.core.domain.model.ProductDetailResponse

interface ProductDetailRepository {
    suspend fun getProductDetail(productId: String): ProductDetailResponse
}
