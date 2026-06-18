package dev.kodex.server.data.service

import dev.kodex.core.config.ConfigQualifier
import dev.kodex.server.data.notification.channel.EmailChannel
import dev.kodex.server.data.notification.templates.VerificationEmailTemplates
import dev.kodex.server.domain.auth.service.VerificationEmailPort
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single(binds = [VerificationEmailPort::class])
class VerificationEmailPortImpl(
    private val emailChannel: EmailChannel,
    @Named(ConfigQualifier.App.BASE_URL) private val appBaseUrl: String,
) : VerificationEmailPort {
    override suspend fun sendMagicLink(to: String, token: String) {
        val link = "$appBaseUrl/auth/verify-email?token=$token"
        emailChannel.send(
            to = to,
            subject = "Verify your KodEx account",
            html = VerificationEmailTemplates.magicLink(link),
        )
    }

    override suspend fun sendOtp(to: String, userId: Long, otp: String) {
        emailChannel.send(
            to = to,
            subject = "Your KodEx verification code: $otp",
            html = VerificationEmailTemplates.otp(otp),
        )
    }
}
