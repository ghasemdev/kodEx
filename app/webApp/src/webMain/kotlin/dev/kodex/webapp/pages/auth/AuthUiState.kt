package dev.kodex.webapp.pages.auth

import androidx.compose.runtime.Immutable
import dev.kodex.shared.ui.UiState
import dev.kodex.webapp.auth.PASSWORD_STRENGTH_MIN_SCORE

private const val TURNSTILE_AFTER_FAILURES = 3

@Immutable
data class AuthUiState(
    // Sign up
    val signUpUsername: String = "",
    val signUpEmail: String = "",
    val signUpPassword: String = "",
    val signUpTurnstileToken: String? = null,
    val usernameAvailable: Boolean? = null,
    val isCheckingUsername: Boolean = false,
    val passwordScore: Int = 0,
    val signUpError: String? = null,
    val signUpSubmitting: Boolean = false,
    val signUpSuccess: Boolean = false,
    // Sign in
    val signInEmail: String = "",
    val signInPassword: String = "",
    val signInTurnstileToken: String? = null,
    val signInError: String? = null,
    val signInSubmitting: Boolean = false,
    val failedLoginAttempts: Int = 0,
    val totpSessionToken: String? = null,
    val totpCode: String = "",
    val totpError: String? = null,
    val totpSubmitting: Boolean = false,
    // Verify email
    val verifyEmailState: UiState<Unit> = UiState.Loading,
    val resendEmail: String = "",
    val resendSubmitting: Boolean = false,
    val resendSent: Boolean = false,
) {
    val signUpButtonEnabled: Boolean
        get() = signUpUsername.isNotBlank() &&
            signUpEmail.isNotBlank() &&
            passwordScore >= PASSWORD_STRENGTH_MIN_SCORE &&
            usernameAvailable == true &&
            !signUpTurnstileToken.isNullOrBlank() &&
            !signUpSubmitting

    val showSignInTurnstile: Boolean
        get() = failedLoginAttempts >= TURNSTILE_AFTER_FAILURES
}
