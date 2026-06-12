package dev.kodex.server.domain.auth.model

import kotlin.time.Instant

data class EmergencyRevokeTokenRecord(
    val id: Long,
    val userId: Long,
    val tokenHash: String,
    val expiresAt: Instant,
    val usedAt: Instant?,
)
