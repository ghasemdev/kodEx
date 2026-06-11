package dev.kodex.server.data.repository

import dev.kodex.core.models.auth.Role
import dev.kodex.server.data.db.tables.UsersTable
import dev.kodex.server.domain.auth.repository.UserRecord
import dev.kodex.server.domain.auth.repository.UserRepository
import kotlin.time.Clock
import kotlin.time.Instant
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update

private const val USERNAME_MAX_LENGTH = 30
private const val USERNAME_BASE_MAX_LEN = 27

class UserRepositoryImpl : UserRepository {
    override suspend fun findById(id: Long): UserRecord? = suspendTransaction {
        UsersTable.selectAll().where { UsersTable.id eq id }.firstOrNull()?.toRecord()
    }

    override suspend fun findByEmail(email: String): UserRecord? = suspendTransaction {
        UsersTable.selectAll().where { UsersTable.email eq email.lowercase() }.firstOrNull()?.toRecord()
    }

    override suspend fun findByUsername(username: String): UserRecord? = suspendTransaction {
        UsersTable.selectAll().where { UsersTable.username eq username }.firstOrNull()?.toRecord()
    }

    override suspend fun isUsernameTaken(username: String): Boolean = suspendTransaction {
        UsersTable.selectAll().where { UsersTable.username eq username }.count() > 0
    }

    override suspend fun isEmailTaken(email: String): Boolean = suspendTransaction {
        UsersTable.selectAll().where { UsersTable.email eq email.lowercase() }.count() > 0
    }

    override suspend fun create(username: String, email: String, passwordHash: String?, role: Role): UserRecord =
        suspendTransaction {
            val now = Clock.System.now()
            val id = UsersTable.insertAndGetId {
                it[UsersTable.username] = username
                it[UsersTable.email] = email.lowercase()
                it[UsersTable.passwordHash] = passwordHash
                it[UsersTable.role] = role
                it[createdAt] = now
                it[updatedAt] = now
            }
            findById(id.value)!!
        }

    override suspend fun setEmailVerified(userId: Long) {
        suspendTransaction {
            UsersTable.update({ UsersTable.id eq userId }) { it[emailVerified] = true }
        }
    }

    override suspend fun updatePasswordHash(userId: Long, hash: String) {
        suspendTransaction {
            UsersTable.update({ UsersTable.id eq userId }) { it[passwordHash] = hash }
        }
    }

    override suspend fun incrementFailedLoginCount(userId: Long) {
        suspendTransaction {
            UsersTable.update({ UsersTable.id eq userId }) {
                it[failedLoginCount] = failedLoginCount + 1
            }
        }
    }

    override suspend fun resetFailedLoginCount(userId: Long) {
        suspendTransaction {
            UsersTable.update({ UsersTable.id eq userId }) { it[failedLoginCount] = 0 }
        }
    }

    override suspend fun lockUntil(userId: Long, until: Instant) {
        suspendTransaction {
            UsersTable.update({ UsersTable.id eq userId }) { it[lockedUntil] = until }
        }
    }

    override suspend fun updateEmail(userId: Long, newEmail: String) {
        suspendTransaction {
            UsersTable.update({ UsersTable.id eq userId }) { it[email] = newEmail.lowercase() }
        }
    }

    override suspend fun updateUsername(userId: Long, newUsername: String) {
        suspendTransaction {
            UsersTable.update({ UsersTable.id eq userId }) { it[username] = newUsername }
        }
    }

    @Suppress("LabeledExpression")
    override suspend fun generateUsername(emailPrefix: String): String {
        val sanitized = emailPrefix.lowercase().replace(Regex("[^a-z0-9_-]"), "").take(USERNAME_MAX_LENGTH)
        val base = sanitized.ifEmpty { "user" }
        return suspendTransaction {
            if (!isUsernameTaken(base)) return@suspendTransaction base
            var suffix = 1
            while (true) {
                val candidate = "${base.take(USERNAME_BASE_MAX_LEN)}_$suffix"
                if (!isUsernameTaken(candidate)) return@suspendTransaction candidate
                suffix++
            }
            @Suppress("UNREACHABLE_CODE")
            base
        }
    }

    private fun ResultRow.toRecord() = UserRecord(
        id = this[UsersTable.id].value,
        username = this[UsersTable.username],
        email = this[UsersTable.email],
        emailVerified = this[UsersTable.emailVerified],
        passwordHash = this[UsersTable.passwordHash],
        role = this[UsersTable.role],
        failedLoginCount = this[UsersTable.failedLoginCount],
        lockedUntil = this[UsersTable.lockedUntil],
        createdAt = this[UsersTable.createdAt],
        updatedAt = this[UsersTable.updatedAt],
    )
}
