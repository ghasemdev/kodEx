package dev.kodex.sandbox

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main() {
    val port = System.getenv("SANDBOX_RUNNER_PORT")?.toInt() ?: 8081
    val host = System.getenv("SANDBOX_RUNNER_HOST") ?: "0.0.0.0"

    embeddedServer(Netty, port = port, host = host) {
        routing {
            get("/health") {
                call.respond(HttpStatusCode.OK)
            }
        }
    }.start(wait = true)
}
