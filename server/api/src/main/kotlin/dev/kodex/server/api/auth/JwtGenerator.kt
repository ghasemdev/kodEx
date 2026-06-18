package dev.kodex.server.api.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import dev.kodex.core.config.ConfigQualifier
import dev.kodex.core.models.auth.Role
import java.util.*
import kotlin.time.Duration.Companion.minutes
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

private val ACCESS_TOKEN_TTL = 15.minutes

private const val MILLIS = 1_000L

@Single
class JwtGenerator(@Named(ConfigQualifier.Auth.JWT_SECRET) private val secret: String) {
    val accessTokenTtlSeconds: Int = ACCESS_TOKEN_TTL.inWholeSeconds.toInt()

    fun generate(userId: Long, role: Role): String {
        val now = Date()
        return JWT.create()
            .withSubject(userId.toString())
            .withClaim("role", role.name)
            .withIssuedAt(now)
            .withExpiresAt(Date(now.time + ACCESS_TOKEN_TTL.inWholeSeconds * MILLIS))
            .sign(Algorithm.HMAC256(secret))
    }
}
