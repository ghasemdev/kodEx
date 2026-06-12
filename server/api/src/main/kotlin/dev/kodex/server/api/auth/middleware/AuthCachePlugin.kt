package dev.kodex.server.api.auth.middleware

import io.ktor.http.HttpHeaders
import io.ktor.server.application.createRouteScopedPlugin

// SEC-006: prevents browsers and proxies from caching auth responses
@Suppress("PropertyName", "RedundantSuppression")
val AuthCachePlugin = createRouteScopedPlugin("AuthCachePlugin") {
    onCall { call ->
        call.response.headers.append(HttpHeaders.CacheControl, "no-store")
    }
}
