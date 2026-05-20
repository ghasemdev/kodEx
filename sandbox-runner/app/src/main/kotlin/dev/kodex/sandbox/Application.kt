package dev.kodex.sandbox

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private const val SECRET_HEADER = "X-Sandbox-Secret"

fun main() {
    val port = System.getenv("SANDBOX_RUNNER_PORT")?.toInt() ?: 8081
    val host = System.getenv("SANDBOX_RUNNER_HOST") ?: "0.0.0.0"
    val sharedSecret = requireNotNull(System.getenv("SANDBOX_SHARED_SECRET")) {
        "Missing required env var: SANDBOX_SHARED_SECRET"
    }

    embeddedServer(Netty, port = port, host = host) {
        install(StatusPages) {
            exception<Throwable> { call, _ ->
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        routing {
            get("/health") {
                call.respond(HttpStatusCode.OK)
            }

            // All routes below require the shared secret
            route("/") {
                intercept(ApplicationCallPipeline.Plugins) {
                    val header = call.request.headers[SECRET_HEADER]
                    if (header != sharedSecret) {
                        call.respond(HttpStatusCode.Unauthorized)
                        finish()
                    }
                }
            }
        }
    }.start(wait = true)
}
