package dev.kodex.server

import dev.kodex.core.env.env
import dev.kodex.core.env.envOrNull
import dev.kodex.server.api.routes.healthRoutes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import org.koin.ktor.plugin.Koin
import kotlin.time.Clock

private const val PORT = 8080

fun main() {
    val port = envOrNull("SERVER_PORT")?.toInt() ?: PORT
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
