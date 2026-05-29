package dev.kodex.webapp.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.koin.dsl.module

val networkModule = module {
    single<HttpClient> {
        HttpClient(Js) {
            install(ContentNegotiation) { json() }
            // Dev: Vite proxies /api/* → http://localhost:8080
            // Prod: same-origin — no base URL override needed
            expectSuccess = false
        }
    }
}
