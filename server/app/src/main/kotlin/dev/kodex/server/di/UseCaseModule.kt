package dev.kodex.server.di

import dev.kodex.server.domain.auth.repository.EmailVerificationTokenRepository
import dev.kodex.server.domain.auth.repository.TokenRepository
import dev.kodex.server.domain.auth.repository.UserRepository
import dev.kodex.server.domain.auth.service.PasswordHasherPort
import dev.kodex.server.domain.auth.service.PasswordStrengthEvaluator
import dev.kodex.server.domain.auth.service.TokenGeneratorPort
import dev.kodex.server.domain.auth.service.VerificationEmailPort
import dev.kodex.server.domain.auth.usecase.RegisterUseCase
import dev.kodex.server.domain.auth.usecase.ResendVerificationUseCase
import dev.kodex.server.domain.auth.usecase.VerifyEmailUseCase
import dev.kodex.server.domain.users.repository.ProfileRepository
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class UseCaseModule {
    @Single
    fun registerUseCase(
        userRepository: UserRepository,
        profileRepository: ProfileRepository,
        tokenRepository: TokenRepository,
        emailVerificationTokenRepository: EmailVerificationTokenRepository,
        passwordStrengthEvaluator: PasswordStrengthEvaluator,
        passwordHasherPort: PasswordHasherPort,
        tokenGeneratorPort: TokenGeneratorPort,
        verificationEmailPort: VerificationEmailPort,
    ) = RegisterUseCase(
        userRepository,
        profileRepository,
        tokenRepository,
        emailVerificationTokenRepository,
        passwordStrengthEvaluator,
        passwordHasherPort,
        tokenGeneratorPort,
        verificationEmailPort,
    )

    @Single
    fun verifyEmailUseCase(
        userRepository: UserRepository,
        tokenRepository: TokenRepository,
        emailVerificationTokenRepository: EmailVerificationTokenRepository,
        tokenGeneratorPort: TokenGeneratorPort,
    ) = VerifyEmailUseCase(userRepository, tokenRepository, emailVerificationTokenRepository, tokenGeneratorPort)

    @Single
    fun resendVerificationUseCase(
        userRepository: UserRepository,
        emailVerificationTokenRepository: EmailVerificationTokenRepository,
        tokenGeneratorPort: TokenGeneratorPort,
        verificationEmailPort: VerificationEmailPort,
    ) = ResendVerificationUseCase(
        userRepository,
        emailVerificationTokenRepository,
        tokenGeneratorPort,
        verificationEmailPort,
    )
}
