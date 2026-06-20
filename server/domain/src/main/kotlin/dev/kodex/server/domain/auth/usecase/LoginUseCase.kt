package dev.kodex.server.domain.auth.usecase

import dev.kodex.server.domain.auth.model.RefreshTokenRecord
import dev.kodex.server.domain.auth.model.UserRecord
import dev.kodex.server.domain.auth.repository.TokenRepository
import dev.kodex.server.domain.auth.repository.UserRepository
import dev.kodex.server.domain.auth.service.AccountLockedEmailPort
import dev.kodex.server.domain.auth.service.LoginAttemptLimiterPort
import dev.kodex.server.domain.auth.service.PasswordHasherPort
import dev.kodex.server.domain.auth.service.TokenGeneratorPort
import dev.kodex.server.domain.totp.repository.TotpRepository
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private const val TURNSTILE_THRESHOLD = 3L
private const val LOCKOUT_THRESHOLD = 10
private val LOCKOUT_DURATION = 30.minutes

class LoginUseCase(
    private val userRepository: UserRepository,
    private val tokenRepository: TokenRepository,
    private val totpRepository: TotpRepository,
    private val passwordHasherPort: PasswordHasherPort,
    private val tokenGeneratorPort: TokenGeneratorPort,
    private val loginAttemptLimiterPort: LoginAttemptLimiterPort,
    private val accountLockedEmailPort: AccountLockedEmailPort,
) {
    sealed class Result {
        data class Success(
            val user: UserRecord,
            val rawRefreshToken: String,
            val refreshToken: RefreshTokenRecord,
        ) : Result()

        data class TotpRequired(val userId: Long) : Result()
        data object InvalidCredentials : Result()
        data class AccountLocked(val lockedUntil: Instant) : Result()
        data object EmailNotVerified : Result()
    }

    suspend fun requiresTurnstile(ipAddress: String): Boolean =
        loginAttemptLimiterPort.currentFailureCount(tokenGeneratorPort.hash(ipAddress)) >= TURNSTILE_THRESHOLD

    @Suppress("ReturnCount")
    suspend fun execute(email: String, password: String, ipAddress: String, deviceHint: String?): Result {
        val ipHash = tokenGeneratorPort.hash(ipAddress)
        val now = Clock.System.now()
        val user = userRepository.findByEmail(email.lowercase())

        val lockedUntil = user?.lockedUntil
        if (lockedUntil != null && lockedUntil > now) return Result.AccountLocked(lockedUntil)

        val passwordValid = user?.passwordHash != null && passwordHasherPort.verify(user.passwordHash, password)
        if (user == null || !passwordValid) {
            loginAttemptLimiterPort.recordFailure(ipHash)
            if (user != null) recordFailedLogin(user)
            return Result.InvalidCredentials
        }

        if (!user.emailVerified) return Result.EmailNotVerified

        loginAttemptLimiterPort.reset(ipHash)
        userRepository.resetFailedLoginCount(user.id)

        val totp = totpRepository.findByUserId(user.id)
        if (totp != null && totp.enabled) return Result.TotpRequired(user.id)

        val (rawRefreshToken, refreshToken) = issueRefreshToken(user.id, ipAddress, deviceHint, now)
        return Result.Success(user, rawRefreshToken, refreshToken)
    }

    private suspend fun recordFailedLogin(user: UserRecord) {
        userRepository.incrementFailedLoginCount(user.id)
        val updated = userRepository.findById(user.id) ?: return
        if (updated.failedLoginCount >= LOCKOUT_THRESHOLD) {
            val until = Clock.System.now() + LOCKOUT_DURATION
            userRepository.lockUntil(user.id, until)
            accountLockedEmailPort.sendAccountLocked(updated.email, until)
        }
    }

    private suspend fun issueRefreshToken(
        userId: Long,
        ipAddress: String,
        deviceHint: String?,
        now: Instant,
    ): Pair<String, RefreshTokenRecord> {
        val rawRefreshToken = tokenGeneratorPort.generate()
        val refreshToken = tokenRepository.create(
            userId = userId,
            tokenHash = tokenGeneratorPort.hash(rawRefreshToken),
            expiresAt = now + REFRESH_TOKEN_TTL,
            deviceHint = deviceHint,
            ipAddress = ipAddress,
        )
        return rawRefreshToken to refreshToken
    }
}
