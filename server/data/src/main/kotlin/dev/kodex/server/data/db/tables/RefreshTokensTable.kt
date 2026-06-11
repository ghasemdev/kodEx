package dev.kodex.server.data.db.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

object RefreshTokensTable : LongIdTable("refresh_tokens") {
    val userId = reference("user_id", UsersTable)
    val tokenHash = char("token_hash", 64).uniqueIndex()
    val issuedAt = timestamp("issued_at")
    val expiresAt = timestamp("expires_at")
    val revokedAt = timestamp("revoked_at").nullable()
    val deviceHint = varchar("device_hint", 255).nullable()
    val ipAddress = varchar("ip_address", 45).nullable()
}
