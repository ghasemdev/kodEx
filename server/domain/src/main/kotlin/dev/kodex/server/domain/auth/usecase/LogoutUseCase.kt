package dev.kodex.server.domain.auth.usecase

import dev.kodex.server.domain.auth.repository.TokenRepository
import dev.kodex.server.domain.auth.service.TokenGeneratorPort

class LogoutUseCase(
    private val tokenRepository: TokenRepository,
    private val tokenGeneratorPort: TokenGeneratorPort,
) {
    suspend fun execute(rawToken: String?) {
        if (rawToken.isNullOrBlank()) return
        tokenRepository.revoke(tokenGeneratorPort.hash(rawToken))
    }
}
