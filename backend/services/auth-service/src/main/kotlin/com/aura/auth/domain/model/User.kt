package com.aura.auth.domain.model

import java.time.LocalDateTime
import java.util.UUID

interface Entity {
    val id: UUID
}

data class User(
    override val id: UUID = UUID.randomUUID(),
    val email: String,
    val passwordHash: String,
    val name: String? = null,
    val avatarUrl: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) : Entity

data class Session(
    override val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val refreshToken: String,
    val deviceInfo: String? = null,
    val ipAddress: String? = null,
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime = LocalDateTime.now()
) : Entity