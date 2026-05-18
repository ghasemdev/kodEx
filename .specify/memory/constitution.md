<!--
SYNC IMPACT REPORT
==================
Version change: 1.4.0 → 1.5.0
Type of bump: MINOR (unified API response envelope, bilingual error messages, DELETE behavior change)

Modified principles:
  - Principle IX (API Design Conventions): replaced flat error schema + "no envelope" rule
    with a unified success/error envelope; changed DELETE from 204 to 200 + data:null;
    added bilingual userMessage (en/fa) in errors; moved pagination into meta.pagination

Previous version notes (1.3.0 → 1.4.0):
  - Principle V (Role-Based Domain Model): added Exam State Machine (DRAFT/PUBLISHED/CLOSED)
  - Principle IX (API Design Conventions): added SSE async result delivery spec
  - Security & Execution Constraints: added CORS policy
  - Technology Stack: all library names now link to GitHub / official docs

Remaining TODOs:
  - TODO(RATIFICATION_DATE): Confirm exact project start date if different from 2026-05-17
  - TODO(KILUA_VERSION): Pin Kilua version once first frontend feature spec is authored
  - TODO(REDIS_USE_CASE): Redis confirmed for refresh tokens + session cache; decide if also used for submission queue/leaderboard before data-model spec
-->

# KodEx Constitution

## Core Principles

### I. Kotlin-First Stack

All application code — backend, web frontend, and evaluation tooling — MUST be written in Kotlin.
The backend MUST use Ktor. The web frontend MUST use Kotlin/JS.
No other server-side or frontend language is permitted without a formal constitution amendment.
Shared domain models (data classes, enums, sealed classes) SHOULD live in a `shared` Kotlin
Multiplatform module consumed by both backend and frontend.

**Rationale**: A single language across the full stack reduces cognitive overhead, enables code
sharing (especially domain models and validation logic), and keeps the hiring/onboarding
profile focused.

### II. Dual Exam Modes

The system MUST support exactly two exam modes:

- **Kotlin Mode**: The participant receives either (a) an input/output specification and writes
  a solution, or (b) a Kotlin file with a `main` function that they must complete. Grading is
  done by comparing stdout against expected output, or by running injected unit tests.
  
  An examiner MAY define test cases as **static** (fixed at exam-creation time) or **dynamic**
  (generated at evaluation time via a server-side generator function or parameterised template).
  Both variants are hidden from participants. A single exam MAY mix static and dynamic test
  cases. Dynamic test cases MUST be reproducible: the generator MUST accept a deterministic
  seed so the same inputs can be regenerated for dispute resolution.

- **Android Mode**: The participant receives a source-code scaffold (an incomplete Android
  project) with clearly marked `TODO` sections. They fill in the implementation. Grading is
  done via injected instrumented or unit tests that exercise the completed scaffold.

Every exam object MUST declare its `mode` field. No hybrid or mixed-mode exams are allowed in
v1. Adding a new mode requires a constitution amendment.

**Rationale**: Keeping two discrete, well-defined modes ensures the evaluation pipeline stays
focused and the sandbox infrastructure remains auditable and predictable. Dynamic test cases
allow examiners to prevent hard-coded solutions and to vary difficulty across exam sessions
without maintaining large banks of manually authored I/O pairs.

### III. Secure Sandbox Execution

All user-submitted code MUST be executed inside an isolated Docker container.
The sandbox container MUST:
- Have no network access.
- Have a hard CPU and memory limit (configurable per exam, defaulting to 2 CPUs / 512 MB RAM).
- Have a hard wall-clock timeout (configurable per exam, defaulting to 10 seconds).
- Mount only the submission files and injected tests — no host filesystem access.
- Be destroyed immediately after execution completes (no container reuse).

The sandbox orchestrator MUST run as a **separate service** (`sandbox-runner`) — a standalone
Ktor process with its own container (the only component with Docker socket access).
The main backend API communicates with `sandbox-runner` over an authenticated HTTP channel
(shared secret header). Direct execution of user code in the main API process or on the host
machine is strictly forbidden.

**Rationale**: Isolating Docker socket access to a single dedicated service limits blast radius
if the orchestration layer is compromised. It also allows `sandbox-runner` to be scaled,
restarted, or replaced independently from the main API.

### IV. Test-Injection Grading

Grading MUST be performed server-side. The backend injects test cases into the user's
submission before it enters the sandbox. The participant MUST NOT see the injected tests.

For Kotlin Mode:
  - **Static test cases**: pre-authored I/O pairs (stdin → expected stdout) or hidden unit
    tests stored in the exam record. Injected verbatim at grading time.
  - **Dynamic test cases**: generated immediately before sandbox launch by a server-side
    generator. The generator receives the exam-defined seed and returns a list of I/O pairs
    or a Kotlin test file. The generated inputs/outputs MUST be persisted alongside the
    submission record for auditability (see Principle VI).
For Android Mode: hidden instrumented/unit tests are merged with the scaffold before building.

The sandbox returns pass/fail/error results per test case to the backend. Scoring logic
(partial credit, weights, penalty) runs exclusively in the backend — never in the sandbox.

**Rationale**: Client-side or sandbox-side scoring is trivially bypassable. Keeping evaluation
logic in trusted server code ensures result integrity. Persisting generated test inputs
guarantees that disputes can be re-evaluated against the exact inputs the participant faced.

### V. Role-Based Domain Model

The system recognizes exactly two user roles:

- **Admin**: Can create, update, and publish exams; define exam mode, test cases, time limits,
  and scoring rules; view all submissions and results.
- **Participant**: Can browse and join published exams; submit solutions; view their own
  submission history and scores. MUST NOT see other participants' submissions or injected tests.

Role enforcement MUST happen at the backend API layer, not only in the UI. Every API endpoint
MUST declare which roles may access it. Unauthenticated requests to protected endpoints MUST
return HTTP 401; insufficient-role requests MUST return HTTP 403.

#### Exam State Machine

Every exam MUST be in exactly one of three states:

| State | Who can see | Participant can submit | Admin can edit |
|---|---|---|---|
| `DRAFT` | Admin only | No | Yes (full edit) |
| `PUBLISHED` | All authenticated users | Yes | **No** |
| `CLOSED` | All authenticated users | No | No |

Transition rules:
- `DRAFT → PUBLISHED`: admin explicit action. Irreversible without closing first.
- `PUBLISHED → CLOSED`: admin action OR automatic when a configured deadline passes.
- `CLOSED` is terminal — no transitions out. A corrected exam requires creating a new exam.
- Modifying test cases, time limits, or questions in `PUBLISHED` state is **forbidden**,
  even for admins. This protects fairness for participants who already submitted.
- Result visibility to participants is configured by admin and takes effect only after `CLOSED`.

**Rationale**: Confusing admin and participant access would compromise exam integrity (leaking
test cases) and participant privacy (exposing others' submissions).

### VI. Auditability & Observability

Every submission event — received, sandbox started, sandbox finished, scored — MUST be logged
with a correlation ID, timestamp, user ID, exam ID, and outcome.
Sandbox resource usage (CPU time, peak memory, exit code) MUST be captured per execution.
Logs MUST be structured (JSON) so they are machine-parseable.
Deletion of submission records or audit logs is PROHIBITED in production.

**Rationale**: Disputes over grading, cheating investigations, and infrastructure debugging
all require a complete, tamper-evident audit trail.

### VII. Architecture & Module Conventions

#### Directory Layout

The repository MUST follow this top-level structure:

```
root/
├── app/
│   ├── shared/          # KMP shared module — domain models, validation, API contracts
│   └── webApp/          # Kotlin/JS + Kilua frontend (v1 only client)
├── server/              # Main backend API service (Ktor JVM)
│   ├── :app             # Composition root — DI wiring (Koin), Ktor engine, plugin setup
│   ├── :api             # HTTP routes, middleware, auth, rate limiting, request/response DTOs
│   ├── :domain          # Use cases (interactors), repository interfaces, domain entities
│   └── :data            # Repository implementations (Exposed), DB schema, Flyway migrations, Redis client
├── sandbox-runner/      # Isolated code-execution service (separate Ktor process)
│   ├── :app             # Ktor entry point, authenticated HTTP API
│   └── :executor        # Docker container lifecycle, image management
└── core/                # Cross-cutting non-domain utilities (logging config, env helpers)
```

Future client modules (`androidApp/`, `iosApp/`, `desktopApp/`) are added directly under
`app/` when required. The `shared/` module MUST remain KMP-compatible at all times.

#### Backend Architecture — Clean Architecture

The `server/` service MUST follow Clean Architecture. Dependency direction is strictly
inward: `:api` → `:domain` ← `:data`. Neither `:domain` nor `shared/` may import `:data`
or `:api`.

| Module | Clean Arch Layer | Responsibilities |
|---|---|---|
| `server:api` | Presentation | Ktor routing, auth middleware, rate limiting, request validation |
| `server:domain` | Domain | Use cases, repository interfaces, domain entities (pure Kotlin) |
| `server:data` | Infrastructure | Exposed table definitions, Flyway migrations, HikariCP pool, Redis client |
| `server:app` | Composition | Koin DI modules, Ktor engine config, environment wiring |

#### Frontend Architecture — MVI

The `webApp/` module MUST follow MVI (Model-View-Intent). Kilua components are pure View;
they emit Intents and render State snapshots. No business logic lives in components.

| Layer | Responsibility |
|---|---|
| View | Kilua components — renders `State`, emits `Intent` |
| ViewModel / Store | Reduces `Intent` → `Action` → new `State`; calls use cases |
| Domain | Use cases imported from `app:shared`; no duplication |
| Data | Ktor Client HTTP calls to `server:api`; browser LocalStorage via `kotlinx-browser` |

#### Code Style

All Kotlin modules MUST use [**Detekt**](https://github.com/detekt/detekt) for static analysis. Configuration lives in
`config/detekt/detekt.yml` at the repository root. CI MUST fail on any Detekt rule
violation. The Detekt configuration MAY integrate `detekt-formatting` (ktlint rules)
to enforce consistent formatting without a separate ktlint pass.

**Rationale**: A single language + clean layering boundary prevents the codebase from
becoming a tangle of cross-cutting concerns as features are added. MVI aligns frontend
architecture with patterns familiar from Android/Compose, reducing context-switching cost.

### VIII. Authentication & Authorization

All protected API endpoints MUST require a valid **JWT access token** in the
`Authorization: Bearer <token>` header. Token issuance and validation MUST use the
`ktor-server-auth-jwt` Ktor plugin.

Token lifecycle:
- **Access token**: short-lived (15 minutes). Carries claims: `sub` (userId), `role`, `iat`, `exp`.
- **Refresh token**: longer-lived (7 days), stored server-side in Redis (revocable).
  Issued alongside the access token on login; exchanged for a new access token on expiry.
- Refresh tokens MUST be invalidated on explicit logout and on password change.

Password storage:
- All user passwords MUST be hashed with **Argon2id** (`argon2-jvm` library) before
  persistence. bcrypt (`jbcrypt`) is acceptable for migration paths but Argon2id is
  the required algorithm for new accounts.
- Plaintext, MD5, SHA-1, and unsalted SHA-256 storage are strictly forbidden.
- The JWT signing secret MUST be at minimum 256 bits, loaded from an environment variable
  (never hardcoded). See Principle XI.

**Rationale**: JWT enables stateless horizontal scaling of the API while the refresh-token
Redis store allows instant revocation without full statefulness. Argon2id is the
OWASP-recommended password hashing algorithm as of 2024 — it is memory-hard and resistant
to GPU-based brute force.

### IX. API Design Conventions

All backend HTTP endpoints MUST follow these conventions:

- **URL prefix**: every endpoint lives under `/api/v1/`. Future versions use `/api/v2/`, etc.
- **HTTP semantics**:
  - `POST` → 201 Created (resource created) or 200 OK (action without new resource)
  - `GET` → 200 OK
  - `PUT` / `PATCH` → 200 OK
  - `DELETE` → **200 OK** with `data: null` (see envelope below — 204 is not used)
  - Client errors → 4xx; server errors → 5xx

#### Unified Response Envelope

Every API response — success **and** error — MUST be wrapped in the following envelope.
The `data` field carries the resource (or `null` for DELETE / actions with no return value).
The `meta` field is always present and carries correlation and service information.

**Success response** (all 2xx responses):
```json
{
  "data": <resource or list or null>,
  "meta": {
    "requestId": "uuid-v4",
    "timestamp": "2026-05-19T10:00:00Z",
    "service": "kodex-api",
    "serviceVersion": "1.0.0"
  }
}
```

**Error response** (all 4xx and 5xx responses):
```json
{
  "data": {
    "code": "SNAKE_CASE_ERROR_CODE",
    "message": "Technical developer-facing description",
    "userMessage": {
      "en": "User-friendly English message",
      "fa": "پیام برای کاربر به فارسی"
    }
  },
  "meta": {
    "requestId": "uuid-v4",
    "timestamp": "2026-05-19T10:00:00Z",
    "service": "kodex-api",
    "serviceVersion": "1.0.0"
  }
}
```

`requestId` MUST match the correlation ID in the structured log entry for the same request
(see Principle VI). `message` is for developers/logs; `userMessage` is for UI display in both
supported languages (English and Persian).

**Paginated list response** — pagination metadata lives inside `meta.pagination`:
```json
{
  "data": [...],
  "meta": {
    "requestId": "uuid-v4",
    "timestamp": "2026-05-19T10:00:00Z",
    "service": "kodex-api",
    "serviceVersion": "1.0.0",
    "pagination": {
      "nextCursor": "opaque-cursor-string",
      "hasMore": true
    }
  }
}
```

Pagination uses **cursor-based** strategy for lists > 100 items. Query params: `cursor`, `limit`.
`pagination` key is omitted from `meta` when the response is not a paginated list.

#### Async Result Delivery

Submission grading is asynchronous (Docker execution time varies). The API MUST expose a
**Server-Sent Events (SSE)** stream for submission status updates:

```
GET /api/v1/submissions/{id}/status
Accept: text/event-stream
```

Event sequence: `QUEUED` → `RUNNING` → `SCORED` | `FAILED`

Each SSE event carries a JSON payload, e.g.:
```
data: {"status":"RUNNING"}
data: {"status":"SCORED","score":85,"passedTests":8,"totalTests":10}
```

The stream MUST close automatically when a terminal state (`SCORED` or `FAILED`) is reached.
Clients that reconnect after a terminal state MUST receive the final state immediately (no
re-execution). The Ktor `ktor-server-sse` plugin MUST be used for SSE support.

**Rationale**: SSE is simpler to implement and scale than WebSocket for this one-directional
use case, and is natively supported by browsers without extra libraries.

**Rationale**: Consistent API shape means the frontend and future clients can share a single
HTTP client layer. A stable error schema makes error handling predictable across all features.

### X. Testing Policy

Each module MUST have tests at the appropriate layer. Tests MUST NOT cross layer boundaries
via mocks — integration tests use real infrastructure.

| Module | Test type | Infrastructure |
|---|---|---|
| `server:domain` | Unit tests | None — pure Kotlin, zero I/O |
| `server:data` | Integration tests | Real PostgreSQL + Redis via [**Testcontainers**](https://github.com/testcontainers/testcontainers-java) |
| `server:api` | Integration tests | **Ktor test engine** + [Testcontainers](https://github.com/testcontainers/testcontainers-java) |
| `sandbox-runner` | Integration tests | Real Docker daemon (CI must have Docker) |
| `app:webApp` | Unit tests (MVI logic) | None; Kilua component tests if library supports |

Test framework: [**Kotest**](https://github.com/kotest/kotest) (Kotlin-native) for all JVM modules. `kotlin.test` for `app:shared`.
Repository interfaces in `:domain` MUST NOT be mocked in `:data` or `:api` tests — use the
real implementation against the Testcontainer instance.

**Rationale**: Mocking repository interfaces in integration tests has historically hidden
migration and query bugs that only surface in production. Testcontainers cost is low;
mock/prod divergence cost is high.

## Technology Stack Requirements

### Library Selection Policy

Prefer Kotlin libraries in this order:
1. Official JetBrains / Kotlin Foundation libraries (`kotlinx-*`)
2. High-star, actively maintained Kotlin-native libraries
3. Java/JVM libraries only when no suitable Kotlin alternative exists

### Backend (`server/`)

| Concern | Library |
|---|---|
| HTTP server | [**Ktor**](https://github.com/ktorio/ktor) (Kotlin JVM) |
| Dependency injection | [**Koin**](https://github.com/InsertKoinIO/koin) (`koin-ktor` + `koin-logger-slf4j`) |
| Serialization | [**kotlinx.serialization**](https://github.com/Kotlin/kotlinx.serialization) |
| Coroutines | [**kotlinx.coroutines**](https://github.com/Kotlin/kotlinx.coroutines) |
| Date/time | [**kotlinx-datetime**](https://github.com/Kotlin/kotlinx-datetime) |
| ORM / SQL DSL | [**Exposed**](https://github.com/JetBrains/Exposed) (JetBrains) |
| DB connection pool | [**HikariCP**](https://github.com/brettwooldridge/HikariCP) |
| DB migrations | [**Flyway**](https://github.com/flyway/flyway) (SQL-file versioned migrations under `server/data/src/resources/db/migration/`) |
| Primary database | **PostgreSQL** |
| Cache / session store | **Redis** ([**Lettuce**](https://github.com/lettuce-io/lettuce-core) coroutine API) |
| JWT authentication | [**ktor-server-auth-jwt**](https://ktor.io/docs/server-jwt.html) (Ktor official plugin) |
| Password hashing | [**argon2-jvm**](https://github.com/phxql/argon2-jvm) (Argon2id — primary); [`jbcrypt`](https://github.com/mindrot/jBCrypt) only for legacy migration paths |
| Logging | [**kotlin-logging**](https://github.com/oshai/kotlin-logging) + [**Logback**](https://github.com/qos-ch/logback) + [**logstash-logback-encoder**](https://github.com/logfellow/logstash-logback-encoder) for JSON output |

All backend modules MUST be written in Kotlin JVM. Java interop is permitted only for
libraries without a Kotlin-native equivalent (e.g., HikariCP, Logback, Flyway, argon2-jvm).

### Frontend (`app/webApp/`)

| Concern | Library |
|---|---|
| Language | Kotlin/JS |
| UI framework | [**Kilua**](https://kilua.dev/) ([GitHub](https://github.com/rjaros/kilua)) |
| Build / hot-reload | **Vite** via Kilua's Gradle plugin (`dev.kilua`) — HMR enabled in dev mode |
| Dependency injection | [**Koin**](https://github.com/InsertKoinIO/koin) (`koin-core` — KMP-compatible JS target) |
| HTTP client | [**Ktor Client**](https://github.com/ktorio/ktor) (JS/Fetch engine) |
| Serialization | [**kotlinx.serialization**](https://github.com/Kotlin/kotlinx.serialization) |
| Client-side logging | [**Napier**](https://github.com/AAkira/Napier) (KMP-native, browser console sink in JS) |
| Client-side storage | [`kotlinx-browser`](https://github.com/Kotlin/kotlinx-browser) LocalStorage wrappers |

### Shared Module (`app/shared/`)

A `shared/` Kotlin Multiplatform module MUST contain all domain models (data classes, enums,
sealed classes), validation logic, and API contract types used by both backend and frontend.
The shared module MUST NOT depend on any platform-specific library. Logging in shared code
is NOT permitted — log at call-site in the platform module.

### Sandbox Runner (`sandbox-runner/`)

| Concern | Library |
|---|---|
| HTTP server | [**Ktor**](https://github.com/ktorio/ktor) (Kotlin JVM) |
| Docker client | [**docker-java**](https://github.com/docker-java/docker-java) (Java lib — no Kotlin-native alternative) |
| Serialization | [**kotlinx.serialization**](https://github.com/Kotlin/kotlinx.serialization) |
| Logging | [**kotlin-logging**](https://github.com/oshai/kotlin-logging) + [**Logback**](https://github.com/qos-ch/logback) JSON |

### Future Client Targets (out of scope for v1)

The codebase SHOULD be structured to allow future addition of Android, iOS, and desktop
clients via Kotlin Multiplatform + Compose Multiplatform. No KMP Compose code should be
written in v1; the `shared/` module must remain KMP-compatible to keep this path open.

### Infrastructure

- **Sandbox**: Docker. Dockerfile templates per exam mode MUST be versioned under `sandbox/`.
- **Container Orchestration**: Docker Engine (single-host) for v1. Kubernetes is out of scope unless amended.
- **Build Tool**: Gradle (Kotlin DSL) for all modules.
- **Code Quality**: Detekt (see Principle VII). Configuration in `config/detekt/detekt.yml`.
  CI pipeline MUST run `./gradlew detekt` and fail on violations.

## Security & Execution Constraints

- Submitted code MUST be scanned for known dangerous patterns (e.g., `Runtime.exec`,
  `ProcessBuilder`, reflection-based class loading) before entering the sandbox. Suspicious
  submissions MUST be rejected with a clear error, not silently failed.
- The sandbox image MUST be rebuilt and re-audited whenever the base JDK or Android build
  tools version changes.
- Password storage: see Principle VIII (Argon2id required).
- JWT signing: see Principle VIII (256-bit minimum secret, env-var loaded).
- All inter-service communication (backend ↔ sandbox orchestrator) MUST occur over
  authenticated channels (shared secret header), never open HTTP.
- Rate limiting MUST be applied to the submission endpoint and the auth endpoints
  (login, token refresh). Limits are configurable per exam/environment.
- **CORS**: The API server MUST configure CORS to allow requests only from the known webapp
  origin. In development: `http://localhost:5173`. In production: the deployed webapp domain
  (injected via environment variable `WEBAPP_ORIGIN`). All other origins MUST be rejected.
  Credentials (cookies/auth headers) MUST be allowed on permitted origins.

### Secrets & Environment Policy

- Every secret (database passwords, JWT signing secret, Redis password, sandbox shared
  secret) MUST be supplied via **environment variables**. No secret may appear in source
  code, Gradle build files, or committed configuration files.
- A `.env.example` file MUST be committed to the repository listing every required
  environment variable with a placeholder value and a comment describing its purpose.
  The actual `.env` file MUST be in `.gitignore`.
- Production code MUST NOT provide default/fallback values for secrets. Missing secrets
  at startup MUST cause immediate application termination with a clear error message.
- Secrets MUST NOT be logged, even at DEBUG level.

## Governance

This constitution supersedes all informal decisions, README notes, and verbal agreements.
Any principle in this document takes precedence over implementation convenience.

**Amendment procedure**:
1. Open a pull request with the proposed change and a written rationale.
2. Increment `CONSTITUTION_VERSION` according to semantic versioning (see header).
3. Update `LAST_AMENDED_DATE` to the merge date.
4. The PR description MUST include a Sync Impact Report covering affected templates and
   follow-up TODOs.
5. Merging the PR constitutes ratification; no separate approval step is required for a
   single-maintainer project (revisit when the team grows).

**Compliance review**: Every feature plan (`plan.md`) MUST include a "Constitution Check"
section that explicitly gates the plan against all active principles before implementation begins.
A feature plan that skips or voids this gate MUST NOT be merged.

**Versioning policy**:
- MAJOR: Removing or fundamentally redefining an existing principle.
- MINOR: Adding a new principle or materially expanding guidance.
- PATCH: Clarifications, wording fixes, non-semantic refinements.

**Version**: 1.5.0 | **Ratified**: 2026-05-17 | **Last Amended**: 2026-05-19
