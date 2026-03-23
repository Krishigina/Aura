package com.aura.auth.application.usecase

import com.aura.auth.domain.model.User
import com.aura.auth.domain.repository.UserRepository
import com.aura.auth.infrastructure.security.PasswordService

class RegisterUseCase(
    private val userRepository: UserRepository,
    private val passwordService: PasswordService
) {
    suspend fun execute(email: String, password: String, name: String?): Result<User> {
        if (email.isBlank() || !email.contains("@")) {
            return Result.failure(IllegalArgumentException("Invalid email"))
        }
        if (password.length < 8) {
            return Result.failure(IllegalArgumentException("Password must be at least 8 characters"))
        }
        
        val existingUser = userRepository.findByEmail(email)
        if (existingUser != null) {
            return Result.failure(IllegalArgumentException("Email already registered"))
        }
        
        val passwordHash = passwordService.hash(password)
        val user = User(
            email = email.lowercase().trim(),
            passwordHash = passwordHash,
            name = name?.trim()
        )
        
        return Result.success(userRepository.save(user))
    }
}