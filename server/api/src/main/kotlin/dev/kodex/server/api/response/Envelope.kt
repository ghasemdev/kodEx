package dev.kodex.server.api.response

import kotlinx.serialization.Serializable
import kotlin.time.Clock

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

fun <T> buildEnvelope(data: T, requestId: String, service: String, version: String): Envelope<T> =
    Envelope(
        data = data,
        meta = Meta(
            requestId = requestId,
            timestamp = Clock.System.now().toString(),
            service = service,
            serviceVersion = version,
        ),
    )
