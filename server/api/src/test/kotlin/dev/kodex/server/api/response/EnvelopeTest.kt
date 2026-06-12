package dev.kodex.server.api.response

import dev.kodex.core.models.api.ApiErrorEnvelope
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import kotlinx.serialization.json.Json

class EnvelopeTest : FunSpec({
    test("buildErrorEnvelope sets error code and message") {
        val envelope = buildErrorEnvelope(
            code = ErrorCode.INTERNAL_SERVER_ERROR,
            message = "An unexpected error occurred.",
            requestId = "req-1",
            service = "kodex-api",
            version = "0.1.0",
        )
        envelope.data.code shouldBe ErrorCode.INTERNAL_SERVER_ERROR
        envelope.data.message shouldBe "An unexpected error occurred."
    }

    test("buildErrorEnvelope returns English userMessage by default") {
        val envelope = buildErrorEnvelope(
            code = ErrorCode.FORBIDDEN,
            message = "Insufficient permissions.",
            requestId = "req-1",
            service = "kodex-api",
            version = "0.1.0",
        )
        envelope.data.userMessage.shouldNotBeEmpty()
    }

    test("buildErrorEnvelope returns Farsi userMessage when lang=fa") {
        val envelope = buildErrorEnvelope(
            code = ErrorCode.FORBIDDEN,
            message = "Insufficient permissions.",
            lang = "fa",
            requestId = "req-1",
            service = "kodex-api",
            version = "0.1.0",
        )
        envelope.data.userMessage.shouldNotBeEmpty()
    }

    test("buildErrorEnvelope falls back to message for unknown code") {
        val envelope = buildErrorEnvelope(
            code = "UNKNOWN_CODE",
            message = "Something went wrong.",
            requestId = "req-1",
            service = "kodex-api",
            version = "0.1.0",
        )
        envelope.data.userMessage shouldBe "Something went wrong."
    }

    test("buildErrorEnvelope falls back to English for unsupported lang") {
        val enEn = buildErrorEnvelope(
            code = ErrorCode.UNAUTHORIZED,
            message = "error",
            lang = "en",
            requestId = "req-1",
            service = "kodex-api",
            version = "0.1.0",
        )
        val enUnknown = buildErrorEnvelope(
            code = ErrorCode.UNAUTHORIZED,
            message = "error",
            lang = "zz",
            requestId = "req-1",
            service = "kodex-api",
            version = "0.1.0",
        )
        enUnknown.data.userMessage shouldBe enEn.data.userMessage
    }

    test("buildErrorEnvelope sets meta requestId") {
        val envelope = buildErrorEnvelope(
            code = ErrorCode.UNAUTHORIZED,
            message = "error",
            requestId = "req-abc",
            service = "kodex-api",
            version = "0.1.0",
        )
        envelope.meta.requestId shouldBe "req-abc"
    }

    test("buildErrorEnvelope sets meta service and version") {
        val envelope = buildErrorEnvelope(
            code = ErrorCode.NOT_FOUND,
            message = "error",
            requestId = "req-1",
            service = "kodex-api",
            version = "1.2.3",
        )
        envelope.meta.service shouldBe "kodex-api"
        envelope.meta.serviceVersion shouldBe "1.2.3"
    }

    test("buildErrorEnvelope sets meta timestamp") {
        val envelope = buildErrorEnvelope(
            code = ErrorCode.INTERNAL_SERVER_ERROR,
            message = "error",
            requestId = "req-1",
            service = "kodex-api",
            version = "0.1.0",
        )
        envelope.meta.timestamp.shouldNotBeEmpty()
    }

    test("ApiErrorEnvelope serialization roundtrip") {
        val original = buildErrorEnvelope(
            code = ErrorCode.INTERNAL_SERVER_ERROR,
            message = "An unexpected error occurred.",
            requestId = "req-roundtrip",
            service = "kodex-api",
            version = "0.1.0",
        )
        val json = Json.encodeToString(ApiErrorEnvelope.serializer(), original)
        val decoded = Json.decodeFromString<ApiErrorEnvelope>(json)
        decoded.data.code shouldBe original.data.code
        decoded.data.message shouldBe original.data.message
        decoded.data.userMessage shouldBe original.data.userMessage
        decoded.meta.requestId shouldBe original.meta.requestId
        decoded.meta.service shouldBe original.meta.service
        decoded.meta.serviceVersion shouldBe original.meta.serviceVersion
        decoded.meta.timestamp shouldBe original.meta.timestamp
    }

    test("buildEnvelope sets data and meta correctly") {
        val envelope = buildEnvelope(
            data = "hello",
            requestId = "req-2",
            service = "kodex-api",
            version = "0.1.0",
        )
        envelope.data shouldBe "hello"
        envelope.meta.requestId shouldBe "req-2"
        envelope.meta.service shouldBe "kodex-api"
        envelope.meta.serviceVersion shouldBe "0.1.0"
        envelope.meta.timestamp.shouldNotBeEmpty()
    }
})
