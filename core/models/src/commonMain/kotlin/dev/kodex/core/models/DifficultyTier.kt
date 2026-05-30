package dev.kodex.core.models

enum class DifficultyTier(val display: String, val colorClass: String) {
    EASY("Easy", "text-success"),
    MEDIUM("Medium", "text-warning"),
    HARD("Hard", "text-error"),
}

fun computeDifficulty(attemptCount: Int, successCount: Int): DifficultyTier {
    if (attemptCount == 0) return DifficultyTier.MEDIUM
    val rate = successCount.toDouble() / attemptCount
    return when {
        rate >= DIFFICULTY_THRESHOLD_EASY -> DifficultyTier.EASY
        rate >= DIFFICULTY_THRESHOLD_MEDIUM -> DifficultyTier.MEDIUM
        else -> DifficultyTier.HARD
    }
}

private const val DIFFICULTY_THRESHOLD_EASY = 0.60
private const val DIFFICULTY_THRESHOLD_MEDIUM = 0.25
