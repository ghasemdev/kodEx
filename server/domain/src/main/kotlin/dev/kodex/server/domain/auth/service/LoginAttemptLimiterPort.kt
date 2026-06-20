package dev.kodex.server.domain.auth.service

interface LoginAttemptLimiterPort {
    suspend fun currentFailureCount(ipHash: String): Long
    suspend fun recordFailure(ipHash: String): Long
    suspend fun reset(ipHash: String)
}
