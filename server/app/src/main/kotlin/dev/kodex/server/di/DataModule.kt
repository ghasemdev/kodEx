package dev.kodex.server.di

import dev.kodex.server.data.notification.channel.EmailChannel
import dev.kodex.server.data.notification.channel.SmsChannel
import dev.kodex.server.data.notification.email.ResendEmailChannel
import dev.kodex.server.data.notification.email.SendGridEmailChannel
import dev.kodex.server.data.notification.infrastructure.CircuitBreaker
import dev.kodex.server.data.notification.infrastructure.QuotaTracker
import dev.kodex.server.data.notification.router.EmailRouter
import dev.kodex.server.data.notification.router.SmsRouter
import dev.kodex.server.data.notification.sms.KavenegarSmsChannel
import dev.kodex.server.data.notification.sms.TwilioSmsChannel
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class DataModule {
    @Single
    fun httpClient(json: Json): HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(json) }
        expectSuccess = false
    }

    // Inject concrete channel types directly to avoid Koin resolving List<EmailChannel>
    // via getAll<EmailChannel>(), which would include EmailRouter itself and cause a
    // circular dependency → StackOverflowError.
    @Single(binds = [EmailChannel::class])
    fun emailRouter(resend: ResendEmailChannel, sendGrid: SendGridEmailChannel, quota: QuotaTracker): EmailRouter {
        val channels = listOfNotNull(
            resend.takeIf { resend.isEnabled },
            sendGrid.takeIf { sendGrid.isEnabled },
        )
        return EmailRouter(
            channels = channels,
            quota = quota,
            breakers = channels.associate { it.name to CircuitBreaker() },
        )
    }

    @Single(binds = [SmsChannel::class])
    fun smsRouter(kavenegar: KavenegarSmsChannel, twilio: TwilioSmsChannel, quota: QuotaTracker): SmsRouter {
        val channels = listOfNotNull(
            kavenegar.takeIf { kavenegar.isEnabled },
            twilio.takeIf { twilio.isEnabled },
        )
        return SmsRouter(
            channels = channels,
            quota = quota,
            breakers = channels.associate { it.name to CircuitBreaker() },
        )
    }
}
