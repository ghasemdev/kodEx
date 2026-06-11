package dev.kodex.server.data.crypto

import java.security.SecureRandom
import kotlin.io.encoding.Base64
import org.koin.core.annotation.Single

@Single
class WebAuthnChallengeCrypto(private val secureRandom: SecureRandom) {
    private val urlSafeNoPad = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

    fun generateAndSign(challengeBytes: ByteArray, keyBase64: String): String =
        urlSafeNoPad.encode(aesGcmEncrypt(challengeBytes, keyBase64, secureRandom))

    fun verifyAndExtract(cookieValue: String, keyBase64: String): ByteArray =
        aesGcmDecrypt(urlSafeNoPad.decode(cookieValue), keyBase64)
}
