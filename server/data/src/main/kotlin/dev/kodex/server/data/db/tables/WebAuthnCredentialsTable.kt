package dev.kodex.server.data.db.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

object WebAuthnCredentialsTable : LongIdTable("webauthn_credentials") {
    val userId = reference("user_id", UsersTable)
    val credentialId = binary("credential_id", 256).uniqueIndex()
    val publicKeyCose = binary("public_key_cose", 512)
    val signCount = long("sign_count").default(0)
    val aaguid = varchar("aaguid", 36).nullable()
    val friendlyName = varchar("friendly_name", 100).nullable()
    val createdAt = timestamp("created_at")
}
