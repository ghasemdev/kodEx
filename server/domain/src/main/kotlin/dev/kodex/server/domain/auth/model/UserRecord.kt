package dev.kodex.server.domain.auth.model

import dev.kodex.core.models.auth.Role
import kotlin.time.Instant

data class UserRecord(
    val id: Long,
    val username: String,
    val email: String,
    val emailVerified: Boolean,
    val passwordHash: String?,
    val role: Role,
    val failedLoginCount: Int,
    val lockedUntil: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
