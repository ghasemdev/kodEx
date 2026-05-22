package dev.kodex.sandbox

import dev.kodex.core.env.envOrNull
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.bearer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

private const val PORT = 8081

fun main() {
    val port = envOrNull("SANDBOX_RUNNER_PORT")?.toInt() ?: PORT
    val host = envOrNull("SANDBOX_RUNNER_HOST") ?: "0.0.0.0"
    val sharedSecret = requireNotNull(envOrNull("SANDBOX_SHARED_SECRET")) {
        "Missing required env var: SANDBOX_SHARED_SECRET"
    }

    embeddedServer(Netty, port = port, host = host) {
        install(StatusPages) {
            exception<Throwable> { call, _ ->
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
        install(Authentication) {
            bearer("secret-auth") {
                authenticate { tokenCredential ->
                    if (tokenCredential.token == sharedSecret) {
                        UserIdPrincipal("internal")
                    } else {
                        null
                    }
                }
            }
        }

        routing {
            get("/health") {
                call.respond(HttpStatusCode.OK)
            }

            // All routes below require the shared secret
            authenticate("secret-auth") {
                route("/") {}
            }
        }
    }.start(wait = true)
}
