package dev.kodex.server.data.repository

import dev.kodex.server.data.db.tables.WebAuthnCredentialsTable
import dev.kodex.server.domain.passkey.repository.PasskeyRecord
import dev.kodex.server.domain.passkey.repository.PasskeyRepository
import kotlin.time.Clock
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update

class PasskeyRepositoryImpl : PasskeyRepository {
    override suspend fun create(
        userId: Long,
        credentialId: ByteArray,
        publicKeyCose: ByteArray,
        signCount: Long,
        aaguid: String?,
        friendlyName: String?,
    ): PasskeyRecord = suspendTransaction {
        val id = WebAuthnCredentialsTable.insertAndGetId {
            it[WebAuthnCredentialsTable.userId] = userId
            it[WebAuthnCredentialsTable.credentialId] = credentialId
            it[WebAuthnCredentialsTable.publicKeyCose] = publicKeyCose
            it[WebAuthnCredentialsTable.signCount] = signCount
            it[WebAuthnCredentialsTable.aaguid] = aaguid
            it[WebAuthnCredentialsTable.friendlyName] = friendlyName
            it[createdAt] = Clock.System.now()
        }
        WebAuthnCredentialsTable.selectAll().where { WebAuthnCredentialsTable.id eq id }.first().toRecord()
    }

    override suspend fun findByCredentialId(credentialId: ByteArray): PasskeyRecord? = suspendTransaction {
        WebAuthnCredentialsTable.selectAll()
            .where { WebAuthnCredentialsTable.credentialId eq credentialId }
            .firstOrNull()
            ?.toRecord()
    }

    override suspend fun findByUserId(userId: Long): List<PasskeyRecord> = suspendTransaction {
        WebAuthnCredentialsTable.selectAll()
            .where { WebAuthnCredentialsTable.userId eq userId }
            .map { it.toRecord() }
    }

    override suspend fun updateSignCount(id: Long, signCount: Long) {
        suspendTransaction {
            WebAuthnCredentialsTable.update({ WebAuthnCredentialsTable.id eq id }) {
                it[WebAuthnCredentialsTable.signCount] = signCount
            }
        }
    }

    override suspend fun delete(id: Long, userId: Long) {
        suspendTransaction {
            WebAuthnCredentialsTable.deleteWhere {
                (WebAuthnCredentialsTable.id eq id) and (WebAuthnCredentialsTable.userId eq userId)
            }
        }
    }

    override suspend fun countForUser(userId: Long): Int = suspendTransaction {
        WebAuthnCredentialsTable.selectAll()
            .where { WebAuthnCredentialsTable.userId eq userId }
            .count()
            .toInt()
    }

    private fun ResultRow.toRecord() = PasskeyRecord(
        id = this[WebAuthnCredentialsTable.id].value,
        userId = this[WebAuthnCredentialsTable.userId].value,
        credentialId = this[WebAuthnCredentialsTable.credentialId],
        publicKeyCose = this[WebAuthnCredentialsTable.publicKeyCose],
        signCount = this[WebAuthnCredentialsTable.signCount],
        aaguid = this[WebAuthnCredentialsTable.aaguid],
        friendlyName = this[WebAuthnCredentialsTable.friendlyName],
        createdAt = this[WebAuthnCredentialsTable.createdAt],
    )
}
