package dev.kodex.webapp.pages.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.kilua.core.IComponent
import dev.kilua.form.InputType
import dev.kilua.html.a
import dev.kilua.html.div
import dev.kilua.html.h1
import dev.kilua.html.p
import dev.kodex.webapp.auth.currentSessionState
import dev.kodex.webapp.core.Router
import dev.kodex.webapp.design.components.Button
import dev.kodex.webapp.design.components.ButtonVariant
import dev.kodex.webapp.design.components.Input
import dev.kodex.webapp.design.i18n.i18n
import dev.kodex.webapp.di.org.koin.compose.koinInject
import dev.kodex.webapp.layout.Footer
import dev.kodex.webapp.layout.GlobalNavBar
import dev.kodex.webapp.turnstile.TurnstileWidget
import dev.kodex.webapp.turnstileSiteKeyFromEnv

@Composable
fun IComponent.SignInPage() {
    val viewModel = koinInject<AuthViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(Unit) {
        onDispose { viewModel.onCleared() }
    }

    SignInPage(
        uiState = uiState,
        onEmailChange = viewModel::onSignInEmailChange,
        onPasswordChange = viewModel::onSignInPasswordChange,
        onTurnstileVerified = viewModel::onSignInTurnstileVerified,
        onSubmit = viewModel::submitSignIn,
        onTotpCodeChange = viewModel::onTotpCodeChange,
        onTotpSubmit = viewModel::submitTotp,
    )
}

@Composable
private fun IComponent.SignInPage(
    uiState: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTurnstileVerified: (String) -> Unit,
    onSubmit: () -> Unit,
    onTotpCodeChange: (String) -> Unit,
    onTotpSubmit: () -> Unit,
) {
    div(className = "min-h-screen bg-surface text-on-surface flex flex-col") {
        GlobalNavBar(session = currentSessionState())
        div(className = "flex-1 flex items-center justify-center px-4 py-12") {
            div(
                id = "signin-card",
                className = "w-full max-w-md rounded-2xl border border-outline/20 bg-surface-container p-8",
            ) {
                if (uiState.totpSessionToken != null) {
                    totpForm(uiState, onTotpCodeChange, onTotpSubmit)
                } else {
                    h1(className = "text-2xl font-bold mb-1") { +i18n.tr("Welcome back") }
                    p(className = "text-sm text-on-surface/60 mb-6") {
                        +i18n.tr("Sign in to continue to KodEx.")
                    }
                    signInForm(uiState, onEmailChange, onPasswordChange, onTurnstileVerified, onSubmit)
                    p(className = "text-sm text-on-surface/60 mt-6 text-center") {
                        +i18n.tr("Don't have an account? ")
                        a(className = "text-primary font-medium cursor-pointer") {
                            +i18n.tr("Sign up")
                            onClick { Router.navigate("/sign-up") }
                        }
                    }
                }
            }
        }
        Footer()
    }
}

@Composable
private fun IComponent.signInForm(
    uiState: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTurnstileVerified: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    div(className = "flex flex-col gap-4") {
        Input(
            value = uiState.signInEmail,
            onValueChange = onEmailChange,
            type = InputType.Email,
            label = i18n.tr("Email"),
            placeholder = "jane@example.com",
            required = true,
            id = "signin-email",
        )
        Input(
            value = uiState.signInPassword,
            onValueChange = onPasswordChange,
            type = InputType.Password,
            label = i18n.tr("Password"),
            required = true,
            id = "signin-password",
        )

        if (uiState.showSignInTurnstile) {
            val siteKey = turnstileSiteKeyFromEnv()
            if (!siteKey.isNullOrBlank()) {
                TurnstileWidget(
                    id = "signin-turnstile",
                    siteKey = siteKey,
                    onVerified = onTurnstileVerified,
                )
            }
        }

        if (uiState.signInError != null) {
            p(className = "text-sm text-error") { +uiState.signInError.orEmpty() }
        }

        val turnstileBlocked = uiState.showSignInTurnstile && uiState.signInTurnstileToken.isNullOrBlank()
        Button(
            id = "signin-submit",
            label = if (uiState.signInSubmitting) i18n.tr("Signing in…") else i18n.tr("Sign in"),
            variant = ButtonVariant.Primary,
            enabled = uiState.signInEmail.isNotBlank() &&
                uiState.signInPassword.isNotBlank() &&
                !uiState.signInSubmitting &&
                !turnstileBlocked,
            className = "w-full justify-center",
            onClick = onSubmit,
        )
    }
}

@Composable
private fun IComponent.totpForm(uiState: AuthUiState, onTotpCodeChange: (String) -> Unit, onTotpSubmit: () -> Unit) {
    h1(className = "text-2xl font-bold mb-1") { +i18n.tr("Two-factor authentication") }
    p(className = "text-sm text-on-surface/60 mb-6") {
        +i18n.tr("Enter the 6-digit code from your authenticator app.")
    }
    div(className = "flex flex-col gap-4") {
        Input(
            value = uiState.totpCode,
            onValueChange = onTotpCodeChange,
            label = i18n.tr("Code"),
            placeholder = "123456",
            required = true,
            id = "signin-totp-code",
            error = uiState.totpError,
        )
        Button(
            id = "signin-totp-submit",
            label = if (uiState.totpSubmitting) i18n.tr("Verifying…") else i18n.tr("Verify"),
            variant = ButtonVariant.Primary,
            enabled = uiState.totpCode.isNotBlank() && !uiState.totpSubmitting,
            className = "w-full justify-center",
            onClick = onTotpSubmit,
        )
    }
}
