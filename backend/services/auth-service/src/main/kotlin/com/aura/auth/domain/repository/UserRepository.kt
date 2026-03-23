package com.aura.auth.domain.repository

import com.aura.auth.domain.model.User
import java.util.UUID

interface UserRepository {
    suspend fun findById(id: UUID): User?
    suspend fun findByEmail(email: String): User?
    suspend fun save(user: User): User
    suspend fun update(user: User): User
    suspend fun delete(id: UUID)
}

interface SessionRepository {
    suspend fun findByRefreshToken(token: String): Session?
    suspend fun findByUserId(userId: UUID): List<Session>
    suspend fun save(session: Session): Session
    suspend fun delete(id: UUID)
    suspend fun deleteByUserId(userId: UUID)
}