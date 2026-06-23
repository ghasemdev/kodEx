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
import dev.kodex.webapp.auth.PasswordStrength
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
fun IComponent.SignUpPage() {
    val viewModel = koinInject<AuthViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(Unit) {
        onDispose { viewModel.onCleared() }
    }

    SignUpPage(
        uiState = uiState,
        onUsernameChange = viewModel::onSignUpUsernameChange,
        onEmailChange = viewModel::onSignUpEmailChange,
        onPasswordChange = viewModel::onSignUpPasswordChange,
        onTurnstileVerified = viewModel::onSignUpTurnstileVerified,
        onSubmit = viewModel::submitSignUp,
    )
}

@Composable
private fun IComponent.SignUpPage(
    uiState: AuthUiState,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTurnstileVerified: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    div(className = "min-h-screen bg-surface text-on-surface flex flex-col") {
        GlobalNavBar(session = currentSessionState())
        div(className = "flex-1 flex items-center justify-center px-4 py-12") {
            div(
                id = "signup-card",
                className = "w-full max-w-md rounded-2xl border border-outline/20 bg-surface-container p-8",
            ) {
                h1(className = "text-2xl font-bold mb-1") { +i18n.tr("Create your account") }
                p(className = "text-sm text-on-surface/60 mb-6") {
                    +i18n.tr("Join KodEx to start solving and competing.")
                }

                signUpForm(
                    uiState = uiState,
                    onUsernameChange = onUsernameChange,
                    onEmailChange = onEmailChange,
                    onPasswordChange = onPasswordChange,
                    onTurnstileVerified = onTurnstileVerified,
                    onSubmit = onSubmit,
                )

                p(className = "text-sm text-on-surface/60 mt-6 text-center") {
                    +i18n.tr("Already have an account? ")
                    a(className = "text-primary font-medium cursor-pointer") {
                        +i18n.tr("Sign in")
                        onClick { Router.navigate("/sign-in") }
                    }
                }
            }
        }
        Footer()
    }
}

@Composable
private fun IComponent.signUpForm(
    uiState: AuthUiState,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTurnstileVerified: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val usernameError = if (uiState.usernameAvailable == false) {
        i18n.tr("This username is already taken.")
    } else {
        null
    }
    val usernameHelper = if (uiState.usernameAvailable == true) i18n.tr("Available") else null

    div(className = "flex flex-col gap-4") {
        Input(
            value = uiState.signUpUsername,
            onValueChange = onUsernameChange,
            label = i18n.tr("Username"),
            placeholder = "jane_doe",
            required = true,
            id = "signup-username",
            error = usernameError,
            helperText = usernameHelper,
        )
        Input(
            value = uiState.signUpEmail,
            onValueChange = onEmailChange,
            type = InputType.Email,
            label = i18n.tr("Email"),
            placeholder = "jane@example.com",
            required = true,
            id = "signup-email",
        )
        Input(
            value = uiState.signUpPassword,
            onValueChange = onPasswordChange,
            type = InputType.Password,
            label = i18n.tr("Password"),
            required = true,
            id = "signup-password",
            helperText = PasswordStrength.label(uiState.passwordScore),
        )
        passwordStrengthBar(uiState.passwordScore)

        val siteKey = turnstileSiteKeyFromEnv()
        if (!siteKey.isNullOrBlank()) {
            TurnstileWidget(
                id = "signup-turnstile",
                siteKey = siteKey,
                onVerified = onTurnstileVerified,
            )
        }

        if (uiState.signUpError != null) {
            p(className = "text-sm text-error") { +uiState.signUpError.orEmpty() }
        }

        Button(
            id = "signup-submit",
            label = if (uiState.signUpSubmitting) i18n.tr("Creating account…") else i18n.tr("Create account"),
            variant = ButtonVariant.Primary,
            enabled = uiState.signUpButtonEnabled,
            className = "w-full justify-center",
            onClick = onSubmit,
        )
    }
}

private const val STRENGTH_SEGMENTS = 4

@Composable
private fun IComponent.passwordStrengthBar(score: Int) {
    div(className = "flex gap-1") {
        repeat(STRENGTH_SEGMENTS) { index ->
            val filled = index < score
            div(
                className = "h-1 flex-1 rounded-full " +
                    if (filled) strengthColor(score) else "bg-outline/20",
            ) {}
        }
    }
}

private fun strengthColor(score: Int): String = when {
    score <= 1 -> "bg-error"
    score == 2 -> "bg-warning"
    else -> "bg-success"
}
