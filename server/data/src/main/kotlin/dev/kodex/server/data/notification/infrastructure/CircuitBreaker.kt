package dev.kodex.server.data.notification.infrastructure

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.atomicfu.atomic

enum class BreakerState { CLOSED, OPEN, HALF_OPEN }

class CircuitBreaker(
    private val failureThreshold: Int = 3,
    private val resetAfter: Duration = 60.seconds,
) {
    private val failureCount = atomic(0)
    private val openedAt = atomic(0L)

    @Volatile
    private var state: BreakerState = BreakerState.CLOSED

    fun isAvailable(): Boolean {
        if (state == BreakerState.CLOSED) return true
        if (state == BreakerState.OPEN) {
            val elapsed = System.currentTimeMillis() - openedAt.value
            if (elapsed >= resetAfter.inWholeMilliseconds) {
                state = BreakerState.HALF_OPEN
                return true
            }
            return false
        }
        return true
    }

    fun recordSuccess() {
        failureCount.value = 0
        state = BreakerState.CLOSED
    }

    fun recordFailure() {
        val count = failureCount.incrementAndGet()
        if (count >= failureThreshold) {
            openedAt.value = System.currentTimeMillis()
            state = BreakerState.OPEN
        }
    }
}
