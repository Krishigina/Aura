package com.aura.feature.home.domain.usecase

import com.aura.feature.home.domain.repository.HomeRepository
class GetHomeUserNameUseCase(private val repository: HomeRepository) {
    operator fun invoke() = repository.getUserName()
}
