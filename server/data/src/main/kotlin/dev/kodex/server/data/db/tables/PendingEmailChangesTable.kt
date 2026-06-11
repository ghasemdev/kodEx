package dev.kodex.server.data.db.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

object PendingEmailChangesTable : LongIdTable("pending_email_changes") {
    val userId = reference("user_id", UsersTable).uniqueIndex()
    val newEmail = varchar("new_email", 255)
    val tokenHash = char("token_hash", 64)
    val requestedAt = timestamp("requested_at")
    val expiresAt = timestamp("expires_at")
    val usedAt = timestamp("used_at").nullable()
}
