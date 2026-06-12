package dev.kodex.server.data.ratelimit

import dev.kodex.core.config.ConfigQualifier
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import kotlinx.coroutines.future.await
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
class RateLimitService(
    @Named(ConfigQualifier.Redis.URL) redisUrl: String,
    @Named(ConfigQualifier.Redis.PASSWORD) redisPassword: String,
) {
    private val client: RedisClient = RedisClient.create(buildUri(redisUrl, redisPassword.ifEmpty { null }))
    private val connection: StatefulRedisConnection<String, String> = client.connect()
    private val commands: RedisAsyncCommands<String, String> = connection.async()

    suspend fun incrementAndGet(key: String, ttlSeconds: Long): Long {
        val count = commands.incr(key).await()
        if (count == 1L) {
            commands.expire(key, ttlSeconds).await()
        }
        return count
    }

    suspend fun get(key: String): Long = commands.get(key).await()?.toLongOrNull() ?: 0L

    suspend fun reset(key: String) {
        commands.del(key).await()
    }

    private fun buildUri(redisUrl: String, password: String?): String {
        if (password.isNullOrBlank()) return redisUrl
        return redisUrl.replace("redis://", "redis://:$password@")
            .replace("redis://:", "redis://:$password@")
    }
}
