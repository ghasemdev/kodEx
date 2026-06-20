package dev.kodex.core.models.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthTokensResponse(
    val accessToken: String,
    val expiresIn: Int,
)

@Serializable
data class TotpChallengeResponse(
    val totpSessionToken: String,
)

@Serializable
data class LoginResponse(
    val accessToken: String? = null,
    val expiresIn: Int? = null,
    val requiresTotp: Boolean = false,
    val totpSessionToken: String? = null,
)

@Serializable
data class UsernameAvailabilityResponse(
    val available: Boolean,
)
