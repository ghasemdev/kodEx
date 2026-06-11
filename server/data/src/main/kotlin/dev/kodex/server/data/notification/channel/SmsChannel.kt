package dev.kodex.server.data.notification.channel

interface SmsChannel {
    val name: String
    val dailyQuota: Int
    suspend fun send(to: String, message: String): Result<Unit>
}
