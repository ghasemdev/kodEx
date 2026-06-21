package dev.kodex.server.api.users

import dev.kodex.core.models.auth.Role
import dev.kodex.core.models.auth.UserDto
import dev.kodex.core.models.auth.UserProfileDto
import dev.kodex.server.api.auth.middleware.requireRole
import dev.kodex.server.api.response.ErrorCode
import dev.kodex.server.api.response.ServiceInfo
import dev.kodex.server.api.response.buildEnvelope
import dev.kodex.server.api.response.buildErrorEnvelope
import dev.kodex.server.api.util.sanitizeRequestId
import dev.kodex.server.domain.auth.model.UserRecord
import dev.kodex.server.domain.auth.repository.UserRepository
import dev.kodex.server.domain.users.model.ProfileRecord
import dev.kodex.server.domain.users.repository.ProfileRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

private const val LANG_HEADER = "lang"
private const val REQUEST_ID_HEADER = "X-Request-Id"

fun Route.userRoutes(userRepository: UserRepository, profileRepository: ProfileRepository, serviceInfo: ServiceInfo) {
    route("/api/v1/users") {
        authenticate("auth-jwt") {
            requireRole(Role.PARTICIPANT, Role.EXAM_CREATOR, Role.ADMIN)
            meRoute(userRepository, profileRepository, serviceInfo)
        }
    }
}

@Suppress("LabeledExpression")
private fun Route.meRoute(
    userRepository: UserRepository,
    profileRepository: ProfileRepository,
    serviceInfo: ServiceInfo,
) {
    get("/me") {
        val requestId = sanitizeRequestId(call.request.headers[REQUEST_ID_HEADER])
        val lang = call.request.headers[LANG_HEADER] ?: "en"
        val userId = call.principal<JWTPrincipal>()?.payload?.subject?.toLongOrNull()
        val user = userId?.let { userRepository.findById(it) }

        if (user == null) {
            call.respond(
                status = HttpStatusCode.NotFound,
                message = with(serviceInfo) {
                    buildErrorEnvelope(ErrorCode.NOT_FOUND, "User not found.", lang, requestId)
                },
            )
            return@get
        }

        val profile = profileRepository.findByUserId(user.id)
        call.respond(
            with(serviceInfo) {
                buildEnvelope(data = user.toDto(profile), requestId = requestId)
            },
        )
    }
}

private fun UserRecord.toDto(profile: ProfileRecord?) = UserDto(
    id = id,
    username = username,
    email = email,
    emailVerified = emailVerified,
    role = role,
    profile = UserProfileDto(
        displayName = profile?.displayName,
        firstName = profile?.firstName,
        lastName = profile?.lastName,
        birthdate = profile?.birthdate,
        avatarUrl = profile?.avatarUrl,
        location = profile?.location,
        githubUrl = profile?.githubUrl,
        linkedinUrl = profile?.linkedinUrl,
        twitterUrl = profile?.twitterUrl,
        websiteUrl = profile?.websiteUrl,
    ),
    createdAt = createdAt,
)
