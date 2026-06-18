package dev.kodex.server.api.auth

import dev.kodex.core.models.auth.AuthTokensResponse
import dev.kodex.core.models.auth.RegisterRequest
import dev.kodex.core.models.auth.UsernameAvailabilityResponse
import dev.kodex.server.api.auth.middleware.AuthCachePlugin
import dev.kodex.server.api.auth.middleware.TurnstileVerifier
import dev.kodex.server.api.response.ErrorCode
import dev.kodex.server.api.response.ServiceInfo
import dev.kodex.server.api.response.buildEnvelope
import dev.kodex.server.api.response.buildErrorEnvelope
import dev.kodex.server.api.util.sanitizeRequestId
import dev.kodex.server.domain.auth.repository.UserRepository
import dev.kodex.server.domain.auth.usecase.RegisterUseCase
import dev.kodex.server.domain.auth.usecase.ResendVerificationUseCase
import dev.kodex.server.domain.auth.usecase.VerifyEmailUseCase
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

private const val PLATFORM_HEADER = "X-Client-Platform"
private const val LANG_HEADER = "lang"
private const val REQUEST_ID_HEADER = "X-Request-Id"
private const val REFRESH_COOKIE = "refresh_token"
private const val REFRESH_MAX_AGE = 7 * 24 * 60 * 60L
private const val DEVICE_HINT_MAX_SIZE = 255

private val ApplicationCall.lang: String get() = request.headers[LANG_HEADER] ?: "en"
private val ApplicationCall.requestId: String get() = sanitizeRequestId(request.headers[REQUEST_ID_HEADER])
private val ApplicationCall.platform: String get() = request.headers[PLATFORM_HEADER] ?: "web"
private val ApplicationCall.deviceHint: String?
    get() = request.headers[HttpHeaders.UserAgent]?.take(DEVICE_HINT_MAX_SIZE)

@Serializable
private data class TokenBody(val token: String)

@Serializable
private data class EmailBody(val email: String)

private fun ApplicationCall.appendRefreshCookie(rawToken: String) {
    response.cookies.append(
        name = REFRESH_COOKIE,
        value = rawToken,
        httpOnly = true,
        secure = true,
        maxAge = REFRESH_MAX_AGE,
        path = "/",
        extensions = mapOf("SameSite" to "Lax"),
    )
}

private fun isMobile(platform: String) = platform == "android" || platform == "ios"

fun Route.authRoutes(
    registerUseCase: RegisterUseCase,
    verifyEmailUseCase: VerifyEmailUseCase,
    resendVerificationUseCase: ResendVerificationUseCase,
    userRepository: UserRepository,
    turnstileVerifier: TurnstileVerifier,
    jwtGenerator: JwtGenerator,
    serviceInfo: ServiceInfo,
) {
    route("/api/v1/auth") {
        install(AuthCachePlugin)
        registerRoute(registerUseCase, turnstileVerifier, jwtGenerator, serviceInfo)
        usernameCheckRoute(userRepository, serviceInfo)
        verifyEmailRoute(verifyEmailUseCase, jwtGenerator, serviceInfo)
        resendVerificationRoute(resendVerificationUseCase, serviceInfo)
    }
}

@Suppress("LabeledExpression", "LongMethod")
private fun Route.registerRoute(
    registerUseCase: RegisterUseCase,
    turnstileVerifier: TurnstileVerifier,
    jwtGenerator: JwtGenerator,
    serviceInfo: ServiceInfo,
) {
    post("/register") {
        val req = call.receive<RegisterRequest>()
        val ip = call.request.origin.remoteHost

        if (!turnstileVerifier.verify(req.turnstileToken, ip)) {
            call.respond(
                status = HttpStatusCode.BadRequest,
                message = with(serviceInfo) {
                    buildErrorEnvelope(ErrorCode.TURNSTILE_FAILED, "Bot detection failed.", call.lang, call.requestId)
                },
            )
            return@post
        }

        when (
            val result = registerUseCase.execute(
                username = req.username,
                email = req.email,
                password = req.password,
                platform = call.platform,
                deviceHint = call.deviceHint,
                ipAddress = ip,
            )
        ) {
            is RegisterUseCase.Result.Success -> {
                val accessToken = jwtGenerator.generate(result.user.id, result.user.role)
                if (!isMobile(call.platform)) call.appendRefreshCookie(result.rawRefreshToken)
                call.respond(
                    status = HttpStatusCode.Created,
                    message = with(serviceInfo) {
                        buildEnvelope(
                            data = AuthTokensResponse(accessToken, jwtGenerator.accessTokenTtlSeconds),
                            requestId = call.requestId,
                        )
                    },
                )
            }

            is RegisterUseCase.Result.UsernameTaken -> {
                call.respond(
                    status = HttpStatusCode.UnprocessableEntity,
                    message = with(serviceInfo) {
                        buildErrorEnvelope(
                            code = ErrorCode.VALIDATION_ERROR,
                            message = "Username is already taken.",
                            lang = call.lang,
                            requestId = call.requestId,
                        )
                    },
                )
            }

            is RegisterUseCase.Result.EmailTaken -> {
                call.respond(
                    status = HttpStatusCode.Conflict,
                    message = with(serviceInfo) {
                        buildErrorEnvelope(
                            code = ErrorCode.EMAIL_ALREADY_REGISTERED,
                            message = "Email already registered.",
                            lang = call.lang,
                            requestId = call.requestId,
                        )
                    },
                )
            }

            is RegisterUseCase.Result.UsernameInvalid -> {
                call.respond(
                    status = HttpStatusCode.UnprocessableEntity,
                    message = with(serviceInfo) {
                        buildErrorEnvelope(
                            code = ErrorCode.VALIDATION_ERROR,
                            message = "Username must be 3–30 chars: lowercase letters, digits, _ or -.",
                            lang = call.lang,
                            requestId = call.requestId,
                        )
                    },
                )
            }

            is RegisterUseCase.Result.PasswordTooWeak -> {
                call.respond(
                    status = HttpStatusCode.UnprocessableEntity,
                    message = with(serviceInfo) {
                        buildErrorEnvelope(
                            code = ErrorCode.VALIDATION_ERROR,
                            message = "Password is too weak. Try a longer or more complex password.",
                            lang = call.lang,
                            requestId = call.requestId,
                        )
                    },
                )
            }
        }
    }
}

private fun Route.usernameCheckRoute(userRepository: UserRepository, serviceInfo: ServiceInfo) {
    suspend fun checkBody(call: ApplicationCall) {
        val username = call.request.queryParameters["username"] ?: ""
        call.respond(
            with(serviceInfo) {
                buildEnvelope(
                    data = UsernameAvailabilityResponse(!userRepository.isUsernameTaken(username)),
                    requestId = call.requestId,
                )
            },
        )
    }
    get("/username/check") { checkBody(call) }
    get("/username/available") { checkBody(call) }
}

@Suppress("LongMethod")
private fun Route.verifyEmailRoute(
    verifyEmailUseCase: VerifyEmailUseCase,
    jwtGenerator: JwtGenerator,
    serviceInfo: ServiceInfo,
) {
    post("/verify-email") {
        val body = call.receive<TokenBody>()
        val ip = call.request.origin.remoteHost

        when (val result = verifyEmailUseCase.execute(body.token, call.deviceHint, ip)) {
            is VerifyEmailUseCase.Result.Success -> {
                val accessToken = jwtGenerator.generate(result.user.id, result.user.role)
                if (!isMobile(call.platform)) call.appendRefreshCookie(result.rawRefreshToken)
                call.respond(
                    with(serviceInfo) {
                        buildEnvelope(
                            data = AuthTokensResponse(
                                accessToken = accessToken,
                                expiresIn = jwtGenerator.accessTokenTtlSeconds,
                            ),
                            requestId = call.requestId,
                        )
                    },
                )
            }

            is VerifyEmailUseCase.Result.TokenExpired -> {
                call.respond(
                    status = HttpStatusCode.UnprocessableEntity,
                    message = with(serviceInfo) {
                        buildErrorEnvelope(
                            code = ErrorCode.TOKEN_EXPIRED,
                            message = "This link has expired. Please request a new one.",
                            lang = call.lang,
                            requestId = call.requestId,
                        )
                    },
                )
            }

            is VerifyEmailUseCase.Result.TokenAlreadyUsed -> {
                call.respond(
                    status = HttpStatusCode.UnprocessableEntity,
                    message = with(serviceInfo) {
                        buildErrorEnvelope(
                            code = ErrorCode.TOKEN_ALREADY_USED,
                            message = "This link has already been used.",
                            lang = call.lang,
                            requestId = call.requestId,
                        )
                    },
                )
            }

            is VerifyEmailUseCase.Result.TokenInvalid -> {
                call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = with(serviceInfo) {
                        buildErrorEnvelope(
                            code = ErrorCode.TOKEN_INVALID,
                            message = "Invalid verification token.",
                            lang = call.lang,
                            requestId = call.requestId,
                        )
                    },
                )
            }
        }
    }
}

private fun Route.resendVerificationRoute(
    resendVerificationUseCase: ResendVerificationUseCase,
    serviceInfo: ServiceInfo,
) {
    post("/verify-email/resend") {
        val body = call.receive<EmailBody>()
        resendVerificationUseCase.execute(body.email, call.platform)
        call.respond(with(serviceInfo) { buildEnvelope(data = null, requestId = call.requestId) })
    }
}
