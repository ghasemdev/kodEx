package dev.kodex.webapp.network

object ApiRoutes {
    const val LANDING_STATS = "/api/v1/stats/landing"

    object Auth {
        private const val BASE = "/api/v1/auth"
        const val REGISTER = "$BASE/register"
        const val VERIFY_EMAIL = "$BASE/verify-email"
        const val LOGIN = "$BASE/login"
        const val TOTP_LOGIN = "$BASE/totp/login"
        const val LOGOUT = "$BASE/logout"
        const val REFRESH = "$BASE/refresh"
        const val USERNAME_AVAILABLE = "$BASE/username/available"
        const val OAUTH_CALLBACK = "$BASE/oauth/{provider}/callback"
        const val FORGOT_PASSWORD = "$BASE/forgot-password"
        const val RESET_PASSWORD = "$BASE/reset-password"
        const val CHANGE_PASSWORD = "$BASE/change-password"
        const val TOTP_SETUP = "$BASE/totp/setup"
        const val TOTP_ENABLE = "$BASE/totp/enable"
        const val TOTP_DISABLE = "$BASE/totp/disable"
        const val PASSKEY_REGISTER_OPTIONS = "$BASE/passkey/register/options"
        const val PASSKEY_REGISTER = "$BASE/passkey/register"
        const val PASSKEY_AUTH_OPTIONS = "$BASE/passkey/auth/options"
        const val PASSKEY_AUTH = "$BASE/passkey/auth"
        const val SESSIONS = "$BASE/sessions"
        const val SESSION_BY_ID = "$BASE/sessions/{id}"
        const val VERIFY_EMAIL_RESEND = "$BASE/verify-email/resend"
    }

    object Users {
        private const val BASE = "/api/v1/users"
        const val ME = "$BASE/me"
    }
}
