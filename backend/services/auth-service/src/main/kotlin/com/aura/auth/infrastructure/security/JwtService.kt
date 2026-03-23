package com.aura.auth.infrastructure.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import java.util.concurrent.TimeUnit

class JwtService(
    private val secret: String,
    private val accessTokenExpiration: Long,
    private val refreshTokenExpiration: Long
) {
    private val algorithm = Algorithm.HMAC256(secret)
    
    fun generateAccessToken(userId: String): String {
        return JWT.create()
            .withClaim("userId", userId)
            .withClaim("type", "access")
            .withIssuedAt(java.util.Date())
            .withExpiresAt(java.util.Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(accessTokenExpiration)))
            .sign(algorithm)
    }
    
    fun verifyToken(token: String): DecodedJWT? {
        return try {
            JWT.require(algorithm).build().verify(token)
        } catch (e: Exception) {
            null
        }
    }
    
    fun getUserId(token: String): String? {
        return verifyToken(token)?.getClaim("userId")?.asString()
    }
}

class PasswordService {
    private val saltLength = 16
    private val hashIterations = 100000
    
    fun hash(password: String): String {
        val salt = generateSalt()
        val hash = pbkdf2(password, salt)
        return "$salt:$hash"
    }
    
    fun verify(password: String, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != 2) return false
        val salt = parts[0]
        val hash = parts[1]
        val computedHash = pbkdf2(password, salt)
        return hash == computedHash
    }
    
    private fun generateSalt(): String {
        val salt = ByteArray(saltLength)
        java.security.SecureRandom().nextBytes(salt)
        return java.util.Base64.getEncoder().encodeToString(salt)
    }
    
    private fun pbkdf2(password: String, salt: String): String {
        val saltBytes = java.util.Base64.getDecoder().decode(salt)
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        var result = password.toByteArray() + saltBytes
        repeat(hashIterations) {
            result = digest.digest(result)
        }
        return java.util.Base64.getEncoder().encodeToString(result)
    }
}