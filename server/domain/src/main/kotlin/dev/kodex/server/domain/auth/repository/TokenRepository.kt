package dev.kodex.server.domain.auth.repository

import kotlin.time.Instant

data class RefreshTokenRecord(
    val id: Long,
    val userId: Long,
    val tokenHash: String,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val revokedAt: Instant?,
    val deviceHint: String?,
    val ipAddress: String?,
)

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
