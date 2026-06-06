package dev.kodex.webapp.pages.landing.model

// ---------------------------------------------------------------------------
// Tier scoring formula (implement in backend/scoring module when ready):
//
//   score = Σ(problem_base_pts × difficulty_multiplier) + Σ(contest_placement_bonus)
//
//   difficulty_multiplier : Easy=1×  Medium=2.5×  Hard=5×
//   contest_placement_bonus: 1st=500  2nd=300  3rd=150  top-10=50
//
//   Tier thresholds:
//     Junior      : score <  1_000
//     Senior      : 1_000 ≤ score <  5_000
//     Master      : 5_000 ≤ score < 15_000
//     Grandmaster : score ≥ 15_000
// ---------------------------------------------------------------------------

import dev.kodex.core.models.DifficultyTier
import dev.kodex.core.models.ExamType
import dev.kodex.core.models.Tier
import dev.kodex.core.models.computeDifficulty

data class LeaderboardEntry(
    val rank: Int,
    val username: String,
    val avatarUrl: String?,
    val tier: Tier,
    val score: Int,
    val solved: Int,
    val streakDays: Int,
)

object PlaceholderLeaderboard {
    @Suppress("PropertyName", "RedundantSuppression")
    val entries: List<LeaderboardEntry> = listOf(
        LeaderboardEntry(1, "kotlin_wizard", null, Tier.GRANDMASTER, 9840, 312, 67),
        LeaderboardEntry(2, "android_pro", null, Tier.MASTER, 8720, 287, 45),
        LeaderboardEntry(3, "coder_x", null, Tier.SENIOR, 7410, 241, 30),
    )
}

data class ProblemSummary(
    val id: String,
    val title: String,
    val type: ExamType,
    val attemptCount: Int,
    val successCount: Int,
) {
    val difficulty: DifficultyTier
        get() = computeDifficulty(attemptCount, successCount)
}

object PlaceholderProblems {
    val entries: List<ProblemSummary> = listOf(
        ProblemSummary("p001", "Reverse a String", ExamType.IO, 1200, 840),
        ProblemSummary("p002", "FizzBuzz with Lambdas", ExamType.INJECTION, 980, 410),
        ProblemSummary("p003", "Binary Search Tree", ExamType.IO, 760, 180),
        ProblemSummary("p004", "Coroutines Quiz", ExamType.QUIZ, 650, 390),
        ProblemSummary("p005", "Android ViewModel Lifecycle", ExamType.QUIZ, 890, 534),
    )
}
