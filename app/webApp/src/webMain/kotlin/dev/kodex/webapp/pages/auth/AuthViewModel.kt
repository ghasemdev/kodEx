package dev.kodex.webapp.pages.auth

import dev.kodex.core.models.auth.RegisterRequest
import dev.kodex.shared.ui.UiState
import dev.kodex.webapp.auth.AuthStore
import dev.kodex.webapp.auth.LoginOutcome
import dev.kodex.webapp.auth.PasswordStrength
import dev.kodex.webapp.core.Router
import dev.kodex.webapp.core.ViewModel
import dev.kodex.webapp.network.ApiException
import dev.kodex.webapp.network.auth.AuthRemoteDataSource
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory

private const val USERNAME_CHECK_DEBOUNCE_MS = 400L

@Factory
class AuthViewModel(
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val authStore: AuthStore,
) : ViewModel() {
    val uiState: StateFlow<AuthUiState>
        field: MutableStateFlow<AuthUiState> = MutableStateFlow(AuthUiState())

    private var usernameCheckJob: Job? = null

    fun onSignUpUsernameChange(value: String) {
        uiState.update { it.copy(signUpUsername = value, usernameAvailable = null) }
        usernameCheckJob?.cancel()
        if (value.isBlank()) return
        usernameCheckJob = viewModelScope.launch {
            delay(USERNAME_CHECK_DEBOUNCE_MS.milliseconds)
            uiState.update { it.copy(isCheckingUsername = true) }
            val available = runCatching { authRemoteDataSource.checkUsernameAvailable(value) }
                .getOrNull()
                ?.available
            uiState.update { it.copy(isCheckingUsername = false, usernameAvailable = available) }
        }
    }

    fun onSignUpEmailChange(value: String) {
        uiState.update { it.copy(signUpEmail = value) }
    }

    fun onSignUpPasswordChange(value: String) {
        uiState.update { it.copy(signUpPassword = value, passwordScore = PasswordStrength.score(value)) }
    }

    fun onSignUpTurnstileVerified(token: String) {
        uiState.update { it.copy(signUpTurnstileToken = token) }
    }

    fun submitSignUp() {
        val current = uiState.value
        val token = current.signUpTurnstileToken ?: return
        viewModelScope.launch {
            uiState.update { it.copy(signUpSubmitting = true, signUpError = null) }
            try {
                val tokens = authRemoteDataSource.register(
                    RegisterRequest(
                        username = current.signUpUsername,
                        email = current.signUpEmail,
                        password = current.signUpPassword,
                        turnstileToken = token,
                    ),
                )
                authStore.setLoggedIn(tokens.accessToken, tokens.expiresIn)
                uiState.update { it.copy(signUpSubmitting = false, signUpSuccess = true) }
                Router.navigate("/verify-email")
            } catch (e: ApiException) {
                uiState.update { it.copy(signUpSubmitting = false, signUpError = e.message) }
            }
        }
    }

    fun onSignInEmailChange(value: String) {
        uiState.update { it.copy(signInEmail = value, signInError = null) }
    }

    fun onSignInPasswordChange(value: String) {
        uiState.update { it.copy(signInPassword = value, signInError = null) }
    }

    fun onSignInTurnstileVerified(token: String) {
        uiState.update { it.copy(signInTurnstileToken = token) }
    }

    fun submitSignIn() {
        val current = uiState.value
        viewModelScope.launch {
            uiState.update { it.copy(signInSubmitting = true, signInError = null) }
            when (
                val outcome = authStore.login(
                    current.signInEmail,
                    current.signInPassword,
                    current.signInTurnstileToken,
                )
            ) {
                LoginOutcome.Success -> {
                    uiState.update { it.copy(signInSubmitting = false, failedLoginAttempts = 0) }
                    Router.navigate("/")
                }

                is LoginOutcome.RequiresTotp -> {
                    uiState.update {
                        it.copy(signInSubmitting = false, totpSessionToken = outcome.totpSessionToken)
                    }
                }

                is LoginOutcome.Error -> {
                    uiState.update {
                        it.copy(
                            signInSubmitting = false,
                            signInError = outcome.message,
                            failedLoginAttempts = it.failedLoginAttempts + 1,
                        )
                    }
                }
            }
        }
    }

    fun onTotpCodeChange(value: String) {
        uiState.update { it.copy(totpCode = value, totpError = null) }
    }

    fun submitTotp() {
        val current = uiState.value
        val sessionToken = current.totpSessionToken ?: return
        viewModelScope.launch {
            uiState.update { it.copy(totpSubmitting = true, totpError = null) }
            when (val outcome = authStore.submitTotp(sessionToken, current.totpCode)) {
                LoginOutcome.Success -> {
                    uiState.update { it.copy(totpSubmitting = false) }
                    Router.navigate("/")
                }

                is LoginOutcome.RequiresTotp -> {
                    uiState.update { it.copy(totpSubmitting = false) }
                }

                is LoginOutcome.Error -> {
                    uiState.update {
                        it.copy(totpSubmitting = false, totpError = outcome.message)
                    }
                }
            }
        }
    }

    fun verifyEmail(token: String) {
        viewModelScope.launch {
            uiState.update { it.copy(verifyEmailState = UiState.Loading) }
            try {
                val tokens = authRemoteDataSource.verifyEmail(token)
                authStore.setLoggedIn(tokens.accessToken, tokens.expiresIn)
                uiState.update { it.copy(verifyEmailState = UiState.Success(Unit)) }
            } catch (e: ApiException) {
                uiState.update { it.copy(verifyEmailState = UiState.Error(e.message)) }
            }
        }
    }

    fun onResendEmailChange(value: String) {
        uiState.update { it.copy(resendEmail = value, resendSent = false) }
    }

    fun resendVerification() {
        val email = uiState.value.resendEmail
        if (email.isBlank()) return
        viewModelScope.launch {
            uiState.update { it.copy(resendSubmitting = true) }
            runCatching { authRemoteDataSource.resendVerification(email) }
            uiState.update { it.copy(resendSubmitting = false, resendSent = true) }
        }
    }
}
