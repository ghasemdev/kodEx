package dev.kodex.webapp.pages.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.kilua.core.IComponent
import dev.kilua.html.div
import dev.kilua.html.h1
import dev.kilua.html.p
import dev.kodex.shared.ui.UiState
import dev.kodex.webapp.auth.currentSessionState
import dev.kodex.webapp.core.Router
import dev.kodex.webapp.design.components.Button
import dev.kodex.webapp.design.components.ButtonVariant
import dev.kodex.webapp.design.components.Input
import dev.kodex.webapp.design.i18n.i18n
import dev.kodex.webapp.di.org.koin.compose.koinInject
import dev.kodex.webapp.layout.Footer
import dev.kodex.webapp.layout.GlobalNavBar
import kotlinx.browser.window

private fun verificationTokenFromUrl(): String? {
    val search = window.location.search.removePrefix("?")
    if (search.isBlank()) return null
    return search.split("&")
        .map { it.split("=", limit = 2) }
        .firstOrNull { it.firstOrNull() == "token" }
        ?.getOrNull(1)
}

@Composable
fun IComponent.VerifyEmailPage() {
    val viewModel = koinInject<AuthViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val token = verificationTokenFromUrl()

    DisposableEffect(Unit) {
        onDispose { viewModel.onCleared() }
    }

    LaunchedEffect(token) {
        if (token != null) viewModel.verifyEmail(token)
    }

    VerifyEmailPage(
        uiState = uiState,
        hasToken = token != null,
        onResendEmailChange = viewModel::onResendEmailChange,
        onResendSubmit = viewModel::resendVerification,
    )
}

@Composable
private fun IComponent.VerifyEmailPage(
    uiState: AuthUiState,
    hasToken: Boolean,
    onResendEmailChange: (String) -> Unit,
    onResendSubmit: () -> Unit,
) {
    div(className = "min-h-screen bg-surface text-on-surface flex flex-col") {
        GlobalNavBar(session = currentSessionState())
        div(className = "flex-1 flex items-center justify-center px-4 py-12") {
            div(
                id = "verify-email-card",
                className = "w-full max-w-md rounded-2xl border border-outline/20 bg-surface-container p-8",
            ) {
                if (hasToken) {
                    verifyEmailTokenState(uiState.verifyEmailState)
                } else {
                    checkInboxState(uiState, onResendEmailChange, onResendSubmit)
                }
            }
        }
        Footer()
    }
}

@Composable
private fun IComponent.verifyEmailTokenState(state: UiState<Unit>) {
    when (state) {
        is UiState.Loading -> {
            h1(className = "text-2xl font-bold mb-1") { +i18n.tr("Verifying your email…") }
        }

        is UiState.Success -> {
            h1(className = "text-2xl font-bold mb-1 text-success") { +i18n.tr("Email verified!") }
            p(className = "text-sm text-on-surface/60 mt-2 mb-6") {
                +i18n.tr("Your account is ready. Let's get started.")
            }
            Button(
                id = "verify-email-continue",
                label = i18n.tr("Go to KodEx"),
                variant = ButtonVariant.Primary,
                className = "w-full justify-center",
                onClick = { Router.navigate("/") },
            )
        }

        is UiState.Error -> {
            h1(className = "text-2xl font-bold mb-1 text-error") { +i18n.tr("Verification failed") }
            p(className = "text-sm text-on-surface/60 mt-2") {
                +(state.message ?: i18n.tr("This link is invalid or has expired."))
            }
        }
    }
}

@Composable
private fun IComponent.checkInboxState(
    uiState: AuthUiState,
    onResendEmailChange: (String) -> Unit,
    onResendSubmit: () -> Unit,
) {
    h1(className = "text-2xl font-bold mb-1") { +i18n.tr("Check your inbox") }
    p(className = "text-sm text-on-surface/60 mt-2 mb-6") {
        +i18n.tr("We sent you a verification link. Click it to activate your account.")
    }

    Input(
        value = uiState.resendEmail,
        onValueChange = onResendEmailChange,
        label = i18n.tr("Email"),
        placeholder = "jane@example.com",
        id = "verify-email-resend-email",
    )

    if (uiState.resendSent) {
        p(className = "text-sm text-success mt-2") { +i18n.tr("If that account exists, a new link was sent.") }
    }

    Button(
        id = "verify-email-resend",
        label = if (uiState.resendSubmitting) i18n.tr("Sending…") else i18n.tr("Resend link"),
        variant = ButtonVariant.Secondary,
        className = "w-full justify-center mt-4",
        enabled = uiState.resendEmail.isNotBlank() && !uiState.resendSubmitting,
        onClick = onResendSubmit,
    )
}
