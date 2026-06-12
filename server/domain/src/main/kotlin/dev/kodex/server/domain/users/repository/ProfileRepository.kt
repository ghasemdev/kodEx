package dev.kodex.server.domain.users.repository

import dev.kodex.server.domain.users.model.ProfileRecord
import kotlinx.datetime.LocalDate

interface ProfileRepository {
    suspend fun findByUserId(userId: Long): ProfileRecord?
    suspend fun create(userId: Long)
    suspend fun update(
        userId: Long,
        displayName: String?,
        firstName: String?,
        lastName: String?,
        birthdate: LocalDate?,
        location: String?,
        githubUrl: String?,
        linkedinUrl: String?,
        twitterUrl: String?,
        websiteUrl: String?,
    )

    suspend fun updateAvatarUrl(userId: Long, avatarUrl: String)
}
