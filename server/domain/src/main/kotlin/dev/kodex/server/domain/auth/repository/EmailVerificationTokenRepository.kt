package dev.kodex.server.domain.auth.repository

import dev.kodex.server.domain.auth.model.DeliveryMode
import dev.kodex.server.domain.auth.model.EmailVerificationTokenRecord
import kotlin.time.Instant

interface EmailVerificationTokenRepository {
    suspend fun create(
        userId: Long,
        tokenHash: String,
        deliveryMode: DeliveryMode,
        expiresAt: Instant,
    ): EmailVerificationTokenRecord

    suspend fun findActiveByHash(tokenHash: String): EmailVerificationTokenRecord?
    suspend fun markUsed(id: Long)
    suspend fun invalidateForUser(userId: Long)
}
