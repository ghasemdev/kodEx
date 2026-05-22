# Research: Project Foundation & Developer Experience Setup

**Feature**: Project Base Setup | **Phase**: 0 | **Date**: 2026-05-18 (updated)

---

## Library Versions (pinned)

| Library                  | Version      | Artifact                                           |
|--------------------------|--------------|----------------------------------------------------|
| Kotlin                   | **2.3.21**   | `org.jetbrains.kotlin`                             |
| JDK                      | **21** (LTS) | Temurin / Eclipse Adoptium                         |
| Gradle                   | **9.5.1**    | build tool                                         |
| Ktor                     | **3.5.0**    | `io.ktor:ktor-server-core`                         |
| Kilua                    | **0.0.34**   | `io.kilua:kilua`                                   |
| Koin                     | **4.2.1**    | `io.insert-koin:koin-core`                         |
| Exposed                  | **1.3.0**    | `org.jetbrains.exposed:exposed-core`               |
| HikariCP                 | **7.0.2**    | `com.zaxxer:HikariCP`                              |
| Flyway                   | **12.6.1**   | `org.flywaydb:flyway-core`                         |
| Lettuce                  | **7.5.2**    | `io.lettuce:lettuce-core`                          |
| argon2-jvm               | **2.12**     | `de.mkammerer:argon2-jvm`                          |
| kotlin-logging           | **8.0.0**    | `io.github.oshai:kotlin-logging-jvm`               |
| Logback                  | **1.5.32**   | `ch.qos.logback:logback-classic`                   |
| logstash-logback-encoder | **9.0**      | `net.logstash.logback:logstash-logback-encoder`    |
| Napier                   | **2.7.1**    | `io.github.aakira:napier`                          |
| docker-java              | **3.7.1**    | `com.github.docker-java:docker-java`               |
| kotlinx.coroutines       | **1.11.0**   | `org.jetbrains.kotlinx:kotlinx-coroutines-core`    |
| kotlinx-datetime         | **0.8.0**    | `org.jetbrains.kotlinx:kotlinx-datetime`           |
| kotlinx.serialization    | **1.11.0**   | `org.jetbrains.kotlinx:kotlinx-serialization-json` |
| Kotest                   | **6.1.11**   | `io.kotest:kotest-runner-junit5`                   |
| Testcontainers           | **2.0.5**    | `org.testcontainers:testcontainers`                |
| Detekt                   | **1.23.8**   | `io.gitlab.arturbosch.detekt:detekt-gradle-plugin` |
| Kover (Gradle plugin)    | **0.9.8**    | `org.jetbrains.kotlinx.kover`                      |

---

## Decision 1 — Kotlin & JVM Version

**Decision**: Kotlin **2.3.21**, JVM target **JDK 21** (LTS)

**Rationale**: Kotlin 2.3.x is the current stable release on the K2 compiler. JDK 25 is
the latest Long-Term Support release (released September 2025, LTS cycle: 17 → 21 → 25).
Gradle 9.5.1 and Ktor 3.5.0 both require JDK 21+ as the minimum; JDK 21 LTS is the
forward-looking choice for a new project.

**Alternatives considered**:

- JDK 21 — still valid LTS but one cycle behind; no reason to start a new project on it.
- JDK 24 — non-LTS; avoid for production.

---

## Decision 2 — Gradle Version & Build Structure

**Decision**: Gradle **9.5.1** with **version catalog** (`gradle/libs.versions.toml`) and
**build-logic convention plugins** (`build-logic/` included module).

**Rationale**: Configuration cache is stable in Gradle 9.x, giving significant CI speedup.
Version catalog eliminates version drift across 9+ modules. Convention plugins
(`kotlin-jvm-convention`, `ktor-service-convention`, `detekt-convention`) share 50+ lines
of Gradle config without copy-paste.

---

## Decision 3 — Code Coverage: Kover 0.9.8 at 90%

**Decision**: **Kover** plugin version `0.9.8`; minimum line coverage threshold **90%**.

**Rationale**: Kover is JetBrains' first-party Kotlin coverage tool — correctly handles
inline functions, data class synthetic methods, and sealed class dispatchers that JaCoCo
misreports. Multi-module aggregation via root `koverReport` task.

**Threshold**: 90% (raised from common 80% default per project requirement). Applied on
the aggregated report; skeleton code must achieve 100% on the minimal hello-world paths.

---

## Decision 4 — Detekt 1.23.8 + detekt-formatting

**Decision**: Single `config/detekt/detekt.yml` at repository root applied via
`detekt-convention.gradle.kts` convention plugin. `detekt-formatting` integrates ktlint
rules for formatting without a separate ktlint step.

---

## Decision 5 — Kilua 0.0.34: JS + WASM-JS Targets with Browser Detection

**Decision**: Build **two targets** — `jsMain` (Kotlin/JS) and `wasmJsMain` (Kotlin/WASM-JS).
The HTML entry point includes a runtime loader that detects browser WASM support and loads
the appropriate bundle.

**Rationale**: WASM-JS executes significantly faster than Kotlin/JS for compute-heavy
frontend code. Modern browsers (Chrome 91+, Firefox 89+, Safari 15+) support WASM.
Older browsers or environments with restricted WASM (some corporate proxies) fall back
to the JS bundle transparently.

**Browser detection pattern** (in `index.html` or a small JS loader):

```js
(async () => {
    const wasmSupported =
        typeof WebAssembly !== 'undefined' &&
        typeof WebAssembly.instantiateStreaming === 'function';
    if (wasmSupported) {
        await import('./wasmJs/kodex-webapp.mjs');   // WASM bundle
    } else {
        await import('./js/kodex-webapp.js');         // JS fallback
    }
})();
```

**Source set layout** for `app/webApp`:

```
src/
├── commonMain/     # All application code (App.kt, MVI store, routes)
├── jsMain/         # JS entry point only (Main.kt calling startApplication)
└── wasmJsMain/     # WASM entry point only (Main.kt calling startApplication)
```

Most code lives in `commonMain`; `jsMain` / `wasmJsMain` contain only the platform entry
points (~5 lines each). Kilua `0.0.34` supports both targets via the `dev.kilua` plugin.

---

## Decision 6 — Dev/Prod Source Set Separation

**Decision**: Both server and frontend use dedicated source sets to ensure dev-only code
(seed data, debug routes, verbose logging) is **never compiled into the production binary**.

### Backend (`server/app`)

```
server/app/src/
├── main/           # Production code — included in Docker image
│   ├── kotlin/
│   └── resources/
│       ├── application.conf          # Base HOCON config (production values)
│       └── logback.xml               # Structured JSON logging (production)
└── dev/            # Dev-only code — excluded from production image
    ├── kotlin/
    │   └── dev/kodex/server/dev/
    │       └── DevModule.kt          # Dev routes (/dev/seed, /dev/reset)
    └── resources/
        ├── application-dev.conf      # Dev config overrides (local DB ports, etc.)
        └── logback-dev.xml           # Human-readable coloured logging (dev)
```

Gradle wires a `devRun` task that includes `devMain` on the classpath alongside `main`.
The production Docker `./gradlew installDist` compiles only `main` — `devMain` sources
are not on the production classpath.

### Frontend (`app/webApp`)

Vite handles dev/prod naturally:

- `jsBrowserDevelopmentRun` / `wasmJsBrowserDevelopmentRun` → Vite dev server, HMR, source maps, verbose logging
- `jsBrowserDistribution` / `wasmJsBrowserDistribution` → minified production bundle, no source maps

A Kilua/Kotlin `object Env` checks `js("import.meta.env.DEV")` to gate dev-only behaviour
(e.g., mock API responses, debug overlays).

---

## Decision 7 — Docker & Local Development Environment

**Decision**: `docker/docker-compose.yml` with services `server`, `webapp`, `postgres`,
`redis`; single `docker compose up --build` for full local dev stack.

**Port assignments**:
| Service | Port |
|---|---|
| server | 8080 |
| webapp (Vite) | 5173 |
| postgres | 5432 |
| redis | 6379 |

---

## Decision 8 — Health Endpoint Scope (dev + production)

**Decision**: `/api/v1/health` is a **production-grade** endpoint, not just a developer
convenience. See `contracts/api-v1.md` for full specification.

**Production consumers**:

- Docker `HEALTHCHECK` instruction in `docker/server.Dockerfile`
- Kubernetes liveness and readiness probes (when k8s is adopted in future)
- Load balancer health check (nginx / cloud LB)
- Uptime monitors (external)

**Implication**: The endpoint must be fast (< 50 ms), must not require authentication,
and must be excluded from rate limiting. In future features, a `/api/v1/health/detail`
(authenticated, admin-only) will expose dependency status (DB pool, Redis ping).

---

## Decision 9 — GitHub Actions CI/CD Structure

**Decision**: Single `.github/workflows/ci.yml` with jobs: `build` → `test` → `quality`.
Triggers: push to any branch + pull_request targeting `develop` or `main`.

**Caching**: Gradle caches on hash of `libs.versions.toml` + all `*.gradle.kts`.
**JDK**: `actions/setup-java` distribution `temurin`, `java-version: 21`.
**Branch protection**: Require all 3 jobs green before merge into `develop` and `main`.
