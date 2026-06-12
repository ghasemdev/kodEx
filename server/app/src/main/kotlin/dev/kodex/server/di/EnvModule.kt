package dev.kodex.server.di

import dev.kodex.core.config.ConfigQualifier
import dev.kodex.core.config.EnvConfig
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
class EnvModule {
    @Single
    @Named(ConfigQualifier.Auth.JWT_SECRET)
    fun jwtSecret(): String = EnvConfig.jwtSecret

    @Single
    @Named(ConfigQualifier.Geo.DB_PATH)
    fun geoIpDbPath(): String = EnvConfig.geoIpDbPath

    @Single
    @Named(ConfigQualifier.Redis.URL)
    fun redisUrl(): String = EnvConfig.redisUrl

    @Single
    @Named(ConfigQualifier.Redis.PASSWORD)
    fun redisPassword(): String = EnvConfig.redisPassword

    @Single
    @Named(ConfigQualifier.Turnstile.SECRET)
    fun turnstileSecret(): String = EnvConfig.cloudflareTurnstileSecret

    @Single
    @Named(ConfigQualifier.Minio.ENDPOINT)
    fun minioEndpoint(): String = EnvConfig.minioEndpoint

    @Single
    @Named(ConfigQualifier.Minio.ACCESS_KEY)
    fun minioAccessKey(): String = EnvConfig.minioAccessKey

    @Single
    @Named(ConfigQualifier.Minio.SECRET_KEY)
    fun minioSecretKey(): String = EnvConfig.minioSecretKey

    @Single
    @Named(ConfigQualifier.Minio.BUCKET_AVATARS)
    fun minioBucketAvatars(): String = EnvConfig.minioBucketAvatars

    @Single
    @Named(ConfigQualifier.Minio.PUBLIC_URL)
    fun minioPublicUrl(): String = EnvConfig.minioPublicUrl

    @Single
    @Named(ConfigQualifier.Email.RESEND_API_KEY)
    fun resendApiKey(): String = EnvConfig.resendApiKey

    @Single
    @Named(ConfigQualifier.Email.SENDGRID_API_KEY)
    fun sendgridApiKey(): String = EnvConfig.sendgridApiKey

    @Single
    @Named(ConfigQualifier.Email.FROM)
    fun emailFrom(): String = EnvConfig.emailFrom

    @Single
    @Named(ConfigQualifier.Sms.KAVENEGAR_API_KEY)
    fun kavenegarApiKey(): String = EnvConfig.kavenegarApiKey

    @Single
    @Named(ConfigQualifier.Sms.TWILIO_ACCOUNT_SID)
    fun twilioAccountSid(): String = EnvConfig.twilioAccountSid

    @Single
    @Named(ConfigQualifier.Sms.TWILIO_AUTH_TOKEN)
    fun twilioAuthToken(): String = EnvConfig.twilioAuthToken

    @Single
    @Named(ConfigQualifier.Sms.TWILIO_FROM_NUMBER)
    fun twilioFromNumber(): String = EnvConfig.twilioFromNumber
}
