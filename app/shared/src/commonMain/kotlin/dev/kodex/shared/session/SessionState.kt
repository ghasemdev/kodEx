package dev.kodex.shared.session

import androidx.compose.runtime.Immutable

@Immutable
sealed class SessionState {
    @Immutable
    data object Guest : SessionState()

    @Immutable
    data class Authenticated(
        val userId: String,
        val username: String,
        val avatarUrl: String?,
        val plan: Plan,
        val roles: Set<UserRole>,
    ) : SessionState()
}

enum class Plan { FREE, PRO }

enum class UserRole { PARTICIPANT, EXAM_CREATOR, ADMIN }
