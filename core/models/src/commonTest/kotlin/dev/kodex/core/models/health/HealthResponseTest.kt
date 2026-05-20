package dev.kodex.core.models.health

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

class HealthResponseTest {
    @Test
    fun serializationRoundTrip() {
        val original = HealthResponse(
            status = "UP",
            startedAt = Clock.System.now(),
        )
        val json = Json.encodeToString(HealthResponse.serializer(), original)
        val decoded = Json.decodeFromString<HealthResponse>(json)
        assertEquals(original, decoded)
    }

    @Test
    fun statusFieldSerializes() {
        val response = HealthResponse(status = "UP", startedAt = Clock.System.now())
        val json = Json.encodeToString(HealthResponse.serializer(), response)
        assert(json.contains("\"status\":\"UP\""))
    }

    @Test
    fun startedAtFieldSerializes() {
        val response = HealthResponse(status = "UP", startedAt = Clock.System.now())
        val json = Json.encodeToString(HealthResponse.serializer(), response)
        assert(json.contains("startedAt"))
    }
}
