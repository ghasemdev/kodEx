package dev.kodex.server.domain.auth.service

interface VerificationEmailPort {
    suspend fun sendMagicLink(to: String, token: String)
    suspend fun sendOtp(to: String, userId: Long, otp: String)
}
