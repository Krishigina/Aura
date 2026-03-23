package com.aura.auth.presentation.router

import com.aura.auth.application.dto.*
import com.aura.auth.application.usecase.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun Route.authRouter(
    registerUseCase: RegisterUseCase,
    loginUseCase: LoginUseCase,
    refreshTokenUseCase: RefreshTokenUseCase
) {
    post("/register") {
        val request = call.receive<RegisterRequest>()
        
        registerUseCase.execute(request.email, request.password, request.name)
            .onSuccess { user ->
                call.respond(AuthResponse(
                    userId = user.id.toString(),
                    email = user.email,
                    name = user.name,
                    accessToken = "",
                    refreshToken = "",
                    expiresAt = ""
                ))
            }
            .onFailure { error ->
                call.respond(400, ErrorResponse("REGISTER_FAILED", error.message ?: "Unknown error"))
            }
    }
    
    post("/login") {
        val request = call.receive<LoginRequest>()
        
        loginUseCase.execute(request.email, request.password, request.deviceInfo, call.request.local.remoteAddress)
            .onSuccess { result ->
                call.respond(AuthResponse(
                    userId = result.user.id.toString(),
                    email = result.user.email,
                    name = result.user.name,
                    accessToken = result.accessToken,
                    refreshToken = result.refreshToken,
                    expiresAt = result.expiresAt.toString()
                ))
            }
            .onFailure { error ->
                call.respond(401, ErrorResponse("LOGIN_FAILED", error.message ?: "Invalid credentials"))
            }
    }
    
    post("/refresh") {
        val request = call.receive<RefreshTokenRequest>()
        
        refreshTokenUseCase.execute(request.refreshToken)
            .onSuccess { result ->
                call.respond(RefreshResponse(
                    accessToken = result.accessToken,
                    refreshToken = result.refreshToken,
                    expiresAt = result.expiresAt.toString()
                ))
            }
            .onFailure { error ->
                call.respond(401, ErrorResponse("REFRESH_FAILED", error.message ?: "Invalid token"))
            }
    }
}

@Serializable
data class RegisterRequest(val email: String, val password: String, val name: String? = null)

@Serializable
data class LoginRequest(val email: String, val password: String, val deviceInfo: String? = null)

@Serializable
data class RefreshTokenRequest(val refreshToken: String)

@Serializable
data class AuthResponse(
    val userId: String, val email: String, val name: String?,
    val accessToken: String, val refreshToken: String, val expiresAt: String
)

@Serializable
data class RefreshResponse(val accessToken: String, val refreshToken: String, val expiresAt: String)

@Serializable
data class ErrorResponse(val code: String, val message: String)