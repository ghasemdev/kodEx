package dev.kodex.server.domain.auth.usecase

import dev.kodex.server.domain.auth.model.RefreshTokenRecord
import dev.kodex.server.domain.auth.model.UserRecord
import dev.kodex.server.domain.auth.repository.EmailVerificationTokenRepository
import dev.kodex.server.domain.auth.repository.TokenRepository
import dev.kodex.server.domain.auth.repository.UserRepository
import dev.kodex.server.domain.auth.service.TokenGeneratorPort
import kotlin.time.Clock

class VerifyEmailUseCase(
    private val userRepository: UserRepository,
    private val tokenRepository: TokenRepository,
    private val emailVerificationTokenRepository: EmailVerificationTokenRepository,
    private val tokenGeneratorPort: TokenGeneratorPort,
) {
    sealed class Result {
        data class Success(
            val user: UserRecord,
            val rawRefreshToken: String,
            val refreshToken: RefreshTokenRecord,
        ) : Result()

        data object TokenExpired : Result()
        data object TokenAlreadyUsed : Result()
        data object TokenInvalid : Result()
    }

    @Suppress("ReturnCount")
    suspend fun execute(rawToken: String, deviceHint: String?, ipAddress: String?): Result {
        val tokenHash = tokenGeneratorPort.hash(rawToken)
        val tokenRecord = emailVerificationTokenRepository.findActiveByHash(tokenHash)
            ?: return Result.TokenInvalid
        if (tokenRecord.usedAt != null) return Result.TokenAlreadyUsed
        if (tokenRecord.expiresAt < Clock.System.now()) return Result.TokenExpired

        emailVerificationTokenRepository.markUsed(tokenRecord.id)
        userRepository.setEmailVerified(tokenRecord.userId)

        val user = userRepository.findById(tokenRecord.userId) ?: return Result.TokenInvalid

        val rawRefreshToken = tokenGeneratorPort.generate()
        val refreshToken = tokenRepository.create(
            userId = user.id,
            tokenHash = tokenGeneratorPort.hash(rawRefreshToken),
            expiresAt = Clock.System.now() + REFRESH_TOKEN_TTL,
            deviceHint = deviceHint,
            ipAddress = ipAddress,
        )

        return Result.Success(user, rawRefreshToken, refreshToken)
    }
}
