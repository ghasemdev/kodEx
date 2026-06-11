package dev.kodex.server.api.auth.middleware

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class TurnstileResponse(val success: Boolean)

private val json = Json { ignoreUnknownKeys = true }
private const val TURNSTILE_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify"

class TurnstileVerifier(
    private val secret: String,
    private val httpClient: HttpClient,
) {
    suspend fun verify(token: String, ip: String): Boolean {
        if (secret.isEmpty()) return true
        return try {
            val response = httpClient.submitForm(
                url = TURNSTILE_URL,
                formParameters = Parameters.build {
                    append("secret", secret)
                    append("response", token)
                    append("remoteip", ip)
                },
            )
            if (!response.status.isSuccess()) return false
            json.decodeFromString<TurnstileResponse>(response.bodyAsText()).success
        } catch (_: Exception) {
            false
        }
    }
}
