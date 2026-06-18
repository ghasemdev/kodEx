package dev.kodex.server.domain.auth.model

import kotlin.time.Instant

data class EmailVerificationTokenRecord(
    val id: Long,
    val userId: Long,
    val tokenHash: String,
    val deliveryMode: DeliveryMode,
    val expiresAt: Instant,
    val usedAt: Instant?,
)
