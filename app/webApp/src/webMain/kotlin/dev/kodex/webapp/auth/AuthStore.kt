package dev.kodex.webapp.auth

import dev.kodex.core.models.auth.LoginRequest
import dev.kodex.core.models.auth.LoginResponse
import dev.kodex.core.models.auth.TotpLoginRequest
import dev.kodex.webapp.network.ApiException
import dev.kodex.webapp.network.auth.AuthRemoteDataSource
import dev.kodex.webapp.network.users.UserRemoteDataSource
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

private val REFRESH_LEAD_TIME = 60.seconds

sealed class LoginOutcome {
    data object Success : LoginOutcome()
    data class RequiresTotp(val totpSessionToken: String) : LoginOutcome()
    data class Error(val message: String) : LoginOutcome()
}

@Single
class AuthStore(
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val userRemoteDataSource: UserRemoteDataSource,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var refreshJob: Job? = null

    val state: StateFlow<AuthState>
        field: MutableStateFlow<AuthState> = MutableStateFlow(AuthState.LoggedOut)

    init {
        scope.launch { refreshNow() }
    }

    suspend fun login(email: String, password: String, turnstileToken: String?): LoginOutcome {
        state.value = AuthState.LoggingIn
        return try {
            applyLoginResponse(authRemoteDataSource.login(LoginRequest(email, password, turnstileToken)))
        } catch (e: ApiException) {
            state.value = AuthState.LoggedOut
            LoginOutcome.Error(e.message)
        }
    }

    suspend fun submitTotp(totpSessionToken: String, code: String): LoginOutcome {
        state.value = AuthState.LoggingIn
        return try {
            val tokens = authRemoteDataSource.totpLogin(TotpLoginRequest(totpSessionToken, code))
            applyTokens(tokens.accessToken, tokens.expiresIn)
            LoginOutcome.Success
        } catch (e: ApiException) {
            state.value = AuthState.RequiresTotp(totpSessionToken)
            LoginOutcome.Error(e.message)
        }
    }

    suspend fun setLoggedIn(accessToken: String, expiresIn: Int) = applyTokens(accessToken, expiresIn)

    suspend fun logout() {
        refreshJob?.cancel()
        runCatching { authRemoteDataSource.logout() }
        state.value = AuthState.LoggedOut
    }

    suspend fun refreshNow(): Boolean = try {
        val tokens = authRemoteDataSource.refresh()
        val current = state.value
        val user = if (current is AuthState.LoggedIn) current.user else userRemoteDataSource.getMe()
        val expiresAt = Clock.System.now() + tokens.expiresIn.seconds
        state.value = AuthState.LoggedIn(user, tokens.accessToken, expiresAt)
        scheduleRefresh(expiresAt)
        true
    } catch (e: Exception) {
        refreshJob?.cancel()
        state.value = AuthState.LoggedOut
        false
    }

    private suspend fun applyLoginResponse(response: LoginResponse): LoginOutcome {
        val totpSessionToken = response.totpSessionToken
        if (response.requiresTotp && totpSessionToken != null) {
            state.value = AuthState.RequiresTotp(totpSessionToken)
            return LoginOutcome.RequiresTotp(totpSessionToken)
        }
        val accessToken = response.accessToken
        val expiresIn = response.expiresIn
        if (accessToken == null || expiresIn == null) {
            state.value = AuthState.LoggedOut
            return LoginOutcome.Error("Unexpected login response.")
        }
        applyTokens(accessToken, expiresIn)
        return LoginOutcome.Success
    }

    private suspend fun applyTokens(accessToken: String, expiresIn: Int) {
        val expiresAt = Clock.System.now() + expiresIn.seconds
        val user = userRemoteDataSource.getMe()
        state.value = AuthState.LoggedIn(user, accessToken, expiresAt)
        scheduleRefresh(expiresAt)
    }

    private fun scheduleRefresh(expiresAt: Instant) {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            delay((expiresAt - Clock.System.now() - REFRESH_LEAD_TIME).coerceAtLeast(Duration.ZERO))
            refreshNow()
        }
    }
}
