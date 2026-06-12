package dev.kodex.server.domain.auth.repository

import dev.kodex.server.domain.auth.model.EmergencyRevokeTokenRecord
import kotlin.time.Instant

interface EmergencyRevokeTokenRepository {
    suspend fun create(userId: Long, tokenHash: String, expiresAt: Instant): EmergencyRevokeTokenRecord
    suspend fun findActiveByHash(tokenHash: String): EmergencyRevokeTokenRecord?
    suspend fun markUsed(id: Long)
}
