package com.aura.core.data.api.model

import kotlinx.serialization.Serializable

@Serializable
internal data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
internal data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String = "user",
    val nickname: String = "",
    val avatar: String? = null,
)

@Serializable
internal data class ProfileAccountUpdateRequest(
    val name: String,
    val nickname: String? = null,
)

@Serializable
internal data class ProfilePasswordUpdateRequest(
    val current_password: String,
    val new_password: String,
)

@Serializable
internal data class ProfileDeleteRequest(
    val current_password: String,
)

@Serializable
internal data class SkinPassportRequest(
    val answers: Map<String, List<String>>,
    val completed_at_epoch_millis: Long? = null,
)
