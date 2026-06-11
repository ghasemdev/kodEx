package dev.kodex.server.data.db.tables

import dev.kodex.core.models.auth.OAuthProvider
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

object OAuthIdentitiesTable : LongIdTable("oauth_identities") {
    val userId = reference("user_id", UsersTable)
    val provider = enumerationByName<OAuthProvider>("provider", 20)
    val providerUserId = varchar("provider_user_id", 255)
    val linkedAt = timestamp("linked_at")

    init {
        uniqueIndex(provider, providerUserId)
    }
}
