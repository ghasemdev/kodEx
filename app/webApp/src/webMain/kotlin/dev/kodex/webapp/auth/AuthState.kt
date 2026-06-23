package dev.kodex.webapp.auth

import androidx.compose.runtime.Immutable
import dev.kodex.core.models.auth.UserDto
import kotlin.time.Instant

@Immutable
sealed class AuthState {
    @Immutable
    data object LoggedOut : AuthState()

    @Immutable
    data object LoggingIn : AuthState()

    @Immutable
    data class LoggedIn(
        val user: UserDto,
        val accessToken: String,
        val expiresAt: Instant,
    ) : AuthState()

    @Immutable
    data class RequiresTotp(val totpSessionToken: String) : AuthState()
}
