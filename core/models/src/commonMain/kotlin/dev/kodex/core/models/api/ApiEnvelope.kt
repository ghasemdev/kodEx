package dev.kodex.core.models.api

import kotlinx.serialization.Serializable

/**
 * Unified API response envelope — single source of truth for both server (response builder)
 * and client (deserialization). Eliminates the server-only `Envelope<T>` duplicate.
 */
@Serializable
data class ApiEnvelope<T>(
    val data: T,
    val meta: ApiMeta,
)

@Serializable
data class ApiMeta(
    val requestId: String,
    val timestamp: String,
    val service: String,
    val serviceVersion: String,
)

@Serializable
data class ApiErrorEnvelope(
    val error: String,
    val meta: ApiMeta,
)
