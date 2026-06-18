package dev.kodex.server.domain.auth.usecase

import dev.kodex.server.domain.auth.model.DeliveryMode
import dev.kodex.server.domain.auth.repository.EmailVerificationTokenRepository
import dev.kodex.server.domain.auth.repository.UserRepository
import dev.kodex.server.domain.auth.service.TokenGeneratorPort
import dev.kodex.server.domain.auth.service.VerificationEmailPort
import kotlin.time.Clock

private val OTP_RANGE = 100_000..999_999

class ResendVerificationUseCase(
    private val userRepository: UserRepository,
    private val emailVerificationTokenRepository: EmailVerificationTokenRepository,
    private val tokenGeneratorPort: TokenGeneratorPort,
    private val verificationEmailPort: VerificationEmailPort,
) {
    suspend fun execute(email: String, platform: String) {
        val user = userRepository.findByEmail(email) ?: return
        if (user.emailVerified) return

        emailVerificationTokenRepository.invalidateForUser(user.id)

        val isMobile = platform == "android" || platform == "ios"
        if (isMobile) {
            val otp = OTP_RANGE.random().toString()
            val expiresAt = Clock.System.now() + OTP_TTL
            emailVerificationTokenRepository.create(
                userId = user.id,
                tokenHash = tokenGeneratorPort.hash("${user.id}:$otp"),
                deliveryMode = DeliveryMode.OTP,
                expiresAt = expiresAt,
            )
            verificationEmailPort.sendOtp(user.email, user.id, otp)
        } else {
            val rawToken = tokenGeneratorPort.generate()
            val expiresAt = Clock.System.now() + MAGIC_LINK_TTL
            emailVerificationTokenRepository.create(
                userId = user.id,
                tokenHash = tokenGeneratorPort.hash(rawToken),
                deliveryMode = DeliveryMode.MAGIC_LINK,
                expiresAt = expiresAt,
            )
            verificationEmailPort.sendMagicLink(user.email, rawToken)
        }
    }
}
