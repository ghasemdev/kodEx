package dev.kodex.server.domain.auth.usecase

import dev.kodex.server.domain.auth.model.RefreshTokenRecord
import dev.kodex.server.domain.auth.model.UserRecord
import dev.kodex.server.domain.auth.repository.TokenRepository
import dev.kodex.server.domain.auth.repository.UserRepository
import dev.kodex.server.domain.auth.service.TokenGeneratorPort
import kotlin.time.Clock

class RefreshTokenUseCase(
    private val tokenRepository: TokenRepository,
    private val userRepository: UserRepository,
    private val tokenGeneratorPort: TokenGeneratorPort,
) {
    sealed class Result {
        data class Success(
            val user: UserRecord,
            val rawRefreshToken: String,
            val refreshToken: RefreshTokenRecord,
        ) : Result()

        data object Invalid : Result()
    }

    suspend fun execute(rawToken: String, deviceHint: String?, ipAddress: String?): Result {
        val oldHash = tokenGeneratorPort.hash(rawToken)
        val existing = tokenRepository.findActiveByHash(oldHash) ?: return Result.Invalid
        val user = userRepository.findById(existing.userId) ?: return Result.Invalid

        val newRawToken = tokenGeneratorPort.generate()
        val rotated = tokenRepository.rotate(
            oldHash = oldHash,
            newHash = tokenGeneratorPort.hash(newRawToken),
            newExpiresAt = Clock.System.now() + REFRESH_TOKEN_TTL,
            deviceHint = deviceHint,
            ipAddress = ipAddress,
        )

        return Result.Success(user, newRawToken, rotated)
    }
}
