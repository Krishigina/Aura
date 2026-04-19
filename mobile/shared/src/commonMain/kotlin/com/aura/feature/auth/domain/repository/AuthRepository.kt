package com.aura.feature.auth.domain.repository

interface AuthRepository {
    suspend fun login(email: String, password: String)
    suspend fun register(name: String, email: String, password: String, nickname: String)
    fun loadSession(): Boolean
    suspend fun validateSession(): Boolean
    fun clearSession()
}
