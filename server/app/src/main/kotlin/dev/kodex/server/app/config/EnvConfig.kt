package dev.kodex.server.app.config

import dev.kodex.core.env.env
import dev.kodex.core.env.envOrNull

object EnvConfig {
    val jwtSecret: String get() = env("JWT_SECRET")

    val githubClientId: String get() = env("GITHUB_CLIENT_ID")
    val githubClientSecret: String get() = env("GITHUB_CLIENT_SECRET")
    val googleClientId: String get() = env("GOOGLE_CLIENT_ID")
    val googleClientSecret: String get() = env("GOOGLE_CLIENT_SECRET")
    val oauthCallbackBaseUrl: String get() = env("OAUTH_CALLBACK_BASE_URL")

    val cloudflareTurnstileSecret: String get() = env("CLOUDFLARE_TURNSTILE_SECRET")

    val resendApiKey: String get() = env("RESEND_API_KEY")
    val sendgridApiKey: String get() = envOrNull("SENDGRID_API_KEY") ?: ""
    val emailFrom: String get() = env("EMAIL_FROM")
    val appBaseUrl: String get() = env("APP_BASE_URL")

    val kavenegarApiKey: String get() = envOrNull("KAVENEGAR_API_KEY") ?: ""
    val twilioAccountSid: String get() = envOrNull("TWILIO_ACCOUNT_SID") ?: ""
    val twilioAuthToken: String get() = envOrNull("TWILIO_AUTH_TOKEN") ?: ""

    val totpEncryptionKey: String get() = env("TOTP_ENCRYPTION_KEY")
    val webAuthnChallengeKey: String get() = env("WEBAUTHN_CHALLENGE_KEY")

    val geoIpDbPath: String get() = env("GEOIP_DB_PATH")

    val redisUrl: String get() = envOrNull("REDIS_URL") ?: "redis://localhost:6379"
    val redisPassword: String get() = envOrNull("REDIS_PASSWORD") ?: ""

    val minioEndpoint: String get() = env("MINIO_ENDPOINT")
    val minioAccessKey: String get() = env("MINIO_ACCESS_KEY")
    val minioSecretKey: String get() = env("MINIO_SECRET_KEY")
    val minioBucketAvatars: String get() = envOrNull("MINIO_BUCKET_AVATARS") ?: "avatars"
    val minioPublicUrl: String get() = env("MINIO_PUBLIC_URL")
}
