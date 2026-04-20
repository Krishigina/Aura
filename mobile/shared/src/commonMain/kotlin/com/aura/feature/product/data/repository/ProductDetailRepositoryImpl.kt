package com.aura.feature.product.data.repository

import com.aura.core.domain.model.ProductDetailResponse
import com.aura.core.domain.repository.SessionRepository
import com.aura.feature.product.data.api.ProductDetailApi
import com.aura.feature.product.domain.repository.ProductDetailRepository

class ProductDetailRepositoryImpl(
    private val productDetailApi: ProductDetailApi,
    private val sessionRepository: SessionRepository,
) : ProductDetailRepository {
    override suspend fun getProductDetail(productId: String): ProductDetailResponse {
        val token = sessionRepository.token()
        val id = productId.toIntOrNull()
        if (token.isNullOrBlank() || id == null) {
            throw IllegalStateException("Нужна авторизация или некорректный продукт")
        }

        return try {
            productDetailApi.getProductDetail(token, id)
        } catch (e: Exception) {
            val message = e.message.orEmpty()
            val isStreamError = message.contains("unexpected end of stream", ignoreCase = true) ||
                message.contains("end of stream", ignoreCase = true)

            if (!isStreamError) {
                throw e
            }

            val baseProduct = productDetailApi
                .getProducts(token = token, hydratePhotos = false)
                .firstOrNull { it.id == id }
            if (baseProduct != null) {
                val photos = runCatching { productDetailApi.getProductPhotos(id) }.getOrDefault(emptyList())
                ProductDetailResponse(product = baseProduct.copy(photos = photos))
            } else {
                throw IllegalStateException("Не удалось загрузить полный профиль продукта. Попробуйте еще раз.")
            }
        }
    }
}
