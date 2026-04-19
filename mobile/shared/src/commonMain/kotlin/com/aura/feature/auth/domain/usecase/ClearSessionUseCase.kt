package com.aura.feature.auth.domain.usecase

import com.aura.feature.auth.domain.repository.AuthRepository

class ClearSessionUseCase(private val repository: AuthRepository) {
    operator fun invoke() = repository.clearSession()
}
