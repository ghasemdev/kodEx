package dev.kodex.shared.session

sealed class SessionState {
    data object Guest : SessionState()
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
