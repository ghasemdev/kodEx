package dev.kodex.webapp.landing

import dev.kodex.core.models.DifficultyTier
import dev.kodex.core.models.ExamType
import dev.kodex.core.models.Tier
import dev.kodex.webapp.pages.landing.model.PlaceholderLeaderboard
import dev.kodex.webapp.pages.landing.model.PlaceholderProblems
import dev.kodex.webapp.pages.landing.model.ProblemSummary
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank

class PlaceholderDataTest : FunSpec({
    context("PlaceholderLeaderboard") {
        test("has exactly 3 entries") {
            PlaceholderLeaderboard.entries shouldHaveSize 3
        }

        test("entries have sequential ranks starting at 1") {
            PlaceholderLeaderboard.entries.forEachIndexed { idx, entry ->
                entry.rank shouldBe idx + 1
            }
        }

        test("first entry has GRANDMASTER tier") {
            PlaceholderLeaderboard.entries.first().tier shouldBe Tier.GRANDMASTER
        }

        test("second entry has MASTER tier") {
            PlaceholderLeaderboard.entries[1].tier shouldBe Tier.MASTER
        }

        test("third entry has SENIOR tier") {
            PlaceholderLeaderboard.entries[2].tier shouldBe Tier.SENIOR
        }

        test("all entries have non-blank usernames") {
            PlaceholderLeaderboard.entries.forEach { entry ->
                entry.username.shouldNotBeBlank()
            }
        }

        test("all entries have positive scores") {
            PlaceholderLeaderboard.entries.forEach { entry ->
                (entry.score > 0) shouldBe true
            }
        }
    }

    context("PlaceholderProblems") {
        test("has exactly 5 entries") {
            PlaceholderProblems.entries shouldHaveSize 5
        }

        test("all entries have non-blank titles") {
            PlaceholderProblems.entries.forEach { problem ->
                problem.title.shouldNotBeBlank()
            }
        }

        test("all entries have non-blank IDs") {
            PlaceholderProblems.entries.forEach { problem ->
                problem.id.shouldNotBeBlank()
            }
        }

        test("first problem has IO type") {
            PlaceholderProblems.entries.first().type shouldBe ExamType.IO
        }

        test("second problem has INJECTION type") {
            PlaceholderProblems.entries[1].type shouldBe ExamType.INJECTION
        }

        test("fourth problem has QUIZ type") {
            PlaceholderProblems.entries[3].type shouldBe ExamType.QUIZ
        }
    }

    context("ProblemSummary difficulty computation") {
        test("high success rate yields EASY") {
            val p = ProblemSummary("x", "T", ExamType.IO, attemptCount = 100, successCount = 70)
            p.difficulty shouldBe DifficultyTier.EASY
        }

        test("medium success rate yields MEDIUM") {
            val p = ProblemSummary("x", "T", ExamType.IO, attemptCount = 100, successCount = 40)
            p.difficulty shouldBe DifficultyTier.MEDIUM
        }

        test("low success rate yields HARD") {
            val p = ProblemSummary("x", "T", ExamType.IO, attemptCount = 100, successCount = 10)
            p.difficulty shouldBe DifficultyTier.HARD
        }

        test("zero attempts yields MEDIUM (default)") {
            val p = ProblemSummary("x", "T", ExamType.QUIZ, attemptCount = 0, successCount = 0)
            p.difficulty shouldBe DifficultyTier.MEDIUM
        }

        test("100% success rate yields EASY") {
            val p = ProblemSummary("x", "T", ExamType.INJECTION, attemptCount = 50, successCount = 50)
            p.difficulty shouldBe DifficultyTier.EASY
        }

        test("first placeholder problem has EASY difficulty (84/100 = 70%)") {
            PlaceholderProblems.entries.first().difficulty shouldBe DifficultyTier.EASY
        }

        test("third placeholder problem has HARD difficulty (180/760 = 24%)") {
            PlaceholderProblems.entries[2].difficulty shouldBe DifficultyTier.HARD
        }
    }
})
