package dev.kodex.core.models

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DifficultyTierTest : FunSpec({
    test("zero attempts returns MEDIUM") {
        computeDifficulty(attemptCount = 0, successCount = 0) shouldBe DifficultyTier.MEDIUM
    }

    test("100% success rate returns EASY") {
        computeDifficulty(attemptCount = 10, successCount = 10) shouldBe DifficultyTier.EASY
    }

    test("exactly 60% success rate returns EASY") {
        computeDifficulty(attemptCount = 10, successCount = 6) shouldBe DifficultyTier.EASY
    }

    test("59% success rate returns MEDIUM") {
        computeDifficulty(attemptCount = 100, successCount = 59) shouldBe DifficultyTier.MEDIUM
    }

    test("exactly 25% success rate returns MEDIUM") {
        computeDifficulty(attemptCount = 100, successCount = 25) shouldBe DifficultyTier.MEDIUM
    }

    test("24% success rate returns HARD") {
        computeDifficulty(attemptCount = 100, successCount = 24) shouldBe DifficultyTier.HARD
    }

    test("0% success rate returns HARD") {
        computeDifficulty(attemptCount = 10, successCount = 0) shouldBe DifficultyTier.HARD
    }

    test("DifficultyTier display names are correct") {
        DifficultyTier.EASY.display shouldBe "Easy"
        DifficultyTier.MEDIUM.display shouldBe "Medium"
        DifficultyTier.HARD.display shouldBe "Hard"
    }

    test("DifficultyTier colorClass values are correct") {
        DifficultyTier.EASY.colorClass shouldBe "text-success"
        DifficultyTier.MEDIUM.colorClass shouldBe "text-warning"
        DifficultyTier.HARD.colorClass shouldBe "text-error"
    }
})
