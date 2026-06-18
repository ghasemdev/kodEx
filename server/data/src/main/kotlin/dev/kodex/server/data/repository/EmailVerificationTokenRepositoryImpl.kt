package dev.kodex.server.data.repository

import dev.kodex.server.data.db.tables.EmailVerificationTokensTable
import dev.kodex.server.domain.auth.model.DeliveryMode
import dev.kodex.server.domain.auth.model.EmailVerificationTokenRecord
import dev.kodex.server.domain.auth.repository.EmailVerificationTokenRepository
import kotlin.time.Clock
import kotlin.time.Instant
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.core.annotation.Single

@Single(binds = [EmailVerificationTokenRepository::class])
class EmailVerificationTokenRepositoryImpl : EmailVerificationTokenRepository {
    override suspend fun create(
        userId: Long,
        tokenHash: String,
        deliveryMode: DeliveryMode,
        expiresAt: Instant,
    ): EmailVerificationTokenRecord = suspendTransaction {
        val id = EmailVerificationTokensTable.insertAndGetId {
            it[EmailVerificationTokensTable.userId] = userId
            it[EmailVerificationTokensTable.tokenHash] = tokenHash
            it[EmailVerificationTokensTable.deliveryMode] = deliveryMode
            it[EmailVerificationTokensTable.expiresAt] = expiresAt
        }
        EmailVerificationTokensTable.selectAll()
            .where { EmailVerificationTokensTable.id eq id }
            .first()
            .toRecord()
    }

    override suspend fun findActiveByHash(tokenHash: String): EmailVerificationTokenRecord? = suspendTransaction {
        EmailVerificationTokensTable.selectAll()
            .where {
                (EmailVerificationTokensTable.tokenHash eq tokenHash) and
                    EmailVerificationTokensTable.usedAt.isNull()
            }
            .firstOrNull()
            ?.toRecord()
    }

    override suspend fun markUsed(id: Long) {
        suspendTransaction {
            EmailVerificationTokensTable.update({ EmailVerificationTokensTable.id eq id }) {
                it[usedAt] = Clock.System.now()
            }
        }
    }

    override suspend fun invalidateForUser(userId: Long) {
        suspendTransaction {
            EmailVerificationTokensTable.update({
                (EmailVerificationTokensTable.userId eq userId) and
                    EmailVerificationTokensTable.usedAt.isNull()
            }) {
                it[usedAt] = Clock.System.now()
            }
        }
    }

    private fun ResultRow.toRecord() = EmailVerificationTokenRecord(
        id = this[EmailVerificationTokensTable.id].value,
        userId = this[EmailVerificationTokensTable.userId].value,
        tokenHash = this[EmailVerificationTokensTable.tokenHash],
        deliveryMode = this[EmailVerificationTokensTable.deliveryMode],
        expiresAt = this[EmailVerificationTokensTable.expiresAt],
        usedAt = this[EmailVerificationTokensTable.usedAt],
    )
}
