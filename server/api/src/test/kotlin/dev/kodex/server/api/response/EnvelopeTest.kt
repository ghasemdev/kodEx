package dev.kodex.server.api.response

import dev.kodex.core.models.api.ApiErrorEnvelope
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import kotlinx.serialization.json.Json

class EnvelopeTest : FunSpec({
    test("buildErrorEnvelope sets error message") {
        val envelope = buildErrorEnvelope(
            message = "An unexpected error occurred.",
            requestId = "req-1",
            service = "kodex-api",
            version = "0.1.0",
        )
        envelope.error shouldBe "An unexpected error occurred."
    }

    test("buildErrorEnvelope sets meta requestId") {
        val envelope = buildErrorEnvelope(
            message = "error",
            requestId = "req-abc",
            service = "kodex-api",
            version = "0.1.0",
        )
        envelope.meta.requestId shouldBe "req-abc"
    }

    test("buildErrorEnvelope sets meta service and version") {
        val envelope = buildErrorEnvelope(
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
            message = "error",
            requestId = "req-1",
            service = "kodex-api",
            version = "0.1.0",
        )
        envelope.meta.timestamp.shouldNotBeEmpty()
    }

    test("ApiErrorEnvelope serialization roundtrip") {
        val original = buildErrorEnvelope(
            message = "An unexpected error occurred.",
            requestId = "req-roundtrip",
            service = "kodex-api",
            version = "0.1.0",
        )
        val json = Json.encodeToString(ApiErrorEnvelope.serializer(), original)
        val decoded = Json.decodeFromString<ApiErrorEnvelope>(json)
        decoded.error shouldBe original.error
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
