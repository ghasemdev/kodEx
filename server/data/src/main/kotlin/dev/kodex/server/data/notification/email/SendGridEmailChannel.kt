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
private data class SendGridRequest(
    val personalizations: List<SendGridPersonalization>,
    val from: SendGridAddress,
    val subject: String,
    val content: List<SendGridContent>,
)

@Serializable
private data class SendGridPersonalization(val to: List<SendGridAddress>)

@Serializable
private data class SendGridAddress(val email: String)

@Serializable
private data class SendGridContent(val type: String, val value: String)

@Single
class SendGridEmailChannel(
    @Named(ConfigQualifier.Email.SENDGRID_API_KEY) private val apiKey: String,
    @Named(ConfigQualifier.Email.FROM) private val from: String,
    private val httpClient: HttpClient,
) : EmailChannel {
    override val name: String = "SendGrid"
    override val dailyQuota: Int = -1

    val isEnabled: Boolean get() = apiKey.isNotEmpty()

    override suspend fun send(to: String, subject: String, html: String): Result<Unit> = try {
        val body = SendGridRequest(
            personalizations = listOf(SendGridPersonalization(to = listOf(SendGridAddress(to)))),
            from = SendGridAddress(from),
            subject = subject,
            content = listOf(SendGridContent(type = "text/html", value = html)),
        )
        val response = httpClient.post("https://api.sendgrid.com/v3/mail/send") {
            bearerAuth(apiKey)
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        if (response.status.isSuccess()) {
            Result.success(Unit)
        } else {
            Result.failure(RuntimeException("SendGrid error ${response.status.value}: ${response.bodyAsText()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
