package dev.kodex.server.data.db.tables

import dev.kodex.server.domain.auth.model.DeliveryMode
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

object EmailVerificationTokensTable : LongIdTable("email_verification_tokens") {
    val userId = reference("user_id", UsersTable)
    val tokenHash = char("token_hash", 64)
    val deliveryMode = enumerationByName<DeliveryMode>("delivery_mode", 20)
    val expiresAt = timestamp("expires_at")
    val usedAt = timestamp("used_at").nullable()
}
