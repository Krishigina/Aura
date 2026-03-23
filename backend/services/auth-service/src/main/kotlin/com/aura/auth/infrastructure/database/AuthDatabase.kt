package com.aura.auth.infrastructure.database

import com.aura.auth.domain.model.User
import com.aura.auth.domain.model.Session
import com.aura.auth.domain.repository.UserRepository
import com.aura.auth.domain.repository.SessionRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID

object UsersTable : Table("users") {
    val id = uuid("id").primaryKey()
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val name = varchar("name", 100).nullable()
    val avatarUrl = varchar("avatar_url", 500).nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

object SessionsTable : Table("sessions") {
    val id = uuid("id").primaryKey()
    val userId = uuid("user_id").references(UsersTable.id)
    val refreshToken = varchar("refresh_token", 255).uniqueIndex()
    val deviceInfo = varchar("device_info", 500).nullable()
    val ipAddress = varchar("ip_address", 45).nullable()
    val expiresAt = datetime("expires_at")
    val createdAt = datetime("created_at")
}

class UserRepositoryImpl(private val database: Database) : UserRepository {
    override suspend fun findById(id: UUID): User? = transaction(database) {
        UsersTable.select { UsersTable.id eq id }.firstOrNull()?.toUser()
    }
    
    override suspend fun findByEmail(email: String): User? = transaction(database) {
        UsersTable.select { UsersTable.email eq email.lowercase() }.firstOrNull()?.toUser()
    }
    
    override suspend fun save(user: User): User = transaction(database) {
        val now = LocalDateTime.now()
        UsersTable.insert {
            it[id] = user.id
            it[email] = user.email
            it[passwordHash] = user.passwordHash
            it[name] = user.name
            it[avatarUrl] = user.avatarUrl
            it[createdAt] = now
            it[updatedAt] = now
        }
        user
    }
    
    override suspend fun update(user: User): User = transaction(database) {
        UsersTable.update({ UsersTable.id eq user.id }) {
            it[name] = user.name
            it[avatarUrl] = user.avatarUrl
            it[updatedAt] = LocalDateTime.now()
        }
        user
    }
    
    override suspend fun delete(id: UUID) = transaction(database) {
        UsersTable.deleteWhere { UsersTable.id eq id }
    }
    
    private fun ResultRow.toUser() = User(
        id = this[UsersTable.id],
        email = this[UsersTable.email],
        passwordHash = this[UsersTable.passwordHash],
        name = this[UsersTable.name],
        avatarUrl = this[UsersTable.avatarUrl],
        createdAt = this[UsersTable.createdAt],
        updatedAt = this[UsersTable.updatedAt]
    )
}

class SessionRepositoryImpl(private val database: Database) : SessionRepository {
    override suspend fun findByRefreshToken(token: String): Session? = transaction(database) {
        SessionsTable.select { SessionsTable.refreshToken eq token }.firstOrNull()?.toSession()
    }
    
    override suspend fun findByUserId(userId: UUID): List<Session> = transaction(database) {
        SessionsTable.select { SessionsTable.userId eq userId }.map { it.toSession() }
    }
    
    override suspend fun save(session: Session): Session = transaction(database) {
        SessionsTable.insert {
            it[id] = session.id
            it[userId] = session.userId
            it[refreshToken] = session.refreshToken
            it[deviceInfo] = session.deviceInfo
            it[ipAddress] = session.ipAddress
            it[expiresAt] = session.expiresAt
            it[createdAt] = session.createdAt
        }
        session
    }
    
    override suspend fun delete(id: UUID) = transaction(database) {
        SessionsTable.deleteWhere { SessionsTable.id eq id }
    }
    
    override suspend fun deleteByUserId(userId: UUID) = transaction(database) {
        SessionsTable.deleteWhere { SessionsTable.userId eq userId }
    }
    
    private fun ResultRow.toSession() = Session(
        id = this[SessionsTable.id],
        userId = this[SessionsTable.userId],
        refreshToken = this[SessionsTable.refreshToken],
        deviceInfo = this[SessionsTable.deviceInfo],
        ipAddress = this[SessionsTable.ipAddress],
        expiresAt = this[SessionsTable.expiresAt],
        createdAt = this[SessionsTable.createdAt]
    )
}