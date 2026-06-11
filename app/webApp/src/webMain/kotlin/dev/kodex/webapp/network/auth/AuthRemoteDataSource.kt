package dev.kodex.webapp.network.auth

import dev.kodex.core.models.auth.AuthTokensResponse
import dev.kodex.core.models.auth.LoginRequest
import dev.kodex.core.models.auth.RegisterRequest
import dev.kodex.core.models.auth.TotpChallengeResponse
import dev.kodex.core.models.auth.TotpLoginRequest
import dev.kodex.core.models.auth.UsernameAvailabilityResponse

interface AuthRemoteDataSource {
    suspend fun register(request: RegisterRequest): AuthTokensResponse
    suspend fun verifyEmail(token: String): Unit
    suspend fun login(request: LoginRequest): AuthTokensResponse
    suspend fun totpLogin(request: TotpLoginRequest): AuthTokensResponse
    suspend fun logout(): Unit
    suspend fun refresh(): AuthTokensResponse
    suspend fun checkUsernameAvailable(username: String): UsernameAvailabilityResponse
    suspend fun forgotPassword(email: String): Unit
    suspend fun resetPassword(token: String, newPassword: String): Unit
    suspend fun changePassword(currentPassword: String, newPassword: String): Unit
    suspend fun getTotpSetupUri(): TotpChallengeResponse
    suspend fun enableTotp(code: String): Unit
    suspend fun disableTotp(code: String): Unit
}
