package com.aura.auth.application.usecase

import com.aura.auth.domain.model.Session
import com.aura.auth.domain.model.User
import com.aura.auth.domain.repository.SessionRepository
import com.aura.auth.domain.repository.UserRepository
import com.aura.auth.infrastructure.security.JwtService
import com.aura.auth.infrastructure.security.PasswordService
import java.time.LocalDateTime
import java.util.UUID

data class AuthResult(
    val user: User,
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: LocalDateTime
)

class LoginUseCase(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val passwordService: PasswordService,
    private val jwtService: JwtService
) {
    suspend fun execute(email: String, password: String, deviceInfo: String?, ipAddress: String?): Result<AuthResult> {
        val user = userRepository.findByEmail(email.lowercase().trim())
            ?: return Result.failure(IllegalArgumentException("Invalid credentials"))
        
        if (!passwordService.verify(password, user.passwordHash)) {
            return Result.failure(IllegalArgumentException("Invalid credentials"))
        }
        
        val refreshToken = UUID.randomUUID().toString()
        val expiresAt = LocalDateTime.now().plusDays(7)
        
        val session = Session(
            userId = user.id,
            refreshToken = refreshToken,
            deviceInfo = deviceInfo,
            ipAddress = ipAddress,
            expiresAt = expiresAt
        )
        sessionRepository.save(session)
        
        val accessToken = jwtService.generateAccessToken(user.id.toString())
        
        return Result.success(AuthResult(user, accessToken, refreshToken, expiresAt))
    }
}