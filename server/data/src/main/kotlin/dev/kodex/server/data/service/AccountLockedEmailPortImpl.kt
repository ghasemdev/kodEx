package dev.kodex.server.data.service

import dev.kodex.server.data.notification.channel.EmailChannel
import dev.kodex.server.data.notification.templates.AccountLockedEmailTemplate
import dev.kodex.server.domain.auth.service.AccountLockedEmailPort
import kotlin.time.Instant
import org.koin.core.annotation.Single

@Single(binds = [AccountLockedEmailPort::class])
class AccountLockedEmailPortImpl(private val emailChannel: EmailChannel) : AccountLockedEmailPort {
    override suspend fun sendAccountLocked(to: String, lockedUntil: Instant) {
        emailChannel.send(
            to = to,
            subject = "Your KodEx account has been temporarily locked",
            html = AccountLockedEmailTemplate.render(lockedUntil.toString()),
        )
    }
}
