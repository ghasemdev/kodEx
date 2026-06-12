package dev.kodex.server.domain.auth.repository

import dev.kodex.server.domain.auth.model.RefreshTokenRecord
import kotlin.time.Instant

interface TokenRepository {
    suspend fun create(
        userId: Long,
        tokenHash: String,
        expiresAt: Instant,
        deviceHint: String?,
        ipAddress: String?,
    ): RefreshTokenRecord

    suspend fun findActiveByHash(tokenHash: String): RefreshTokenRecord?
    suspend fun revoke(tokenHash: String)
    suspend fun revokeAll(userId: Long)
    suspend fun revokeAllExcept(userId: Long, exceptHash: String)
    suspend fun rotate(
        oldHash: String,
        newHash: String,
        newExpiresAt: Instant,
        deviceHint: String?,
        ipAddress: String?,
    ): RefreshTokenRecord

    suspend fun findActiveByUserId(userId: Long): List<RefreshTokenRecord>
    suspend fun revokeById(id: Long, userId: Long)
}
