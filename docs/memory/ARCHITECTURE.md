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

### 2026-06-07 - A5: Ktor HttpClient must not have a hardcoded base URL — relative paths only

**Status**: Active

**Why this is durable**
`install(DefaultRequest) { url("http://localhost:8080") }` is a dev shortcut that silently
misdirects every production API call to the visitor's local port 8080 instead of the backend.
This was SEC-003 in feature/003: all landing stats calls in production targeted users' own
machines. Removing this block also revealed that the Vite proxy rule must be explicit in
`build.gradle.kts` — without it, `/api/*` requests return 404 from Vite.

**Constraint**
1. `NetworkKoinModule.kt` MUST NOT install `DefaultRequest { url(...) }`. All `ApiRoutes`
   constants use relative paths (`/api/v1/...`).
2. `app/webApp/build.gradle.kts` MUST declare the dev proxy in the `vite { server { } }` block:
   ```kotlin
   proxy("/api", "http://localhost:8080")
   ```
   Vite uses this to forward `/api/*` → `localhost:8080` in development. In production, the
   same relative paths resolve as same-origin requests to the production backend.
3. If a non-Vite environment needs an explicit base URL, supply it via a `BuildConfig` constant
   from an env var — never hardcoded.

**Reconsider when**: A non-Vite build environment is introduced that cannot use a dev proxy.

---

### 2026-05-29 - A4: Frontend XSS boundary — text-node-only rendering in design system components

**Status**: Active

**Why this is durable**
The security model of the KodEx frontend relies on Kilua's `+` operator always producing DOM
text nodes (via `textContent`), never raw HTML. This is the structural XSS control for the
entire design system. Any deviation breaks the model and requires explicit security review.

**Constraint**
All design system components in `app/webApp/src/webMain/kotlin/.../design/components/` MUST
render user-controlled content exclusively via Kilua's `+` text-node operator. Prohibited
without explicit security review and CSP policy update:
- `element.innerHTML` / `element.outerHTML`
- `js("…innerHTML…")` or equivalent DOM write expressions
- Third-party Kilua plugins that render arbitrary HTML strings

API-bound data class fields that land in DOM attributes (`className`, `id`, `href`) MUST be
validated in the data class `init` block using `require()` against an allowlist or regex.
**Pattern**: `NavItem.key` and `NavItem.icon` in `NavBar.kt`.

**Reconsider when**: A rich-text or Markdown rendering component is required — that case
needs a sanitisation library (e.g., DOMPurify) and a CSP update to allow its hash.

---

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
