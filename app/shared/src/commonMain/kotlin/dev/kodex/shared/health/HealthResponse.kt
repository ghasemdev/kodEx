package dev.kodex.shared.health

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class HealthResponse(
    val status: String,
    val startedAt: Instant,
)
