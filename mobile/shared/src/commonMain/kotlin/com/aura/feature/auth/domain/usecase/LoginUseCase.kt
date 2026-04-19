package com.aura.feature.auth.domain.usecase

import com.aura.feature.auth.domain.repository.AuthRepository
class LoginUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String) = repository.login(email, password)
}
