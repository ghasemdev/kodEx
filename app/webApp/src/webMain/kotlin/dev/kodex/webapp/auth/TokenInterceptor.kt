package dev.kodex.webapp.auth

import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.plugin
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodedPath

private const val AUTH_PATH_PREFIX = "/api/v1/auth/"

@Suppress("LateinitUsage")
class TokenInterceptorConfig {
    lateinit var authStore: Lazy<AuthStore>
}

// authStore is Lazy<AuthStore> — not resolved at HttpClient construction time — to avoid a
// circular dependency (AuthStore -> AuthRemoteDataSource -> HttpClient). It's only realized
// the first time a request actually runs, by which point the client itself is fully built.
val TokenInterceptorPlugin = createClientPlugin("TokenInterceptor", ::TokenInterceptorConfig) {
    val authStore = pluginConfig.authStore
    client.plugin(HttpSend).intercept { request ->
        val loggedIn = authStore.value.state.value as? AuthState.LoggedIn
        if (loggedIn != null) {
            request.headers.remove(HttpHeaders.Authorization)
            request.headers.append(HttpHeaders.Authorization, "Bearer ${loggedIn.accessToken}")
        }

        var call = execute(request)
        val isAuthEndpoint = request.url.encodedPath.startsWith(AUTH_PATH_PREFIX)
        if (!isAuthEndpoint && call.response.status == HttpStatusCode.Unauthorized && authStore.value.refreshNow()) {
            val refreshed = authStore.value.state.value as? AuthState.LoggedIn
            if (refreshed != null) {
                request.headers.remove(HttpHeaders.Authorization)
                request.headers.append(HttpHeaders.Authorization, "Bearer ${refreshed.accessToken}")
                call = execute(request)
            }
        }
        call
    }
}
