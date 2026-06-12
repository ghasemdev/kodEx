package dev.kodex.server.domain.auth.model

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
