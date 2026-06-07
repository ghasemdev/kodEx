# Tasks: Landing Page (003)

**Branch**: `feature/003-landing-page` | **Spec**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)

**Format**: `- [ ] T### [P?] [US?] Description — file/path`
- **[P]** = parallelisable (no dependency on concurrent tasks)
- **[US#]** = user story this task belongs to
- **Tests requested**: yes (spec FR-031–FR-035, plan §Testing Strategy, coverage ≥ 90%)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Dependencies, interop layer, abstract ViewModel, Koin module, routing skeleton.
No user story work begins until this phase is complete.

- [x] T001 Add `npm("gsap", "3.15.0")` to `webMain.dependencies` block and add `ktor-server-rate-limit = { module = "io.ktor:ktor-server-rate-limit", version.ref = "ktor" }` entry to `gradle/libs.versions.toml` — `app/webApp/build.gradle.kts`, `gradle/libs.versions.toml`
- [x] T002 [P] Create `GsapInterop.kt`: `@JsModule("gsap") @JsNonModule external object Gsap` with `to`, `from`, `fromTo`, `timeline`, `set`, `registerPlugin`, `matchMedia` functions; `@JsModule("gsap/ScrollTrigger") @JsNonModule external val ScrollTrigger: dynamic` — `app/webApp/src/webMain/kotlin/dev/kodex/webapp/gsap/GsapInterop.kt`
- [x] T003 [P] Create abstract `ViewModel` base class with `viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)` and `fun onCleared() = viewModelScope.cancel()` — `app/webApp/src/webMain/kotlin/dev/kodex/webapp/core/ViewModel.kt`
- [x] T004 [P] Create `expect val ioDispatcher: CoroutineDispatcher` in `commonMain`; `actual` for `jsMain` and `wasmJsMain` = `Dispatchers.Default`; `actual` for `jvmMain` = `Dispatchers.IO` — `app/shared/src/commonMain/.../coroutines/AppDispatchers.kt` + actuals
- [x] T005 [P] Create generic `sealed class UiState<out T>` with `Loading`, `Success<T>(data: T)`, `Error(message: String?)` in `commonMain` — `app/shared/src/commonMain/.../ui/UiState.kt`
- [x] T006 Create `AppModule.kt` Koin module: `single<HttpClient>` (Ktor JS, ContentNegotiation json, defaultRequest with contentType), `single<LandingStatsRepository>` bound to `LandingStatsRepositoryImpl`, `factory<LandingViewModel>` — `app/webApp/src/webMain/kotlin/dev/kodex/webapp/di/AppModule.kt`
- [x] T007 Update `App.kt`: call `startKoin { modules(appModule) }` before `root()`; call `Gsap.registerPlugin(ScrollTrigger)`; add `gsap.matchMedia()` reduced-motion guard (`(prefers-reduced-motion: reduce)` → no-op context, `no-preference` → `setupAnimations()`); replace `div { +"Hello KodEx" }` with Kilua routing DSL (`route("/")` → `LandingPage()`, `route("*")` → `NotFoundPage()`) — `app/webApp/src/webMain/kotlin/dev/kodex/webapp/App.kt`
- [x] T008 Add GSAP smoke-test entry to `PlaygroundRegistry.kt`: a `div` that slides from `x:-100` to `x:0` via `gsap.to()` on mount — verify both JS and WASM targets load the module without error — `app/webApp/src/webMain/kotlin/dev/kodex/webapp/playground/PlaygroundRegistry.kt`

**Checkpoint**: `./gradlew :app:webApp:jsViteRun` starts without errors. Playground smoke-test div animates at `http://localhost:5173/playground`.

---

## Phase 2: Foundational (Blocking Shared Models)

**Purpose**: KMP models and enums used by multiple user stories. Must be complete before any story phase.

- [x] T009 [P] Create `LandingStatsResponse` `@Serializable` data class (`totalProblems`, `totalUsers`, `totalContests`: Int) in `core:models` — `core/models/src/commonMain/kotlin/dev/kodex/core/models/landing/LandingStatsResponse.kt`
- [x] T010 [P] Create `DifficultyTier` enum (`EASY`, `MEDIUM`, `HARD`) with `display` and `colorClass` fields; add `fun computeDifficulty(attemptCount: Int, successCount: Int): DifficultyTier` (≥60% → EASY, ≥25% → MEDIUM, else HARD; `attemptCount==0` → MEDIUM) — `core/models/src/commonMain/kotlin/dev/kodex/core/models/DifficultyTier.kt`
- [x] T011 [P] Create `ExamType` enum (`QUIZ`, `IO`, `INJECTION`) with `displayName: String` — `core/models/src/commonMain/kotlin/dev/kodex/core/models/ExamType.kt`
- [x] T012 [P] Create `Tier` enum (`JUNIOR`, `SENIOR`, `MASTER`, `GRANDMASTER`) with `displayName` and `colorToken` — `core/models/src/commonMain/kotlin/dev/kodex/core/models/Tier.kt`
- [x] T013 [P] Create `SessionState` sealed class (`Guest`, `Authenticated(userId, username, avatarUrl: String?, plan: Plan, roles: Set<UserRole>)`); `Plan` enum (`FREE`, `PRO`); `UserRole` enum (`PARTICIPANT`, `EXAM_CREATOR`, `ADMIN`) — `app/shared/src/commonMain/kotlin/dev/kodex/shared/session/SessionState.kt`
- [x] T014 [P] Create `LandingStats` domain model in `app:shared` (`dev.kodex.shared.landing`); `LandingStatsResponse.toDomainModel()` is a private extension inside `LandingStatsRepositoryImpl.kt` — `app/shared/src/commonMain/kotlin/dev/kodex/shared/landing/LandingStats.kt`
- [x] T015 [P] Create `LandingStatsRepository` interface in `app:shared`; create `LandingStatsRepositoryImpl` in webApp (`dev.kodex.webapp.pages.landing.data`) using Ktor Client with `withContext(ioDispatcher)` and `ApiEnvelope<LandingStatsResponse>` from `core:models`; `NetworkModule` updated with `ignoreUnknownKeys = true` — `app/shared/src/commonMain/kotlin/dev/kodex/shared/landing/LandingStatsRepository.kt`, `app/webApp/.../pages/landing/data/LandingStatsRepositoryImpl.kt`
- [x] T016 Create `LandingUiState` data class with `statsState: UiState<LandingStats> = UiState.Loading`; create `LandingViewModel` extending `ViewModel`, `init` launches `repository.fetchStats()` and updates state to `UiState.Success` or `UiState.Error`; `AppModule` wired with `single { LandingStatsRepositoryImpl(...) } bind LandingStatsRepository::class` and `factory { LandingViewModel(...) }` — `app/webApp/.../pages/landing/LandingUiState.kt`, `LandingViewModel.kt`

**Checkpoint**: `./gradlew :app:webApp:jsBrowserTest` — `LandingViewModelTest` Loading→Success and Loading→Error transitions pass; scope cancelled on `onCleared()`.

---

## Phase 3: US1 — Hero + Exam Types (First-Time Visitor Understands the Platform, P1) 🎯 MVP

**Goal**: Visitor lands, hero animation plays automatically (Kotlin tab then Android tab), exam types explained.
**Independent Test**: Open `http://localhost:5173`. Hero visible without scrolling; Kotlin tab typewriter plays; switches to Android tab ~6s; ExamTypes section visible below fold.

### Tests — US1

- [x] T017 [P] [US1] `HeroSectionTest`: verify `StatCounter` renders shimmer class when `UiState.Loading`; renders numeric text when `UiState.Success`; renders "—" when `UiState.Error`; active tab updates on click — `app/webApp/src/webTest/kotlin/dev/kodex/webapp/landing/HeroSectionTest.kt`
- [x] T018 [P] [US1] `LandingStatsRepositoryImplTest`: mock `LandingStatsRemoteDataSource` to return a fixture `LandingStatsResponse`; verify `.fetchStats()` returns the correct `LandingStats` domain model via `toDomainModel()`; delegate-count and zero-value tests — `app/webApp/src/webTest/kotlin/dev/kodex/webapp/landing/LandingStatsRepositoryImplTest.kt`

### Implementation — US1

- [x] T019 [US1] Create `HeroSection.kt` static layout: left column with `i18n.tr()`-wrapped headline, tagline, two CTA buttons ([Get Started], [Explore Problems]); right column Mac-style editor panel with functional window chrome (`PanelState`: Normal/TerminalClosed/Minimized/Maximized); `StatCounter` composable with shimmer skeleton (`stat-shimmer` class) when `value == null`, dash when `error = true`, numeric text otherwise; `overflow-clip` on panel fixes small-screen terminal overflow — `app/webApp/src/webMain/kotlin/dev/kodex/webapp/pages/landing/sections/HeroSection.kt`
- [x] T020 [US1] Hero background: two GSAP `gsap.to()` radial gradient divs (`hero-blob-1`, `hero-blob-2`) with `repeat:-1, yoyo:true, duration:10/12` at staggered offsets; colours `bg-primary/20` and `bg-secondary/15`; animation via `LaunchedEffect(Unit)` with null-guard — `app/webApp/src/webMain/kotlin/dev/kodex/webapp/pages/landing/sections/HeroSection.kt`
- [x] T021 [US1] Editor panel animation: Compose-state typewriter (`typedChars` per-char at 38ms); compile bar `scaleX: 0→1` via `gsap.to()`; three test rows fade in individually via `LaunchedEffect(visibleRows)` + `gsap.from(y=6, opacity=0)` at 320ms intervals; functional mac dots — red closes terminal (`TerminalClosed`), yellow minimizes (`Minimized`), green maximizes (`Maximized` → `flex-[2] lg:max-w-2xl`); `LaunchedEffect(panelState)` fires `gsap.from(scale=0.97, opacity=0.8)` on each state change — `app/webApp/src/webMain/kotlin/dev/kodex/webapp/pages/landing/sections/HeroSection.kt`
- [x] T022 [US1] Android tab animation: same Compose-state typewriter approach with Android code snippet; three Android test rows slide in; `LaunchedEffect(activeTab)` auto-switches back to Kotlin after `LOOP_PAUSE_MS` — `app/webApp/src/webMain/kotlin/dev/kodex/webapp/pages/landing/sections/HeroSection.kt`
- [x] T023 [US1] Manual tab switch: `onClick` on tab button updates `activeTab` Compose state → `LaunchedEffect(activeTab)` restarts animation from scratch (resets typedChars/showCompileStatus/showTestRows, new typing delay, then loops) — `app/webApp/src/webMain/kotlin/dev/kodex/webapp/pages/landing/sections/HeroSection.kt`
- [x] T024 [US1] Stats counter roll-up: `LaunchedEffect(statsState)` on `UiState.Success` runs 60-step coroutine loop over 1.8s updating `problemsDisplay`/`usersDisplay`/`contestsDisplay` state; Compose re-renders via text nodes (no innerHTML — security-safe) — `app/webApp/src/webMain/kotlin/dev/kodex/webapp/pages/landing/sections/HeroSection.kt`
- [x] T025 [P] [US1] Create `ExamTypesSection.kt`: three cards from `EXAM_CARDS` list (Quiz, I/O, Injection) each with icon, `i18n.tr()` title, description, code snippet; per-card `gsap.from(y=40, opacity=0, ease="back.out(1.2)")` with `scrollTrigger:{trigger, start:"top 82%", once:true}` and staggered delays — `app/webApp/src/webMain/kotlin/dev/kodex/webapp/pages/landing/sections/ExamTypesSection.kt`
- [x] T026 [US1] Create `LandingPage.kt`: `remember { KoinPlatform.getKoin().get<LandingViewModel>() }`, `DisposableEffect` with `onDispose { viewModel.onCleared() }`, `collectAsState()` on uiState, compose `HeroSection` + `ExamTypesSection` (GlobalNavBar/PageTransition/Footer deferred to Phase 4); `App.kt` routing wired `"/" → LandingPage()` — `app/webApp/src/webMain/kotlin/dev/kodex/webapp/pages/landing/LandingPage.kt`

**Checkpoint**: `./gradlew :app:webApp:jsViteRun` → `http://localhost:5173`; hero auto-plays both tabs; ExamTypes fan-in on scroll; counters skeleton then animate to numbers; T017–T018 tests pass.

---

## Phase 4: US5 — Navbar + Layout (Returning Logged-In User Navigates via Navbar, P2)

**Goal**: Navbar shows correct guest or authenticated state; profile dropdown; theme/lang toggles; page transitions.
**Independent Test**: Guest: Sign In/Up visible, no avatar. Authenticated stub: avatar + plan badge visible, dropdown opens with 7 items, Create Exam shown for EXAM_CREATOR. Theme and language toggles persist across reload.

### Tests — US5

- [x] T027 [P] [US5] `GlobalNavBarTest`: render with `SessionState.Guest` → verify Sign In/Sign Up visible, avatar absent, Create Exam absent; render with `SessionState.Authenticated(roles={PARTICIPANT})` → avatar+username+plan visible, Sign In absent, Create Exam absent; render with `roles={EXAM_CREATOR}` → Create Exam visible — `app/webApp/src/webTest/.../landing/GlobalNavBarTest.kt`
- [x] T028 [P] [US5] `LandingRoutesTest` (backend): `GET /api/v1/stats/landing` → 200, `Content-Type: application/json`, body contains `data.totalProblems`, `data.totalUsers`, `data.totalContests`, `Cache-Control: public, max-age=300` header present; uses `ktor-server-test-host` — `server/api/src/test/.../routes/LandingRoutesTest.kt`
- [x] T029 [P] [US5] `LandingRateLimitTest` (backend): send 61 identical requests to `GET /api/v1/stats/landing` within 1 minute; verify 61st response is `429 Too Many Requests` — `server/api/src/test/.../routes/LandingRateLimitTest.kt`

### Implementation — US5

- [x] T030 [US5] Create `PageTransition.kt`: `@Composable` wrapper `div` that fires `Gsap.from(el, {opacity:0, y:12, duration:0.28, ease:"power2.out"})` via `LaunchedEffect(Unit)` using a `ref` to the container element; skipped when reduced-motion — `app/webApp/.../layout/PageTransition.kt`
- [x] T031 [US5] Create `GlobalNavBar.kt` — guest state: logo SVG, nav links (Problems, Contests, Leaderboard) with i18n labels, theme toggle button (sun/moon icon → `ThemeManager.setTheme()`), language toggle button (EN/FA → `LocaleManager.setCurrentLocale()`), Sign In + Sign Up buttons; all text via `i18n.tr()`; animated hover underline indicator via `gsap.to(indicatorEl, {x: targetLeft, width: targetWidth, duration:0.2})` on `mouseenter` of nav links — `app/webApp/.../layout/GlobalNavBar.kt`
- [x] T032 [US5] Add `GlobalNavBar.kt` authenticated state: replace Sign In/Up with avatar image (`img` with `avatarUrl` or initials fallback circle with colour from `userId.hashCode()`), username, plan badge (`🆓 Free` / `⭐ Pro`); profile dropdown opens on avatar click with `gsap.from(dropdown, {scaleY:0, opacity:0, duration:0.18, transformOrigin:"top right"})`; dropdown contains My Profile, My Badges, My Trophies, My Stats, My Contests, Settings, Log Out (all i18n) — `app/webApp/.../layout/GlobalNavBar.kt`
- [x] T033 [US5] Add `GlobalNavBar.kt` role gate: Create Exam nav item rendered only when `session.roles.contains(EXAM_CREATOR) || session.roles.contains(ADMIN)`; mobile hamburger button at ≤768px with slide-in drawer using `gsap.from(drawer, {x:"-100%", duration:0.25})` — `app/webApp/.../layout/GlobalNavBar.kt`
- [x] T034 [US5] Add ARIA to `GlobalNavBar.kt`: `aria-label` on icon-only buttons (theme toggle, lang toggle, hamburger), `aria-current="page"` on active nav link, `role="menu"` + `aria-expanded` on profile dropdown, `aria-live="polite"` on plan badge — `app/webApp/.../layout/GlobalNavBar.kt`
- [x] T035 [US5] Create `Footer.kt`: 3-column grid using Tailwind `grid-cols-3 gap-8`; Platform column (Problems, Contests, Leaderboard, Create Exam), Community column (GitHub, Twitter/X, Telegram, Discord links with `target="_blank" rel="noopener"`), Legal column (About, Contact, Privacy Policy, Terms, Cookies); KodEx tagline + copyright; all links via `i18n.tr()`; social icon hover bounce via `gsap.to(icon, {scale:1.15, ease:"elastic.out(1,0.5)", duration:0.3})` — `app/webApp/.../layout/Footer.kt`
- [x] T036 [US5] Create `NotFoundPage.kt`: minimal 404 with i18n heading and back-to-home link — `app/webApp/.../pages/NotFoundPage.kt`
- [x] T037 [US5] Backend — add `ktor-server-rate-limit` to `server/app/build.gradle.kts` dependencies; in `Application.kt` add `install(RateLimit) { register(RateLimitName("public")) { rateLimiter(limit=60, refillPeriod=60.seconds); requestKey { call -> call.request.origin.remoteHost } } }` — `server/app/build.gradle.kts`, `server/app/src/main/kotlin/dev/kodex/server/Application.kt`
- [x] T038 [US5] Create `LandingRoutes.kt`: `get("/api/v1/stats/landing")` inside `rateLimit(RateLimitName("public"))`, sets `Cache-Control: public, max-age=300`, responds with `buildSuccessEnvelope(LandingStatsResponse(totalProblems=1247, totalUsers=8432, totalContests=342), requestId=call.requestId())`; register `landingRoutes()` in `Application.kt` routing block — `server/api/.../routes/LandingRoutes.kt`, `server/app/.../Application.kt`

**Checkpoint**: Navbar renders guest/authenticated correctly; profile dropdown animates open; theme+lang toggles persist on reload; `./gradlew :server:api:test` — T028 and T029 pass; T027 passes.

---

## Phase 5: US2 — Gamification (Visitor Explores Gamification, P2)

**Goal**: Tiers, badges (with 3D flip), and trophy preview all visible and interactive in the gamification section.
**Independent Test**: Scroll to gamification section → tier bar animates fill → badge grid appears (8 badges with custom SVG icons) → hover badge → card flips to show unlock condition → trophy rows visible. Leaderboard preview section renders below.

### Tests — US2

- [x] T039 [P] [US2] `BadgeDefinitionTest`: verify all 8 `BadgeDefinition` entries pass `init`-block validation (`id` regex `^[a-z0-9-]+$`, `iconPath` regex `^icons/[a-z0-9/.-]+\.svg$`); no duplicate ids; all fields non-blank — `app/webApp/src/webTest/.../landing/BadgeDefinitionTest.kt`

### Implementation — US2

- [x] T040 [US2] Create `BadgeDefinition.kt` data class with `init`-block validation; create `object Badges` with all 8 definitions (kotlin-ninja, android-architect, night-owl, steady-hand, bug-hunter, speed-demon, creative-chaos, hello-world) using `iconPath = "icons/badges/{id}.svg"`; all `name`, `description`, `unlockCondition` strings wrapped in `i18n.tr()` — `app/webApp/.../pages/landing/model/BadgeDefinition.kt`
- [x] T041 [US2] Add 8 placeholder badge SVG files (simple geometric shapes — hexagon + letter initial) to resources; these are placeholder assets to be replaced by Figma-designed SVGs — `app/webApp/src/webMain/resources/icons/badges/*.svg`
- [x] T042 [US2] Add 4 tier SVG icon placeholders and 3 trophy SVG placeholders to resources — `app/webApp/src/webMain/resources/icons/tiers/*.svg`, `resources/icons/trophies/*.svg`
- [x] T043 [P] [US2] Create `PlaceholderData.kt`: `object PlaceholderLeaderboard` with 3 `LeaderboardEntry` rows (rank, username, avatarUrl=null, tier, score, solved, streakDays) — `app/webApp/.../pages/landing/model/PlaceholderData.kt`
- [x] T044 [US2] Create `GamificationSection.kt`: section heading (i18n); tier bar with 4 tier labels + filled segment; badge grid (8 `BadgeCard` composables); trophy case preview (2–3 rows with SVG cup icon + contest name + date); all text via `i18n.tr()` — `app/webApp/.../pages/landing/sections/GamificationSection.kt`
- [x] T045 [US2] Add tier bar ScrollTrigger animation: on enter `gsap.to(fillBar, {width:"50%", duration:1.2, ease:"power2.inOut"})` (placeholder fill — real value comes from SessionState in spec 011); badge grid items `from({y:20, opacity:0, stagger:0.08, ease:"power1.out"})` — `app/webApp/.../pages/landing/sections/GamificationSection.kt`
- [x] T046 [US2] Add `BadgeCard` composable with CSS 3D flip on hover: front face (badge SVG icon + name), back face (unlock condition text, max 2 lines); CSS `transform-style:preserve-3d`, `rotateY(180deg)` transition 0.4s on `hover`; keyboard: `Enter`/`Space` toggles flip state via Compose `var flipped by remember` — `app/webApp/.../pages/landing/sections/GamificationSection.kt`
- [x] T047 [US2] Create `LeaderboardSection.kt`: header row + 3 placeholder rows from `PlaceholderLeaderboard`; avatar circle (null avatarUrl → initials); tier icon from SVG; score roll-up on scroll entry (`gsap.to(scoreEl, {innerHTML:n, snap:"innerHTML", duration:1.6, ease:"power1.out"})`); "you" row shown when `sessionState is Authenticated` (placeholder rank=48, stubbed); link to full leaderboard page — `app/webApp/.../pages/landing/sections/LeaderboardSection.kt`

**Checkpoint**: Gamification section scrolls into view with animations; badge hover flips; leaderboard scores count up; T039 passes.

---

## Phase 6: US3 — Problem Dataset (Visitor Discovers Problem Dataset, P3)

**Goal**: Problem sample section shows 5 rows with dynamic difficulty, type, title; links to /problems.
**Independent Test**: Scroll to problems section → 5 rows slide in from left → difficulty badges computed from solve rates → "Explore Problems" link present.

### Implementation — US3

- [x] T048 [P] [US3] Add `ProblemSummary` to `PlaceholderData.kt`: 5 entries with `id`, `title`, `type: ExamType`, `attemptCount`, `successCount`; verify `difficulty` computed property calls `computeDifficulty()` correctly — `app/webApp/.../pages/landing/model/PlaceholderData.kt`
- [x] T049 [US3] Create `ProblemsSection.kt`: section heading (i18n); filter hint chips (All / Kotlin / Android / Easy / Medium / Hard — decorative, no logic on landing); 5-row table with columns (title, type badge, difficulty badge); link to full problems page; ScrollTrigger `start:"top 85%", once:true` → `gsap.from(rows, {x:-20, opacity:0, stagger:0.06, ease:"power1.out"})` — `app/webApp/.../pages/landing/sections/ProblemsSection.kt`

**Checkpoint**: Problems section renders with correct difficulty labels computed from placeholder data; rows animate in; T017 (HeroSection) still passes after changes.

---

## Phase 7: US4 — Create Exam CTA (Educator / Organiser, P4)

**Goal**: Exam creation section visible; benefits listed with animated checkmarks; CTA navigates to sign-up or exam creation.
**Independent Test**: Scroll to Create Exam section → checklist items appear one by one → CTA button present and clickable.

### Implementation — US4

- [x] T050 [US4] Create `CreateExamSection.kt`: dark background block; heading (i18n); 5-item animated checklist (Three exam types, Automated grading, Real-time leaderboard, Badge rewards, Export results) — each item `gsap.from({opacity:0, x:-12, stagger:0.15, ease:"power2.out"})` on ScrollTrigger enter; dual CTA button row: primary "Create an Exam →" (→ `/signup` if guest, → `/exam/new` if EXAM_CREATOR/ADMIN), secondary "See Demo" (→ `/problems`) — `app/webApp/.../pages/landing/sections/CreateExamSection.kt`

**Checkpoint**: Create Exam section renders; checklist animates in; CTA is clickable; all US1–US4 sections visible in a single scroll from top to bottom.

---

## Phase 8: i18n, Responsive, Accessibility, Screenshot Tests

**Purpose**: Cross-cutting polish that applies to all user stories. Coverage target ≥ 90%.

- [x] T051 Add all landing page translation keys to `messages.pot` (section headings, button labels, badge names, tier names, exam type names, footer links, ARIA labels — minimum 60 new keys); add corresponding English translations to `messages-en.po`; stub Persian keys as `msgstr ""` in `messages-fa.po` — `app/webApp/src/webMain/resources/modules/i18n/messages.pot`, `messages-en.po`, `messages-fa.po`
- [x] T052 [P] Responsive audit: verify at 375px (mobile) — hero stacks vertically, hamburger visible, badge grid 2-column, problems table scrollable; at 768px (tablet) — hero side-by-side, navbar full; at 1280px (desktop) — full layout. Fix any layout issues using existing `BreakpointTier` — all section files
- [x] T053 [P] Keyboard navigation audit: Tab through entire page — navbar links, theme/lang toggles, hero CTAs, tab buttons in editor, badge cards (Enter to flip), leaderboard link, CTA buttons, footer links. Fix any un-focusable elements by adding `tabindex(0)` + `onKeydown` — `GlobalNavBar.kt`, `HeroSection.kt`, `GamificationSection.kt`, `Footer.kt`
- [x] T054 [P] Reduced-motion audit: in DevTools set `prefers-reduced-motion: reduce`; verify all GSAP `matchMedia` branches fire correctly — hero animation absent, all ScrollTrigger sections visible immediately, no layout shifts. Fix any GSAP calls outside the `no-preference` branch — all animated components
- [x] T055 [P] RTL audit (Persian locale): switch language to FA; verify all spacing uses logical Tailwind properties (`ms-*`, `ps-*`, `start-*`); no hardcoded `left-*`/`right-*`; RTL layout correct for navbar, hero, all sections — all components
- [x] T056 Screenshot test `LandingPageScreenshotTest.kt`: render full landing page (mock `UiState.Success` stats), capture above-fold hero at 1280px desktop dark theme with Kotlin tab active, commit PNG baseline — `app/webApp/src/webTest/.../screenshot/LandingPageScreenshotTest.kt`
- [x] T057 [P] Screenshot test `LandingPageMobileScreenshotTest.kt`: render landing page, capture above-fold at 375px mobile light theme, hamburger menu visible, commit PNG baseline — `app/webApp/src/webTest/.../screenshot/LandingPageMobileScreenshotTest.kt`
- [x] T058 [P] Screenshot test `GlobalNavBarScreenshotTest.kt`: capture guest state (dark) and authenticated state (dark, plan=FREE), commit PNG baselines — `app/webApp/src/webTest/.../screenshot/GlobalNavBarScreenshotTest.kt`
- [x] T059 Run `./gradlew :app:webApp:detekt` — fix all violations in new files. No `@Suppress` without a comment. Split any function exceeding `CognitiveComplexMethod` threshold (animation builders → named helpers) — all new files
- [x] T060 Run `./gradlew koverVerify` — confirm aggregate coverage ≥ 90% after all new code; add any missing tests to close gaps — CI validation

**Checkpoint**: All tests green, Detekt clean, Kover ≥ 90%, screenshots committed. `./gradlew :app:webApp:jsViteRun` → full animated landing page at `http://localhost:5173`.

---

## Dependencies & Execution Order

### Phase Dependencies

```
Phase 1 (Setup)
  └─► Phase 2 (Foundational) — BLOCKS everything
        └─► Phase 3 (US1 Hero) — MVP
        └─► Phase 4 (US5 Navbar) — can run in parallel with Phase 3
        └─► Phase 5 (US2 Gamification) — after Phase 3 LandingPage.kt exists (T026)
        └─► Phase 6 (US3 Problems) — independent after Phase 2
        └─► Phase 7 (US4 CTA) — independent after Phase 2
        └─► Phase 8 (Polish) — after all story phases
```

### User Story Dependencies

| Story | Depends on | Independent test |
|-------|------------|-----------------|
| US1 (P1) — Hero | Phase 2 | Hero animation plays, ExamTypes visible |
| US5 (P2) — Navbar | Phase 2 | Navbar renders correctly for guest + auth |
| US2 (P2) — Gamification | Phase 2, T026 (LandingPage shell) | Gamification section scrolls + animates |
| US3 (P3) — Problems | Phase 2 | Problems section rows visible + animated |
| US4 (P4) — Create CTA | Phase 2 | CTA section visible, checklist animates |

### Parallel Opportunities Within Phases

**Phase 2** (all independent, different files):
`T009 T010 T011 T012 T013 T014` → can run in parallel; then `T015 T016` sequentially after T014.

**Phase 3** (within US1):
`T017 T018` tests → write first; then `T019 T020 T021 T022` animation builders → `T023 T024` wiring; `T025 T026` after T019.

**Phase 4** (within US5):
`T027 T028 T029` tests first; then `T030 T031` layout → `T032 T033 T034` navbar features → `T035 T036 T037 T038` footer + backend.

**Phase 8** (all independent):
`T051 T052 T053 T054 T055 T056 T057 T058 T059 T060` can all run in parallel.

---

## Implementation Strategy

### MVP (Phase 1 + 2 + 3 only — User Story 1)

1. Complete Phase 1 (Setup)
2. Complete Phase 2 (Foundational models)
3. Complete Phase 3 (US1 — Hero section + ExamTypes)
4. **Stop and validate**: `jsViteRun` → hero plays, exam types visible, stats load
5. This alone is a shippable teaser page

### Full Landing Page

Complete phases 4–8 in any order after Phase 3 is done.
Stories US5 (Navbar) and US2 (Gamification) are the most impactful after MVP.

---

## Task Summary

| Phase | US | Tasks | Parallelisable |
|-------|----|-------|----------------|
| 1 Setup | — | T001–T008 | T002 T003 T004 T005 T006 |
| 2 Foundational | — | T009–T016 | T009–T014 |
| 3 US1 Hero | US1 | T017–T026 | T017 T018 T025 |
| 4 US5 Navbar | US5 | T027–T038 | T027 T028 T029 T030 T031 |
| 5 US2 Gamification | US2 | T039–T047 | T039 T043 |
| 6 US3 Problems | US3 | T048–T049 | T048 |
| 7 US4 CTA | US4 | T050 | — |
| 8 Polish | — | T051–T060 | T052–T060 |
| **Total** | | **60 tasks** | **~25 parallelisable** |
