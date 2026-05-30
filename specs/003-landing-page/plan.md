# Implementation Plan: Landing Page

**Branch**: `feature/003-landing-page` | **Date**: 2026-05-29 | **Spec**: [spec.md](spec.md)

---

## Summary

Build the public-facing landing page for KodEx — a Kotlin/JS + Kilua single-page
application section that converts first-time visitors via a scripted GSAP animation hero
(dual Kotlin/Android code editor tabs), scroll-triggered section reveals, and gamification
teasers. The page is fully public; authenticated users receive a personalised navbar.
One lightweight backend endpoint serves live platform statistics for the hero counter animation.

---

## Technical Context

**Language/Version**: Kotlin 2.3, Kotlin/JS (ES2015 IR), Kotlin/WASM-JS

**Primary Dependencies**:
- Kilua (UI framework, existing)
- GSAP **3.15.0** + ScrollTrigger plugin — `npm("gsap", "3.15.0")` in `webMain.dependencies`
- Ktor Client JS (fetch engine, existing) — for `GET /api/v1/stats/landing`
- kotlinx.serialization (existing)
- ThemeManager (existing, `remember = true` already wired in `App.kt`)
- LocaleManager + i18n (existing, `initI18n()` already wired in `App.kt`)

**Storage**: None (landing page is read-only). LocalStorage only via ThemeManager + LocaleManager (already handled).

**Testing**: Kotest (existing), screenshot tests via Karma/Chrome (existing pattern from spec 002)

**Target Platform**: Browser — JS + WASM-JS targets, Vite bundler

**Performance Goals**:
- Lighthouse Performance ≥ 80 desktop / ≥ 70 mobile
- Hero animation completes within 5 seconds of page load
- GSAP + ScrollTrigger bundle contribution < 80 KB gzipped

**Constraints**:
- All animation is purely presentational — no real Kotlin compilation in the browser
- `innerHTML` / `outerHTML` strictly forbidden (§A4 / security constitution §5)
- Reduced-motion: all GSAP animations must be skipped when `prefers-reduced-motion: reduce`
- Kilua `+` operator only for user-controlled text content (existing XSS rule)
- GSAP wrappers use `@JsModule` — no raw `js("gsap.to(…)")` calls in component code

**Scale/Scope**: Single page, ~7 sections, 1 new backend endpoint, 15–20 new Kotlin/JS files

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| § | Principle | Landing Page Impact | Status |
|---|-----------|---------------------|--------|
| I | Kotlin-First Stack | All UI in Kotlin/JS + Kilua. GSAP used via `@JsModule` interop, not raw JS calls. | ✅ Pass |
| II | Dual Exam Modes | Hero animation showcases both Kotlin (I/O) and Android (Injection) modes in a tabbed demo. | ✅ Pass |
| III | Secure Sandbox | No code execution on the landing page — animation is purely visual. | ✅ N/A |
| IV | Test-Injection Grading | Injection demo in hero is scripted animation, not real grading. | ✅ N/A |
| V | Role-Based Domain Model | Navbar `Create Exam` item visibility gated on `EXAM_CREATOR` / `ADMIN` role check. Full role model implemented in spec 009; landing page only reads the role from the JWT session. | ✅ Pass |
| VI | Auditability | No submission events on landing page; audit logging N/A here. | ✅ N/A |
| VII | Architecture | Landing page follows MVI: `LandingPage` = View, `LandingViewModel` = ViewModel/Store, `LandingStatsRepository` = Data layer. `GlobalNavBar` and `Footer` are reusable layout components under `layout/`. | ✅ Pass |
| VIII | Auth & AuthZ | Navbar reads JWT session state (already in ThemeManager/LocaleManager pattern). No protected content on landing page — entire page public. | ✅ Pass |
| IX | API Design | New `GET /api/v1/stats/landing` follows `/api/v1/` prefix and `{data, meta}` envelope. No auth required. | ✅ Pass |
| X | Testing Policy | Screenshot test for above-fold hero. Unit tests for `LandingViewModel`. Component tests for `HeroSection` state transitions. | ✅ Pass |

**Security constitution §5 (Frontend XSS Controls)**: All text content via Kilua `+` operator.
Badge/tier labels are static string constants — not user-controlled. No `innerHTML` anywhere.

**Post-Phase-1 re-check**: Required before implementation begins.

---

## MVI + Ktor Client + Koin Wiring

Canonical layering pattern for `GET /api/v1/stats/landing`. All future pages follow the
same structure — this is the reference implementation.

### Layer Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│  View  (Kilua @Composable)                                        │
│  LandingPage.kt                                                   │
│                                                                   │
│    // Page renders IMMEDIATELY — no global loading block          │
│    val uiState by viewModel.uiState.collectAsState()              │
│                                                                   │
│    GlobalNavBar(...)                                              │
│    HeroSection(statsState = uiState.statsState)  ← only counters │
│    ExamTypesSection()           // static — no network dep        │
│    GamificationSection()        // static — no network dep        │
│    ...                                                            │
│    Footer()                                                       │
└──────────────┬───────────────────────────────────────────────────┘
               │ observes StateFlow<LandingUiState>
               ▼
┌──────────────────────────────────────────────────────────────────┐
│  ViewModel  (MVI — Kotlin/JS-safe ViewModel)                  │
│  LandingViewModel.kt                                              │
│                                                                   │
│    // Only the stats counters have a loading state                │
│    data class LandingUiState(                                     │
│      val statsState: StatsState = StatsState.Loading              │
│    )                                                              │
│    sealed class StatsState {                                      │
│      object Loading : StatsState()                                │
│      data class Loaded(val stats: LandingStats) : StatsState()   │
│      object Error   : StatsState()                                │
│    }                                                              │
│                                                                   │
│    init {                                                         │
│      viewModelScope.launch {                                      │
│        _uiState.update { it.copy(statsState =                    │
│          try { StatsState.Loaded(repository.fetchStats()) }       │
│          catch (e: Exception) { StatsState.Error }                │
│        )}                                                         │
│      }                                                            │
│    }                                                              │
└──────────────┬───────────────────────────────────────────────────┘
               │ calls suspend fun → returns domain model
               ▼
┌──────────────────────────────────────────────────────────────────┐
│  Repository  (data layer — Dependency Inversion)                  │
│                                                                   │
│  // dev.kodex.shared.landing                                      │
│  interface LandingStatsRepository {          // app:shared        │
│    suspend fun fetchStats(): LandingStats    // domain model      │
│  }                                                                │
│                                                                   │
│  // Three-layer data stack (D12):                                 │
│  //                                                               │
│  // LandingStatsRemoteDataSource (interface, webApp)              │
│  //   ↑ implemented by                                           │
│  // LandingStatsRemoteDataSourceImpl — Ktor transport only,       │
│  //   uses ApiRoutes.LANDING_STATS (D14), returns raw response   │
│  //   ↑ used by                                                  │
│  // LandingStatsRepositoryImpl — selects source, maps domain     │
│                                                                   │
│  // dev.kodex.webapp.pages.landing.data                          │
│  interface LandingStatsRemoteDataSource {                         │
│    suspend fun fetchStats(): LandingStatsResponse                 │
│  }                                                                │
│                                                                   │
│  @Single(binds=[LandingStatsRemoteDataSource::class])            │
│  class LandingStatsRemoteDataSourceImpl(client: HttpClient) {    │
│    override suspend fun fetchStats(): LandingStatsResponse =      │
│      withContext(ioDispatcher) {                                  │
│        client.get(ApiRoutes.LANDING_STATS)                        │
│          .body<ApiEnvelope<LandingStatsResponse>>().data          │
│      }                                                            │
│  }                                                                │
│                                                                   │
│  @Single(binds=[LandingStatsRepository::class])                  │
│  class LandingStatsRepositoryImpl(                                │
│    private val remoteDataSource: LandingStatsRemoteDataSource     │
│  ) : LandingStatsRepository {                                     │
│    override suspend fun fetchStats(): LandingStats =              │
│      remoteDataSource.fetchStats().toDomainModel()                │
│    // Future: inject LocalDataSource; cache-or-remote logic here  │
│  }                                                                │
│                                                                   │
│  // private extension inside LandingStatsRepositoryImpl.kt:      │
│  private fun LandingStatsResponse.toDomainModel() = LandingStats(│
│    totalProblems = totalProblems, totalUsers = totalUsers, …      │
│  )                                                                │
└──────────────┬───────────────────────────────────────────────────┘
               │ injected by Koin
               ▼
┌──────────────────────────────────────────────────────────────────┐
│  Ktor Client  (HTTP, Js fetch engine)                             │
│  — shared singleton via Koin                                      │
└──────────────────────────────────────────────────────────────────┘
```

**Why domain model, not UI model, in the repository**:
The repository sits at the boundary between Data and Domain layers. Its return type must
be the domain model (`LandingStats`) so the interface can be defined in `app:shared`
without depending on any UI type. The ViewModel is the only layer that maps domain
models → UI state. This preserves Dependency Inversion: the interface does not know how
the data is displayed.

**Why only stats counters are skeleton, not the whole page**:
Sections whose content is static (ExamTypes, Gamification, Problems sample, Footer) have
zero network dependencies — they render instantly from compile-time constants. Only the
three counter numbers in HeroSection (`totalProblems`, `totalUsers`, `totalContests`)
need a network response. Blocking the entire page for one partial data point creates
unnecessary perceived latency and harms LCP (Largest Contentful Paint).

```kotlin
// HeroSection — only the counters show skeleton
@Composable
fun IComponent.HeroSection(statsState: StatsState) {
    // ... headline, tagline, CTAs render immediately ...

    // Only these three numbers are skeleton until data arrives
    when (statsState) {
        is StatsState.Loading -> {
            StatCounter(label = "problems", value = null)   // shows shimmer
            StatCounter(label = "developers", value = null)
            StatCounter(label = "contests", value = null)
        }
        is StatsState.Loaded -> {
            StatCounter("problems",   statsState.stats.totalProblems)
            StatCounter("developers", statsState.stats.totalUsers)
            StatCounter("contests",   statsState.stats.totalContests)
        }
        is StatsState.Error -> {
            StatCounter("problems",   value = null, error = true)  // shows "—"
            StatCounter("developers", value = null, error = true)
            StatCounter("contests",   value = null, error = true)
        }
    }
}
```

---

## ViewModel (Kotlin/JS — no Android dependency)

`androidx.lifecycle.ViewModel` is **JVM/Android only** — does not compile for
Kotlin/JS or WASM-JS targets. The project defines its own abstract `ViewModel`:

```kotlin
// app/webApp/src/webMain/kotlin/dev/kodex/webapp/core/ViewModel.kt

abstract class ViewModel {
    // SupervisorJob: child failures don't cancel siblings or the parent scope.
    // Dispatchers.Main.immediate: browser microtask queue; if already on Main
    // the coroutine starts synchronously — avoids one extra frame delay for
    // UI updates. Available in kotlinx-coroutines-core JS target since 1.6.0.
    val viewModelScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun onCleared() = viewModelScope.cancel()
}
```

### Dispatchers.IO on Kotlin/JS

`Dispatchers.IO` is **JVM-only** — it does not exist in Kotlin/JS or WASM-JS.
Ktor Client JS uses the browser Fetch API which is non-blocking by nature.
For code-pattern consistency with future KMP/JVM modules, define a multiplatform
dispatcher via `expect`/`actual` in `app:shared`:

```kotlin
// app/shared/src/commonMain/…/coroutines/AppDispatchers.kt
expect val ioDispatcher: CoroutineDispatcher

// app/shared/src/jsMain/…  (+ wasmJsMain actual)
actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
// JS is single-threaded — Default = main event loop.
// Fetch API never blocks; no IO thread pool needed.

// app/shared/src/jvmMain/…  (for server-side use in future specs)
actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
```

**Repository usage** — always wrap the network call with `withContext(ioDispatcher)`:

```kotlin
class LandingStatsRepositoryImpl(
    private val client: HttpClient,
) : LandingStatsRepository {
    override suspend fun fetchStats(): LandingStats =
        withContext(ioDispatcher) {      // Default on JS/WASM, IO on JVM
            client
                .get("/api/v1/stats/landing")
                .body<ApiEnvelope<LandingStatsResponse>>()
                .data
                .toDomainModel()
        }
}
```

`withContext(ioDispatcher)` is a no-op context switch on JS (already on Default/Main)
but makes the intent explicit and keeps the pattern uniform across all platforms.

### UiState Generic Pattern (Loading / Error / Success)

Use one consistent sealed class everywhere — not page-specific names:

```kotlin
// app/shared/src/commonMain/…/ui/UiState.kt
sealed class UiState<out T> {
    object Loading                               : UiState<Nothing>()
    data class Success<out T>(val data: T)       : UiState<T>()
    data class Error(val message: String? = null): UiState<Nothing>()
}
```

Landing page stats:
```kotlin
data class LandingUiState(
    val statsState: UiState<LandingStats> = UiState.Loading
)
```

All future pages (`ProblemsPage`, `ContestsPage`, `LeaderboardPage`) use `UiState<T>`
directly — no bespoke state class per feature unless the shape genuinely differs.

### Lifecycle Wiring in Kilua Composable

```kotlin
@Composable
fun IComponent.LandingPage() {
    val viewModel = remember { KoinPlatform.getKoin().get<LandingViewModel>() }
    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(Unit) {
        onDispose { viewModel.onCleared() }
    }
    // render …
}
```

### Concrete LandingViewModel

```kotlin
class LandingViewModel(
    private val repository: LandingStatsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LandingUiState())
    val uiState: StateFlow<LandingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(statsState =
                try   { UiState.Success(repository.fetchStats()) }
                catch (e: Exception) { UiState.Error(e.message) }
            )}
        }
    }
}
```

---

## Koin Module

```kotlin
// DI uses Koin Annotations (D11) — no manual DSL wiring. The compiler plugin
// (`io.insert-koin.compiler.plugin:1.0.0`) processes annotations at compile time.

// NetworkKoinModule.kt — provides HttpClient (third-party, needs a @Module provider fn)
@Module
class NetworkKoinModule {
    @Single
    fun httpClient(): HttpClient = HttpClient(Js) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        expectSuccess = false
    }
}

// AppModule.kt — root module; @ComponentScan discovers all @Single/@Factory in the package tree
@Module(includes = [NetworkKoinModule::class])
@ComponentScan("dev.kodex.webapp")
class AppKoinModule

// Annotated classes (auto-discovered by ComponentScan):
// @Single(binds=[LandingStatsRemoteDataSource::class]) LandingStatsRemoteDataSourceImpl
// @Single(binds=[LandingStatsRepository::class])       LandingStatsRepositoryImpl
// @Factory                                              LandingViewModel

// App.kt entry point:
// startKoin { modules(AppKoinModule().module) }  // .module generated by compiler plugin
```

```kotlin
// App.kt — inside start(), before root {}
startKoin { modules(appModule) }
```

---

## Intent Flow (MVI)

Landing page has no user-driven mutations — only an initial data load intent. The full
pattern is documented here for reference; all future pages with user actions extend it.

```
User Action  →  Intent (sealed class)
                  ↓ viewModel.onIntent(intent)
             Action / Effect (internal)
                  ↓ reduce(currentState, action) → newState
             UiState (StateFlow)
                  ↓ collectAsState() in View
             Composable re-render
```

For landing page, the implicit init-block load is the only "intent". The `Intent` sealed
class is omitted here and will be introduced with the first interactive page (spec 005).

---

## Project Structure

### Documentation (this feature)

```text
specs/003-landing-page/
├── plan.md              ← this file
├── research.md          ← Phase 0 output
├── data-model.md        ← Phase 1 output
├── quickstart.md        ← Phase 1 output
├── contracts/
│   └── landing-stats.md ← Phase 1 output
└── tasks.md             ← Phase 2 output (/speckit-tasks — not created by /speckit-plan)
```

### Source Code

```text
app/webApp/src/webMain/kotlin/dev/kodex/webapp/
├── App.kt                                    ← UPDATE: startKoin { AppKoinModule().module }
├── network/
│   └── ApiRoutes.kt                          ← NEW: object ApiRoutes { const val LANDING_STATS }
├── core/
│   └── ViewModel.kt                      ← NEW: abstract class, SupervisorJob + Main.immediate
├── gsap/
│   └── GsapInterop.kt                        ← NEW: @JsModule Gsap + ScrollTrigger wrappers
├── di/
│   ├── AppModule.kt       ← NEW: @Module(includes=[NetworkKoinModule]) @ComponentScan root
│   └── NetworkKoinModule.kt ← NEW: @Module @Single fun httpClient(): HttpClient
├── layout/
│   ├── GlobalNavBar.kt                       ← NEW: logo, nav links, theme/lang toggles,
│   │                                                 guest buttons OR avatar+dropdown+plan badge
│   ├── Footer.kt                             ← NEW: 3-column footer + social links
│   └── PageTransition.kt                     ← NEW: GSAP fade+y enter animation wrapper
├── pages/
│   ├── landing/
│   │   ├── LandingPage.kt                    ← NEW: page root — wraps sections, owns VM
│   │   ├── LandingViewModel.kt               ← NEW: extends ViewModel, fetches stats
│   │   ├── sections/
│   │   │   ├── HeroSection.kt                ← NEW: gradient mesh bg + code editor + counters
│   │   │   ├── ExamTypesSection.kt           ← NEW: 3 exam type cards, fan-in animation
│   │   │   ├── GamificationSection.kt        ← NEW: tiers + badge flip cards + trophy preview
│   │   │   ├── ProblemsSection.kt            ← NEW: 5-row sample table, slide-left animation
│   │   │   ├── LeaderboardSection.kt         ← NEW: top-3 rows + score roll-up
│   │   │   └── CreateExamSection.kt          ← NEW: checklist CTA
│   │   └── model/
│   │       ├── LandingUiState.kt             ← NEW: data class wrapping UiState<LandingStats>
│   │       ├── BadgeDefinition.kt            ← NEW: static catalogue, init-block validation
│   │       └── PlaceholderData.kt            ← NEW: PlaceholderProblems + PlaceholderLeaderboard
│   │   └── data/
│   │       ├── LandingStatsRemoteDataSource.kt     ← NEW: interface (returns API response type)
│   │       ├── LandingStatsRemoteDataSourceImpl.kt ← NEW: Ktor transport; ApiRoutes constant
│   │       └── LandingStatsRepositoryImpl.kt       ← NEW: source selector + toDomainModel()
│   └── NotFoundPage.kt                       ← NEW: 404 fallback route

app/shared/src/commonMain/kotlin/dev/kodex/shared/
└── landing/
    ├── LandingStats.kt                       ← NEW: domain model (lives in app:shared, not webApp)
    └── LandingStatsRepository.kt             ← NEW: interface (domain model return type)
└── session/
    └── SessionState.kt                       ← NEW: sealed class + Plan + UserRole enums

app/webApp/src/webMain/resources/modules/i18n/
└── messages.pot                              ← UPDATE: add landing page string keys

core/models/src/commonMain/kotlin/dev/kodex/core/models/
├── api/
│   └── ApiEnvelope.kt                        ← NEW: @Serializable{data:T} — frontend deserializer
├── landing/
│   └── LandingStatsResponse.kt               ← NEW: @Serializable API response
├── DifficultyTier.kt                         ← NEW: enum + computeDifficulty()
├── ExamType.kt                               ← NEW: enum (QUIZ / IO / INJECTION)
└── Tier.kt                                   ← NEW: enum (JUNIOR / SENIOR / MASTER / GRANDMASTER)

server/api/src/main/kotlin/dev/kodex/server/api/routes/
└── LandingRoutes.kt                          ← NEW: GET /api/v1/stats/landing + rate limit

server/app/src/main/kotlin/dev/kodex/server/
└── Application.kt                            ← UPDATE: install RateLimit + landingRoutes()

gradle/libs.versions.toml                     ← UPDATE: ktor-server-rate-limit entry

app/webApp/src/webTest/kotlin/dev/kodex/webapp/
├── landing/
│   ├── LandingViewModelTest.kt
│   ├── LandingStatsRepositoryImplTest.kt
│   ├── BadgeDefinitionTest.kt
│   ├── HeroSectionTest.kt
│   ├── GlobalNavBarTest.kt
│   └── BadgeCardTest.kt
└── screenshot/
    ├── LandingPageScreenshotTest.kt
    ├── LandingPageMobileScreenshotTest.kt
    └── GlobalNavBarScreenshotTest.kt

server/api/src/test/kotlin/dev/kodex/server/api/routes/
├── LandingRoutesTest.kt
└── LandingRateLimitTest.kt
```

---

## Phase 0: Research

All findings documented in [research.md](research.md).

### R-001 — GSAP 3.15.0 Kotlin/JS Interop

**Decision**: `@JsModule("gsap") external val gsap: Gsap` (named val) + `external interface Gsap`
+ `external interface GsapVars : JsAny`. Callsites use `unsafeJso<GsapVars> { ... }` from
`js.objects.unsafeJso` — no raw `js("gsap…")` strings anywhere in component code.

> **Note**: The Kotlin/JS IR compiler requires `@JsModule` on a `val` declaration, not on an
> `external object`. The `@JsNonModule` annotation is NOT used with the IR compiler. The legacy
> `external object Gsap` pattern worked only with the old JS backend and does not compile with IR.

**GSAP 3.15.0 key facts**:
- npm package: `gsap` (same for all 3.x versions)
- ScrollTrigger: bundled under `gsap/ScrollTrigger` — no separate npm package needed
- `gsap.registerPlugin(ScrollTrigger)` must be called once before first use
- `gsap.matchMedia()` → used for `prefers-reduced-motion` handling
- Timeline sequencing: `.to()`, `.from()`, `.fromTo()`, `.call()`, `.add()`, `"-=0.5"` position

**`GsapInterop.kt` canonical shape** (implemented in Phase 1):
```kotlin
@JsModule("gsap")
external val gsap: Gsap

external interface Gsap {
    fun to(targets: Element, vars: GsapVars): JsAny
    fun from(targets: Element, vars: GsapVars): JsAny
    fun fromTo(targets: Element, fromVars: GsapVars, toVars: GsapVars): JsAny
    fun timeline(vars: GsapVars = definedExternally): JsAny
    fun set(targets: Element, vars: GsapVars)
    fun registerPlugin(vararg plugins: JsAny)
    fun matchMedia(): JsAny
}

// All numeric vars use Double; only repeat uses Int (-1 = infinite loop)
external interface GsapVars : JsAny {
    var x: Double; var y: Double; var opacity: Double
    var scale: Double; var scaleX: Double; var scaleY: Double
    var duration: Double; var stagger: Double
    var repeat: Int; var yoyo: Boolean
    var ease: String; var transformOrigin: String; var width: String
    var onComplete: () -> Unit; var onUpdate: () -> Unit
}

@JsModule("gsap/ScrollTrigger")
external val ScrollTrigger: JsAny
```

**Callsite pattern**:
```kotlin
gsap.fromTo(
    el,
    unsafeJso { x = -100.0; opacity = 0.0 },
    unsafeJso { x = 0.0; opacity = 1.0; duration = 1.0; ease = "power2.out" },
)
```

**Reduced motion pattern** (GSAP 3.15.0 native):
```kotlin
val mm = gsap.matchMedia().asDynamic()
mm.add("(prefers-reduced-motion: reduce)") {
    // no-op context: sections are pre-shown via CSS
}
mm.add("(prefers-reduced-motion: no-preference)") {
    setupScrollTriggers()
    setupHeroTimeline()
}
```

**Rationale**: Framework-agnostic, works in Kilua without React. Version 3.15.0 confirmed
by user. ScrollTrigger is the standard plugin for scroll-based reveals.

**Alternatives considered**: CSS animations (insufficient for timeline sequences + tab
switching), Framer Motion (React-only — rejected), Anime.js (smaller ecosystem).

### R-002 — GSAP + Kotlin/JS Module Resolution in Vite

**Decision**: `@JsModule("gsap")` resolves via Vite's standard npm module resolution when
`npm("gsap", "3.15.0")` is declared in `webMain.dependencies`. No Vite config override needed.

For WASM-JS target: same npm declaration applies; Vite handles both targets identically.

**ScrollTrigger sub-path import**: `@JsModule("gsap/ScrollTrigger")` works with Vite because
GSAP 3.x exports ScrollTrigger as a named ES module sub-path in its `package.json` exports map.

### R-003 — Hero Animation Sequence Design

**Decision**: Two-phase scripted GSAP timeline, auto-cycling:

```
Phase A — Kotlin tab (active):
  t=0.0   Cursor blinks in empty editor
  t=0.5   Code typed char-by-char (stagger 0.04s/char, ~2s total for ~50 chars)
  t=2.5   "▶ Compiling..." text fades in
  t=3.0   Spinner (3 rotations, 0.5s each)
  t=4.5   Test row 1 slides in: "✓  Test 1   9ms    PASS"
  t=4.8   Test row 2 slides in: "✓  Test 2   12ms   PASS"
  t=5.1   Test row 3 slides in: "✓  Test 3   8ms    PASS  🎉"
  t=5.8   Brief pause

Phase B — Android tab (auto-switch):
  t=6.0   Tab switches to "Android" with a slide transition
  t=6.2   Editor content clears; new code typed (~60 chars, stagger 0.03s, ~2s)
  t=8.2   "💉 Injecting 5 hidden test cases..." text appears
  t=9.0   Test row 1: "✓  onClick handler   PASS"
  t=9.4   Test row 2: "✓  ViewModel state   PASS"
  t=9.8   Test row 3: "✓  UI assertion      PASS  🎉"
  t=10.5  Brief pause, then loop back to Phase A
```

Manual tab click: jumps to that phase and restarts its sequence.

### R-004 — LandingStats API: Backend Design

**Decision**: Single public `GET /api/v1/stats/landing` endpoint — no auth required.
Returns aggregated counts. Initially can return hardcoded/seeded values (real DB queries
added when Users, Exams, and Submissions tables exist in later specs).

Response follows §IX unified envelope:
```json
{
  "data": {
    "totalProblems": 1247,
    "totalUsers": 8432,
    "totalContests": 342
  },
  "meta": {
    "requestId": "uuid",
    "service": "kodex-api",
    "version": "0.1.0",
    "timestamp": "2026-05-29T10:00:00Z"
  }
}
```

Cache-Control: `public, max-age=300` (5-minute cache — stats don't need to be real-time).

### R-005 — Placeholder vs Live Data Strategy

**Decision**: Landing page v1 uses live API only for `LandingStats` (hero counters).
Leaderboard preview (top 3 rows) and problems sample (5 rows) use **static placeholder data**
defined in Kotlin objects. These will be replaced with real API calls in spec 010
(Leaderboard) and spec 005 (Problems) respectively.

Rationale: avoids blocking landing page delivery on unimplemented backend features.

---

## Phase 1: Design & Contracts

### Data Model → [data-model.md](data-model.md)

See data-model.md for full entity definitions.

### Contracts → [contracts/landing-stats.md](contracts/landing-stats.md)

See contracts/landing-stats.md for the API contract.

### Quickstart → [quickstart.md](quickstart.md)

See quickstart.md for dev setup instructions.

---

## Backend Rate Limiting (Public Endpoint)

`GET /api/v1/stats/landing` is public and unauthenticated, making it a target for
scraping and abuse. Ktor 3.5.0 ships `ktor-server-rate-limit` — add it to the catalog
and apply it to this route.

```toml
# gradle/libs.versions.toml
ktor-server-rate-limit = { module = "io.ktor:ktor-server-rate-limit", version.ref = "ktor" }
```

```kotlin
// server/app — Application.kt
install(RateLimit) {
    // Shared bucket for all public API routes
    register(RateLimitName("public")) {
        rateLimiter(limit = 60, refillPeriod = 60.seconds)  // 60 req/min per IP
        requestKey { call -> call.request.origin.remoteHost }
    }
}

// server/api — LandingRoutes.kt
fun Route.landingRoutes() {
    rateLimit(RateLimitName("public")) {
        get("/api/v1/stats/landing") {
            call.response.headers.append("Cache-Control", "public, max-age=300")
            call.respond(/* ... */)
        }
    }
}
```

**Reasoning**: The 5-minute `Cache-Control` already handles legitimate repeated loads.
The rate limit catches automated requests that bypass caching. 60 req/min per IP is
generous enough for any real browser session (the page loads this endpoint once).

---

## Routing (Kilua SPA Router)

Kilua has a built-in routing DSL that handles browser History API navigation for SPAs.
This spec introduces the router and registers the first two routes. Future specs add more.

```kotlin
// App.kt — replace the current div { +"Hello KodEx" } block
root("root") {
    val isDev = isDev()

    routing {
        // Dev-only playground (existing, gated by isDev)
        if (isDev) {
            route("/playground") { PlaygroundApp() }
        }

        // Landing page — spec 003
        route("/") { LandingPage() }

        // Future routes (registered here as specs are implemented):
        // route("/problems")    { ProblemsPage() }   // spec 005
        // route("/contests")    { ContestsPage() }   // spec 006
        // route("/leaderboard") { LeaderboardPage() } // spec 010
        // route("/login")       { LoginPage() }       // spec 004
        // route("/signup")      { SignUpPage() }      // spec 004
        // route("/profile/:id") { ProfilePage() }     // spec 011

        // 404 fallback
        route("*") { NotFoundPage() }
    }
}
```

**Page Transition Animations**

Transitions must feel smooth and professional — **not** mobile-app-like (no swipe/slide
between pages). The target: a subtle fade + 12px upward translate, 280ms ease-out.
Fast enough not to feel sluggish; present enough to signal the context change.

```kotlin
// layout/PageTransition.kt
// Wraps page content with a GSAP enter animation on mount
@Composable
fun IComponent.PageTransition(content: @Composable IComponent.() -> Unit) {
    val containerRef = remember { mutableStateOf<HTMLElement?>(null) }

    div(className = "page-transition-root", ref = containerRef) {
        content()
    }

    LaunchedEffect(Unit) {
        containerRef.value?.let { el ->
            Gsap.from(el, js("""{ opacity: 0, y: 12, duration: 0.28, ease: "power2.out" }"""))
        }
    }
}
```

Every page component wraps its content in `PageTransition`. No exit animation — the
incoming page appears over the outgoing one (simplest, most reliable cross-browser).

---

## Animation Design

> "پشمت بریزه" — this section defines every animation on the landing page.

### 1. Hero Background — Ambient Gradient Mesh

A slow-moving animated gradient mesh behind the hero text. Uses two or three radial
gradients that move in subtle arcs via GSAP `to()` with `repeat:-1, yoyo:true`.

```
Colors: --color-primary/20  --color-secondary/15  --color-tertiary/10
Motion: each blob moves ~80px over 8–12s, yoyo, staggered starts
```

No particle system (performance risk on mobile). The gradient mesh alone gives depth.

### 2. Hero Code Editor — Typewriter Sequence

Full timeline in `research.md` → R-003. Key visual details:

- **Font**: JetBrains Mono (already installed as npm font)
- **Cursor**: blinking `|` via CSS `@keyframes blink` (not GSAP — saves a timeline slot)
- **Syntax highlighting**: static CSS classes for keywords (`fun`, `class`, `return`) —
  no runtime highlight.js on the typed-in text (highlight.js used for CodeBlock component
  elsewhere)
- **Compile bar**: thin progress bar that fills from 0→100% in 0.8s (GSAP `scaleX`)
- **Test rows**: each row slides in from `x: -16, opacity: 0` with `power2.out`
- **🎉 success pop**: the emoji scales `0 → 1.3 → 1.0` with `elastic.out(1, 0.6)`

### 3. Scroll-Triggered Section Entrances

Each section below the hero has a different entrance to avoid monotony:

| Section | Entrance | GSAP config |
|---------|----------|-------------|
| Exam Types | Cards fan up from `y:40, opacity:0` | `stagger:0.12, ease:"back.out(1.2)"` |
| Gamification | Section title first, then tier bar fills, then badges float up | `stagger:0.08` on badge grid |
| Problems | Table rows slide from `x:-20, opacity:0` | `stagger:0.06, ease:"power1.out"` |
| Leaderboard | Rows drop from `y:-16, opacity:0` | `stagger:0.1, ease:"power2.out"` |
| Create Exam | Checklist items appear one by one with checkmark draw | `stagger:0.15` |

ScrollTrigger config for all sections:
```kotlin
ScrollTrigger.create(js("""{
  trigger: el,
  start: "top 82%",
  once: true,
  onEnter: { tl.play() }
}"""))
```

`once: true` — animation plays once, never replays on re-scroll.

### 4. Gamification — Badge 3D Flip on Hover

Pure CSS `transform-style: preserve-3d` + `rotateY(180deg)` on hover. GSAP is used only
to trigger the class that activates the flip — the flip itself is CSS transition.

```
Front face: badge icon + name
Back face:  unlock condition text (truncated to 2 lines)
Flip duration: 0.4s, ease: cubic-bezier(0.25, 0.46, 0.45, 0.94)
```

### 5. Tier Progress Bar

Animated fill on scroll enter: the filled segment grows from `width:0` to target width
(e.g., 50% for Senior Coder) over 1.2s with `ease: "power2.inOut"`.

### 6. Leaderboard Counter Roll-Up

Score numbers count up from 0 to final value using GSAP's `snap` trick:

```kotlin
Gsap.to(scoreEl, js("""{ innerHTML: 9840, snap: "innerHTML", duration: 1.6,
                          ease: "power1.out", onUpdate: formatNumber }"""))
```

`formatNumber` adds comma separators: `9,840`.

### 7. Hero Stats Counter Roll-Up

Same snap trick, but waits for `StatsState.Loaded` before starting. If stats are still
loading when hero animates in, the counters run their animation once the data arrives
(GSAP `.from()` called in a `LaunchedEffect` keyed on the loaded state).

### 8. Navbar — Hover Indicator

Active link has an animated underline that slides between items on hover:
one `<div>` acting as the indicator, repositioned via GSAP `to()` on `mouseenter`.

```kotlin
// duration: 0.2, ease: "power2.out", x: targetLeft, width: targetWidth
```

### 9. Profile Dropdown

Dropdown appears with `scaleY: 0→1, opacity: 0→1` from `transform-origin: top right`.
Duration: 0.18s. Closed by reversing the same GSAP tween.

### 10. Footer Social Icons

On hover: icon scales `1→1.15` with `elastic.out(1, 0.5)` — a subtle bounce.

---

## i18n

The landing page is fully internationalised via the existing `kilua-i18n` + `gettext`
setup from spec 002. Every visible string goes through `i18n.tr("…")`.

```kotlin
// Translation keys to add to messages.pot (en baseline):
"KodEx — The Kotlin & Android Coding Arena"
"Compete. Build. Master."
"Get Started"
"Explore Problems"
"problems"
"developers"
"contests"
"Three ways to challenge developers"
"Multiple Choice"
"I/O Test Cases"
"Injection / Project"
// … all section headings, button labels, badge names, tier names, footer links
```

**RTL support**: Persian locale sets `dir="rtl"` on `<html>` (already wired in `App.kt`
via `LocaleManager.registerLocaleListener`). Tailwind v4's logical properties
(`ms-*`, `me-*`, `ps-*`, `pe-*`, `start-*`, `end-*`) are used throughout — no `left-*`
or `right-*` hardcoded values in landing page components.

**Messages file locations** (following spec 002 pattern):
- `src/webMain/resources/modules/i18n/messages.pot` — source strings
- `src/webMain/resources/modules/i18n/messages-en.po` — English translations
- `src/webMain/resources/modules/i18n/messages-fa.po` — Persian translations (T-bonus)

---

## Testing Strategy

### Coverage Target: ≥ 90% (existing Kover threshold — must not regress)

### Unit Tests — Logic

| Test class | What it tests |
|------------|---------------|
| `LandingViewModelTest` | `StatsState` transitions: Loading → Loaded, Loading → Error; ViewModel cancels scope on `onCleared()` |
| `LandingStatsRepositoryImplTest` | Mock `HttpClient`; verifies `fetchStats()` maps `LandingStatsResponse` → `LandingStats` via `.toDomainModel()` correctly |
| `BadgeDefinitionTest` | All 8 `BadgeDefinition` instances pass `init` block validation (`id` regex, `iconClass` regex) |

```kotlin
// Example: LandingViewModelTest.kt
class LandingViewModelTest : StringSpec({

    "statsState transitions to Loaded on successful fetch" {
        val fakeStats = LandingStats(totalProblems = 10, totalUsers = 5, totalContests = 2)
        val fakeRepo = object : LandingStatsRepository {
            override suspend fun fetchStats() = fakeStats
        }
        val vm = LandingViewModel(fakeRepo)
        // give the coroutine a tick
        delay(1)
        vm.uiState.value.statsState shouldBe StatsState.Loaded(fakeStats)
        vm.onCleared()
    }

    "statsState transitions to Error on fetch failure" {
        val fakeRepo = object : LandingStatsRepository {
            override suspend fun fetchStats(): LandingStats = error("network error")
        }
        val vm = LandingViewModel(fakeRepo)
        delay(1)
        vm.uiState.value.statsState shouldBe StatsState.Error
        vm.onCleared()
    }
})
```

### UI Component Tests — Rendering

| Test class | What it tests |
|------------|---------------|
| `HeroSectionTest` | Renders skeleton (`StatCounter` with `null`) when `StatsState.Loading`; renders values when `StatsState.Loaded`; shows "—" on `StatsState.Error` |
| `GlobalNavBarTest` | Guest state → Sign In / Sign Up visible, avatar absent; Authenticated state → avatar + plan badge visible, sign-in absent; `EXAM_CREATOR` role → Create Exam item visible |
| `BadgeCardTest` | Badge name rendered via `+` operator (text node, no innerHTML); flip state toggled on hover |

Follow the existing Kotest browser-test pattern from `spec 002` (`ThemeModeTest`,
`BreakpointTierTest`). Tests run in headless Chrome via Karma.

### Screenshot Tests

| Test class | Baseline |
|------------|---------|
| `LandingPageScreenshotTest` | Above-fold hero, dark theme, desktop 1280px — Kotlin tab active |
| `LandingPageMobileScreenshotTest` | Above-fold hero, light theme, mobile 375px — hamburger visible |
| `GlobalNavBarScreenshotTest` | Navbar guest state (dark) + authenticated state (dark) |

Follow existing `ButtonScreenshotTest.kt` pattern (html2canvas bridge, pixel diff).

### Backend Tests

| Test class | What it tests |
|------------|---------------|
| `LandingRoutesTest` | `GET /api/v1/stats/landing` → 200, correct JSON envelope, `Cache-Control: public, max-age=300` |
| `LandingRateLimitTest` | 61st request in same window → 429 Too Many Requests |

Use `ktor-server-test-host` (already in catalog).

### Detekt

All new files must pass `./gradlew detekt` with zero violations.
No `@Suppress` annotations without a comment explaining why.
`CognitiveComplexMethod` threshold applies to animation code — split large timelines into
named helper functions if needed (e.g., `buildKotlinTabTimeline()`, `buildAndroidTabTimeline()`).

---

## Implementation Phases

### Phase 1 — Foundation (GSAP + ViewModel + Routing)

| Task | Description | Files |
|------|-------------|-------|
| T001 | Add `npm("gsap", "3.15.0")` to `webMain.dependencies`; add `ktor-server-rate-limit` to `libs.versions.toml` | `build.gradle.kts`, `libs.versions.toml` |
| T002 | `GsapInterop.kt`: `@JsModule("gsap")` Gsap external object + `@JsModule("gsap/ScrollTrigger")` external val | `gsap/GsapInterop.kt` |
| T003 | `ViewModel.kt`: abstract class with `viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)` and `onCleared()` | `core/ViewModel.kt` |
| T004 | `AppModule.kt`: Koin module — `single<HttpClient>`, `single<LandingStatsRepository>`, `factory<LandingViewModel>` | `di/AppModule.kt` |
| T005 | Update `App.kt`: `startKoin { modules(appModule) }` + Kilua routing DSL (`route("/")`, `route("*")` fallback) + register ScrollTrigger + `gsap.matchMedia()` reduced-motion guard | `App.kt` |
| T006 | Smoke-test GSAP in playground: `gsap.to()` on a div, verify JS + WASM both resolve the module | `PlaygroundRegistry.kt` |

### Phase 2 — Layout Shell

| Task | Description | Files |
|------|-------------|-------|
| T007 | `PageTransition.kt`: GSAP `from({opacity:0, y:12, duration:0.28})` on mount; respects reduced-motion guard | `layout/PageTransition.kt` |
| T008 | `GlobalNavBar.kt` — guest state: logo, nav links (Problems, Contests, Leaderboard), theme toggle (ThemeManager), language toggle (LocaleManager), Sign In / Sign Up buttons | `layout/GlobalNavBar.kt` |
| T009 | `GlobalNavBar.kt` — authenticated state: avatar, username, plan badge (Free/Pro), profile dropdown (7 items), animated hover underline indicator via GSAP | `layout/GlobalNavBar.kt` |
| T010 | `GlobalNavBar.kt` — role gate (`EXAM_CREATOR` / `ADMIN` → show Create Exam) + mobile hamburger + slide-in drawer | `layout/GlobalNavBar.kt` |
| T011 | `Footer.kt`: 3-column grid (Platform, Community/social, Legal), tagline, copyright; social icon hover bounce via GSAP | `layout/Footer.kt` |
| T012 | `NotFoundPage.kt`: minimal 404 with back-to-home link | `pages/NotFoundPage.kt` |
| T013 | `LandingPage.kt`: composes GlobalNavBar + PageTransition + all sections + Footer; owns LandingViewModel via `remember` + `DisposableEffect` | `pages/landing/LandingPage.kt` |

### Phase 3 — Data Layer + Hero Section

| Task | Description | Files |
|------|-------------|-------|
| T014 | `LandingStatsResponse.kt` (core:models, `@Serializable`); `LandingStats.kt` (domain model, webApp); `toDomainModel()` extension | `core/models/…/LandingStatsResponse.kt`, `model/LandingStats.kt` |
| T015 | `LandingStatsRepository` interface (app:shared, returns domain model); `LandingStatsRepositoryImpl` (webApp, Ktor Client, `.toDomainModel()`) | `app/shared/…/LandingStatsRepository.kt`, `…/LandingStatsRepositoryImpl.kt` |
| T016 | `LandingUiState.kt` + `StatsState` sealed class; `LandingViewModel.kt` extending `ViewModel` | `model/LandingUiState.kt`, `LandingViewModel.kt` |
| T017 | `HeroSection.kt` — static layout: left column (headline i18n, tagline i18n, two CTA buttons, `StatCounter` components), right column (tabbed editor panel); `StatCounter` shows shimmer skeleton when `StatsState.Loading`, "—" on `StatsState.Error` | `sections/HeroSection.kt` |
| T018 | Hero animated gradient mesh background (two/three GSAP yoyo blobs, `repeat:-1`) | `sections/HeroSection.kt` |
| T019 | Hero animation Phase A (Kotlin tab): typewriter stagger → compile bar scaleX → test rows slide-in → `elastic.out` emoji pop; GSAP timeline | `sections/HeroSection.kt` |
| T020 | Hero animation Phase B (Android tab): auto-switch at t=6s → injection typewriter → `💉` message fade-in → Android test rows → loop | `sections/HeroSection.kt` |
| T021 | Manual tab switch: kills current timeline, `gsap.set()` resets, starts selected phase sub-timeline | `sections/HeroSection.kt` |
| T022 | Stats counter roll-up: `gsap.to(el, { innerHTML: n, snap: "innerHTML", duration:1.8, onUpdate: formatNumber })`; fires from `LaunchedEffect` keyed on `StatsState.Loaded` | `sections/HeroSection.kt` |

### Phase 4 — Backend Stats Endpoint

| Task | Description | Files |
|------|-------------|-------|
| T023 | Add `ktor-server-rate-limit` dependency to `server:app` `build.gradle.kts` | `server/app/build.gradle.kts` |
| T024 | `LandingRoutes.kt`: `GET /api/v1/stats/landing` inside `rateLimit(RateLimitName("public"))` block; `Cache-Control: public, max-age=300`; hardcoded stats for v1 | `server/api/…/routes/LandingRoutes.kt` |
| T025 | `Application.kt`: `install(RateLimit)` with `"public"` bucket (60 req/min per IP); register `landingRoutes()` | `server/app/…/Application.kt` |

### Phase 5 — Scroll-Triggered Sections + i18n

| Task | Description | Files |
|------|-------------|-------|
| T026 | `BadgeDefinition.kt`: `object Badges` catalogue — 8 entries with `init`-block validation (id regex `^[a-z0-9-]+$`, iconClass regex); all labels via `i18n.tr()` | `model/BadgeDefinition.kt` |
| T027 | `PlaceholderData.kt`: `PlaceholderProblems` (5 rows) + `PlaceholderLeaderboard` (3 rows) static objects | `model/PlaceholderData.kt` |
| T028 | `ExamTypesSection.kt`: 3 cards with i18n labels + short code snippet illustration; ScrollTrigger `back.out(1.2)` fan-in (stagger 0.12s) | `sections/ExamTypesSection.kt` |
| T029 | `GamificationSection.kt`: tier bar (animated fill on scroll), badge grid (8 flip-cards, CSS 3D transform, GSAP class-swap on hover), trophy preview (2–3 rows) | `sections/GamificationSection.kt` |
| T030 | `ProblemsSection.kt`: 5-row table from `PlaceholderProblems`, ScrollTrigger slide-from-left (stagger 0.06s) | `sections/ProblemsSection.kt` |
| T031 | `LeaderboardSection.kt`: 3-row table + "you" row (when authed), score roll-up on scroll entry (`snap innerHTML`), link to full leaderboard | `sections/LeaderboardSection.kt` |
| T032 | `CreateExamSection.kt`: checklist items appear one-by-one (stagger 0.15s), dual-role CTA | `sections/CreateExamSection.kt` |
| T033 | `messages.pot` update: add all landing page translation keys; add EN translations; Persian keys stubbed as TODO | `resources/modules/i18n/messages.pot`, `messages-en.po` |

### Phase 6 — Responsive, Accessibility, Reduced-Motion

| Task | Description | Files |
|------|-------------|-------|
| T034 | Responsive audit: 375 px (mobile stacked hero, hamburger menu), 768 px (tablet), 1280 px (desktop side-by-side hero) — use existing `BreakpointTier` breakpoints | All section + layout files |
| T035 | Keyboard nav: Tab order correct on navbar, badge cards focusable with Enter-to-flip, dropdown navigable, CTAs reachable | `GlobalNavBar.kt`, `GamificationSection.kt` |
| T036 | ARIA: `aria-label` on icon buttons, `aria-current="page"` on active nav link, `role="menu"` + `aria-expanded` on profile dropdown, `aria-live="polite"` on stats counters | `GlobalNavBar.kt`, `HeroSection.kt` |
| T037 | Reduced-motion: verify `gsap.matchMedia()` skips all timelines and ScrollTrigger entrance animations; sections visible statically via CSS | All animated components |
| T038 | RTL audit (Persian locale): verify all spacing uses logical properties (`ms-*`, `ps-*`, `start-*`), no hardcoded `left`/`right` | All components |

### Phase 7 — Tests

| Task | Description | Files |
|------|-------------|-------|
| T039 | `LandingViewModelTest.kt`: `StatsState` Loading→Loaded, Loading→Error; scope cancelled on `onCleared()` | `webTest/…/landing/LandingViewModelTest.kt` |
| T040 | `LandingStatsRepositoryImplTest.kt`: mock `HttpClient`, verify `toDomainModel()` mapping correctness | `webTest/…/landing/LandingStatsRepositoryImplTest.kt` |
| T041 | `BadgeDefinitionTest.kt`: all 8 entries pass `init`-block validation; no invalid id/iconClass | `webTest/…/landing/BadgeDefinitionTest.kt` |
| T042 | `HeroSectionTest.kt`: skeleton shown on `Loading`; values rendered on `Loaded`; "—" shown on `Error`; tab switch updates active-tab state | `webTest/…/landing/HeroSectionTest.kt` |
| T043 | `GlobalNavBarTest.kt`: guest → Sign In/Up visible, avatar absent; authenticated → avatar + plan badge; `EXAM_CREATOR` → Create Exam visible | `webTest/…/landing/GlobalNavBarTest.kt` |
| T044 | `LandingRoutesTest.kt`: 200 + correct JSON envelope + `Cache-Control` header | `server/…/LandingRoutesTest.kt` |
| T045 | `LandingRateLimitTest.kt`: 61st request within 60s → 429 | `server/…/LandingRateLimitTest.kt` |
| T046 | Screenshot: `LandingPageScreenshotTest.kt` — above-fold, dark, 1280px; `LandingPageMobileScreenshotTest.kt` — above-fold, light, 375px | `webTest/…/screenshot/` |
| T047 | Verify Kover coverage does not drop below 90% after all new files: `./gradlew koverVerify` | CI |

---

## Complexity Tracking

No constitution violations. No complexity exceptions required.

---

## Key Dependencies Between Tasks

```
T001 (GSAP npm + rate-limit catalog)
  └─► T002 (GsapInterop.kt)
  └─► T023–T025 (backend rate limit)

T002 (GsapInterop)
  └─► T005 (App.kt: registerPlugin + matchMedia)
        └─► T006 (playground smoke test)
              └─► T018–T022 (hero animations)
              └─► T028–T032 (scroll sections)

T003 (ViewModel)
  └─► T004 (AppModule)
        └─► T016 (LandingViewModel)
              └─► T013 (LandingPage)

T007–T012 (layout shell)
  └─► T013 (LandingPage.kt)

T014 (LandingStatsResponse + domain model)
  └─► T015 (Repository interface + impl)
        └─► T016 (LandingViewModel)
              └─► T017–T022 (HeroSection)

T023–T025 (backend endpoint)
  └─► T022 (live counter roll-up — mock used until endpoint ready)

T026–T032 (scroll sections)
  └─► T039–T047 (tests)
```

---

## Custom Badge & Cup Icons

FontAwesome is **not used** for badges, cups, and tier icons. Custom SVG assets give
KodEx a unique visual identity that no other platform has.

### Design Tool Recommendation

**Figma** (free tier) is the primary tool. Workflow:
1. Design each badge as a `64×64` SVG in Figma with the badge shape (hexagon / circle /
   shield) + inner icon + colour gradient.
2. Export as optimised SVG with [SVGOMG](https://svgomg.net/) (removes metadata, reduces
   file size 40–60%).
3. Store in `app/webApp/src/webMain/resources/icons/badges/` as `{id}.svg`.
4. Load via Kilua's `img(src = "/icons/badges/kotlin-ninja.svg")` or inline as a Kotlin
   string constant for small icons.

For **animated badge unlocks** (when the user earns a badge), **Lottie** JSON animations
can be added in a future spec — `npm("lottie-web")` + `@JsModule("lottie-web")` interop.
This is deferred out of scope for the landing page (static display only here).

### Icon Asset Strategy

| Asset type | Format | Storage |
|------------|--------|---------|
| Badge icons (8) | SVG, 64×64 | `resources/icons/badges/` |
| Tier icons (4) | SVG, 32×32 | `resources/icons/tiers/` |
| Trophy / Cup icons (3 sizes) | SVG, 48×48 | `resources/icons/trophies/` |
| Navbar logo | SVG | `resources/icons/logo.svg` |

**`BadgeDefinition` change**: replace `iconClass: String` (FA class) with
`iconPath: String` (resource-relative path, e.g., `"icons/badges/kotlin-ninja.svg"`).
Validation in `init` block: `iconPath` matches `^icons/[a-z0-9/.-]+\\.svg$`.

---

## Out of Scope (deferred)

- Real leaderboard data (spec 010)
- Real problems catalogue data (spec 005)
- Auth state management (spec 004) — navbar reads from a `UserSession` interface with a stub implementation for now
- OAuth buttons on Sign In page (spec 004)
- Pro/Free plan logic (future billing spec)
