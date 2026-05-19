package dev.kodex.core.models.health

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class HealthResponse(
    val status: String,
    val startedAt: Instant,
)
