package com.aura.feature.auth.domain.usecase

import com.aura.feature.auth.domain.repository.AuthRepository

class LoadSessionUseCase(private val repository: AuthRepository) {
    operator fun invoke(): Boolean = repository.loadSession()
}
