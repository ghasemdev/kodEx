package dev.kodex.server.domain.auth.model

import dev.kodex.core.models.auth.OAuthProvider
import kotlin.time.Instant

data class OAuthIdentityRecord(
    val id: Long,
    val userId: Long,
    val provider: OAuthProvider,
    val providerUserId: String,
    val linkedAt: Instant,
)
