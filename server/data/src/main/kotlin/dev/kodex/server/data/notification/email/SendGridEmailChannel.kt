package dev.kodex.server.data.notification.email

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

class SendGridEmailChannel(
    private val apiKey: String,
    private val from: String,
    private val httpClient: HttpClient,
) : EmailChannel {
    override val name: String = "SendGrid"
    override val dailyQuota: Int = -1

    override suspend fun send(to: String, subject: String, html: String): Result<Unit> {
        return try {
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
}
