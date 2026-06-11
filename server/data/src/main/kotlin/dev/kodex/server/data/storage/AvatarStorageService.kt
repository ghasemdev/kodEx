package dev.kodex.server.data.storage

import io.minio.MinioClient
import io.minio.PutObjectArgs
import java.io.ByteArrayInputStream

private const val MAX_SIZE_BYTES = 5 * 1024 * 1024

private val MAGIC_BYTES = mapOf(
    "jpg" to byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()),
    "png" to byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte()),
    "webp" to byteArrayOf(0x52.toByte(), 0x49.toByte(), 0x46.toByte(), 0x46.toByte()),
)

class AvatarStorageService(
    endpoint: String,
    accessKey: String,
    secretKey: String,
    private val bucket: String,
    private val publicUrl: String,
) {
    private val minio: MinioClient = MinioClient.builder()
        .endpoint(endpoint)
        .credentials(accessKey, secretKey)
        .build()

    fun upload(userId: Long, bytes: ByteArray, mimeType: String): String {
        require(bytes.size <= MAX_SIZE_BYTES) { "Avatar exceeds 5 MB limit" }
        val ext = detectExtension(bytes) ?: error("Unsupported image format")
        val objectName = "$userId.$ext"
        minio.putObject(
            PutObjectArgs.builder()
                .bucket(bucket)
                .`object`(objectName)
                .stream(ByteArrayInputStream(bytes), bytes.size.toLong(), -1)
                .contentType(mimeType)
                .build(),
        )
        return "$publicUrl/$bucket/$objectName"
    }

    private fun detectExtension(bytes: ByteArray): String? {
        for ((ext, magic) in MAGIC_BYTES) {
            if (bytes.size >= magic.size && bytes.take(magic.size).toByteArray().contentEquals(magic)) {
                return ext
            }
        }
        return null
    }
}
