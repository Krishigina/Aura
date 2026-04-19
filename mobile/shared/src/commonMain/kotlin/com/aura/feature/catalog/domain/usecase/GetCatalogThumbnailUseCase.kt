package com.aura.feature.catalog.domain.usecase

import com.aura.feature.catalog.domain.repository.CatalogRepository
class GetCatalogThumbnailUseCase(private val repository: CatalogRepository) {
    suspend operator fun invoke(url: String) = repository.loadThumbnailData(url)
}
