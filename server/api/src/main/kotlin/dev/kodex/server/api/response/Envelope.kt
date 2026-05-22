package dev.kodex.server.api.response

import kotlin.time.Clock
import kotlinx.serialization.Serializable

@Serializable
data class Meta(
    val requestId: String,
    val timestamp: String,
    val service: String,
    val serviceVersion: String,
)

@Serializable
data class Envelope<T>(
    val data: T,
    val meta: Meta,
)

@Serializable
data class ErrorEnvelope(
    val error: String,
    val meta: Meta,
)

fun <T> buildEnvelope(data: T, requestId: String, service: String, version: String): Envelope<T> = Envelope(
    data = data,
    meta = Meta(
        requestId = requestId,
        timestamp = Clock.System.now().toString(),
        service = service,
        serviceVersion = version,
    ),
)

fun buildErrorEnvelope(message: String, requestId: String, service: String, version: String): ErrorEnvelope =
    ErrorEnvelope(
        error = message,
        meta = Meta(
            requestId = requestId,
            timestamp = Clock.System.now().toString(),
            service = service,
            serviceVersion = version,
        ),
    )
