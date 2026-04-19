package com.aura.feature.catalog.data.repository

import com.aura.core.domain.repository.SessionRepository
import com.aura.feature.catalog.data.api.CatalogApi
import com.aura.feature.catalog.data.mapper.toCatalogProduct
import com.aura.feature.catalog.domain.model.CatalogProduct
import com.aura.feature.catalog.domain.repository.CatalogRepository
import com.aura.feature.catalog.shouldHydrateCatalogProductPhotos

class CatalogRepositoryImpl(
    private val catalogApi: CatalogApi,
    private val sessionRepository: SessionRepository,
) : CatalogRepository {
    private var cachedProducts: List<CatalogProduct>? = null

    override suspend fun getProducts(): List<CatalogProduct> {
        cachedProducts?.let { cached ->
            if (cached.isNotEmpty()) return cached
        }

        val products = catalogApi
            .getProducts(sessionRepository.token(), hydratePhotos = shouldHydrateCatalogProductPhotos())
            .map { it.toCatalogProduct() }
        cachedProducts = products
        return products
    }

    override suspend fun loadThumbnailData(url: String): String? {
        return catalogApi.loadImageDataFromUrl(url)
    }
}
