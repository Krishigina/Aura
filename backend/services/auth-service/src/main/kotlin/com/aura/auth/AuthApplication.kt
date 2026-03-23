package com.aura.auth

import com.aura.auth.domain.model.User
import com.aura.auth.domain.model.Session
import com.aura.auth.domain.repository.UserRepository
import com.aura.auth.domain.repository.SessionRepository
import com.aura.auth.application.usecase.RegisterUseCase
import com.aura.auth.application.usecase.LoginUseCase
import com.aura.auth.application.usecase.RefreshTokenUseCase
import com.aura.auth.infrastructure.database.UserRepositoryImpl
import com.aura.auth.infrastructure.database.SessionRepositoryImpl
import com.aura.auth.infrastructure.security.JwtService
import com.aura.auth.infrastructure.security.PasswordService
import com.aura.auth.presentation.router.authRouter
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import org.jetbrains.exposed.sql.Database
import kotlinx.serialization.json.Json

data class AppConfig(
    val host: String = "0.0.0.0",
    val port: Int = 8001,
    val database: DatabaseConfig = DatabaseConfig(),
    val jwt: JwtConfig = JwtConfig()
)

data class DatabaseConfig(
    val host: String = "localhost",
    val port: Int = 5432,
    val name: String = "aura",
    val user: String = "aura_user",
    val password: String = "aura_password"
)

data class JwtConfig(
    val secret: String = "aura-jwt-secret-key-2025",
    val accessTokenExpiration: Long = 3600,
    val refreshTokenExpiration: Long = 604800
)

fun loadConfig(): AppConfig {
    return AppConfig()
}

fun main() {
    val config = loadConfig()
    val database = Database.connect(
        url = "jdbc:postgresql://${config.database.host}:${config.database.port}/${config.database.name}",
        user = config.database.user,
        password = config.database.password
    )
    
    val passwordService = PasswordService()
    val jwtService = JwtService(
        secret = config.jwt.secret,
        accessTokenExpiration = config.jwt.accessTokenExpiration,
        refreshTokenExpiration = config.jwt.refreshTokenExpiration
    )
    
    val userRepository = UserRepositoryImpl(database)
    val sessionRepository = SessionRepositoryImpl(database)
    
    val registerUseCase = RegisterUseCase(userRepository, passwordService)
    val loginUseCase = LoginUseCase(userRepository, sessionRepository, passwordService, jwtService)
    val refreshTokenUseCase = RefreshTokenUseCase(sessionRepository, jwtService)
    
    embeddedServer(Netty, port = config.port) {
        install(ContentNegotiation) {
            json(Json { prettyPrint = true; isLenient = true })
        }
        
        routing {
            authRouter(registerUseCase, loginUseCase, refreshTokenUseCase)
        }
    }.start(wait = true)
}