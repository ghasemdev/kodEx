package dev.kodex.webapp.di

import dev.kodex.shared.landing.LandingStatsRepository
import dev.kodex.webapp.pages.landing.LandingViewModel
import dev.kodex.webapp.pages.landing.data.LandingStatsRepositoryImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.koin.dsl.module

val appModule = module {
    single<HttpClient> {
        HttpClient(Js) {
            install(ContentNegotiation) { json() }
            // Dev: Vite proxies /api/* → http://localhost:8080
            // Prod: same-origin — no base URL override needed
            expectSuccess = false
        }
    }

    single<LandingStatsRepository> {
        LandingStatsRepositoryImpl(client = get())
    }

    factory { LandingViewModel(repository = get()) }
}
