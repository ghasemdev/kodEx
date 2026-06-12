package dev.kodex.server.data.notification.sms

import dev.kodex.core.config.ConfigQualifier
import dev.kodex.server.data.notification.channel.SmsChannel
import io.ktor.client.HttpClient
import io.ktor.client.request.basicAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
class TwilioSmsChannel(
    @Named(ConfigQualifier.Sms.TWILIO_ACCOUNT_SID) private val accountSid: String,
    @Named(ConfigQualifier.Sms.TWILIO_AUTH_TOKEN) private val authToken: String,
    @Named(ConfigQualifier.Sms.TWILIO_FROM_NUMBER) private val fromNumber: String,
    private val httpClient: HttpClient,
) : SmsChannel {
    override val name: String = "Twilio"
    override val dailyQuota: Int = -1

    val isEnabled: Boolean get() = accountSid.isNotEmpty()

    override suspend fun send(to: String, message: String): Result<Unit> {
        return try {
            val url = "https://api.twilio.com/2010-04-01/Accounts/$accountSid/Messages.json"
            val response = httpClient.submitForm(
                url = url,
                formParameters = Parameters.build {
                    append("To", to)
                    append("From", fromNumber)
                    append("Body", message)
                },
            ) {
                basicAuth(accountSid, authToken)
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(RuntimeException("Twilio error ${response.status.value}: ${response.bodyAsText()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
