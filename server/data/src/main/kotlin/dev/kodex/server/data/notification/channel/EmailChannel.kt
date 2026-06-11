package dev.kodex.server.data.notification.channel

interface EmailChannel {
    val name: String
    val dailyQuota: Int
    suspend fun send(to: String, subject: String, html: String): Result<Unit>
}
