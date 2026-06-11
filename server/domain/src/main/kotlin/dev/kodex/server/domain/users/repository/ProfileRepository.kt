package dev.kodex.server.domain.users.repository

import kotlin.time.Instant
import kotlinx.datetime.LocalDate

data class ProfileRecord(
    val userId: Long,
    val displayName: String?,
    val firstName: String?,
    val lastName: String?,
    val birthdate: LocalDate?,
    val avatarUrl: String?,
    val location: String?,
    val githubUrl: String?,
    val linkedinUrl: String?,
    val twitterUrl: String?,
    val websiteUrl: String?,
    val updatedAt: Instant,
)

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
