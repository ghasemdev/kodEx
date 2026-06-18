package dev.kodex.core.config

sealed class ConfigQualifier {
    sealed class Auth {
        companion object {
            const val JWT_SECRET = "config.jwt.secret"
        }
    }

    sealed class Geo {
        companion object {
            const val DB_PATH = "config.geoip.db_path"
        }
    }

    sealed class Redis {
        companion object {
            const val URL = "config.redis.url"
            const val PASSWORD = "config.redis.password"
        }
    }

    sealed class Turnstile {
        companion object {
            const val SECRET = "config.turnstile.secret"
        }
    }

    sealed class Minio {
        companion object {
            const val ENDPOINT = "config.minio.endpoint"
            const val ACCESS_KEY = "config.minio.access_key"
            const val SECRET_KEY = "config.minio.secret_key"
            const val BUCKET_AVATARS = "config.minio.bucket_avatars"
            const val PUBLIC_URL = "config.minio.public_url"
        }
    }

    sealed class Email {
        companion object {
            const val RESEND_API_KEY = "config.email.resend_api_key"
            const val SENDGRID_API_KEY = "config.email.sendgrid_api_key"
            const val FROM = "config.email.from"
        }
    }

    sealed class App {
        companion object {
            const val BASE_URL = "config.app.base_url"
        }
    }

    sealed class Sms {
        companion object {
            const val KAVENEGAR_API_KEY = "config.sms.kavenegar_api_key"
            const val TWILIO_ACCOUNT_SID = "config.sms.twilio_account_sid"
            const val TWILIO_AUTH_TOKEN = "config.sms.twilio_auth_token"
            const val TWILIO_FROM_NUMBER = "config.sms.twilio_from_number"
        }
    }
}
