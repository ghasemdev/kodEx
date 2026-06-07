package dev.kodex.server.api.response

import dev.kodex.core.models.api.ApiEnvelope
import dev.kodex.core.models.api.ApiErrorEnvelope
import dev.kodex.core.models.api.ApiMeta
import kotlin.time.Clock

// Envelope data classes live in core:models (ApiEnvelope, ApiMeta, ApiErrorEnvelope).
// These builder functions remain server-side — they populate service/version from server config.
fun <T> buildEnvelope(data: T, requestId: String, service: String, version: String): ApiEnvelope<T> = ApiEnvelope(
    data = data,
    meta = ApiMeta(
        requestId = requestId,
        timestamp = Clock.System.now().toString(),
        service = service,
        serviceVersion = version,
    ),
)

fun buildErrorEnvelope(message: String, requestId: String, service: String, version: String): ApiErrorEnvelope =
    ApiErrorEnvelope(
        error = message,
        meta = ApiMeta(
            requestId = requestId,
            timestamp = Clock.System.now().toString(),
            service = service,
            serviceVersion = version,
        ),
    )
