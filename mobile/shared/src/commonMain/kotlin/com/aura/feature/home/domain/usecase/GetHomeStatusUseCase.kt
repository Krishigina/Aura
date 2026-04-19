package com.aura.feature.home.domain.usecase

import com.aura.feature.home.domain.repository.HomeRepository
class GetHomeStatusUseCase(private val repository: HomeRepository) {
    suspend operator fun invoke(latitude: Double?, longitude: Double?) = repository.getStatus(latitude, longitude)
}
