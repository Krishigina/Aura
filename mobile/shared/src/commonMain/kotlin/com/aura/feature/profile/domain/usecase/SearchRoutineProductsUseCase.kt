package com.aura.feature.profile.domain.usecase

import com.aura.feature.profile.domain.repository.ProfileRoutineRepository

class SearchRoutineProductsUseCase(private val repository: ProfileRoutineRepository) {
    suspend operator fun invoke(query: String) = repository.searchProducts(query)
}
