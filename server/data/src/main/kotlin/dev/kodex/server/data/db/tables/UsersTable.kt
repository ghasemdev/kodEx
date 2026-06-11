package dev.kodex.server.data.db.tables

import dev.kodex.core.models.auth.Role
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

object UsersTable : LongIdTable("users") {
    val username = varchar("username", 30).uniqueIndex()
    val email = varchar("email", 255).uniqueIndex()
    val emailVerified = bool("email_verified").default(false)
    val passwordHash = varchar("password_hash", 255).nullable()
    val role = enumerationByName<Role>("role", 20).default(Role.PARTICIPANT)
    val failedLoginCount = integer("failed_login_count").default(0)
    val lockedUntil = timestamp("locked_until").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
