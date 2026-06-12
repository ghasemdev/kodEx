package dev.kodex.server.api.response

import dev.kodex.core.models.api.ApiEnvelope
import dev.kodex.core.models.api.ApiError
import dev.kodex.core.models.api.ApiErrorEnvelope
import dev.kodex.core.models.api.ApiMeta
import kotlin.time.Clock

fun <T> buildEnvelope(data: T, requestId: String, service: String, version: String): ApiEnvelope<T> = ApiEnvelope(
    data = data,
    meta = ApiMeta(
        requestId = requestId,
        timestamp = Clock.System.now().toString(),
        service = service,
        serviceVersion = version,
    ),
)

fun buildErrorEnvelope(
    code: String,
    message: String,
    lang: String = DEFAULT_LANG,
    requestId: String,
    service: String,
    version: String,
): ApiErrorEnvelope {
    val effectiveLang = lang.takeIf { it in SUPPORTED_LANGUAGES } ?: DEFAULT_LANG
    val localizedMessage = errorUserMessages[code]?.get(effectiveLang)
        ?: errorUserMessages[code]?.get(DEFAULT_LANG)
        ?: message
    return ApiErrorEnvelope(
        data = ApiError(code = code, message = message, userMessage = localizedMessage),
        meta = ApiMeta(
            requestId = requestId,
            timestamp = Clock.System.now().toString(),
            service = service,
            serviceVersion = version,
        ),
    )
}
