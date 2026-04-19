package com.aura.feature.catalog.domain.repository

import com.aura.feature.catalog.domain.model.CatalogProduct

interface CatalogRepository {
    suspend fun getProducts(): List<CatalogProduct>
    suspend fun loadThumbnailData(url: String): String?
}
