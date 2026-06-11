package dev.kodex.server.domain.auth.repository

import kotlin.time.Instant

data class EmergencyRevokeTokenRecord(
    val id: Long,
    val userId: Long,
    val tokenHash: String,
    val expiresAt: Instant,
    val usedAt: Instant?,
)

interface EmergencyRevokeTokenRepository {
    suspend fun create(userId: Long, tokenHash: String, expiresAt: Instant): EmergencyRevokeTokenRecord
    suspend fun findActiveByHash(tokenHash: String): EmergencyRevokeTokenRecord?
    suspend fun markUsed(id: Long)
}
