package dev.kodex.core.models.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

val testMeta = ApiMeta(
    requestId = "req-123",
    timestamp = "2026-06-07T00:00:00Z",
    service = "kodex-api",
    serviceVersion = "0.1.0",
)

val testError = ApiError(
    code = "NOT_FOUND",
    message = "Resource not found.",
    userMessage = "The requested resource could not be found.",
)

class ApiEnvelopeTest : FunSpec({
    test("ApiEnvelope serialization round trip with string data") {
        val envelope = ApiEnvelope(data = "hello", meta = testMeta)
        val json = Json.encodeToString(ApiEnvelope.serializer(serializer<String>()), envelope)
        json shouldContain "hello"
        json shouldContain "req-123"
    }

    test("ApiMeta fields serialize correctly") {
        val json = Json.encodeToString(ApiMeta.serializer(), testMeta)
        json shouldContain "req-123"
        json shouldContain "kodex-api"
        json shouldContain "0.1.0"
    }

    test("ApiMeta round trip") {
        val json = Json.encodeToString(ApiMeta.serializer(), testMeta)
        val decoded = Json.decodeFromString<ApiMeta>(json)
        decoded shouldBe testMeta
    }

    test("ApiError fields serialize correctly") {
        val json = Json.encodeToString(ApiError.serializer(), testError)
        json shouldContain "NOT_FOUND"
        json shouldContain "Resource not found."
        json shouldContain "The requested resource could not be found."
    }

    test("ApiErrorEnvelope serialization round trip") {
        val errorEnvelope = ApiErrorEnvelope(data = testError, meta = testMeta)
        val json = Json.encodeToString(ApiErrorEnvelope.serializer(), errorEnvelope)
        val decoded = Json.decodeFromString<ApiErrorEnvelope>(json)
        decoded shouldBe errorEnvelope
    }

    test("ApiErrorEnvelope data fields serialize") {
        val errorEnvelope = ApiErrorEnvelope(data = testError, meta = testMeta)
        val json = Json.encodeToString(ApiErrorEnvelope.serializer(), errorEnvelope)
        json shouldContain "NOT_FOUND"
        json shouldContain testMeta.requestId
    }

    test("ApiError userMessage is a single string") {
        val error = ApiError(code = "UNAUTHORIZED", message = "Unauthorized", userMessage = "دسترسی غیرمجاز")
        val json = Json.encodeToString(ApiError.serializer(), error)
        val decoded = Json.decodeFromString<ApiError>(json)
        decoded.userMessage shouldBe "دسترسی غیرمجاز"
    }
})
