package dev.kodex.server.data.notification.infrastructure

import dev.kodex.server.data.notification.channel.EmailChannel
import dev.kodex.server.data.notification.channel.SmsChannel
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.atomicfu.AtomicLong
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class QuotaTracker {
    private val counts = ConcurrentHashMap<String, AtomicLong>()

    init {
        CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                val now = System.currentTimeMillis()
                val midnight = nextMidnightMillis(now)
                delay((midnight - now).milliseconds)
                counts.values.forEach { it.value = 0L }
            }
        }
    }

    fun usedToday(name: String): Long = counts[name]?.value ?: 0L

    fun increment(channelName: String) {
        counts.getOrPut(channelName) { atomic(0L) }.incrementAndGet()
    }

    fun remainingCapacity(channel: EmailChannel): Long {
        if (channel.dailyQuota < 0) return Long.MAX_VALUE
        return maxOf(0L, channel.dailyQuota - usedToday(channel.name))
    }

    fun remainingCapacitySms(channel: SmsChannel): Long {
        if (channel.dailyQuota < 0) return Long.MAX_VALUE
        return maxOf(0L, channel.dailyQuota - usedToday(channel.name))
    }

    private fun nextMidnightMillis(now: Long): Long {
        val oneDayMs = 24.hours.inWholeMilliseconds
        return (now / oneDayMs + 1) * oneDayMs
    }
}
