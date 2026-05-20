# Tasks: Project Foundation & Developer Experience Setup

**Feature**: `feature/001-project-base-setup` | **Spec**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)

**User Stories**: US1 (P1) — Run System · US2 (P2) — Quality Checks · US3 (P3) — CI/CD

## Format: `[ID] [P?] [Story?] Description — file path`

- **[P]**: Parallelizable (no shared file, no incomplete dependency)
- **[USN]**: Belongs to user story N

---

## Phase 1: Setup (Project Scaffolding)

**Purpose**: Skeleton directories, root Gradle files, secrets template — no business logic.

- [X] T001 Create top-level directory tree: `core/`, `core/models/`, `app/shared/`, `app/webApp/`, `server/app/`,
  `server/api/`, `server/domain/`, `server/data/`, `sandbox-runner/app/`, `sandbox-runner/executor/`, `config/detekt/`,
  `sandbox/kotlin/`, `sandbox/android/`, `docker/`, `.github/workflows/`
- [X] T002 Create `settings.gradle.kts` — `rootProject.name = "kodex"`,
  `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")`, `includeBuild("build-logic")`, include all 11 submodules (
  `:core`, `:core:models`, `:app:shared`, `:app:webApp`, `:server:app`, `:server:api`, `:server:domain`, `:server:data`,
  `:sandbox-runner:app`, `:sandbox-runner:executor`)
- [X] T003 Create `gradle/libs.versions.toml` — all 21 pinned versions from research.md (Kotlin 2.3.21, Ktor 3.5.0,
  Kilua 0.0.34, Koin 4.2.1, Exposed 1.3.0, HikariCP 7.0.2, Flyway 12.6.1, Lettuce 7.5.2, argon2-jvm 2.12, kotlin-logging
  **8.0.03** (8.0.0 not published), Logback 1.5.32, logstash-logback-encoder 9.0, Napier 2.7.1, docker-java 3.7.1,
  kotlinx.coroutines 1.11.0, kotlinx-datetime 0.8.0, kotlinx.serialization 1.11.0, Kotest 6.1.11, Testcontainers 2.0.5,
  Detekt 1.23.8, Kover 0.9.8, Gradle 9.5.1) — include all bundle and alias definitions
- [X] T004 Create `gradle/wrapper/gradle-wrapper.properties` — `distributionUrl` pointing to Gradle 9.5.1 bin; run
  `gradle wrapper` or create manually; commit wrapper JAR + scripts (`gradlew`, `gradlew.bat`)
- [X] T005 [P] Create `.gitignore` — entries: `.env`, `.gradle/`, `build/`, `*.class`, `local.properties`, `*.iml`,
  `.idea/`, `node_modules/`, `dist/`; create `.env.example` — all 14 env vars from data-model.md with placeholder values
  and inline comments

**Checkpoint**: `ls -R` shows all directories; `cat settings.gradle.kts` includes all modules.

---

## Phase 2: Foundational (Gradle Build System)

**Purpose**: Convention plugins + root build config. BLOCKS all user story work.

**⚠️ CRITICAL**: No module can be compiled until this phase is complete.

- [x] T006 Create `build-logic/build.gradle.kts` — Kotlin DSL; declare plugin dependencies from `libs.versions.toml`:
  Kotlin Multiplatform, Kotlin JVM, Detekt, Kover, Kilua (`dev.kilua`) gradle plugins
- [x] T007 [P] Create `build-logic/src/main/kotlin/kotlin-jvm-convention.gradle.kts` — apply `org.jetbrains.kotlin.jvm`;
  set `jvmTarget = "21"` and `languageVersion = "2.3"`; add kotlinx.serialization + coroutines as `api` dependencies;
  configure Kotest JUnit5 test runner; enable `useJUnitPlatform()`
- [x] T008 [P] Create `build-logic/src/main/kotlin/kotlin-kmp-convention.gradle.kts` — apply
  `org.jetbrains.kotlin.multiplatform`; configure JVM target (21), `js(IR) { browser() }`, `wasmJs { browser() }`
  targets; add kotlinx.serialization (common); configure `kotlin.test` for commonTest
- [x] T009 [P] Create `build-logic/src/main/kotlin/ktor-service-convention.gradle.kts` — applies
  `kotlin-jvm-convention`; adds Ktor server deps (`ktor-server-core`, `ktor-server-netty`,
  `ktor-server-content-negotiation`, `ktor-serialization-kotlinx-json`, `ktor-server-call-logging`,
  `ktor-server-auth-jwt`, `ktor-server-sse`, `ktor-server-status-pages`, `ktor-server-cors`); adds Koin (`koin-ktor`,
  `koin-logger-slf4j`); adds kotlin-logging + Logback + logstash-logback-encoder
- [x] T010 [P] Create `build-logic/src/main/kotlin/detekt-convention.gradle.kts` — apply `io.gitlab.arturbosch.detekt`;
  set config file to `rootProject.file("config/detekt/detekt.yml")`; add `detekt-formatting` plugin dependency;
  configure source sets to include `src/main/kotlin` and `src/dev/kotlin`
- [x] T011 Create `config/detekt/detekt.yml` — enable style, naming, complexity rule sets; disable rules that produce
  false positives on skeleton code (`UnnecessaryAbstractClass`, `ForbiddenComment`); `detekt-formatting` block mirrors
  ktlint defaults; zero violations expected on clean skeleton
- [x] T012 Create root `build.gradle.kts` — apply Kover plugin (`org.jetbrains.kotlinx.kover`); configure
  `koverReport { verify { rule { minBound(90) } } }` and `koverReport { defaults { mergeWith(subprojects) } }`; apply
  Detekt via `allprojects { apply(plugin = "detekt-convention") }`; register aggregated `detektAll` task

**Checkpoint**: `./gradlew help` and `./gradlew projects` succeed without errors; all subproject configurations are
visible.

---

## Phase 3: User Story 1 — Developer Runs the System (Priority: P1) 🎯 MVP

**Goal**: `docker compose up --build` brings up all services; browser shows "Hello KodEx"; `/api/v1/health` returns the
envelope response.

**Independent Test**: Clone repo on a fresh machine → follow README → open `http://localhost:5173` → see "Hello KodEx" →
run `curl -s http://localhost:8080/api/v1/health | jq` → see `{"data":{"status":"UP",...},"meta":{...}}`.

### core/ module

- [x] T013 [P] [US1] Create `core/build.gradle.kts` — apply `kotlin-jvm-convention`; no external dependencies (only
  stdlib + coroutines from convention)
- [x] T014 [P] [US1] Create `core/src/main/kotlin/dev/kodex/core/env/EnvConfig.kt` — `fun env(key: String): String`
  reads `System.getenv(key)`, throws `IllegalStateException("Missing required env var: $key")` if absent;
  `fun envOrNull(key: String): String?` for optional vars
- [x] T015 [P] [US1] Create `core/src/main/kotlin/dev/kodex/core/logging/LoggingConfig.kt` — `object LoggingConfig` with
  `fun setupMDC(requestId: String)` that puts `requestId` into Logback MDC; import `org.slf4j.MDC`

### core/models/ module (KMP) — shared domain models

- [x] T016 [US1] Create `core/models/build.gradle.kts` — apply `kotlin-kmp-convention`; add `kotlinx-datetime` to
  commonMain
- [x] T017 [P] [US1] Create `core/models/src/commonMain/kotlin/dev/kodex/core/models/health/HealthResponse.kt` —
  `@Serializable data class HealthResponse(val status: String, val startedAt: Instant)` using `kotlin.time.Instant` (
  kotlinx.datetime.Instant deprecated in Kotlin 2.3)

### app/shared/ module (KMP) — client-side shared layer

- [x] T016b [US1] Create `app/shared/build.gradle.kts` — apply `kotlin-kmp-convention`; add `api(projects.core.models)`
  to re-export shared models to all clients; no direct kotlinx-datetime dep (comes from core:models)

### server/ modules (domain → data → api → app)

- [x] T018 [P] [US1] Create `server/domain/build.gradle.kts` — apply `kotlin-jvm-convention`; add
  `implementation(projects.core.models)`; create `src/main/kotlin/dev/kodex/server/domain/.gitkeep` placeholder to
  satisfy Gradle source set
- [x] T019 [P] [US1] Create `server/data/build.gradle.kts` — apply `kotlin-jvm-convention`; add
  `implementation(projects.server.domain)`; add Exposed, HikariCP, Flyway, Lettuce from version catalog; create
  placeholder source file `src/main/kotlin/dev/kodex/server/data/.gitkeep`
- [x] T020 [US1] Create `server/api/build.gradle.kts` — apply `ktor-service-convention`; add
  `implementation(projects.server.domain)`, `implementation(projects.core.models)`, `implementation(projects.core)`; add
  Ktor test engine + Kotest to `testImplementation`
- [x] T021 [US1] Create `server/api/src/main/kotlin/dev/kodex/server/api/routes/HealthRoutes.kt` —
  `fun Routing.healthRoutes(startedAt: Instant, version: String)`: registers `get("/api/v1/health")` that responds with
  `{"data": HealthResponse(status="UP", startedAt=startedAt), "meta": {"requestId": ..., "timestamp": ..., "service": "kodex-api", "serviceVersion": version}}`;
  sets `Cache-Control: no-store` header; uses `dev.kodex.core.models.health.HealthResponse`
- [x] T022 [US1] Create `server/app/build.gradle.kts` — apply `ktor-service-convention`; add
  `implementation(project(":server:api"))`, `implementation(project(":server:domain"))`,
  `implementation(project(":server:data"))`, `implementation(project(":core"))`; configure `devMain` source set as a
  named source set on the `main` classpath via a `devRun` task; configure
  `application { mainClass = "dev.kodex.server.ApplicationKt" }`; configure `installDist` to exclude devMain
- [x] T023 [US1] Create `server/app/src/main/kotlin/dev/kodex/server/Application.kt` — `fun main()`: reads
  `SERVER_PORT`/`SERVER_HOST` from env via `EnvConfig`; starts `embeddedServer(Netty, port, host)`; installs plugins:
  `ContentNegotiation { json() }`, `CallLogging`, `CORS { allowHost(env("WEBAPP_ORIGIN")) }`,
  `StatusPages { exception<Throwable> { ... } }`; starts Koin with an empty `serverModule`; registers
  `healthRoutes(startedAt = Clock.System.now(), version = BuildConfig.VERSION)`
- [x] T024 [P] [US1] Create `server/app/src/main/resources/application.conf` — HOCON:
  `ktor.deployment.port = ${?SERVER_PORT}`, `ktor.deployment.host = ${?SERVER_HOST}`; create `application-dev.conf` with
  `include "application.conf"` + dev-specific overrides (log level DEBUG, port 8080)
- [x] T025 [P] [US1] Create `server/app/src/main/resources/logback.xml` — JSON appender using
  `net.logstash.logback.encoder.LogstashEncoder`; includes `requestId` MDC field; production format; create
  `server/app/src/dev/resources/logback-dev.xml` — `PatternLayoutEncoder` with coloured human-readable output for local
  dev
- [x] T026 [US1] Create `server/app/src/dev/kotlin/dev/kodex/server/dev/DevModule.kt` — dev-only Koin module + registers
  `get("/dev/ping") { call.respond("pong") }` debug route; only compiled when `devRun` task is used; NOT included in
  `installDist` production build

### sandbox-runner/ modules

- [x] T027 [P] [US1] Create `sandbox-runner/executor/build.gradle.kts` — apply `kotlin-jvm-convention`; add
  `docker-java` from version catalog; create `src/main/kotlin/dev/kodex/sandbox/executor/.gitkeep` placeholder
- [x] T028 [US1] Create `sandbox-runner/app/build.gradle.kts` — apply `ktor-service-convention`; add
  `implementation(project(":sandbox-runner:executor"))`; create
  `sandbox-runner/app/src/main/kotlin/dev/kodex/sandbox/Application.kt` — minimal Ktor server skeleton (port from env,
  single `/health` route returning 200)

### app/webApp/ module (Kilua JS + WASM-JS)

- [x] T029 [US1] Create `app/webApp/build.gradle.kts` — apply `kotlin-kmp-convention` + `dev.kilua` plugin; configure
  `js(IR) { browser { binaries.executable() } }` + `wasmJs { browser { binaries.executable() } }` targets; add Kilua to
  commonMain; add `ktor-client-js` + `ktor-client-wasm` per target; add Napier + kotlinx-browser to commonMain; add
  `implementation(projects.app.shared)`
- [x] T030 [P] [US1] Create `app/webApp/src/commonMain/kotlin/dev/kodex/webapp/App.kt` — Kilua root component:
  `@Composable fun App()` renders `div { +"Hello KodEx" }` (single structural placeholder page); create
  `app/webApp/src/commonMain/kotlin/dev/kodex/webapp/store/AppStore.kt` — empty MVI store skeleton (
  `sealed class Intent`, `data class State(val loading: Boolean = false)`)
- [x] T031 [P] [US1] Create `app/webApp/src/jsMain/kotlin/dev/kodex/webapp/Main.kt` — JS entry point:
  `fun main() { startApplication { App() } }` (~5 lines); create
  `app/webApp/src/wasmJsMain/kotlin/dev/kodex/webapp/Main.kt` — identical WASM-JS entry point
- [x] T032 [US1] Create `app/webApp/vite.config.ts` — proxy `/api` → `http://localhost:8080`; configure MIME type
  `application/wasm` for `.wasm` files; WASM-JS bundle in `wasmJs/` subdirectory

### Docker & Infrastructure

- [x] T033 [P] [US1] Create `sandbox/kotlin/Dockerfile` — `FROM eclipse-temurin:21-jre`; `--network=none`; read-only
  root FS mount guidance in comments; create `sandbox/android/Dockerfile` — placeholder with `FROM ubuntu:24.04` comment
  block (Android build sandbox, not implemented in this feature)
- [x] T034 [P] [US1] Create `docker/server.Dockerfile` — multi-stage: stage 1 `gradle:9.5.1-jdk25` runs
  `./gradlew :server:app:installDist` (only `main` source set, no devMain); stage 2 `eclipse-temurin:21-jre` copies
  `build/install/app/`; exposes 8080;
  `HEALTHCHECK --interval=10s CMD curl -f http://localhost:8080/api/v1/health || exit 1`; create
  `docker/sandbox-runner.Dockerfile` — same multi-stage pattern for `sandbox-runner:app`
- [x] T035 [US1] Create `docker/docker-compose.yml` — services: `postgres` (image: postgres:17, port 5432, env
  DB_USER/DB_PASSWORD/DB_NAME from .env), `redis` (image: redis:7-alpine, port 6379), `server` (build
  docker/server.Dockerfile, port 8080, env_file .env, depends_on postgres+redis, healthcheck), `webapp` (build via
  `./gradlew :app:webApp:jsBrowserDevelopmentRun` or separate Dockerfile, port 5173, depends_on server); all services
  log to stdout

### Documentation

- [x] T036 [US1] Create `README.md` — mirrors `specs/001-project-base-setup/quickstart.md`: prerequisites table (JDK 21,
  Docker), clone+configure, `docker compose -f docker/docker-compose.yml up --build`, verify (browser + curl), quality
  checks, frontend HMR, stop (`down -v`), troubleshooting table (port conflicts, missing JDK, first-build cache)

**Checkpoint**: `docker compose -f docker/docker-compose.yml up --build` → `http://localhost:5173` shows "Hello KodEx" →
`curl -s http://localhost:8080/api/v1/health | jq` returns
`{"data":{"status":"UP","service":"kodex-api",...},"meta":{"requestId":...}}`. US1 acceptance scenarios 1–3 all pass.

---

## Phase 4: User Story 2 — Developer Validates Code Quality Locally (Priority: P2)

**Goal**: `./gradlew build detekt` exits 0 on clean skeleton. Introducing a violation causes a non-zero exit with the
exact file and rule identified. Coverage report generated at 90%+ threshold.

**Independent Test**: Add `val unused = 1` to any file → `./gradlew detekt` exits non-zero citing the file + rule.
Revert → exits 0. Run `./gradlew test koverXmlReport` → `build/reports/kover/html/index.html` exists with ≥ 90%
coverage.

- [x] T037 [P] [US2] Create `server/api/src/test/kotlin/dev/kodex/server/api/HealthRouteTest.kt` — Kotest `FunSpec`; use
  Ktor `testApplication { application { ... } }`; test: `GET /api/v1/health` returns 200; response body deserializes to
  envelope; `data.status == "UP"`; `data.service == "kodex-api"`; `meta.requestId` is a valid UUID; `meta.timestamp` is
  a parseable ISO-8601 string; response header `Cache-Control` is `no-store`
- [x] T038 [P] [US2] Create `core/models/src/commonTest/kotlin/dev/kodex/core/models/health/HealthResponseTest.kt` —
  `kotlin.test` `@Test fun serializationRoundTrip()`: encode `HealthResponse` to JSON string, decode back, assert
  equality; verifies `@Serializable` annotation and `kotlin.time.Instant` serialization work correctly

**Checkpoint**: `./gradlew test koverXmlReport detekt` exits 0. Reports present: `build/reports/kover/html/index.html`,
`build/reports/detekt/detekt.html`. Coverage ≥ 90% (skeleton has minimal code, tests cover all paths). US2 acceptance
scenarios 1–3 all pass.

---

## Phase 5: User Story 3 — Automated CI/CD Validation (Priority: P3)

**Goal**: Every push triggers the pipeline; a failing test blocks PR merge; a green pipeline unblocks it. Pipeline
completes in < 10 minutes (SC-003).

**Independent Test**: Open a PR with `val unused = 1` → pipeline fails on `quality` job → merge button blocked. Revert →
pipeline passes → merge available.

- [x] T039 [US3] Create `.github/workflows/ci.yml` — trigger: `push` (all branches) + `pull_request` (targeting
  `develop` or `main`); jobs: **build** (`./gradlew assemble`), **test** (`./gradlew test koverXmlReport`, upload
  coverage artifact), **quality** (`./gradlew detekt`); each job: `runs-on: ubuntu-latest`, `actions/setup-java@v4` (
  distribution: temurin, java-version: 21), Gradle cache action keyed on
  `hash(gradle/libs.versions.toml, **/*.gradle.kts)`; `needs:` chain enforces build → test → quality order;
  `fail-fast: false` so all jobs report
- [x] T040 [US3] Append GitHub branch protection setup section to `README.md` — explain: Settings → Branches → Add rule
  for `develop` and `main`; require status checks `ci / build`, `ci / test`, `ci / quality`; require branches up to
  date; dismiss stale reviews on push; document that this must be configured by a repo admin after first successful
  pipeline run

**Checkpoint**: Push branch to GitHub → Actions tab shows 3 green jobs → PR shows all checks passing. Introduce a test
failure → `ci / test` shows red → merge blocked. US3 acceptance scenarios 1–3 all pass.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [x] T041 [P] Validate `quickstart.md` end-to-end on a fresh checkout — follow every step literally; update any step
  that no longer matches actual behaviour (port numbers, command output format, timing)
- [x] T042 [P] Run full quality suite from clean state: `./gradlew clean build detekt` — confirm no leftover debug code,
  no unused imports, no TODO comments leaked into production source sets; confirm `./gradlew :server:app:installDist`
  produces a runnable artifact without devMain code

---

## Dependencies & Execution Order

### Phase Dependencies

```
Phase 1 (Setup)
    ↓
Phase 2 (Foundational — Build System)          ← BLOCKS everything below
    ↓
Phase 3 (US1 — System)    Phase 4 (US2 — Quality)    Phase 5 (US3 — CI/CD)
    ↓                           ↓                          ↓
                        Phase 6 (Polish — all phases done)
```

- **Phase 1**: No dependencies — start immediately
- **Phase 2**: Depends on Phase 1 completion — BLOCKS all module compilation
- **Phase 3 (US1)**: Depends on Phase 2; internal order: `core/` + `core/models/` → `app/shared/` + `server/domain/` +
  `server/data/` → `server/api/` → `server/app/` → `app/webApp/` → Docker → README
- **Phase 4 (US2)**: Can start as soon as `server/api/` (T020–T021) is done — does NOT require Docker or webapp
- **Phase 5 (US3)**: Can start as soon as Phase 3 is complete — only needs passing tests + working build

### Within Phase 3 — Internal Dependencies

```
T013–T015 (core/)
    ↓
T016–T017 (core/models/ — KMP shared models)
    ↓
T016b (app/shared/)    T018 (server/domain/)      T027 (sandbox/executor/)
                    ↓               ↓
                T019 (server/data/) ↓
                            T020–T021 (server/api/)
                                    ↓
                            T022–T026 (server/app/)
                                    ↓
                T029–T032 (app/webApp/)    T028 (sandbox/app/)
                                    ↓
                        T033–T035 (Docker)
                                    ↓
                                T036 (README)
```

### Parallel Opportunities

**Phase 2** — T007, T008, T009, T010 all write different files → run in parallel after T006

**Phase 3 — Same level, different modules** (run in parallel after dependencies met):

```
Parallel group A (after Phase 2):  T013+T014+T015 (core/) ‖ T027 (sandbox/executor/)
Parallel group A2 (after T013):    T016+T017 (core/models/) → then T016b (app/shared/) ‖ T018 (server/domain/)
Parallel group B (after group A):  T018 (domain/) ‖ T019 (data/)
Parallel group C (after T020):     T024 (app.conf) ‖ T025 (logback.xml) ‖ T026 (DevModule.kt)
Parallel group D (after T023):     T030 (App.kt+AppStore.kt) ‖ T031 (Main.kt×2) ‖ T032 (vite.config.ts)
Parallel group E (after T028):     T033 (sandbox Dockerfiles) ‖ T034 (server/sandbox-runner Dockerfiles)
```

**Phase 4**: T037 (HealthRouteTest in server:api) ‖ T038 (HealthResponseTest in core:models) — different modules, run in
parallel

---

## Implementation Strategy

### MVP (User Story 1 Only)

1. Complete Phase 1: Setup (T001–T005)
2. Complete Phase 2: Foundational build system (T006–T012)
3. Complete Phase 3: Working system (T013–T036)
4. **STOP AND VALIDATE**: `docker compose up --build` → verify browser + API
5. Ship: developers can now clone and run the project

### Incremental Delivery

1. Phase 1 + 2 → Gradle builds → Foundation ready
2. Phase 3 → Working system → US1 ✅ (MVP)
3. Phase 4 → Quality tooling → US2 ✅
4. Phase 5 → CI/CD pipeline → US3 ✅
5. Phase 6 → Polish → Feature complete

### Parallel Strategy (if multiple developers)

After Phase 2 completes:

- **Dev A**: `core/` + `app/shared/` + `server/*` modules (T013–T026)
- **Dev B**: `app/webApp/` Kilua frontend (T029–T032, starts after T016–T017)
- **Dev C**: `sandbox-runner/` + Docker files + CI (T027–T028, T033–T035, T039–T040)

---

## Summary

| Phase            | Tasks          | User Story | Parallelizable   |
|------------------|----------------|------------|------------------|
| 1 — Setup        | T001–T005 (5)  | —          | T005             |
| 2 — Foundational | T006–T012 (7)  | —          | T007–T010        |
| 3 — US1 System   | T013–T036 (24) | US1 (P1)   | Groups A–E above |
| 4 — US2 Quality  | T037–T038 (2)  | US2 (P2)   | T037 ‖ T038      |
| 5 — US3 CI/CD    | T039–T040 (2)  | US3 (P3)   | —                |
| 6 — Polish       | T041–T042 (2)  | —          | T041 ‖ T042      |
| **Total**        | **42 tasks**   |            |                  |

**MVP scope**: Phases 1–3 only (T001–T036) → US1 complete and independently testable.
