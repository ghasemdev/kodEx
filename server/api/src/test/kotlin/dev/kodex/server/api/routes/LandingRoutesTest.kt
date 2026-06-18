package dev.kodex.server.api.routes

import dev.kodex.core.models.api.ApiEnvelope
import dev.kodex.core.models.landing.LandingStatsResponse
import dev.kodex.server.api.util.serviceInfo
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json

class LandingRoutesTest : FunSpec({
    test("GET /api/v1/stats/landing returns 200") {
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                routing { landingRoutes(serviceInfo) }
            }
            val response = client.get("/api/v1/stats/landing")
            response.status shouldBe HttpStatusCode.OK
        }
    }

    test("GET /api/v1/stats/landing returns JSON content type") {
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                routing { landingRoutes(serviceInfo) }
            }
            val response = client.get("/api/v1/stats/landing")
            response.headers["Content-Type"] shouldContain ContentType.Application.Json.toString()
        }
    }

    test("GET /api/v1/stats/landing body contains totalProblems") {
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                routing { landingRoutes(serviceInfo) }
            }
            val response = client.get("/api/v1/stats/landing")
            val body = JSON
                .decodeFromString<ApiEnvelope<LandingStatsResponse>>(response.bodyAsText())
            body.data.totalProblems shouldBe 1247
        }
    }

    test("GET /api/v1/stats/landing body contains totalUsers") {
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                routing { landingRoutes(serviceInfo) }
            }
            val response = client.get("/api/v1/stats/landing")
            val body = JSON
                .decodeFromString<ApiEnvelope<LandingStatsResponse>>(response.bodyAsText())
            body.data.totalUsers shouldBe 8432
        }
    }

    test("GET /api/v1/stats/landing body contains totalContests") {
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                routing { landingRoutes(serviceInfo) }
            }
            val response = client.get("/api/v1/stats/landing")
            val body = JSON
                .decodeFromString<ApiEnvelope<LandingStatsResponse>>(response.bodyAsText())
            body.data.totalContests shouldBe 342
        }
    }

    test("GET /api/v1/stats/landing sets Cache-Control public max-age=300") {
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                routing { landingRoutes(serviceInfo) }
            }
            val response = client.get("/api/v1/stats/landing")
            response.headers["Cache-Control"] shouldBe "public, max-age=300"
        }
    }
})

private val JSON = Json { ignoreUnknownKeys = true }
