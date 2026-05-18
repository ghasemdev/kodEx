# Implementation Plan: Project Foundation & Developer Experience Setup

**Branch**: `feature/001-project-base-setup` | **Date**: 2026-05-18 | **Spec**: [spec.md](spec.md)

## Summary

Scaffold the complete multi-module Kotlin project as defined in Constitution §VII, with a
working hello-world endpoint on both frontend and backend, full quality tooling (Detekt +
Kover), a Docker Compose local-dev environment, and a GitHub Actions CI/CD pipeline. No
business logic is introduced; every module is created as a compilable skeleton with the
correct dependency graph and a passing test suite.

## Technical Context

**Language/Version**: Kotlin **2.3.21** (JVM + JS + WASM-JS + KMP targets); JDK **25** (LTS)

**Primary Dependencies**:
- Backend: Ktor **3.5.0**, Koin **4.2.1**, kotlinx.serialization **1.11.0**,
  kotlinx.coroutines **1.11.0**, kotlinx-datetime **0.8.0**, Exposed **1.3.0**,
  HikariCP **7.0.2**, Flyway **12.6.1**, Lettuce **7.5.2**, ktor-server-auth-jwt,
  argon2-jvm **2.12**, kotlin-logging **8.0.0** + Logback **1.5.32** +
  logstash-logback-encoder **9.0**
- Frontend: Kilua **0.0.34** (JS + WASM-JS targets), Koin **4.2.1** (koin-core),
  Ktor Client (JS/WASM), kotlinx.serialization **1.11.0**, Napier **2.7.1**,
  kotlinx-browser
- Sandbox Runner: Ktor **3.5.0**, docker-java **3.7.1**, kotlinx.serialization **1.11.0**,
  kotlin-logging + Logback
- Quality: Detekt **1.23.8** (+ detekt-formatting), Kover **0.9.8** (90% threshold)
- Testing: Kotest **6.1.11** (JVM modules), kotlin.test (shared),
  Testcontainers **2.0.5**; Gradle **9.5.1**

**Storage**: PostgreSQL + Redis — both in Docker Compose for local dev.
  Server skeleton does NOT connect to either DB (no business logic in this feature).

**Testing**: Kotest for all JVM modules; kotlin.test for app:shared; Kover for coverage
  reporting at **90% minimum threshold**; Testcontainers wired as a test dependency.

**Target Platform**: JDK 25 (LTS) for server + sandbox-runner; Kotlin/JS **and** WASM-JS
  for webApp (browser selects bundle at runtime based on WASM support detection).

**Project Type**: Multi-module Kotlin Multiplatform monorepo (web service + web frontend).

**Performance Goals**:
- SC-001: New developer up and running in < 5 minutes
- SC-003: CI pipeline completes full build-test-quality cycle in < 10 minutes
- Health endpoint: < 50 ms response (no DB calls)

**Constraints**:
- All secrets via environment variables (Constitution §XI)
- Gradle build reproducible and cacheable (configuration cache enabled)
- Zero Detekt violations on skeleton; zero Kover violations (≥ 90% on skeleton paths)
- `devMain` source set never compiled into production Docker image

**Scale/Scope**: Skeleton only. No business logic, no DB migrations, no auth flows.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Principle | Applicable | Status | Notes |
|---|---|---|---|
| I — Kotlin-First Stack | ✅ Yes | ✅ PASS | All modules in Kotlin; no other language |
| II — Dual Exam Modes | ➖ N/A | — | No exam logic in skeleton |
| III — Secure Sandbox | ✅ Partial | ✅ PASS | sandbox-runner module scaffolded; Dockerfiles versioned under `sandbox/`; no execution logic yet |
| IV — Test-Injection Grading | ➖ N/A | — | No grading logic |
| V — Role-Based Domain Model | ✅ Partial | ✅ PASS | `/api/v1/health` is intentionally public (health checks are a universal exception to auth gates); all future endpoints will declare roles |
| VI — Auditability & Observability | ✅ Partial | ✅ PASS | Structured JSON logging infrastructure configured (Logback + logstash-logback-encoder); no submission events yet |
| VII — Architecture & Module Conventions | ✅ Yes | ✅ PASS | Primary deliverable; full directory layout per constitution; build-logic convention plugins; Detekt configured |
| VIII — Authentication & Authorization | ➖ N/A | — | No protected endpoints in skeleton |
| IX — API Design Conventions | ✅ Yes | ✅ PASS | `/api/v1/health` follows prefix convention; error response plugin configured |
| X — Testing Policy | ✅ Yes | ✅ PASS | Kotest + Kover wired in all JVM modules; Testcontainers available as dependency |
| Secrets & Environment Policy | ✅ Yes | ✅ PASS | `.env.example` committed; `.env` in `.gitignore`; no hardcoded values |

**Gate result**: ALL PASS — proceed to Phase 0.

## Project Structure

### Documentation (this feature)

```text
specs/001-project-base-setup/
├── plan.md              ← this file
├── research.md          ← Phase 0 output
├── data-model.md        ← Phase 1 output
├── quickstart.md        ← Phase 1 output
├── contracts/
│   └── api-v1.md        ← Phase 1 output
└── tasks.md             ← /speckit-tasks output (not created by /speckit-plan)
```

### Source Code (repository root)

```text
root/
├── build-logic/                        # Convention plugins (shared Gradle config)
│   ├── build.gradle.kts
│   └── src/main/kotlin/
│       ├── kotlin-jvm-convention.gradle.kts
│       ├── kotlin-kmp-convention.gradle.kts
│       ├── ktor-service-convention.gradle.kts
│       └── detekt-convention.gradle.kts
│
├── gradle/
│   └── libs.versions.toml              # Central version catalog
│
├── app/
│   ├── shared/                         # KMP module (domain models, API contracts)
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── commonMain/kotlin/dev/kodex/shared/
│   │       └── commonTest/kotlin/dev/kodex/shared/
│   └── webApp/                         # Kotlin/JS + WASM-JS + Kilua frontend
│       ├── build.gradle.kts
│       ├── vite.config.ts              # Vite config; proxy /api → localhost:8080 in dev
│       └── src/
│           ├── commonMain/kotlin/dev/kodex/webapp/
│           │   ├── App.kt              # Kilua root component (Hello KodEx page)
│           │   └── store/AppStore.kt  # MVI store (placeholder)
│           ├── jsMain/kotlin/dev/kodex/webapp/
│           │   └── Main.kt            # JS entry point (~5 lines)
│           └── wasmJsMain/kotlin/dev/kodex/webapp/
│               └── Main.kt            # WASM-JS entry point (~5 lines)
│           # Runtime detection: index.html loader picks wasmJs if WebAssembly available,
│           # else falls back to js bundle transparently.
│
├── server/                             # Main backend API service
│   ├── app/                            # Composition root
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── main/kotlin/dev/kodex/server/
│   │       │   └── Application.kt     # Ktor engine, Koin DI wiring (production)
│   │       ├── dev/kotlin/dev/kodex/server/dev/
│   │       │   └── DevModule.kt       # Dev-only: seed routes, debug endpoints
│   │       └── main/resources/
│   │           ├── application.conf          # Base HOCON (production)
│   │           ├── application-dev.conf      # Dev overrides (local ports, mock flags)
│   │           ├── logback.xml               # JSON structured logging (production)
│   │           └── logback-dev.xml           # Coloured human-readable (dev)
│   ├── api/                            # HTTP routes, middleware
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── main/kotlin/dev/kodex/server/api/
│   │       │   └── routes/HealthRoutes.kt
│   │       └── test/kotlin/dev/kodex/server/api/
│   │           └── HealthRouteTest.kt  # Ktor test engine
│   ├── domain/                         # Use cases, repository interfaces (empty)
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/dev/kodex/server/domain/
│   └── data/                           # Repository implementations (empty)
│       ├── build.gradle.kts
│       └── src/main/kotlin/dev/kodex/server/data/
│
├── sandbox-runner/                     # Isolated code-execution service (skeleton)
│   ├── app/
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/dev/kodex/sandbox/
│   │       └── Application.kt
│   └── executor/
│       ├── build.gradle.kts
│       └── src/main/kotlin/dev/kodex/sandbox/executor/
│
├── core/                               # Cross-cutting: logging config, env helpers
│   ├── build.gradle.kts
│   └── src/main/kotlin/dev/kodex/core/
│       ├── logging/LoggingConfig.kt    # Logback + JSON encoder setup
│       └── env/EnvConfig.kt           # Env var loading with missing-key crash
│
├── config/
│   └── detekt/
│       └── detekt.yml                  # Project-wide Detekt rules
│
├── sandbox/                            # Sandbox Docker image templates (Constitution §III)
│   ├── kotlin/Dockerfile               # JVM sandbox image
│   └── android/Dockerfile             # Android build sandbox image (placeholder)
│
├── docker/
│   ├── server.Dockerfile               # Production image for server
│   ├── sandbox-runner.Dockerfile       # Production image for sandbox-runner
│   └── docker-compose.yml             # Local dev: server + webapp + postgres + redis
│
├── .github/
│   └── workflows/
│       └── ci.yml                      # Build + test + detekt on push and PR
│
├── .env.example                        # Required env vars with placeholder values
├── .gitignore                          # Includes .env
├── settings.gradle.kts                 # Root settings, module includes
├── build.gradle.kts                    # Root build: detekt + kover aggregation
└── README.md                           # Quick-start guide (mirrors quickstart.md)
```

**Structure Decision**: Constitution §VII layout followed exactly. `build-logic/` convention
plugins eliminate duplicated Gradle config across 9 submodules. `libs.versions.toml`
ensures identical versions everywhere. `devMain` source set on `server:app` keeps
dev-only code (seed data, debug routes) out of the production Docker image — the
`installDist` task only includes `main`. Frontend JS/WASM split handled by Kilua's
`dev.kilua` plugin generating both bundles; runtime `index.html` loader detects WASM
support and selects the bundle transparently.

## Complexity Tracking

No Constitution violations. No complexity justification required.
