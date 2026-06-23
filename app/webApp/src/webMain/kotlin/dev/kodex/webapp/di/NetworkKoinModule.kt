package dev.kodex.webapp.di

import dev.kodex.webapp.auth.AuthStore
import dev.kodex.webapp.auth.TokenInterceptorPlugin
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.browser.localStorage
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class NetworkKoinModule {
    @Single
    fun httpClient(authStore: Lazy<AuthStore>): HttpClient = HttpClient(Js) {
        install(ContentNegotiation) {
            // ignoreUnknownKeys: server ApiMeta fields are deserialized too, but
            // keeping this guard is defensive against any future envelope additions.
            json(Json { ignoreUnknownKeys = true })
        }
        defaultRequest {
            headers {
                append("lang", localStorage.getItem("kodex-locale") ?: "en")
            }
        }
        expectSuccess = false
        install(TokenInterceptorPlugin) { this.authStore = authStore }
    }
}
