package dev.kodex.server.domain.users.repository

import dev.kodex.core.models.auth.SessionDto

interface SessionRepository {
    suspend fun findActiveByUserId(userId: Long, currentTokenHash: String?): List<SessionDto>
    suspend fun revokeById(sessionId: Long, userId: Long)
    suspend fun revokeAllExceptCurrent(userId: Long, currentTokenHash: String)
}
