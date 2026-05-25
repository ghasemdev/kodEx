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
