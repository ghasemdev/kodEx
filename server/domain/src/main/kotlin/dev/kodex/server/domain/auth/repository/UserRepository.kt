package dev.kodex.server.domain.auth.repository

import dev.kodex.core.models.auth.Role
import dev.kodex.server.domain.auth.model.UserRecord
import kotlin.time.Instant

@Suppress("TooManyFunctions", "ComplexInterface")
interface UserRepository {
    suspend fun findById(id: Long): UserRecord?
    suspend fun findByEmail(email: String): UserRecord?
    suspend fun findByUsername(username: String): UserRecord?
    suspend fun isUsernameTaken(username: String): Boolean
    suspend fun isEmailTaken(email: String): Boolean
    suspend fun create(username: String, email: String, passwordHash: String?, role: Role): UserRecord
    suspend fun setEmailVerified(userId: Long)
    suspend fun updatePasswordHash(userId: Long, hash: String)
    suspend fun incrementFailedLoginCount(userId: Long)
    suspend fun resetFailedLoginCount(userId: Long)
    suspend fun lockUntil(userId: Long, until: Instant)
    suspend fun updateEmail(userId: Long, newEmail: String)
    suspend fun updateUsername(userId: Long, newUsername: String)
    suspend fun generateUsername(emailPrefix: String): String
}
