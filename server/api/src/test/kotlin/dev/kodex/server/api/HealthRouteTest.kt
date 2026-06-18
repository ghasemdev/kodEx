package dev.kodex.server.api

import dev.kodex.core.models.api.ApiEnvelope
import dev.kodex.core.models.health.HealthResponse
import dev.kodex.server.api.routes.healthRoutes
import dev.kodex.server.api.util.serviceInfo
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
    val version = "0.1.0"

    test("GET /api/v1/health returns 200") {
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                routing { healthRoutes(startedAt = startedAt, info = serviceInfo) }
            }
            val response = client.get("/api/v1/health")
            response.status shouldBe HttpStatusCode.OK
        }
    }

    test("GET /api/v1/health returns envelope with status UP") {
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                routing { healthRoutes(startedAt = startedAt, info = serviceInfo) }
            }
            val response = client.get("/api/v1/health")
            val body = Json.decodeFromString<ApiEnvelope<HealthResponse>>(response.bodyAsText())
            body.data.status shouldBe "UP"
        }
    }

    test("GET /api/v1/health meta has service kodex-api") {
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                routing { healthRoutes(startedAt = startedAt, info = serviceInfo) }
            }
            val response = client.get("/api/v1/health")
            val body = Json.decodeFromString<ApiEnvelope<HealthResponse>>(response.bodyAsText())
            body.meta.service shouldBe "kodex-api-test"
            body.meta.serviceVersion shouldBe version
        }
    }

    test("GET /api/v1/health meta requestId is non-empty") {
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                routing { healthRoutes(startedAt = startedAt, info = serviceInfo) }
            }
            val response = client.get("/api/v1/health")
            val body = Json.decodeFromString<ApiEnvelope<HealthResponse>>(response.bodyAsText())
            body.meta.requestId.shouldNotBeEmpty()
        }
    }

    test("GET /api/v1/health meta timestamp is non-empty") {
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                routing { healthRoutes(startedAt = startedAt, info = serviceInfo) }
            }
            val response = client.get("/api/v1/health")
            val body = Json.decodeFromString<ApiEnvelope<HealthResponse>>(response.bodyAsText())
            body.meta.timestamp.shouldNotBeEmpty()
        }
    }

    test("GET /api/v1/health sets Cache-Control no-store") {
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                routing { healthRoutes(startedAt = startedAt, info = serviceInfo) }
            }
            val response = client.get("/api/v1/health")
            response.headers["Cache-Control"] shouldBe "no-store"
        }
    }

    test("GET /api/v1/health valid UUID in X-Request-Id is echoed back unchanged") {
        val clientRequestId = "550e8400-e29b-41d4-a716-446655440000"
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                routing { healthRoutes(startedAt = startedAt, info = serviceInfo) }
            }
            val response = client.get("/api/v1/health") {
                headers.append("X-Request-Id", clientRequestId)
            }
            val body = Json.decodeFromString<ApiEnvelope<HealthResponse>>(response.bodyAsText())
            body.meta.requestId shouldBe clientRequestId
        }
    }

    test("GET /api/v1/health non-UUID X-Request-Id is replaced with a server-generated UUID") {
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                routing { healthRoutes(startedAt = startedAt, info = serviceInfo) }
            }
            val response = client.get("/api/v1/health") {
                headers.append("X-Request-Id", "not-a-uuid")
            }
            val body = Json.decodeFromString<ApiEnvelope<HealthResponse>>(response.bodyAsText())
            body.meta.requestId shouldNotBe "not-a-uuid"
            body.meta.requestId.shouldNotBeEmpty()
        }
    }
})
