package dev.kodex.server

import dev.kodex.core.env.env
import dev.kodex.core.env.envOrNull
import dev.kodex.server.api.routes.healthRoutes
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.plugin.Koin
import kotlin.time.Clock

fun main() {
    val port = envOrNull("SERVER_PORT")?.toInt() ?: 8080
    val host = envOrNull("SERVER_HOST") ?: "0.0.0.0"
    val webAppOrigin = env("WEBAPP_ORIGIN")
    val startedAt = Clock.System.now()

    embeddedServer(Netty, port = port, host = host) {
        install(ContentNegotiation) { json() }

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

        install(StatusPages) {
            exception<Throwable> { call, cause ->
                call.respondText(
                    "Internal Server Error: ${cause.message}",
                    status = HttpStatusCode.InternalServerError,
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
