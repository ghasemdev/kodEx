# Data Model: Landing Page (003)

**Date**: 2026-05-29 | **Branch**: `feature/003-landing-page`

> Landing page is read-only. No new database tables required.
> All entities below are either API response models (KMP shared) or frontend-only view models.

---

## 1. LandingStatsResponse *(KMP shared — `core:models`)*

Returned by `GET /api/v1/stats/landing`.

| Field | Type | Description |
|-------|------|-------------|
| `totalProblems` | `Int` | Total problems in the catalogue |
| `totalUsers` | `Int` | Total registered users |
| `totalContests` | `Int` | Total contests ever held |

```kotlin
@Serializable
data class LandingStatsResponse(
    val totalProblems: Int,
    val totalUsers: Int,
    val totalContests: Int,
)
```

**Notes**: v1 returns hardcoded values. Real DB aggregation added in spec 005/006.

---

## 2. LandingStats *(frontend view model — `webapp`)*

Frontend-local copy of `LandingStatsResponse`. Keeps webapp decoupled from core models.

| Field | Type | Description |
|-------|------|-------------|
| `totalProblems` | `Int` | Mirrored from `LandingStatsResponse` |
| `totalUsers` | `Int` | Mirrored |
| `totalContests` | `Int` | Mirrored |

---

## 3. BadgeDefinition *(frontend-only static catalogue)*

One entry per badge. Stored as a static `object Badges` list — not from API.

| Field | Type | Description |
|-------|------|-------------|
| `id` | `String` | Stable kebab-case identifier (e.g., `"kotlin-ninja"`) |
| `name` | `String` | Display name (e.g., `"Kotlin Ninja"`) |
| `iconPath` | `String` | Resource-relative path to custom SVG (e.g., `"icons/badges/kotlin-ninja.svg"`) |
| `description` | `String` | One-line description shown on badge card front |
| `unlockCondition` | `String` | Condition shown on badge card flip (hover reveal) |

**No FontAwesome** — icons are custom-designed SVGs stored in
`app/webApp/src/webMain/resources/icons/badges/`. Designed in Figma, optimised with
SVGOMG. See plan.md §Custom Badge & Cup Icons for the full asset workflow.

**Validation** (in `init` block, per §A4 security constraint):
- `id`: matches `^[a-z0-9-]+$`
- `iconPath`: matches `^icons/[a-z0-9/.-]+\.svg$` (no path traversal, SVG only)

---

## 4. LeaderboardEntry *(frontend placeholder — replaced by spec 010)*

Minimal data needed for the 3-row leaderboard preview on the landing page.

| Field | Type | Description |
|-------|------|-------------|
| `rank` | `Int` | Position (1-based) |
| `username` | `String` | Display username |
| `avatarUrl` | `String?` | Profile picture URL; `null` → render initials fallback |
| `tier` | `Tier` | One of `JUNIOR`, `SENIOR`, `MASTER`, `GRANDMASTER` |
| `score` | `Int` | Cumulative score |
| `solved` | `Int` | Total problems solved |
| `streakDays` | `Int` | Current consecutive-day streak |

---

## 5. Tier *(enum — `core:models`)*

Shared across landing, leaderboard, and profile specs.

| Value | Display Name | Colour Token |
|-------|-------------|--------------|
| `JUNIOR` | Junior Coder | `--color-bronze` |
| `SENIOR` | Senior Coder | `--color-silver` |
| `MASTER` | Master | `--color-gold` |
| `GRANDMASTER` | Grandmaster | `--color-diamond` |

---

## 6. ProblemSummary *(frontend placeholder — replaced by spec 005)*

Minimal data needed for the 5-row problems sample on the landing page.

| Field | Type | Description |
|-------|------|-------------|
| `id` | `String` | Problem identifier (e.g., `"001"`) |
| `title` | `String` | Display title |
| `type` | `ExamType` | `QUIZ`, `IO`, or `INJECTION` |
| `attemptCount` | `Int` | Total unique participants who attempted this problem |
| `successCount` | `Int` | Total who solved it successfully |
| `difficulty` | `DifficultyTier` | Computed property — see §8 |

---

## 7. ExamType *(enum — `core:models`)*

| Value | Display Name | Description |
|-------|-------------|-------------|
| `QUIZ` | Multiple Choice | Single or multi-select options |
| `IO` | I/O Test Cases | Code judged by input→output pairs |
| `INJECTION` | Injection / Project | Hidden test cases injected at grade time; covers both Kotlin snippets and Android project templates |

---

## 8. DifficultyTier *(computed — `core:models`)*

Difficulty is **not stored** — it is computed dynamically from solve statistics.
A problem's difficulty reflects real-world data: how many people tried it and how many
succeeded. Static labels assigned by the author are replaced by this formula.

```kotlin
// core:models
enum class DifficultyTier(val display: String, val colorClass: String) {
    EASY  ("Easy",   "text-success"),
    MEDIUM("Medium", "text-warning"),
    HARD  ("Hard",   "text-error"),
}

fun computeDifficulty(attemptCount: Int, successCount: Int): DifficultyTier {
    if (attemptCount == 0) return DifficultyTier.MEDIUM   // no data yet → neutral
    val solveRate = successCount.toDouble() / attemptCount
    return when {
        solveRate >= 0.60 -> DifficultyTier.EASY    // ≥60% of attempts succeed
        solveRate >= 0.25 -> DifficultyTier.MEDIUM  // 25–59%
        else              -> DifficultyTier.HARD    // <25%
    }
}
```

`ProblemSummary.difficulty` is a computed val calling `computeDifficulty(attemptCount, successCount)`.
No migration needed — existing static difficulty labels can be seeded as `attemptCount=0`.

---

## 9. SessionState *(sealed class — `app:shared`)*

Used by `GlobalNavBar` to render guest vs authenticated state.

```
SessionState
├── Guest                        — not logged in
└── Authenticated
    ├── userId: String
    ├── username: String
    ├── avatarUrl: String?       — profile picture URL; null → render initials circle
    ├── plan: Plan               — FREE | PRO
    └── roles: Set<UserRole>     — PARTICIPANT | EXAM_CREATOR | ADMIN
```

**Avatar fallback**: when `avatarUrl` is `null` or fails to load, render a coloured
circle with the first letter of `username` in white. Colour is derived from a stable
hash of `userId` so the same user always gets the same colour.

**Stub for v1**: `SessionRepository` returns `SessionState.Guest` until spec 004.

---

## 10. UiState *(generic sealed class — `app:shared`)*

Single reusable state wrapper for all async data in the app.

```kotlin
sealed class UiState<out T> {
    object Loading                               : UiState<Nothing>()
    data class Success<out T>(val data: T)       : UiState<T>()
    data class Error(val message: String? = null): UiState<Nothing>()
}
```

## 11. LandingUiState *(frontend ViewModel state)*

Emitted by `LandingViewModel` as a `StateFlow`. The page renders immediately;
only the three stat counters use the async `UiState<LandingStats>` wrapper.

```kotlin
data class LandingUiState(
    val statsState: UiState<LandingStats> = UiState.Loading
    // future fields: sessionState, etc.
)
```

In `HeroSection`, `statsState` drives **only** the counter display:
- `UiState.Loading`  → shimmer skeleton on the three numbers
- `UiState.Success`  → numbers rendered; GSAP roll-up fires once
- `UiState.Error`    → "—" shown; no roll-up animation
