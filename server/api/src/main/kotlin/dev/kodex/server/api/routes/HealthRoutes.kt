package dev.kodex.server.api.routes

import dev.kodex.core.models.health.HealthResponse
import dev.kodex.server.api.response.buildEnvelope
import io.ktor.http.HttpHeaders
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import kotlin.time.Instant
import kotlin.uuid.Uuid

fun Routing.healthRoutes(startedAt: Instant, version: String) {
    get("/api/v1/health") {
        call.response.header(HttpHeaders.CacheControl, "no-store")
        val requestId = call.request.headers["X-Request-Id"] ?: Uuid.random().toString()
        call.respond(
            buildEnvelope(
                data = HealthResponse(status = "UP", startedAt = startedAt),
                requestId = requestId,
                service = "kodex-api",
                version = version,
            ),
        )
    }
}
