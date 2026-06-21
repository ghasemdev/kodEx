package dev.kodex.server.api.auth

import dev.kodex.core.models.auth.AuthTokensResponse
import dev.kodex.core.models.auth.LoginRequest
import dev.kodex.core.models.auth.LoginResponse
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
import dev.kodex.server.domain.auth.usecase.LoginUseCase
import dev.kodex.server.domain.auth.usecase.LogoutUseCase
import dev.kodex.server.domain.auth.usecase.RefreshTokenUseCase
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

private fun ApplicationCall.clearRefreshCookie() {
    response.cookies.append(
        name = REFRESH_COOKIE,
        value = "",
        httpOnly = true,
        secure = true,
        maxAge = 0,
        path = "/",
        extensions = mapOf("SameSite" to "Lax"),
    )
}

private fun isMobile(platform: String) = platform == "android" || platform == "ios"

fun Route.authRoutes(
    registerUseCase: RegisterUseCase,
    verifyEmailUseCase: VerifyEmailUseCase,
    resendVerificationUseCase: ResendVerificationUseCase,
    loginUseCase: LoginUseCase,
    refreshTokenUseCase: RefreshTokenUseCase,
    logoutUseCase: LogoutUseCase,
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
        loginRoute(loginUseCase, turnstileVerifier, jwtGenerator, serviceInfo)
        refreshRoute(refreshTokenUseCase, jwtGenerator, serviceInfo)
        logoutRoute(logoutUseCase, serviceInfo)
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

@Suppress("LongMethod", "LabeledExpression")
private fun Route.loginRoute(
    loginUseCase: LoginUseCase,
    turnstileVerifier: TurnstileVerifier,
    jwtGenerator: JwtGenerator,
    serviceInfo: ServiceInfo,
) {
    post("/login") {
        val req = call.receive<LoginRequest>()
        val ip = call.request.origin.remoteHost

        if (loginUseCase.requiresTurnstile(ip) && !turnstileVerifier.verify(req.turnstileToken ?: "", ip)) {
            call.respond(
                status = HttpStatusCode.BadRequest,
                message = with(serviceInfo) {
                    buildErrorEnvelope(ErrorCode.TURNSTILE_FAILED, "Bot detection failed.", call.lang, call.requestId)
                },
            )
            return@post
        }

        when (val result = loginUseCase.execute(req.email, req.password, ip, call.deviceHint)) {
            is LoginUseCase.Result.Success -> {
                val accessToken = jwtGenerator.generate(result.user.id, result.user.role)
                if (!isMobile(call.platform)) call.appendRefreshCookie(result.rawRefreshToken)
                call.respond(
                    with(serviceInfo) {
                        buildEnvelope(
                            data = LoginResponse(
                                accessToken = accessToken,
                                expiresIn = jwtGenerator.accessTokenTtlSeconds,
                            ),
                            requestId = call.requestId,
                        )
                    },
                )
            }

            is LoginUseCase.Result.TotpRequired -> {
                val totpSessionToken = jwtGenerator.generateTotpSessionToken(result.userId)
                call.respond(
                    with(serviceInfo) {
                        buildEnvelope(
                            data = LoginResponse(requiresTotp = true, totpSessionToken = totpSessionToken),
                            requestId = call.requestId,
                        )
                    },
                )
            }

            is LoginUseCase.Result.InvalidCredentials -> {
                call.respond(
                    status = HttpStatusCode.Unauthorized,
                    message = with(serviceInfo) {
                        buildErrorEnvelope(
                            code = ErrorCode.INVALID_CREDENTIALS,
                            message = "Incorrect email or password.",
                            lang = call.lang,
                            requestId = call.requestId,
                        )
                    },
                )
            }

            is LoginUseCase.Result.AccountLocked -> {
                call.respond(
                    status = HttpStatusCode.Forbidden,
                    message = with(serviceInfo) {
                        buildErrorEnvelope(
                            code = ErrorCode.ACCOUNT_LOCKED,
                            message = "Your account has been temporarily locked. Please try again later.",
                            lang = call.lang,
                            requestId = call.requestId,
                        )
                    },
                )
            }

            is LoginUseCase.Result.EmailNotVerified -> {
                call.respond(
                    status = HttpStatusCode.Forbidden,
                    message = with(serviceInfo) {
                        buildErrorEnvelope(
                            code = ErrorCode.EMAIL_NOT_VERIFIED,
                            message = "Please verify your email address before continuing.",
                            lang = call.lang,
                            requestId = call.requestId,
                        )
                    },
                )
            }
        }
    }
}

@Suppress("LabeledExpression")
private fun Route.refreshRoute(
    refreshTokenUseCase: RefreshTokenUseCase,
    jwtGenerator: JwtGenerator,
    serviceInfo: ServiceInfo,
) {
    post("/refresh") {
        val rawToken = call.request.cookies[REFRESH_COOKIE]
        if (rawToken.isNullOrBlank()) {
            call.respond(
                status = HttpStatusCode.Unauthorized,
                message = with(serviceInfo) {
                    buildErrorEnvelope(ErrorCode.UNAUTHORIZED, "Missing refresh token.", call.lang, call.requestId)
                },
            )
            return@post
        }

        val ip = call.request.origin.remoteHost
        when (val result = refreshTokenUseCase.execute(rawToken, call.deviceHint, ip)) {
            is RefreshTokenUseCase.Result.Success -> {
                val accessToken = jwtGenerator.generate(result.user.id, result.user.role)
                call.appendRefreshCookie(result.rawRefreshToken)
                call.respond(
                    with(serviceInfo) {
                        buildEnvelope(
                            data = AuthTokensResponse(accessToken, jwtGenerator.accessTokenTtlSeconds),
                            requestId = call.requestId,
                        )
                    },
                )
            }

            is RefreshTokenUseCase.Result.Invalid -> {
                call.clearRefreshCookie()
                call.respond(
                    status = HttpStatusCode.Unauthorized,
                    message = with(serviceInfo) {
                        buildErrorEnvelope(ErrorCode.UNAUTHORIZED, "Invalid refresh token.", call.lang, call.requestId)
                    },
                )
            }
        }
    }
}

private fun Route.logoutRoute(logoutUseCase: LogoutUseCase, serviceInfo: ServiceInfo) {
    post("/logout") {
        val rawToken = call.request.cookies[REFRESH_COOKIE]
        logoutUseCase.execute(rawToken)
        call.clearRefreshCookie()
        call.respond(with(serviceInfo) { buildEnvelope(data = null, requestId = call.requestId) })
    }
}
