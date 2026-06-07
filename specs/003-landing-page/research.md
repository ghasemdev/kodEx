# Research: Landing Page (003)

**Date**: 2026-05-29 | **Branch**: `feature/003-landing-page`

---

## R-001 — GSAP 3.15.0 Kotlin/JS Interop

**Decision**: `@JsModule` external object declarations for Gsap and ScrollTrigger.
All GSAP API calls go through typed Kotlin wrappers in `GsapInterop.kt` — no raw
`js("gsap…")` strings in component code.

**npm package**: `gsap` version `3.15.0`
**Gradle**: `implementation(npm("gsap", "3.15.0"))` inside `webMain.dependencies`

**Minimal `GsapInterop.kt`**:

```kotlin
@JsModule("gsap")
@JsNonModule
external object Gsap {
    fun to(targets: dynamic, vars: dynamic): dynamic
    fun from(targets: dynamic, vars: dynamic): dynamic
    fun fromTo(targets: dynamic, fromVars: dynamic, toVars: dynamic): dynamic
    fun timeline(vars: dynamic = definedExternally): dynamic
    fun set(targets: dynamic, vars: dynamic)
    fun registerPlugin(vararg plugins: dynamic)
    fun matchMedia(): dynamic
    val effects: dynamic
}

@JsModule("gsap/ScrollTrigger")
@JsNonModule
external val ScrollTrigger: dynamic

// ScrollTrigger is registered once at app startup:
// Gsap.registerPlugin(ScrollTrigger)
```

**Key APIs used**:

| API | Purpose |
|-----|---------|
| `gsap.timeline()` | Hero animation sequence (chained phases) |
| `gsap.to(el, {...})` | Move / fade elements; counter roll-up |
| `gsap.from(el, {...})` | Slide-in from offset position |
| `gsap.set(el, {...})` | Instant property set (reset between loops) |
| `gsap.registerPlugin(ScrollTrigger)` | One-time plugin activation |
| `gsap.matchMedia()` | Reduced-motion media query branching |
| `ScrollTrigger.create({...})` | Scroll-based animation triggers |

**Reduced-motion guard** (GSAP 3.x native pattern):
```kotlin
val mm = Gsap.matchMedia()
mm.add("(prefers-reduced-motion: reduce)") {
    // no-op context: sections are pre-shown via CSS
}
mm.add("(prefers-reduced-motion: no-preference)") {
    setupScrollTriggers()
    setupHeroTimeline()
}
```

**Rationale**: Framework-agnostic; no React dependency. 3.15.0 confirmed by project owner.
ScrollTrigger bundled within the same `gsap` npm package — no extra dependency.

**Alternatives rejected**:
- Framer Motion: React-only, incompatible with Kilua
- CSS animations: insufficient for multi-phase timeline + tab switching
- Anime.js: smaller ecosystem, no official Kotlin bindings

---

## R-002 — GSAP + Vite Module Resolution

**Decision**: Works out-of-the-box. No Vite config changes required.

GSAP 3.x ships an `exports` map in `package.json` exposing both the main module and
sub-paths (e.g., `gsap/ScrollTrigger`) as ES modules. Vite resolves these automatically.
The Kilua Vite Gradle plugin passes `npm()` declarations through to `node_modules` — the
same mechanism used for `highlight.js` already in the project.

Both JS and WASM-JS targets share the same `webMain` npm declarations, so a single
`npm("gsap", "3.15.0")` entry covers both targets.

**CSP note**: The existing `wasm-unsafe-eval` directive in both `index.html` and
`Application.kt` is already present (added in spec 002 security hardening commit
`a7e4a9c2`). GSAP itself does not require any additional CSP directives.

---

## R-003 — Hero Animation Sequence (Scripted)

**Decision**: Two-phase looping GSAP timeline. No real Kotlin compilation — purely visual.

### Phase A — Kotlin tab

```
t=0.0s   Cursor blink (CSS keyframes, starts immediately)
t=0.5s   Code typed char-by-char  (stagger 0.04s, ~50 chars ≈ 2.5s)
t=3.0s   "▶ Compiling..." fades in (duration 0.3s)
t=3.3s   Compile spinner (3 rotations × 0.35s = 1.05s)
t=4.4s   Test row 1 slides from x=-20 + opacity 0→1  (0.25s)
t=4.7s   Test row 2 slides in
t=5.0s   Test row 3 slides in + 🎉 icon pops (scale 0→1.2→1, 0.3s)
t=5.8s   Pause
```

### Phase B — Android tab (auto-switches at t=6.0s)

```
t=6.0s   Tab indicator animates to "Android" (x slide, 0.2s)
t=6.2s   Editor content fades out, new code appears empty
t=6.4s   Android code typed  (stagger 0.03s, ~65 chars ≈ 2.0s)
t=8.4s   "💉 Injecting 5 hidden test cases..." fades in
t=9.2s   Test row 1: onClick handler PASS (0.25s)
t=9.5s   Test row 2: ViewModel state PASS
t=9.8s   Test row 3: UI assertion   PASS + 🎉 pop
t=10.6s  Pause, then gsap.call(() → restartFromPhaseA())
```

**Manual tab switch**: `onClick` kills current timeline, `gsap.set()` resets elements,
then starts the appropriate phase's sub-timeline.

**Loop strategy**: `gsap.timeline({ repeat: -1 })` on a wrapper timeline that contains
both phases sequentially.

---

## R-004 — Landing Stats API

**Decision**: `GET /api/v1/stats/landing` — public endpoint, no auth, 5-minute cache.

For v1, returns hardcoded/seeded values. Real DB queries added incrementally in later
specs when User, Contest, and Problem tables exist.

**Backend route** (`server/api` module):
```kotlin
fun Route.landingRoutes() {
    get("/api/v1/stats/landing") {
        call.response.headers.append("Cache-Control", "public, max-age=300")
        call.respond(
            buildSuccessEnvelope(
                data = LandingStatsResponse(
                    totalProblems = 1247,
                    totalUsers = 8432,
                    totalContests = 342,
                ),
                requestId = call.requestId(),
            )
        )
    }
}
```

**KMP model** (`core:models`):
```kotlin
@Serializable
data class LandingStatsResponse(
    val totalProblems: Int,
    val totalUsers: Int,
    val totalContests: Int,
)
```

**Frontend Ktor Client call** (in `LandingStatsRepository`):
```kotlin
suspend fun fetchStats(): LandingStats =
    client.get("/api/v1/stats/landing")
          .body<ApiEnvelope<LandingStatsResponse>>()
          .data
          .toLandingStats()
```

---

## R-005 — Placeholder Data Strategy

**Decision**: Problems sample and leaderboard preview use static Kotlin objects for v1.

```kotlin
object PlaceholderProblems {
    val items = listOf(
        ProblemSummary("001", "Two Sum Kotlin Style",         ExamType.IO,        Difficulty.EASY),
        ProblemSummary("002", "Coroutine Flow Merge",         ExamType.INJECTION, Difficulty.MEDIUM),
        ProblemSummary("003", "Android ViewModel Lifecycle",  ExamType.QUIZ,      Difficulty.MEDIUM),
        ProblemSummary("004", "Sealed Class Dispatch",        ExamType.IO,        Difficulty.HARD),
        ProblemSummary("005", "Compose Recomposition Fix",    ExamType.INJECTION, Difficulty.HARD),
    )
}

object PlaceholderLeaderboard {
    val entries = listOf(
        LeaderboardEntry(rank=1, username="ghasem.shirdel", tier=Tier.GRANDMASTER, score=9840, solved=342, streakDays=47),
        LeaderboardEntry(rank=2, username="kotlin.ninja",   tier=Tier.MASTER,      score=8220, solved=289, streakDays=23),
        LeaderboardEntry(rank=3, username="android.arch",   tier=Tier.MASTER,      score=7105, solved=201, streakDays=12),
    )
}
```

These objects are replaced with API calls in specs 005 and 010 with zero changes to the
section components (the ViewModel injects the data source).

---

## R-006 — Auth Session Interface (Stub for Landing Page)

**Decision**: Define a `UserSession` interface in `app:shared`; provide a `StubUserSession`
(guest) for v1. Spec 004 (Auth) replaces the stub with a real JWT-backed implementation.

```kotlin
// app/shared — SessionState sealed class
sealed class SessionState {
    object Guest : SessionState()
    data class Authenticated(
        val userId: String,
        val username: String,
        val plan: Plan,           // FREE or PRO
        val roles: Set<UserRole>, // PARTICIPANT, EXAM_CREATOR, ADMIN
    ) : SessionState()
}

enum class Plan { FREE, PRO }
enum class UserRole { PARTICIPANT, EXAM_CREATOR, ADMIN }
```

The `GlobalNavBar` receives `SessionState` as a parameter — no direct dependency on auth
implementation. The landing page `LandingViewModel` exposes `SessionState` from a
`SessionRepository` that returns `SessionState.Guest` until spec 004 is implemented.

---

## R-007 — Badge Definitions (Static Catalogue)

All 8 badge definitions are static constants. No API call needed.

| ID | Name | Icon class | Unlock Condition |
|----|------|-----------|-----------------|
| `kotlin-ninja` | Kotlin Ninja | `fa-solid fa-user-ninja` | Complete 3 Kotlin exams with 100% score |
| `android-architect` | Android Architect | `fa-solid fa-building-columns` | Submit an Injection exam scored "Clean Architecture" |
| `night-owl` | Night Owl | `fa-solid fa-moon` | Participate in any exam starting after 00:00 |
| `steady-hand` | Steady Hand | `fa-solid fa-fire` | Participate in at least 1 exam per week for 5 consecutive weeks |
| `bug-hunter` | Bug Hunter | `fa-solid fa-bug` | Submit an accepted bug report on a question |
| `speed-demon` | Speed Demon | `fa-solid fa-bolt` | Submit a correct solution within 10 minutes |
| `creative-chaos` | Creative Chaos | `fa-solid fa-dice` | Unconventional solution that passes all tests |
| `hello-world` | Hello World | `fa-solid fa-hand-wave` | First code submission ever |
