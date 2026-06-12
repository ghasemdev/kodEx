package dev.kodex.server.domain.users.model

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
