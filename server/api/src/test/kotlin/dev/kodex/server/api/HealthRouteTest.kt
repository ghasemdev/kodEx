package dev.kodex.server.api

import dev.kodex.core.models.health.HealthResponse
import dev.kodex.server.api.response.Envelope
import dev.kodex.server.api.routes.healthRoutes
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeEmpty
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.time.Clock
import kotlinx.serialization.json.Json

class HealthRouteTest : FunSpec({
    val startedAt = Clock.System.now()
    val version = "0.1.0-SNAPSHOT"

    test("GET /api/v1/health returns 200") {
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                routing { healthRoutes(startedAt = startedAt, version = version) }
            }
            val response = client.get("/api/v1/health")
            response.status shouldBe HttpStatusCode.OK
        }
    }

    test("GET /api/v1/health returns envelope with status UP") {
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                routing { healthRoutes(startedAt = startedAt, version = version) }
            }
            val response = client.get("/api/v1/health")
            val body = Json.decodeFromString<Envelope<HealthResponse>>(response.bodyAsText())
            body.data.status shouldBe "UP"
        }
    }

    test("GET /api/v1/health meta has service kodex-api") {
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                routing { healthRoutes(startedAt = startedAt, version = version) }
            }
            val response = client.get("/api/v1/health")
            val body = Json.decodeFromString<Envelope<HealthResponse>>(response.bodyAsText())
            body.meta.service shouldBe "kodex-api"
            body.meta.serviceVersion shouldBe version
        }
    }

    test("GET /api/v1/health meta requestId is non-empty") {
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                routing { healthRoutes(startedAt = startedAt, version = version) }
            }
            val response = client.get("/api/v1/health")
            val body = Json.decodeFromString<Envelope<HealthResponse>>(response.bodyAsText())
            body.meta.requestId.shouldNotBeEmpty()
        }
    }

    test("GET /api/v1/health meta timestamp is non-empty") {
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                routing { healthRoutes(startedAt = startedAt, version = version) }
            }
            val response = client.get("/api/v1/health")
            val body = Json.decodeFromString<Envelope<HealthResponse>>(response.bodyAsText())
            body.meta.timestamp.shouldNotBeEmpty()
        }
    }

    test("GET /api/v1/health sets Cache-Control no-store") {
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                routing { healthRoutes(startedAt = startedAt, version = version) }
            }
            val response = client.get("/api/v1/health")
            response.headers["Cache-Control"] shouldBe "no-store"
        }
    }

    test("GET /api/v1/health X-Request-Id header is forwarded as requestId") {
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                routing { healthRoutes(startedAt = startedAt, version = version) }
            }
            val response = client.get("/api/v1/health") {
                headers.append("X-Request-Id", "test-request-id-123")
            }
            val body = Json.decodeFromString<Envelope<HealthResponse>>(response.bodyAsText())
            body.meta.requestId shouldBe "test-request-id-123"
            body.meta.requestId shouldNotBe null
        }
    }
})
