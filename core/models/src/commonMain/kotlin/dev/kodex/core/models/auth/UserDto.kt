package dev.kodex.core.models.auth

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: Long,
    val username: String,
    val email: String,
    val emailVerified: Boolean,
    val role: Role,
    val profile: UserProfileDto,
    val createdAt: Instant,
)

@Serializable
data class UserProfileDto(
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
)

@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val birthdate: LocalDate? = null,
    val location: String? = null,
    val githubUrl: String? = null,
    val linkedinUrl: String? = null,
    val twitterUrl: String? = null,
    val websiteUrl: String? = null,
)

@Serializable
data class SessionDto(
    val id: Long,
    val deviceHint: String?,
    val ipAddress: String?,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val isCurrent: Boolean,
)

@Serializable
data class OAuthProviderDto(
    val provider: OAuthProvider,
    val linkedAt: Instant,
)

@Serializable
data class PasskeyDto(
    val id: Long,
    val friendlyName: String?,
    val aaguid: String?,
    val createdAt: Instant,
)
