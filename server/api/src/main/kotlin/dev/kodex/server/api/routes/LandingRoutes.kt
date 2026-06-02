package dev.kodex.server.api.routes

import dev.kodex.core.models.landing.LandingStatsResponse
import dev.kodex.server.api.response.buildEnvelope
import io.ktor.http.HttpHeaders
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import kotlin.uuid.Uuid

private const val TOTAL_PROBLEMS = 1247
private const val TOTAL_USERS = 8432
private const val TOTAL_CONTESTS = 342

fun Routing.landingRoutes(service: String = "kodex-api", version: String = "x.y.z") {
    get("/api/v1/stats/landing") {
        call.response.header(HttpHeaders.CacheControl, "public, max-age=300")
        val requestId = call.request.headers["X-Request-Id"] ?: Uuid.random().toString()
        call.respond(
            buildEnvelope(
                data = LandingStatsResponse(
                    totalProblems = TOTAL_PROBLEMS,
                    totalUsers = TOTAL_USERS,
                    totalContests = TOTAL_CONTESTS,
                ),
                requestId = requestId,
                service = service,
                version = version,
            ),
        )
    }
}
