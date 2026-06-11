package dev.kodex.core.models.auth

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val turnstileToken: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val turnstileToken: String? = null,
)

@Serializable
data class TotpLoginRequest(
    val totpSessionToken: String,
    val totpCode: String,
)

@Serializable
data class ChangeUsernameRequest(
    val newUsername: String,
)
