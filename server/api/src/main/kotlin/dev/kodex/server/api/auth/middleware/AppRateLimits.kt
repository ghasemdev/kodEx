package dev.kodex.server.api.auth.middleware

import io.ktor.server.plugins.ratelimit.RateLimitName

object AppRateLimits {
    val PUBLIC = RateLimitName("public")
    val AUTH = RateLimitName("auth")
}
