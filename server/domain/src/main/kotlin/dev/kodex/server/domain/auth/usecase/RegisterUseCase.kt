package dev.kodex.server.domain.auth.usecase

import dev.kodex.core.models.auth.Role
import dev.kodex.server.domain.auth.model.DeliveryMode
import dev.kodex.server.domain.auth.model.RefreshTokenRecord
import dev.kodex.server.domain.auth.model.UserRecord
import dev.kodex.server.domain.auth.repository.EmailVerificationTokenRepository
import dev.kodex.server.domain.auth.repository.TokenRepository
import dev.kodex.server.domain.auth.repository.UserRepository
import dev.kodex.server.domain.auth.service.PasswordHasherPort
import dev.kodex.server.domain.auth.service.PasswordStrengthEvaluator
import dev.kodex.server.domain.auth.service.TokenGeneratorPort
import dev.kodex.server.domain.auth.service.VerificationEmailPort
import dev.kodex.server.domain.users.repository.ProfileRepository
import kotlin.time.Clock

private val USERNAME_REGEX = Regex("^[a-z0-9_-]{3,30}$")
private val OTP_RANGE = 100_000..999_999

class RegisterUseCase(
    private val userRepository: UserRepository,
    private val profileRepository: ProfileRepository,
    private val tokenRepository: TokenRepository,
    private val emailVerificationTokenRepository: EmailVerificationTokenRepository,
    private val passwordStrengthEvaluator: PasswordStrengthEvaluator,
    private val passwordHasherPort: PasswordHasherPort,
    private val tokenGeneratorPort: TokenGeneratorPort,
    private val verificationEmailPort: VerificationEmailPort,
) {
    sealed class Result {
        data class Success(
            val user: UserRecord,
            val rawRefreshToken: String,
            val refreshToken: RefreshTokenRecord,
        ) : Result()

        data object UsernameTaken : Result()
        data object EmailTaken : Result()
        data object UsernameInvalid : Result()
        data object PasswordTooWeak : Result()
    }

    @Suppress("ReturnCount")
    suspend fun execute(
        username: String,
        email: String,
        password: String,
        platform: String,
        deviceHint: String?,
        ipAddress: String?,
    ): Result {
        if (!USERNAME_REGEX.matches(username)) return Result.UsernameInvalid
        if (!passwordStrengthEvaluator.meetsMinimumStrength(password)) return Result.PasswordTooWeak
        if (userRepository.isUsernameTaken(username)) return Result.UsernameTaken
        if (userRepository.isEmailTaken(email)) return Result.EmailTaken

        val passwordHash = passwordHasherPort.hash(password)
        val user = userRepository.create(username, email.lowercase(), passwordHash, Role.PARTICIPANT)
        profileRepository.create(user.id)

        val isMobile = platform == "android" || platform == "ios"
        if (isMobile) {
            val otp = OTP_RANGE.random().toString()
            val otpHash = tokenGeneratorPort.hash("${user.id}:$otp")
            val expiresAt = Clock.System.now() + OTP_TTL
            emailVerificationTokenRepository.create(user.id, otpHash, DeliveryMode.OTP, expiresAt)
            verificationEmailPort.sendOtp(user.email, user.id, otp)
        } else {
            val rawToken = tokenGeneratorPort.generate()
            val tokenHash = tokenGeneratorPort.hash(rawToken)
            val expiresAt = Clock.System.now() + MAGIC_LINK_TTL
            emailVerificationTokenRepository.create(user.id, tokenHash, DeliveryMode.MAGIC_LINK, expiresAt)
            verificationEmailPort.sendMagicLink(user.email, rawToken)
        }

        val rawRefreshToken = tokenGeneratorPort.generate()
        val refreshTokenHash = tokenGeneratorPort.hash(rawRefreshToken)
        val refreshToken = tokenRepository.create(
            userId = user.id,
            tokenHash = refreshTokenHash,
            expiresAt = Clock.System.now() + REFRESH_TOKEN_TTL,
            deviceHint = deviceHint,
            ipAddress = ipAddress,
        )

        return Result.Success(user, rawRefreshToken, refreshToken)
    }
}
