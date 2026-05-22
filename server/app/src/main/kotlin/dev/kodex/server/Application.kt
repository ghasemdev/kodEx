package dev.kodex.server

import dev.kodex.core.env.env
import dev.kodex.core.env.envOrNull
import dev.kodex.server.api.response.buildErrorEnvelope
import dev.kodex.server.api.routes.healthRoutes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import kotlin.time.Clock
import kotlin.uuid.Uuid
import org.koin.ktor.plugin.Koin

private const val PORT = 8080

fun main() {
    val port = envOrNull("SERVER_PORT")?.toInt() ?: PORT
    val host = envOrNull("SERVER_HOST") ?: "0.0.0.0"
    val webAppOrigin = env("WEBAPP_ORIGIN")
    val startedAt = Clock.System.now()

    embeddedServer(Netty, port = port, host = host) {
        install(ContentNegotiation) { json() }

        // SEC-007: standard browser security headers
        install(DefaultHeaders) {
            header("X-Content-Type-Options", "nosniff")
            header("X-Frame-Options", "DENY")
            header("Referrer-Policy", "strict-origin-when-cross-origin")
        }

        install(CORS) {
            allowOrigins { it == webAppOrigin }
            allowCredentials = true
            allowNonSimpleContentTypes = true
            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Delete)
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.Authorization)
        }

        // SEC-004: log full exception server-side; return generic message to client
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                call.application.log.error("Unhandled exception", cause)
                val requestId = call.request.headers["X-Request-Id"] ?: Uuid.random().toString()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    buildErrorEnvelope(
                        message = "An unexpected error occurred.",
                        requestId = requestId,
                        service = "kodex-api",
                        version = BuildConfig.VERSION,
                    ),
                )
            }
        }

        install(Koin) {
            modules(serverModule)
        }

        routing {
            healthRoutes(startedAt = startedAt, version = BuildConfig.VERSION)
        }
    }.start(wait = true)
}
