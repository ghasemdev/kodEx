package dev.kodex.server.data.ratelimit

import dev.kodex.server.domain.auth.service.LoginAttemptLimiterPort
import org.koin.core.annotation.Single

private const val ATTEMPT_TTL_SECONDS = 600L

@Single(binds = [LoginAttemptLimiterPort::class])
class LoginAttemptLimiterPortImpl(private val rateLimitService: RateLimitService) : LoginAttemptLimiterPort {
    override suspend fun currentFailureCount(ipHash: String): Long = rateLimitService.get(key(ipHash))

    override suspend fun recordFailure(ipHash: String): Long =
        rateLimitService.incrementAndGet(key(ipHash), ATTEMPT_TTL_SECONDS)

    override suspend fun reset(ipHash: String) = rateLimitService.reset(key(ipHash))

    private fun key(ipHash: String) = "login:attempts:$ipHash"
}
