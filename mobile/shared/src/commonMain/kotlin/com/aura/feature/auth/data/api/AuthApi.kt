package com.aura.feature.auth.data.api

import com.aura.core.domain.model.AuthResponse
import com.aura.core.domain.model.BackendUser

interface AuthApi {
    suspend fun login(email: String, password: String): AuthResponse
    suspend fun register(name: String, email: String, password: String, nickname: String): AuthResponse
    suspend fun getMe(token: String): BackendUser
}
