package dev.kodex.webapp.network.auth

import dev.kodex.core.models.auth.AuthTokensResponse
import dev.kodex.core.models.auth.LoginRequest
import dev.kodex.core.models.auth.LoginResponse
import dev.kodex.core.models.auth.RegisterRequest
import dev.kodex.core.models.auth.TotpChallengeResponse
import dev.kodex.core.models.auth.TotpLoginRequest
import dev.kodex.core.models.auth.UsernameAvailabilityResponse
import dev.kodex.shared.coroutines.ioDispatcher
import dev.kodex.webapp.network.ApiRoutes
import dev.kodex.webapp.network.envelopeDataOrThrow
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single

@Serializable
private data class TokenBody(val token: String)

@Serializable
private data class EmailBody(val email: String)

@Serializable
private data class ResetPasswordBody(val token: String, val newPassword: String)

@Serializable
private data class ChangePasswordBody(val currentPassword: String, val newPassword: String)

@Serializable
private data class TotpCodeBody(val code: String)

@Single(binds = [AuthRemoteDataSource::class])
class AuthRemoteDataSourceImpl(private val client: HttpClient) : AuthRemoteDataSource {
    override suspend fun register(request: RegisterRequest): AuthTokensResponse = withContext(ioDispatcher) {
        client
            .post(ApiRoutes.Auth.REGISTER) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            .envelopeDataOrThrow()
    }

    override suspend fun verifyEmail(token: String): AuthTokensResponse = withContext(ioDispatcher) {
        client
            .post(ApiRoutes.Auth.VERIFY_EMAIL) {
                contentType(ContentType.Application.Json)
                setBody(TokenBody(token))
            }
            .envelopeDataOrThrow()
    }

    override suspend fun resendVerification(email: String) {
        withContext(ioDispatcher) {
            client
                .post(ApiRoutes.Auth.VERIFY_EMAIL_RESEND) {
                    contentType(ContentType.Application.Json)
                    setBody(EmailBody(email))
                }
                .envelopeDataOrThrow<Unit?>()
        }
    }

    override suspend fun login(request: LoginRequest): LoginResponse = withContext(ioDispatcher) {
        client
            .post(ApiRoutes.Auth.LOGIN) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            .envelopeDataOrThrow()
    }

    override suspend fun totpLogin(request: TotpLoginRequest): AuthTokensResponse = withContext(ioDispatcher) {
        client
            .post(ApiRoutes.Auth.TOTP_LOGIN) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            .envelopeDataOrThrow()
    }

    override suspend fun logout() {
        withContext(ioDispatcher) {
            client.post(ApiRoutes.Auth.LOGOUT).envelopeDataOrThrow<Unit?>()
        }
    }

    override suspend fun refresh(): AuthTokensResponse = withContext(ioDispatcher) {
        client.post(ApiRoutes.Auth.REFRESH).envelopeDataOrThrow()
    }

    override suspend fun checkUsernameAvailable(username: String): UsernameAvailabilityResponse =
        withContext(ioDispatcher) {
            client
                .get(ApiRoutes.Auth.USERNAME_AVAILABLE) { parameter("username", username) }
                .envelopeDataOrThrow()
        }

    override suspend fun forgotPassword(email: String) {
        withContext(ioDispatcher) {
            client
                .post(ApiRoutes.Auth.FORGOT_PASSWORD) {
                    contentType(ContentType.Application.Json)
                    setBody(EmailBody(email))
                }
                .envelopeDataOrThrow<Unit?>()
        }
    }

    override suspend fun resetPassword(token: String, newPassword: String) {
        withContext(ioDispatcher) {
            client
                .post(ApiRoutes.Auth.RESET_PASSWORD) {
                    contentType(ContentType.Application.Json)
                    setBody(ResetPasswordBody(token, newPassword))
                }
                .envelopeDataOrThrow<Unit?>()
        }
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String) {
        withContext(ioDispatcher) {
            client
                .post(ApiRoutes.Auth.CHANGE_PASSWORD) {
                    contentType(ContentType.Application.Json)
                    setBody(ChangePasswordBody(currentPassword, newPassword))
                }
                .envelopeDataOrThrow<Unit?>()
        }
    }

    override suspend fun getTotpSetupUri(): TotpChallengeResponse = withContext(ioDispatcher) {
        client.get(ApiRoutes.Auth.TOTP_SETUP).envelopeDataOrThrow()
    }

    override suspend fun enableTotp(code: String) {
        withContext(ioDispatcher) {
            client
                .post(ApiRoutes.Auth.TOTP_ENABLE) {
                    contentType(ContentType.Application.Json)
                    setBody(TotpCodeBody(code))
                }
                .envelopeDataOrThrow<Unit?>()
        }
    }

    override suspend fun disableTotp(code: String) {
        withContext(ioDispatcher) {
            client
                .post(ApiRoutes.Auth.TOTP_DISABLE) {
                    contentType(ContentType.Application.Json)
                    setBody(TotpCodeBody(code))
                }
                .envelopeDataOrThrow<Unit?>()
        }
    }
}
