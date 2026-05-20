package dev.kodex.core.models.health

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class HealthResponseTest : FunSpec({
    test("serialization round trip") {
        val original = HealthResponse(
            status = "UP",
            startedAt = Clock.System.now()
        )

        val json = Json.encodeToString(HealthResponse.serializer(), original)
        val decoded = Json.decodeFromString<HealthResponse>(json)

        decoded shouldBe original
    }

    test("status field serializes") {
        val response = HealthResponse(
            status = "UP",
            startedAt = Clock.System.now()
        )

        val json = Json.encodeToString(HealthResponse.serializer(), response)

        json shouldContain "\"status\":\"UP\""
    }

    test("startedAt field serializes") {
        val response = HealthResponse(
            status = "UP",
            startedAt = Clock.System.now()
        )

        val json = Json.encodeToString(HealthResponse.serializer(), response)

        json shouldContain "startedAt"
    }
})
