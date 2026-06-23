@file:Suppress("ComplexInterface", "TooManyFunctions")

package dev.kodex.webapp.network.auth

import dev.kodex.core.models.auth.AuthTokensResponse
import dev.kodex.core.models.auth.LoginRequest
import dev.kodex.core.models.auth.LoginResponse
import dev.kodex.core.models.auth.RegisterRequest
import dev.kodex.core.models.auth.TotpChallengeResponse
import dev.kodex.core.models.auth.TotpLoginRequest
import dev.kodex.core.models.auth.UsernameAvailabilityResponse

interface AuthRemoteDataSource {
    suspend fun register(request: RegisterRequest): AuthTokensResponse
    suspend fun verifyEmail(token: String): AuthTokensResponse
    suspend fun resendVerification(email: String)
    suspend fun login(request: LoginRequest): LoginResponse
    suspend fun totpLogin(request: TotpLoginRequest): AuthTokensResponse
    suspend fun logout()
    suspend fun refresh(): AuthTokensResponse
    suspend fun checkUsernameAvailable(username: String): UsernameAvailabilityResponse
    suspend fun forgotPassword(email: String)
    suspend fun resetPassword(token: String, newPassword: String)
    suspend fun changePassword(currentPassword: String, newPassword: String)
    suspend fun getTotpSetupUri(): TotpChallengeResponse
    suspend fun enableTotp(code: String)
    suspend fun disableTotp(code: String)
}
