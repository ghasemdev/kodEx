package dev.kodex.server.data.db.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

object KnownLoginIpsTable : LongIdTable("known_login_ips") {
    val userId = reference("user_id", UsersTable)
    val ipHash = char("ip_hash", 64)
    val countryCode = char("country_code", 2).nullable()
    val city = varchar("city", 100).nullable()
    val firstSeenAt = timestamp("first_seen_at")
    val lastSeenAt = timestamp("last_seen_at")

    init {
        uniqueIndex(userId, ipHash)
    }
}
