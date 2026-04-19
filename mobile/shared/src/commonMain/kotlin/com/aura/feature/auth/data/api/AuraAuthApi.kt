package com.aura.feature.auth.data.api

import com.aura.core.data.api.client.AuthNetworkClient
import com.aura.core.domain.model.AuthResponse
import com.aura.core.domain.model.BackendUser

internal class AuraAuthApi(
    private val apiClient: AuthNetworkClient,
) : AuthApi {
    override suspend fun login(email: String, password: String): AuthResponse {
        return apiClient.login(email, password)
    }

    override suspend fun register(name: String, email: String, password: String, nickname: String): AuthResponse {
        return apiClient.register(name, email, password, nickname)
    }

    override suspend fun getMe(token: String): BackendUser {
        return apiClient.getMe(token)
    }
}
