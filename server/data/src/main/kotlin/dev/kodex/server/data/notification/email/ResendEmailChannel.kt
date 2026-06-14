package dev.kodex.server.data.notification.email

import dev.kodex.core.config.ConfigQualifier
import dev.kodex.server.data.notification.channel.EmailChannel
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Serializable
private data class ResendEmailRequest(
    val from: String,
    val to: List<String>,
    val subject: String,
    val html: String,
)

@Single
class ResendEmailChannel(
    @Named(ConfigQualifier.Email.RESEND_API_KEY) private val apiKey: String,
    @Named(ConfigQualifier.Email.FROM) private val from: String,
    private val httpClient: HttpClient,
) : EmailChannel {
    override val name: String = "Resend"
    override val dailyQuota: Int = 3000

    val isEnabled: Boolean get() = apiKey.isNotEmpty()

    override suspend fun send(to: String, subject: String, html: String): Result<Unit> = try {
        val response = httpClient.post("https://api.resend.com/emails") {
            bearerAuth(apiKey)
            contentType(ContentType.Application.Json)
            setBody(ResendEmailRequest(from = from, to = listOf(to), subject = subject, html = html))
        }
        if (response.status.isSuccess()) {
            Result.success(Unit)
        } else {
            Result.failure(RuntimeException("Resend error ${response.status.value}: ${response.bodyAsText()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
