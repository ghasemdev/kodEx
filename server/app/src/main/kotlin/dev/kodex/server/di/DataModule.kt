package dev.kodex.server.di

import dev.kodex.core.env.envOrNull
import dev.kodex.server.api.auth.middleware.TurnstileVerifier
import dev.kodex.server.app.config.EnvConfig
import dev.kodex.server.data.geoip.GeoIpService
import dev.kodex.server.data.notification.channel.EmailChannel
import dev.kodex.server.data.notification.channel.SmsChannel
import dev.kodex.server.data.notification.email.ResendEmailChannel
import dev.kodex.server.data.notification.email.SendGridEmailChannel
import dev.kodex.server.data.notification.infrastructure.CircuitBreaker
import dev.kodex.server.data.notification.infrastructure.QuotaTracker
import dev.kodex.server.data.notification.router.EmailRouter
import dev.kodex.server.data.notification.router.SmsRouter
import dev.kodex.server.data.notification.sms.KavenegarSmsChannel
import dev.kodex.server.data.notification.sms.TwilioSmsChannel
import dev.kodex.server.data.ratelimit.RateLimitService
import dev.kodex.server.data.repository.EmergencyRevokeTokenRepositoryImpl
import dev.kodex.server.data.repository.OAuthIdentityRepositoryImpl
import dev.kodex.server.data.repository.PasskeyRepositoryImpl
import dev.kodex.server.data.repository.ProfileRepositoryImpl
import dev.kodex.server.data.repository.SessionRepositoryImpl
import dev.kodex.server.data.repository.TokenRepositoryImpl
import dev.kodex.server.data.repository.TotpRepositoryImpl
import dev.kodex.server.data.repository.UserRepositoryImpl
import dev.kodex.server.data.storage.AvatarStorageService
import dev.kodex.server.domain.auth.repository.EmergencyRevokeTokenRepository
import dev.kodex.server.domain.auth.repository.OAuthIdentityRepository
import dev.kodex.server.domain.auth.repository.TokenRepository
import dev.kodex.server.domain.auth.repository.UserRepository
import dev.kodex.server.domain.passkey.repository.PasskeyRepository
import dev.kodex.server.domain.totp.repository.TotpRepository
import dev.kodex.server.domain.users.repository.ProfileRepository
import dev.kodex.server.domain.users.repository.SessionRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@Suppress("TooManyFunctions")
class DataModule {
    // ── Shared HTTP client for outgoing requests ──────────────────────────────
    @Single
    fun httpClient(): HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        expectSuccess = false
    }

    // ── Infrastructure services ───────────────────────────────────────────────
    @Single
    fun geoIpService(): GeoIpService = GeoIpService(EnvConfig.geoIpDbPath)

    @Single
    fun rateLimitService(): RateLimitService =
        RateLimitService(EnvConfig.redisUrl, EnvConfig.redisPassword.ifEmpty { null })

    @Single
    fun avatarStorageService(): AvatarStorageService = AvatarStorageService(
        endpoint = EnvConfig.minioEndpoint,
        accessKey = EnvConfig.minioAccessKey,
        secretKey = EnvConfig.minioSecretKey,
        bucket = EnvConfig.minioBucketAvatars,
        publicUrl = EnvConfig.minioPublicUrl,
    )

    @Single
    fun turnstileVerifier(httpClient: HttpClient): TurnstileVerifier =
        TurnstileVerifier(secret = EnvConfig.cloudflareTurnstileSecret, httpClient = httpClient)

    // ── Notification infrastructure ───────────────────────────────────────────
    @Single
    fun quotaTracker(): QuotaTracker = QuotaTracker()

    // ── Email providers ───────────────────────────────────────────────────────
    @Single
    fun resendEmailChannel(httpClient: HttpClient): ResendEmailChannel =
        ResendEmailChannel(apiKey = EnvConfig.resendApiKey, from = EnvConfig.emailFrom, httpClient = httpClient)

    @Single
    fun sendGridEmailChannel(httpClient: HttpClient): SendGridEmailChannel =
        SendGridEmailChannel(apiKey = EnvConfig.sendgridApiKey, from = EnvConfig.emailFrom, httpClient = httpClient)

    @Single
    fun emailProviders(resend: ResendEmailChannel, sendGrid: SendGridEmailChannel): List<EmailChannel> = listOfNotNull(
        resend.takeIf { EnvConfig.resendApiKey.isNotEmpty() },
        sendGrid.takeIf { EnvConfig.sendgridApiKey.isNotEmpty() },
    )

    @Single(binds = [EmailChannel::class])
    fun emailRouter(channels: List<EmailChannel>, quota: QuotaTracker): EmailRouter = EmailRouter(
        channels = channels,
        quota = quota,
        breakers = channels.associate { it.name to CircuitBreaker() },
    )

    // ── SMS providers ─────────────────────────────────────────────────────────
    @Single
    fun kavenegarSmsChannel(httpClient: HttpClient): KavenegarSmsChannel =
        KavenegarSmsChannel(apiKey = EnvConfig.kavenegarApiKey, httpClient = httpClient)

    @Single
    fun twilioSmsChannel(httpClient: HttpClient): TwilioSmsChannel = TwilioSmsChannel(
        accountSid = EnvConfig.twilioAccountSid,
        authToken = EnvConfig.twilioAuthToken,
        fromNumber = envOrNull("TWILIO_FROM_NUMBER") ?: "",
        httpClient = httpClient,
    )

    @Single
    fun smsProviders(kavenegar: KavenegarSmsChannel, twilio: TwilioSmsChannel): List<SmsChannel> =
        listOfNotNull(
            kavenegar.takeIf { EnvConfig.kavenegarApiKey.isNotEmpty() },
            twilio.takeIf { EnvConfig.twilioAccountSid.isNotEmpty() },
        )

    @Single(binds = [SmsChannel::class])
    fun smsRouter(channels: List<SmsChannel>, quota: QuotaTracker): SmsRouter = SmsRouter(
        channels = channels,
        quota = quota,
        breakers = channels.associate { it.name to CircuitBreaker() },
    )

    // ── Repository bindings ───────────────────────────────────────────────────
    @Single(binds = [UserRepository::class])
    fun userRepository(): UserRepository = UserRepositoryImpl()

    @Single(binds = [TokenRepository::class])
    fun tokenRepository(): TokenRepository = TokenRepositoryImpl()

    @Single(binds = [OAuthIdentityRepository::class])
    fun oAuthIdentityRepository(): OAuthIdentityRepository = OAuthIdentityRepositoryImpl()

    @Single(binds = [EmergencyRevokeTokenRepository::class])
    fun emergencyRevokeTokenRepository(): EmergencyRevokeTokenRepository = EmergencyRevokeTokenRepositoryImpl()

    @Single(binds = [ProfileRepository::class])
    fun profileRepository(): ProfileRepository = ProfileRepositoryImpl()

    @Single(binds = [SessionRepository::class])
    fun sessionRepository(): SessionRepository = SessionRepositoryImpl()

    @Single(binds = [PasskeyRepository::class])
    fun passkeyRepository(): PasskeyRepository = PasskeyRepositoryImpl()

    @Single(binds = [TotpRepository::class])
    fun totpRepository(): TotpRepository = TotpRepositoryImpl()
}
