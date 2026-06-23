package dev.kodex.webapp.network

import dev.kodex.core.models.api.ApiEnvelope
import dev.kodex.core.models.api.ApiErrorEnvelope
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess

class ApiException(val code: String, override val message: String) : Exception(message)

suspend inline fun <reified T> HttpResponse.envelopeDataOrThrow(): T {
    if (!status.isSuccess()) {
        val error = body<ApiErrorEnvelope>().data
        throw ApiException(error.code, error.userMessage)
    }
    return body<ApiEnvelope<T>>().data
}
