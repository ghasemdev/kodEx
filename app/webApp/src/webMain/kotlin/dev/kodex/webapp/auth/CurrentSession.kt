package dev.kodex.webapp.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.kodex.shared.session.Plan
import dev.kodex.shared.session.SessionState
import dev.kodex.shared.session.UserRole
import dev.kodex.webapp.di.org.koin.compose.koinInject

fun AuthState.toSessionState(): SessionState = when (this) {
    is AuthState.LoggedIn -> SessionState.Authenticated(
        userId = user.id.toString(),
        username = user.username,
        avatarUrl = user.profile.avatarUrl,
        plan = Plan.FREE,
        roles = setOf(UserRole.valueOf(user.role.name)),
    )

    AuthState.LoggedOut, AuthState.LoggingIn, is AuthState.RequiresTotp -> SessionState.Guest
}

@Composable
fun currentSessionState(): SessionState {
    val authStore = koinInject<AuthStore>()
    val authState by authStore.state.collectAsState()
    return authState.toSessionState()
}
