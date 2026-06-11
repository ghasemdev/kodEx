package dev.kodex.server.data.notification.sms

import dev.kodex.server.data.notification.channel.SmsChannel
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess

class KavenegarSmsChannel(
    private val apiKey: String,
    private val httpClient: HttpClient,
) : SmsChannel {
    override val name: String = "Kavenegar"
    override val dailyQuota: Int = -1

    override suspend fun send(to: String, message: String): Result<Unit> {
        return try {
            val url = "https://api.kavenegar.com/v1/$apiKey/sms/send.json"
            val response = httpClient.submitForm(
                url = url,
                formParameters = Parameters.build {
                    append("receptor", to)
                    append("message", message)
                },
            )
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(RuntimeException("Kavenegar error ${response.status.value}: ${response.bodyAsText()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
