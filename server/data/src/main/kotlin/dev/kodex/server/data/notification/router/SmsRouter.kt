package dev.kodex.server.data.notification.router

import dev.kodex.server.data.notification.channel.SmsChannel
import dev.kodex.server.data.notification.infrastructure.CircuitBreaker
import dev.kodex.server.data.notification.infrastructure.QuotaTracker

class SmsRouter(
    private val channels: List<SmsChannel>,
    private val quota: QuotaTracker,
    private val breakers: Map<String, CircuitBreaker>,
) : SmsChannel {
    override val name: String = "SmsRouter"
    override val dailyQuota: Int = -1

    override suspend fun send(to: String, message: String): Result<Unit> {
        val available = channels
            .filter { breakers[it.name]?.isAvailable() != false }
            .filter { quota.remainingCapacitySms(it) > 0 }
            .sortedByDescending { quota.remainingCapacitySms(it) }

        for (channel in available) {
            val result = channel.send(to, message)
            if (result.isSuccess) {
                quota.increment(channel.name)
                breakers[channel.name]?.recordSuccess()
                return result
            } else {
                breakers[channel.name]?.recordFailure()
            }
        }

        return Result.failure(NoChannelAvailableException("All SMS channels exhausted or broken"))
    }
}
