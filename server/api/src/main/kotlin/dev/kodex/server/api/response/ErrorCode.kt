@file:Suppress("PropertyName", "RedundantSuppression")

package dev.kodex.server.api.response

internal const val DEFAULT_LANG = "en"
internal val SUPPORTED_LANGUAGES = setOf("en", "fa")

object ErrorCode {
    // Auth
    const val UNAUTHORIZED = "UNAUTHORIZED"
    const val FORBIDDEN = "FORBIDDEN"
    const val ACCOUNT_LOCKED = "ACCOUNT_LOCKED"
    const val EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED"
    const val INVALID_CREDENTIALS = "INVALID_CREDENTIALS"
    const val TOKEN_EXPIRED = "TOKEN_EXPIRED"
    const val TOKEN_ALREADY_USED = "TOKEN_ALREADY_USED"
    const val TOKEN_INVALID = "TOKEN_INVALID"
    const val TURNSTILE_FAILED = "TURNSTILE_FAILED"
    const val EMAIL_ALREADY_REGISTERED = "EMAIL_ALREADY_REGISTERED"

    // Resources
    const val NOT_FOUND = "NOT_FOUND"
    const val CONFLICT = "CONFLICT"
    const val VALIDATION_ERROR = "VALIDATION_ERROR"

    // Infra
    const val INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR"
    const val TOO_MANY_REQUESTS = "TOO_MANY_REQUESTS"
    const val SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE"
}

// ── Auth errors ───────────────────────────────────────────────────────────────
private val authMessages: Map<String, Map<String, String>> = mapOf(
    ErrorCode.UNAUTHORIZED to mapOf(
        "en" to "Authentication required. Please sign in and try again.",
        "fa" to "احراز هویت الزامی است. لطفاً وارد شوید و دوباره تلاش کنید.",
    ),
    ErrorCode.FORBIDDEN to mapOf(
        "en" to "You don't have permission to perform this action.",
        "fa" to "شما مجوز انجام این عملیات را ندارید.",
    ),
    ErrorCode.ACCOUNT_LOCKED to mapOf(
        "en" to "Your account has been temporarily locked. Please try again later.",
        "fa" to "حساب کاربری شما به‌طور موقت قفل شده است. لطفاً بعداً دوباره تلاش کنید.",
    ),
    ErrorCode.EMAIL_NOT_VERIFIED to mapOf(
        "en" to "Please verify your email address before continuing.",
        "fa" to "لطفاً قبل از ادامه آدرس ایمیل خود را تأیید کنید.",
    ),
    ErrorCode.INVALID_CREDENTIALS to mapOf(
        "en" to "Incorrect email or password.",
        "fa" to "ایمیل یا رمز عبور اشتباه است.",
    ),
    ErrorCode.TOKEN_EXPIRED to mapOf(
        "en" to "This link has expired. Please request a new one.",
        "fa" to "این لینک منقضی شده است. لطفاً یک لینک جدید درخواست کنید.",
    ),
    ErrorCode.TOKEN_ALREADY_USED to mapOf(
        "en" to "This link has already been used.",
        "fa" to "این لینک قبلاً استفاده شده است.",
    ),
    ErrorCode.TOKEN_INVALID to mapOf(
        "en" to "Invalid verification token.",
        "fa" to "توکن تأیید نامعتبر است.",
    ),
    ErrorCode.TURNSTILE_FAILED to mapOf(
        "en" to "Bot detection failed. Please try again.",
        "fa" to "تشخیص ربات ناموفق بود. لطفاً دوباره تلاش کنید.",
    ),
    ErrorCode.EMAIL_ALREADY_REGISTERED to mapOf(
        "en" to "This email address is already registered.",
        "fa" to "این آدرس ایمیل قبلاً ثبت شده است.",
    ),
)

// ── Resource errors ───────────────────────────────────────────────────────────
private val resourceMessages: Map<String, Map<String, String>> = mapOf(
    ErrorCode.NOT_FOUND to mapOf(
        "en" to "The requested resource was not found.",
        "fa" to "منبع درخواست شده یافت نشد.",
    ),
    ErrorCode.CONFLICT to mapOf(
        "en" to "A conflict occurred. The resource may already exist.",
        "fa" to "تعارضی رخ داد. ممکن است این منبع از قبل وجود داشته باشد.",
    ),
    ErrorCode.VALIDATION_ERROR to mapOf(
        "en" to "The provided data is invalid.",
        "fa" to "داده‌های وارد شده نامعتبر است.",
    ),
)

// ── Infra errors ──────────────────────────────────────────────────────────────
private val infraMessages: Map<String, Map<String, String>> = mapOf(
    ErrorCode.INTERNAL_SERVER_ERROR to mapOf(
        "en" to "An unexpected error occurred. Please try again later.",
        "fa" to "خطای غیرمنتظره‌ای رخ داد. لطفاً دوباره تلاش کنید.",
    ),
    ErrorCode.TOO_MANY_REQUESTS to mapOf(
        "en" to "Too many requests. Please wait a moment and try again.",
        "fa" to "درخواست‌های زیادی ارسال شد. لطفاً کمی صبر کنید و دوباره تلاش کنید.",
    ),
    ErrorCode.SERVICE_UNAVAILABLE to mapOf(
        "en" to "The service is temporarily unavailable. Please try again shortly.",
        "fa" to "سرویس به‌طور موقت در دسترس نیست. لطفاً به زودی دوباره تلاش کنید.",
    ),
)

// Merged catalog — add new feature groups here: `+ paymentMessages + notifMessages`
internal val errorUserMessages: Map<String, Map<String, String>> =
    authMessages + resourceMessages + infraMessages
