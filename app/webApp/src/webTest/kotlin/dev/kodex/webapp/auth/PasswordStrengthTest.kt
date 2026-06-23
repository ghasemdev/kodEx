package dev.kodex.webapp.auth

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PasswordStrengthTest : FunSpec({
    test("blank password scores 0") {
        PasswordStrength.score("") shouldBe 0
    }

    test("common weak password scores low") {
        (PasswordStrength.score("password123") <= 1) shouldBe true
    }

    test("long random password scores high") {
        (PasswordStrength.score("Tr0ub4dor&3xQz9!mK") >= 3) shouldBe true
    }
})
