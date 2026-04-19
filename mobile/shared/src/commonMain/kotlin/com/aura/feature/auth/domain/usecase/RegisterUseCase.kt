package com.aura.feature.auth.domain.usecase

import com.aura.feature.auth.domain.repository.AuthRepository
class RegisterUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(name: String, email: String, password: String, nickname: String) =
        repository.register(name, email, password, nickname)
}
