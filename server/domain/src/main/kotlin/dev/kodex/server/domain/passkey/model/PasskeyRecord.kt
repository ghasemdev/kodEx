package dev.kodex.server.domain.passkey.model

import kotlin.time.Instant

data class PasskeyRecord(
    val id: Long,
    val userId: Long,
    val credentialId: ByteArray,
    val publicKeyCose: ByteArray,
    val signCount: Long,
    val aaguid: String?,
    val friendlyName: String?,
    val createdAt: Instant,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PasskeyRecord

        if (id != other.id) return false
        if (userId != other.userId) return false
        if (signCount != other.signCount) return false
        if (!credentialId.contentEquals(other.credentialId)) return false
        if (!publicKeyCose.contentEquals(other.publicKeyCose)) return false
        if (aaguid != other.aaguid) return false
        if (friendlyName != other.friendlyName) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + userId.hashCode()
        result = 31 * result + signCount.hashCode()
        result = 31 * result + credentialId.contentHashCode()
        result = 31 * result + publicKeyCose.contentHashCode()
        result = 31 * result + (aaguid?.hashCode() ?: 0)
        result = 31 * result + (friendlyName?.hashCode() ?: 0)
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
