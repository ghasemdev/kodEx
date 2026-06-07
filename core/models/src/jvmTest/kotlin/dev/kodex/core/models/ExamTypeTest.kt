package dev.kodex.core.models

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ExamTypeTest : FunSpec({
    test("QUIZ displayName is Multiple Choice") {
        ExamType.QUIZ.displayName shouldBe "Multiple Choice"
    }

    test("IO displayName is I/O Test Cases") {
        ExamType.IO.displayName shouldBe "I/O Test Cases"
    }

    test("INJECTION displayName is Injection / Project") {
        ExamType.INJECTION.displayName shouldBe "Injection / Project"
    }

    test("all three exam types exist") {
        ExamType.entries.size shouldBe 3
    }
})
