package dev.kodex.server.api.routes

import dev.kodex.core.models.health.HealthResponse
import dev.kodex.server.api.response.ServiceInfo
import dev.kodex.server.api.response.buildEnvelope
import dev.kodex.server.api.util.sanitizeRequestId
import io.ktor.http.HttpHeaders
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import kotlin.time.Instant

fun Routing.healthRoutes(startedAt: Instant, info: ServiceInfo) {
    get("/api/v1/health") {
        call.response.header(HttpHeaders.CacheControl, "no-store")
        val requestId = sanitizeRequestId(call.request.headers["X-Request-Id"])
        with(info) {
            call.respond(
                buildEnvelope(
                    data = HealthResponse(status = "UP", startedAt = startedAt),
                    requestId = requestId,
                ),
            )
        }
    }
}
