package com.aura.feature.auth.data.repository

import com.aura.core.domain.repository.SessionRepository
import com.aura.feature.auth.data.api.AuthApi
import com.aura.feature.auth.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val authApi: AuthApi,
    private val sessionRepository: SessionRepository,
) : AuthRepository {
    override suspend fun login(email: String, password: String) {
        val response = authApi.login(email.trim(), password)
        sessionRepository.setToken(response.access_token)
        sessionRepository.setUser(response.user)
    }

    override suspend fun register(name: String, email: String, password: String, nickname: String) {
        val response = authApi.register(
            name = name.trim(),
            email = email.trim(),
            password = password,
            nickname = nickname.trim(),
        )
        sessionRepository.setToken(response.access_token)
        sessionRepository.setUser(response.user)
    }

    override fun loadSession(): Boolean {
        return sessionRepository.loadSession()
    }

    override suspend fun validateSession(): Boolean {
        val token = sessionRepository.token()
        if (token.isNullOrBlank()) {
            sessionRepository.clear()
            return false
        }

        return runCatching { authApi.getMe(token) }
            .onSuccess { sessionRepository.setUser(it) }
            .fold(
                onSuccess = { true },
                onFailure = { error ->
                    val message = error.message.orEmpty()
                    val unauthorized = message.contains("401") ||
                        message.contains("403") ||
                        message.contains("unauthorized", ignoreCase = true) ||
                        message.contains("forbidden", ignoreCase = true)
                    if (unauthorized) {
                        sessionRepository.clear()
                        false
                    } else {
                        true
                    }
                },
            )
    }

    override fun clearSession() {
        sessionRepository.clear()
    }
}
