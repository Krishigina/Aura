package com.aura.feature.catalog.domain.usecase

import com.aura.feature.catalog.domain.repository.CatalogRepository
class GetCatalogProductsUseCase(private val repository: CatalogRepository) {
    suspend operator fun invoke() = repository.getProducts()
}
