package com.aura.feature.catalog.data.mapper

import com.aura.core.domain.model.Product
import com.aura.feature.catalog.domain.model.CatalogProduct

fun Product.toCatalogProduct(): CatalogProduct = CatalogProduct(
    id = id,
    name = name,
    brand = brand,
    productType = product_type,
    segment = segment,
    category = category,
    skinTypes = skin_type.orEmpty(),
    compatibilityPercent = compatibilityPercent,
    photos = photos.orEmpty(),
    imageUrl = imageUrl,
    thumbnailUrl = thumbnailUrl,
    hasVideo = hasVideo,
    decision = decision,
    explanations = explanations,
)
