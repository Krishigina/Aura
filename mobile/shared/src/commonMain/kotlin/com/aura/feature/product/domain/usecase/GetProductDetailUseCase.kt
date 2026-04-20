package com.aura.feature.product.domain.usecase

import com.aura.feature.product.domain.repository.ProductDetailRepository
class GetProductDetailUseCase(private val repository: ProductDetailRepository) {
    suspend operator fun invoke(productId: String) = repository.getProductDetail(productId)
}
