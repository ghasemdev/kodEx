package dev.kodex.server.data.db.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

object PasswordResetTokensTable : LongIdTable("password_reset_tokens") {
    val userId = reference("user_id", UsersTable)
    val tokenHash = char("token_hash", 64).uniqueIndex()
    val expiresAt = timestamp("expires_at")
    val usedAt = timestamp("used_at").nullable()
}
