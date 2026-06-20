package dev.kodex.server.domain.auth.service

import kotlin.time.Instant

interface AccountLockedEmailPort {
    suspend fun sendAccountLocked(to: String, lockedUntil: Instant)
}
