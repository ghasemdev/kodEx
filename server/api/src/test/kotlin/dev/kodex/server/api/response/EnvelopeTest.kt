package dev.kodex.server.api.response

import dev.kodex.core.models.api.ApiErrorEnvelope
import dev.kodex.server.api.util.serviceInfo
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import kotlinx.serialization.json.Json

class EnvelopeTest : FunSpec({
    test("buildErrorEnvelope sets error code and message") {
        with(serviceInfo) {
            val envelope = buildErrorEnvelope(
                code = ErrorCode.INTERNAL_SERVER_ERROR,
                message = "An unexpected error occurred.",
                requestId = "req-1",
            )

            envelope.data.code shouldBe ErrorCode.INTERNAL_SERVER_ERROR
            envelope.data.message shouldBe "An unexpected error occurred."
        }
    }

    test("buildErrorEnvelope returns English userMessage by default") {
        with(serviceInfo) {
            val envelope = buildErrorEnvelope(
                code = ErrorCode.FORBIDDEN,
                message = "Insufficient permissions.",
                requestId = "req-1",
            )
            envelope.data.userMessage.shouldNotBeEmpty()
        }
    }

    test("buildErrorEnvelope returns Farsi userMessage when lang=fa") {
        with(serviceInfo) {
            val envelope = buildErrorEnvelope(
                code = ErrorCode.FORBIDDEN,
                message = "Insufficient permissions.",
                lang = "fa",
                requestId = "req-1",
            )
            envelope.data.userMessage.shouldNotBeEmpty()
        }
    }

    test("buildErrorEnvelope falls back to message for unknown code") {
        with(serviceInfo) {
            val envelope = buildErrorEnvelope(
                code = "UNKNOWN_CODE",
                message = "Something went wrong.",
                requestId = "req-1",
            )
            envelope.data.userMessage shouldBe "Something went wrong."
        }
    }

    test("buildErrorEnvelope falls back to English for unsupported lang") {
        with(serviceInfo) {
            val enEn = buildErrorEnvelope(
                code = ErrorCode.UNAUTHORIZED,
                message = "error",
                lang = "en",
                requestId = "req-1",
            )
            val enUnknown = buildErrorEnvelope(
                code = ErrorCode.UNAUTHORIZED,
                message = "error",
                lang = "zz",
                requestId = "req-1",
            )
            enUnknown.data.userMessage shouldBe enEn.data.userMessage
        }
    }

    test("buildErrorEnvelope sets meta requestId") {
        with(serviceInfo) {
            val envelope = buildErrorEnvelope(
                code = ErrorCode.UNAUTHORIZED,
                message = "error",
                requestId = "req-abc",
            )
            envelope.meta.requestId shouldBe "req-abc"
        }
    }

    test("buildErrorEnvelope sets meta service and version") {
        with(ServiceInfo(name = "kodex-api-test", version = "1.2.3")) {
            val envelope = buildErrorEnvelope(
                code = ErrorCode.NOT_FOUND,
                message = "error",
                requestId = "req-1",
            )
            envelope.meta.service shouldBe "kodex-api-test"
            envelope.meta.serviceVersion shouldBe "1.2.3"
        }
    }

    test("buildErrorEnvelope sets meta timestamp") {
        with(serviceInfo) {
            val envelope = buildErrorEnvelope(
                code = ErrorCode.INTERNAL_SERVER_ERROR,
                message = "error",
                requestId = "req-1",
            )
            envelope.meta.timestamp.shouldNotBeEmpty()
        }
    }

    test("ApiErrorEnvelope serialization roundtrip") {
        with(serviceInfo) {
            val original = buildErrorEnvelope(
                code = ErrorCode.INTERNAL_SERVER_ERROR,
                message = "An unexpected error occurred.",
                requestId = "req-roundtrip",
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
    }

    test("buildEnvelope sets data and meta correctly") {
        with(serviceInfo) {
            val envelope = buildEnvelope(
                data = "hello",
                requestId = "req-2",
            )
            envelope.data shouldBe "hello"
            envelope.meta.requestId shouldBe "req-2"
            envelope.meta.service shouldBe "kodex-api-test"
            envelope.meta.serviceVersion shouldBe "0.1.0"
            envelope.meta.timestamp.shouldNotBeEmpty()
        }
    }
})
