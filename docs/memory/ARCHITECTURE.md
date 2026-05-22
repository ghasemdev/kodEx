# Architecture

Last reviewed: 2026-05-20

## System Overview

KodEx is a multi-service system running inside Docker Compose (single-host, v1). Three backend services + one frontend, sharing a PostgreSQL DB and Redis instance.

## Major Components

| Component | Module | Type | Role |
|---|---|---|---|
| `core` | `:core` | JVM | `EnvConfig`, `LoggingConfig` (MDC), cross-cutting JVM utilities |
| `core/models` | `:core:models` | **KMP** | Shared domain models, API contracts — used by server AND all clients |
| `app/shared` | `:app:shared` | **KMP** | Client-side shared layer; re-exports `core:models`; future: split into ui/data/domain |
| `app/webApp` | `:app:webApp` | KMP JS/WASM | Kilua frontend |
| `server/api` | `:server:api` | JVM | Ktor routes, auth middleware, DTOs |
| `server/domain` | `:server:domain` | JVM | Use cases, repository interfaces |
| `server/data` | `:server:data` | JVM | Exposed ORM, Flyway, HikariCP, Redis (Lettuce) |
| `server/app` | `:server:app` | JVM | Ktor engine, Koin DI composition root |
| `sandbox-runner` | `:sandbox-runner:app` | JVM | Ktor service — only component with Docker socket access |

## Boundaries

```
core:models (KMP)
    ↑ api()           ↑ implementation()
app:shared        server:domain
    ↑                     ↑
app:webApp         server:api → server:data
                         ↑
                     server:app (composition root)
```

- `server/api` → `server/domain` ← `server/data` (Clean Architecture — domain has no outward deps)
- `server:domain` and `app:shared` MUST NOT import each other — both import from `core:models`
- `sandbox-runner` ↔ `server/app` via HTTP + shared secret header (no direct DB access)
- Frontend ↔ API via `/api/v1/` REST + SSE

## Integrations

- **PostgreSQL** (via Exposed + HikariCP): primary store for users, exams, submissions
- **Redis** (via Lettuce): refresh token store + session cache
- **Docker Engine**: sandbox-runner manages container lifecycle via `docker-java`
- **Vite**: frontend build + HMR proxy (`/api` → `http://localhost:8080`)

## Durable Constraints

### 2026-05-23 - A3: Convention plugin composition — do not add build config directly to module files

**Status**: Active

**Why this is durable**
The `build-logic/` convention hierarchy is the single source of truth for all build config. Bypassing it by adding plugins directly to module `build.gradle.kts` files causes silent divergence (e.g., no detekt config, wrong JMH settings, missing guards).

**Constraint**
Every capability (benchmark, detekt, kover, allOpen) has an owning convention plugin. Module build files MUST only apply the appropriate top-level convention (`kotlin-kmp-convention`, `kotlin-jvm-convention`, or `ktor-service-convention`) and declare their own inter-module `dependencies {}`. Any change to quality tooling, benchmark config, or compiler options goes into the relevant convention, not individual modules.

Full hierarchy: see `§VII — Architecture & Module Conventions` → Convention Plugin Map.

**Special case**: `kover(projects.*)` module inclusions cannot be inside a convention plugin because `projects.*` typesafe accessors are unavailable in `build-logic`. These stay in root `build.gradle.kts`.

**Reconsider when**: Gradle stabilises typesafe accessors in composite builds.

---

### 2026-05-20 - A1: Type-Safe Project Accessors are mandatory — string literals are prohibited

**Status**: Active

**Why this is durable**
String-based `project(":path:module")` fails at configuration time, not compile time, and is not refactor-aware. This mistake occurred once in `sandbox-runner/app/build.gradle.kts`.

**Constraint**
All inter-module dependencies must use type-safe accessors:
- ✅ `projects.sandboxRunner.executor`
- ❌ `project(":sandbox-runner:executor")`

`enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")` is active in `settings.gradle.kts`. The rule is codified in §VII of the constitution.

**Reconsider when**: Gradle stabilises this feature and changes the syntax.

---

### 2026-05-20 - A2: `devMain` source set — dev-only code is excluded from the production artifact

**Status**: Active

**Why this is durable**
Any future feature that needs dev-only behaviour (seed data, debug routes, verbose logging) must follow this pattern — placing dev code in `main` means it ships to production.

**Constraint**
In `server:app`:
- `src/dev/kotlin/` — included only in the `devRun` task classpath
- `src/dev/resources/` — `logback-dev.xml` (human-readable, coloured output)
- `installDist` excludes this source set entirely

Pattern: `devMain` source set extends `main` classpath, activated by a `devRun` task, excluded from `distributions`.

## Risks / Complexity Hotspots

- Sandbox container lifecycle: timeout handling, cleanup on crash, resource limit enforcement
- SSE stream management: terminal state detection, reconnect behaviour, auth on stream
- Exam state machine enforcement: concurrent state transitions, deadline-triggered CLOSED

## Keep Here

- Stable module boundaries and dependency directions
- Integration constraints that affect multiple features
- Known complexity hotspots

## Never Store Here

- Implementation details of individual tasks
- Stale diagrams (keep diagrams current or remove them)
