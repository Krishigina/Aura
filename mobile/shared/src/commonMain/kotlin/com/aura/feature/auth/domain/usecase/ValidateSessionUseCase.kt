package com.aura.feature.auth.domain.usecase

import com.aura.feature.auth.domain.repository.AuthRepository

class ValidateSessionUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(): Boolean = repository.validateSession()
}
