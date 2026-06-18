@file:Suppress("MagicNumber")

package dev.kodex.server.api.routes

import dev.kodex.server.api.util.serviceInfo
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.time.Duration.Companion.minutes

class LandingRateLimitTest : FunSpec({
    test("GET /api/v1/stats/landing returns 429 after 60 requests within 1 minute") {
        testApplication {
            application {
                install(ContentNegotiation) { json() }
                install(RateLimit) {
                    register(RateLimitName("public")) {
                        rateLimiter(limit = 60, refillPeriod = 1.minutes)
                        requestKey { call -> call.request.local.remoteHost }
                    }
                }
                routing {
                    rateLimit(RateLimitName("public")) {
                        landingRoutes(serviceInfo)
                    }
                }
            }

            repeat(60) {
                val response = client.get("/api/v1/stats/landing")
                response.status shouldBe HttpStatusCode.OK
            }

            val response = client.get("/api/v1/stats/landing")
            response.status shouldBe HttpStatusCode.TooManyRequests
        }
    }
})
