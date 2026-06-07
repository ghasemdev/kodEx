# Technical Decisions (`docs/memory/`)

For governance-level decisions and project standards, see `.specify/memory/constitution.md`.

## Entry Lifecycle

```
Active → Needs Review → Superseded → (pruned)
```

---

### 2026-05-20 - D1: Use `kotlin.time.Instant` in Kotlin 2.3+, not `kotlinx.datetime.Instant`

**Status**: Active

**Why this is durable**
Every new module that works with `Instant` must use the correct import — the mistake only surfaces as a deprecation warning at compile time, not an error.

**Decision**
In Kotlin 2.3+, use `kotlin.time.Instant` (stdlib). Do not use `kotlinx.datetime.Instant`, which is deprecated. No extra dependency on `kotlinx-datetime` is required unless you need calendar-aware operations or `LocalDateTime` conversion.

**Evidence**
`HealthResponse.kt` in `core:models` and all date-aware models use `kotlin.time.Instant`.

**Tradeoffs**
- Gained: no extra dependency, stable stdlib API
- Made harder: converting to `LocalDateTime` still requires `kotlinx-datetime`

---

### 2026-05-23 - D3: CI benchmarks use `benchmarkFast`, not `benchmark`

**Status**: Active

**Why this is durable**
The kotlinx.benchmark `main` configuration runs full JMH warmup (many iterations × 5 s each × multiple JVM forks). On GitHub Actions this takes many minutes per PR. The `fast` configuration (1 iteration / 500 ms / 1 fork) is CI-appropriate.

**Decision**
- CI command: `./gradlew benchmarkFast benchmarkMerge -PbenchmarkConfig=fast`
- `benchmarkFast` is the root aggregation task defined in `benchmark-aggregation-convention`
- `-PbenchmarkConfig=fast` tells `benchmarkMerge` to scan `build/reports/benchmarks/fast/` (not `main/`)
- `benchmark` (full) is available for local profiling — do NOT use it in CI

KMP module fast task: `jvmBenchmarkFastBenchmark`. JVM module fast task: `jvmFastBenchmark`.

**Tradeoffs**
- Gained: CI benchmark job finishes in seconds, not minutes
- Lost: CI does not catch JMH warmup-sensitive regressions (acceptable — trend detection via `benchmark-action` still works)

---

### 2026-05-23 - D4: `detekt-convention` is the only correct way to wire Detekt — direct plugin application is prohibited

**Status**: Active

**Why this is durable**
Before this decision, `kotlin-kmp-convention` and `kotlin-jvm-convention` applied `id("io.gitlab.arturbosch.detekt")` directly, but never applied `detekt-convention`. This meant Detekt ran with default config — ignoring `config/detekt/detekt.yml`, producing no structured reports (html/xml/sarif), and missing `detekt-formatting`. The bug was silent: Detekt ran but applied no project rules.

**Decision**
`detekt-convention` is the single authority for Detekt setup. It:
1. Applies `id("io.gitlab.arturbosch.detekt")`
2. Sets `config.setFrom(rootProject.file("config/detekt/detekt.yml"))` + `buildUponDefaultConfig = true`
3. Configures all report formats via `tasks.withType<Detekt>().configureEach`
4. Adds `detektPlugins("detekt-formatting:VERSION")`

Both `kotlin-kmp-convention` and `kotlin-jvm-convention` apply `detekt-convention` — modules inherit it automatically.

**Never do**: `id("io.gitlab.arturbosch.detekt")` in a module build file or a convention other than `detekt-convention`.

---

### 2026-05-24 - D5: Design tokens expressed as Tailwind v4 `@theme` CSS variables

**Status**: Active

**Why this is durable**
Tailwind v4 uses CSS custom properties under `@theme` as the token layer — separate JS/TS token files (`theme.config.js` token extensions) are not needed. All tokens (colors, radius, spacing, font stacks) live in `tailwind.css` as `--color-*`, `--font-*`, etc., and Tailwind's engine references them directly. Keeping tokens in CSS avoids a JS/CSS split and makes them available via `var(--color-primary)` in both Tailwind utilities and custom CSS rules.

**Decision**
Express all design tokens as `@theme { --color-primary: …; }` declarations in `tailwind.css`. No separate JSON/TS/JS token file. Kotlin constants in `DesignTokens.kt` are optional aliases for compile-time safety.

**Tradeoffs**
- Gained: single source of truth, tokens usable in arbitrary CSS, Tailwind purge just works
- Made harder: Kotlin code cannot reference tokens at compile time without a separate mirror object

---

### 2026-05-24 - D6: Dev playground isolated via `import.meta.env.DEV` Vite tree-shaking

**Status**: Active

**Why this is durable**
The `webMain` source set is shared between JS and WASM-JS targets — there is no `webDevMain`/`devMain` equivalent for these targets. Vite's dead-code elimination on `import.meta.env.DEV` (which evaluates to `false` in production builds) is the standard mechanism to exclude dev-only code from the production bundle without a separate source set or Gradle module.

**Decision**
Guard all playground entry points with `if (js("import.meta.env.DEV"))` in Kotlin, which Vite strips in production webpack. Verified: `grep PlaygroundApp build/dist/js/productionExecutable/` returns no results after `jsBrowserProductionWebpack`.

**Tradeoffs**
- Gained: no extra Gradle module; playground code co-located with components it previews; HMR works seamlessly in dev
- Made harder: the guard is a runtime check (not compile-time), so playground code is compiled — only excluded from the production bundle by the bundler

---

### 2026-05-29 - D7: CSP must be in both `index.html` meta tag AND Ktor response headers

**Status**: Active

**Why this is durable**
In the KodEx architecture `index.html` is served by Vite (dev) and the webapp Docker service
(prod) — neither path goes through the Ktor API server. Ktor's `DefaultHeaders` plugin applies
only to JSON API responses, not to the HTML shell that boots the Kilua/JS app. A CSP set only
in Ktor is invisible to the browser when it loads `index.html`; a CSP set only in `index.html`
leaves API responses unprotected. Both layers are required and must be kept in sync.

**Decision**
Maintain CSP in two places simultaneously:
1. `app/webApp/src/webMain/resources/index.html` (both jsMain and wasmJsMain) —
   `<meta http-equiv="Content-Security-Policy" content="…">` and `<meta name="referrer" …>`
2. `server/app/src/main/kotlin/dev/kodex/server/Application.kt` — `install(DefaultHeaders)`
   block with `Content-Security-Policy`, `X-Content-Type-Options`, `X-Frame-Options`,
   `Referrer-Policy`, `Permissions-Policy`

Current policy: `default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline';
font-src 'self' data:; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none';`

`'unsafe-inline'` for `style-src` is required by Tailwind v4's utility-class runtime.

**Tradeoffs**
- Gained: defence-in-depth across both HTML document and API layer; correct in all environments
- Made harder: two declarations must stay manually in sync — if policy changes, both files update
- Reconsider when: a reverse proxy (nginx, Caddy) serves both HTML and API responses, allowing a single header location

---

### 2026-05-31 - D10: Unified `ApiEnvelope` in `core:models` — single source of truth for server+client

**Status**: Active

**Why this is durable**
Two `Envelope<T>` classes existed in parallel: one in `server/api/response/Envelope.kt` (JVM-only),
one in `core/models/api/ApiEnvelope.kt` (KMP). Any schema change (adding a meta field, renaming)
would require updating both. The KMP module is the correct single home.

**Decision**
`core/models` owns `ApiEnvelope<T>`, `ApiMeta`, `ApiErrorEnvelope` — all `@Serializable` KMP types.
`server/api/response/Envelope.kt` keeps only the two builder functions (`buildEnvelope`, `buildErrorEnvelope`)
that construct these types from server-side params (service name, version). No data class definitions
remain in the server module.

**Tradeoffs**
- Gained: one schema definition, both server and client use the same type; frontend can declare `ApiEnvelope<T>` without duplication
- Made harder: server now has a runtime dependency on `core:models` (already existed; no new dep added)
- Note: `ignoreUnknownKeys = true` on the client JSON config is still recommended as a defensive measure

---

### 2026-05-31 - D11: Koin Annotations (`@Single`, `@Factory`) + Compiler Plugin for DI

**Status**: Active

**Why this is durable**
DSL-based `module { single<X> { X() } }` requires manual wiring and has no compile-time validation.
The Koin Compiler Plugin (`io.insert-koin.compiler.plugin`) generates verified Koin module code from
annotations at compile time — missing bindings are caught before runtime.

**Decision**
- Library: `io.insert-koin:koin-annotations:2.3.1` (`koin-annotations` in catalog)
- Plugin: `io.insert-koin.compiler.plugin:1.0.0` (already applied to `server/app` and `app/webApp`)
- Annotate classes with `@Single(binds = [Interface::class])` / `@Factory`
- Provide third-party types (e.g., `HttpClient`) via `@Module` class with `@Single fun providerFn()` methods
- Root module uses `@Module(includes = [...]) @ComponentScan("dev.kodex.X")` — compiler auto-discovers all annotated classes
- App entry-point calls `startKoin { modules(AppKoinModule().module) }` (generated `.module` extension)

**Tradeoffs**
- Gained: compile-time binding validation, less boilerplate, consistent style across server and client
- Made harder: `@ComponentScan` scans only the current compilation unit's sources — cross-module classes must be included via `@Module(includes=[OtherModule::class])`

---

### 2026-05-31 - D12: Repository → RemoteDataSource separation for data layer

**Status**: Active

**Why this is durable**
Putting HTTP calls directly in `LandingStatsRepositoryImpl` conflates two concerns: "where do I get data"
(data source selection) and "how do I get it from the network" (HTTP transport). When a local cache is
added the repository would need to grow HTTP-awareness instead of delegating.

**Decision**
Three-layer data stack per feature:
1. `LandingStatsRemoteDataSource` (interface, webApp) — raw network contract, returns API response type
2. `LandingStatsRemoteDataSourceImpl` — Ktor Client impl, returns `LandingStatsResponse`
3. `LandingStatsRepositoryImpl` — selects source (remote only for v1), maps to domain model via `toDomainModel()`

The repository is the only layer visible to the ViewModel. The `toDomainModel()` mapping is a **private extension** inside `LandingStatsRepositoryImpl.kt` — not in `core:models` or `app:shared`, since mapping is an implementation detail of this specific repository.

**Tradeoffs**
- Gained: adding a `LandingStatsLocalDataSource` for caching only touches the repository; the remote impl and ViewModel are unaffected
- Made harder: one extra file per feature; acceptable given the payoff at cache-introduction time

---

### 2026-05-31 - D13: `_field` / `val field` StateFlow pattern is correct in Kotlin 2.3.21

**Status**: Active

**Why this is durable**
Kotlin 2.2 introduced "Explicit Backing Fields" (KEEP-0068) as an experimental feature. It is NOT stable
in 2.3.21. More importantly, it does NOT solve the private-mutable / public-immutable StateFlow split
cleanly, because the backing field is only accessible inside the property's getter/setter scope — not
from `init` blocks or other class methods where `update {}` calls live.

**Decision**
Keep the `_uiState` / `uiState` double-property pattern in all ViewModels:
```kotlin
private val _uiState = MutableStateFlow(SomeUiState())
val uiState: StateFlow<SomeUiState> = _uiState.asStateFlow()
```
Do NOT use experimental backing field syntax. This is the idiomatic Kotlin approach for 2.3.21 and remains
so until KEEP-0068 reaches stable status and covers this use case.

---

### 2026-06-07 - D15: `sanitizeRequestId()` is the only correct pattern for X-Request-Id

**Status**: Active

**Why this is durable**
Security Constitution §6 requires UUID validation before reflecting `X-Request-Id` into any
response body or log. The inline fallback `call.request.headers["X-Request-Id"] ?: Uuid.random().toString()`
is the **vulnerable pattern** — it was present at 3 call sites (LandingRoutes, HealthRoutes,
Application StatusPages) and removed in SEC-001 remediation (feature/003). Any new server
endpoint that echoes this header without the utility silently violates the constitution and
enables log injection (CWE-117).

**Decision**
Use `sanitizeRequestId()` from `server/api/src/main/kotlin/dev/kodex/server/api/util/RequestId.kt`
at every call site that reads and reflects `X-Request-Id`:

```kotlin
val requestId = sanitizeRequestId(call.request.headers["X-Request-Id"])
```

The utility rejects anything that does not match the UUID regex (case-insensitive, RFC 4122 format)
and replaces it with `Uuid.random()`. Both lowercase and uppercase UUIDs pass through unchanged.
Do not re-inline the `?: Uuid.random()` shorthand — it is the removed vulnerable form.

**Tradeoffs**
- Gained: log-injection prevention; constitution compliance at all server endpoints
- Made harder: new endpoint authors must know to import from `server:api` util — not `server:app`

---

### 2026-05-31 - D14: API route constants in `ApiRoutes` object

**Status**: Active

**Why this is durable**
Inline string literals for paths like `"/api/v1/stats/landing"` create a maintenance hazard: renaming
or versioning a route requires finding all string occurrences. Tests, data sources, and mocks should all
reference the same constant.

**Decision**
`app/webApp/src/webMain/kotlin/dev/kodex/webapp/network/ApiRoutes.kt` — a singleton `object ApiRoutes`
with a `const val` per endpoint. All Ktor Client call sites use `ApiRoutes.LANDING_STATS` etc.
Naming convention: `SNAKE_CASE` matching the resource name.

---

### 2026-05-20 - D2: kotlin-logging version must be `8.0.03`, not `8.0.0`

**Status**: Active

**Why this is durable**
Version `8.0.0` of `io.github.oshai:kotlin-logging-jvm` is not published to Maven Central. Gradle fails with a `MISSING` artifact error, and the error message is not obvious.

**Decision**
Pin kotlin-logging to `8.0.03` in `gradle/libs.versions.toml`. Before upgrading, verify the artifact exists on Maven Central before changing the version.

**Evidence**
`libs.versions.toml`: `kotlin-logging = "8.0.03"` (discovered after `8.0.0` caused a build failure).

**Tradeoffs**
- Made harder: the unusual patch version may cause confusion in future upgrades
