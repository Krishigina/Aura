package com.aura.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class BackendUser(
    val id: Int,
    val name: String,
    val email: String,
    val role: String = "user",
    val nickname: String? = null,
    val phone: String? = null,
    val avatar: String? = null,
    val created_at: String? = null
)

@Serializable
data class AuthResponse(
    val access_token: String,
    val token_type: String = "bearer",
    val user: BackendUser
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String = "user",
    val nickname: String = "",
    val phone: String? = null,
    val avatar: String? = null
)

@Serializable
data class SkinPassportRequest(
    val answers: Map<String, List<String>>,
    val completed_at_epoch_millis: Long? = null
)

@Serializable
data class SkinPassportResponse(
    val completed_at_epoch_millis: Long? = null,
    val answers: Map<String, List<String>> = emptyMap()
)
