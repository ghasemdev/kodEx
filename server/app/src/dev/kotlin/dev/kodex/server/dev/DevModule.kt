package dev.kodex.server.dev

import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import org.koin.dsl.module

val DEV_MODULE = module {
    // dev-only bindings
}

fun Routing.devRoutes() {
    get("/dev/ping") {
        call.respond("pong")
    }
}
