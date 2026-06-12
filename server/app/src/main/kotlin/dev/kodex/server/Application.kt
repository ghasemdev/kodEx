package dev.kodex.server

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import dev.kodex.core.config.EnvConfig
import dev.kodex.core.env.env
import dev.kodex.core.env.envOrNull
import dev.kodex.core.models.auth.Role
import dev.kodex.server.api.auth.middleware.AppRateLimits
import dev.kodex.server.api.auth.middleware.ForbiddenException
import dev.kodex.server.api.response.ErrorCode
import dev.kodex.server.api.response.buildErrorEnvelope
import dev.kodex.server.api.routes.healthRoutes
import dev.kodex.server.api.routes.landingRoutes
import dev.kodex.server.api.util.sanitizeRequestId
import dev.kodex.server.di.KoinServerApplication
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.koin.plugin.module.dsl.withConfiguration

private const val PORT = 8080
private const val X_REQUEST_ID = "X-Request-Id"
private const val HEADER_LANGUAGE = "lang"

@Suppress("LongMethod")
fun main() {
    val port = envOrNull("SERVER_PORT")?.toInt() ?: PORT
    val host = envOrNull("SERVER_HOST") ?: "0.0.0.0"
    val webAppOrigin = env("WEBAPP_ORIGIN")
    val startedAt = Clock.System.now()

    embeddedServer(Netty, port = port, host = host) {
        install(ContentNegotiation) { json() }

        // SEC-007: standard browser security headers
        install(DefaultHeaders) {
            header("X-Content-Type-Options", "nosniff")
            header("X-Frame-Options", "DENY")
            header("Referrer-Policy", "strict-origin-when-cross-origin")
            header("Permissions-Policy", "camera=(), microphone=(), geolocation=()")
            header(
                "Content-Security-Policy",
                "default-src 'self'; " +
                    "script-src 'self' 'wasm-unsafe-eval'; " +
                    "style-src 'self' 'unsafe-inline'; " +
                    "font-src 'self' data:; " +
                    "img-src 'self' data:; " +
                    "connect-src 'self'; " +
                    "frame-ancestors 'none';",
            )
        }

        install(CORS) {
            allowOrigins { it == webAppOrigin }
            allowCredentials = true
            allowNonSimpleContentTypes = true
            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Delete)
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.Authorization)
        }

        // SEC-004: log full exception server-side; return generic message to client
        install(StatusPages) {
            exception<ForbiddenException> { call, _ ->
                val requestId = sanitizeRequestId(call.request.headers[X_REQUEST_ID])
                val lang = call.request.headers[HEADER_LANGUAGE] ?: "en"
                call.respond(
                    HttpStatusCode.Forbidden,
                    buildErrorEnvelope(
                        code = ErrorCode.FORBIDDEN,
                        message = "Insufficient permissions.",
                        lang = lang,
                        requestId = requestId,
                        service = BuildConfig.SERVICE_NAME,
                        version = BuildConfig.VERSION,
                    ),
                )
            }
            exception<Throwable> { call, cause ->
                call.application.log.error("Unhandled exception", cause)
                val requestId = sanitizeRequestId(call.request.headers[X_REQUEST_ID])
                val lang = call.request.headers[HEADER_LANGUAGE] ?: "en"
                call.respond(
                    HttpStatusCode.InternalServerError,
                    buildErrorEnvelope(
                        code = ErrorCode.INTERNAL_SERVER_ERROR,
                        message = "An unexpected error occurred.",
                        lang = lang,
                        requestId = requestId,
                        service = BuildConfig.SERVICE_NAME,
                        version = BuildConfig.VERSION,
                    ),
                )
            }
        }

        // T037: HS256 JWT — validates sub + role claims; rejects tokens with missing role
        install(Authentication) {
            jwt("auth-jwt") {
                realm = "KodEx"
                verifier(
                    JWT.require(Algorithm.HMAC256(EnvConfig.jwtSecret))
                        .withClaimPresence("sub")
                        .withClaimPresence("role")
                        .build(),
                )
                validate { credential ->
                    credential.payload.getClaim("role")?.asString()
                        ?.let { runCatching { Role.valueOf(it) }.getOrNull() }
                        ?.let { JWTPrincipal(credential.payload) }
                }
                challenge { _, _ ->
                    val requestId = sanitizeRequestId(call.request.headers[X_REQUEST_ID])
                    val lang = call.request.headers[HEADER_LANGUAGE] ?: "en"
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        buildErrorEnvelope(
                            code = ErrorCode.UNAUTHORIZED,
                            message = "Missing or invalid authentication token.",
                            lang = lang,
                            requestId = requestId,
                            service = BuildConfig.SERVICE_NAME,
                            version = BuildConfig.VERSION,
                        ),
                    )
                }
            }
        }

        install(XForwardedHeaders) // T038: normalises remoteHost after proxy
        install(RateLimit) {
            register(AppRateLimits.PUBLIC) {
                rateLimiter(limit = 60, refillPeriod = 1.minutes)
                requestKey { call -> call.request.origin.remoteHost }
            }
            // T041: tighter limit for all auth POST endpoints
            register(AppRateLimits.AUTH) {
                rateLimiter(limit = 20, refillPeriod = 10.seconds)
                requestKey { call -> call.request.origin.remoteHost }
            }
        }

        install(Koin) {
            slf4jLogger()
            withConfiguration<KoinServerApplication>()
        }

        routing {
            healthRoutes(startedAt = startedAt, service = BuildConfig.SERVICE_NAME, version = BuildConfig.VERSION)
            rateLimit(AppRateLimits.PUBLIC) {
                landingRoutes(service = BuildConfig.SERVICE_NAME, version = BuildConfig.VERSION)
            }
        }
    }.start(wait = true)
}
