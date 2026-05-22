# Worklog

Concise high-value entries only. This is NOT a changelog.
Only record durable lessons — what future work should know, not what was done.

---

### 2026-05-23 - W2: Build system & CI/CD refactor — convention composition + benchmark fast mode

**Branch**: `feature/001-project-base-setup` (commit `9b9c9968`)

**What future work needs to know**:

**Convention composition pattern** — all build config now flows through composed conventions. The hierarchy is: `benchmark-convention` + `detekt-convention` → `kotlin-kmp-convention` / `kotlin-jvm-convention` → `ktor-service-convention`. Never add Detekt, benchmark, or allOpen config directly to a module build file.

**CI benchmark is now fast** — `./gradlew benchmarkFast benchmarkMerge -PbenchmarkConfig=fast` runs in seconds (1 iter / 500 ms / 1 fork). The full `benchmark` task is for local profiling only. KMP guard task names: `jvmBenchmarkBenchmark` (main), `jvmBenchmarkFastBenchmark` (fast). JVM guard task names: `jvmBenchmark`, `jvmFastBenchmark`.

**OWASP convention** — `dependency-check-convention` owns OWASP config. The plugin artifact (`org.owasp:dependency-check-gradle`) must be in `build-logic/build.gradle.kts` as an `implementation()` dep, not just as a `libs.plugins.*` entry. Required secret: `NVD_API_KEY`.

**Root build.gradle.kts is intentionally thin** — only 3 `apply false` version-pins (compose, compose-compiler, kilua) + 3 convention applies + `kover()` module inclusions. Resist the urge to add config directly here; it belongs in a convention.

**KMP benchmark guard was missing** — before this refactor, modules using `kotlin-kmp-convention` with no benchmark sources would fail JMH with "No benchmarks to run". Both conventions now have `afterEvaluate { onlyIf("has benchmark sources") { ... } }` guards for both main and fast benchmark tasks.

---

### 2026-05-20 - W1: Feature 001 complete — KodEx project foundation

**Milestone**: `feature/001-project-base-setup` → ready for merge to `develop`

**What future work needs to know**:
- Gradle convention plugins live in `build-logic/` — before adding a dependency to a module, check the relevant convention plugin first; it may already provide the dependency transitively
- `core:models` is the single source of truth for shared domain models — no other module should define models that cross the client/server boundary
- `WEBAPP_ORIGIN` is a required runtime env var — it has a placeholder in `.env.example` but must be set correctly in any deployed environment
- The CI pipeline now has 6 jobs with `assemble` as the common gate: `test` (push), `coverage` (PR), `benchmark` (PR, fast mode), `detekt` (PR), `dependency-check` (PR→main + weekly)
- The Vite dev server runs inside Docker Compose via a Node image and proxies `/api` — for local HMR development, running `./gradlew :app:webApp:jsBrowserDevelopmentRun` directly is faster and avoids the Docker overhead
