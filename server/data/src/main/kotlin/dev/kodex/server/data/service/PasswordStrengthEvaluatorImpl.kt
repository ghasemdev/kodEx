package dev.kodex.server.data.service

import com.nulabinc.zxcvbn.Zxcvbn
import dev.kodex.server.domain.auth.service.PasswordStrengthEvaluator
import org.koin.core.annotation.Single

private const val MIN_SCORE = 2

@Single(binds = [PasswordStrengthEvaluator::class])
class PasswordStrengthEvaluatorImpl : PasswordStrengthEvaluator {
    private val zxcvbn = Zxcvbn()

    override fun meetsMinimumStrength(password: String): Boolean = zxcvbn.measure(password).score >= MIN_SCORE
}
