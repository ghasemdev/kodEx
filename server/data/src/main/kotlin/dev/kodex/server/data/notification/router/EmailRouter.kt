package dev.kodex.server.data.notification.router

import dev.kodex.server.data.notification.channel.EmailChannel
import dev.kodex.server.data.notification.infrastructure.CircuitBreaker
import dev.kodex.server.data.notification.infrastructure.QuotaTracker

class NoChannelAvailableException(message: String) : Exception(message)

class EmailRouter(
    private val channels: List<EmailChannel>,
    private val quota: QuotaTracker,
    private val breakers: Map<String, CircuitBreaker>,
) : EmailChannel {
    override val name: String = "EmailRouter"
    override val dailyQuota: Int = -1

    override suspend fun send(to: String, subject: String, html: String): Result<Unit> {
        val available = channels
            .filter { breakers[it.name]?.isAvailable() != false }
            .filter { quota.remainingCapacity(it) > 0 }
            .sortedByDescending { quota.remainingCapacity(it) }

        for (channel in available) {
            val result = channel.send(to, subject, html)
            if (result.isSuccess) {
                quota.increment(channel.name)
                breakers[channel.name]?.recordSuccess()
                return result
            } else {
                breakers[channel.name]?.recordFailure()
            }
        }

        return Result.failure(NoChannelAvailableException("All email channels exhausted or broken"))
    }
}
