package dev.kodex.server.data.db.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

object TotpConfigsTable : LongIdTable("totp_configs") {
    val userId = reference("user_id", UsersTable).uniqueIndex()
    val secretEncrypted = text("secret_encrypted")
    val enabled = bool("enabled").default(false)
    val backupCodesHashes = text("backup_codes_hashes").default("[]")
    val updatedAt = timestamp("updated_at")
}
