package com.aura.feature.catalog.domain.model

import com.aura.core.domain.model.ProductPhoto

data class CatalogProduct(
    val id: Int,
    val name: String?,
    val brand: String?,
    val productType: String?,
    val segment: String?,
    val category: String?,
    val skinTypes: List<String>,
    val compatibilityPercent: Int?,
    val photos: List<ProductPhoto>,
    val imageUrl: String?,
    val thumbnailUrl: String?,
    val hasVideo: Boolean,
    val decision: String?,
    val explanations: List<String>,
)
