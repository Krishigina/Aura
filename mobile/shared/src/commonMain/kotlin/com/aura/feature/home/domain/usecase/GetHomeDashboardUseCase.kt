package com.aura.feature.home.domain.usecase

import com.aura.feature.home.domain.repository.HomeRepository
class GetHomeDashboardUseCase(private val repository: HomeRepository) {
    suspend operator fun invoke() = repository.getDashboard()
}
