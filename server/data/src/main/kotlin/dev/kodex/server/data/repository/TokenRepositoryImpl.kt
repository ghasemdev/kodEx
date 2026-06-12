package dev.kodex.server.data.repository

import dev.kodex.server.data.db.tables.RefreshTokensTable
import dev.kodex.server.domain.auth.model.RefreshTokenRecord
import dev.kodex.server.domain.auth.repository.TokenRepository
import kotlin.time.Clock
import kotlin.time.Instant
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.core.annotation.Single

@Single(binds = [TokenRepository::class])
class TokenRepositoryImpl : TokenRepository {
    override suspend fun create(
        userId: Long,
        tokenHash: String,
        expiresAt: Instant,
        deviceHint: String?,
        ipAddress: String?,
    ): RefreshTokenRecord = suspendTransaction {
        val id = RefreshTokensTable.insertAndGetId {
            it[RefreshTokensTable.userId] = userId
            it[RefreshTokensTable.tokenHash] = tokenHash
            it[issuedAt] = Clock.System.now()
            it[RefreshTokensTable.expiresAt] = expiresAt
            it[RefreshTokensTable.deviceHint] = deviceHint
            it[RefreshTokensTable.ipAddress] = ipAddress
        }
        RefreshTokensTable.selectAll().where { RefreshTokensTable.id eq id }.first().toRecord()
    }

    override suspend fun findActiveByHash(tokenHash: String): RefreshTokenRecord? = suspendTransaction {
        val now = Clock.System.now()
        RefreshTokensTable.selectAll()
            .where {
                (RefreshTokensTable.tokenHash eq tokenHash) and
                    RefreshTokensTable.revokedAt.isNull() and
                    (RefreshTokensTable.expiresAt greater now)
            }
            .firstOrNull()
            ?.toRecord()
    }

    override suspend fun revoke(tokenHash: String) {
        suspendTransaction {
            RefreshTokensTable.update({ RefreshTokensTable.tokenHash eq tokenHash }) {
                it[revokedAt] = Clock.System.now()
            }
        }
    }

    override suspend fun revokeAll(userId: Long) {
        suspendTransaction {
            RefreshTokensTable.update({ RefreshTokensTable.userId eq userId }) {
                it[revokedAt] = Clock.System.now()
            }
        }
    }

    override suspend fun revokeAllExcept(userId: Long, exceptHash: String) {
        suspendTransaction {
            RefreshTokensTable.update({
                (RefreshTokensTable.userId eq userId) and
                    (RefreshTokensTable.tokenHash neq exceptHash) and
                    RefreshTokensTable.revokedAt.isNull()
            }) {
                it[revokedAt] = Clock.System.now()
            }
        }
    }

    override suspend fun rotate(
        oldHash: String,
        newHash: String,
        newExpiresAt: Instant,
        deviceHint: String?,
        ipAddress: String?,
    ): RefreshTokenRecord = suspendTransaction {
        val existing = findActiveByHash(oldHash) ?: error("Refresh token not found or already revoked")
        revoke(oldHash)
        create(existing.userId, newHash, newExpiresAt, deviceHint, ipAddress)
    }

    override suspend fun findActiveByUserId(userId: Long): List<RefreshTokenRecord> = suspendTransaction {
        val now = Clock.System.now()
        RefreshTokensTable.selectAll()
            .where {
                (RefreshTokensTable.userId eq userId) and
                    RefreshTokensTable.revokedAt.isNull() and
                    (RefreshTokensTable.expiresAt greater now)
            }
            .orderBy(RefreshTokensTable.issuedAt, SortOrder.DESC)
            .map { it.toRecord() }
    }

    override suspend fun revokeById(id: Long, userId: Long) {
        suspendTransaction {
            RefreshTokensTable.update({
                (RefreshTokensTable.id eq id) and (RefreshTokensTable.userId eq userId)
            }) {
                it[revokedAt] = Clock.System.now()
            }
        }
    }

    private fun ResultRow.toRecord() = RefreshTokenRecord(
        id = this[RefreshTokensTable.id].value,
        userId = this[RefreshTokensTable.userId].value,
        tokenHash = this[RefreshTokensTable.tokenHash],
        issuedAt = this[RefreshTokensTable.issuedAt],
        expiresAt = this[RefreshTokensTable.expiresAt],
        revokedAt = this[RefreshTokensTable.revokedAt],
        deviceHint = this[RefreshTokensTable.deviceHint],
        ipAddress = this[RefreshTokensTable.ipAddress],
    )
}
