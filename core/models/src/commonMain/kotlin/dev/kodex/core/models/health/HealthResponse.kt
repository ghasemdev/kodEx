package dev.kodex.core.models.health

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val startedAt: Instant,
)
