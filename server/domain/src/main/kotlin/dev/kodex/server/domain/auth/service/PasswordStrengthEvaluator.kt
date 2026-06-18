package dev.kodex.server.domain.auth.service

fun interface PasswordStrengthEvaluator {
    fun meetsMinimumStrength(password: String): Boolean
}
