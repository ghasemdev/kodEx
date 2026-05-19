# Architecture

Last reviewed: 2026-05-19

## System Overview

KodEx is a multi-service system running inside Docker Compose (single-host, v1). Three backend services + one frontend, sharing a PostgreSQL DB and Redis instance.

## Major Components

| Component | Module | Role |
|---|---|---|
| `server/app` | `:server:app` | Main Ktor API — auth, exam CRUD, submission intake, SSE delivery |
| `server/api` | `:server:api` | Route definitions, request/response models |
| `server/domain` | `:server:domain` | Business logic, use cases |
| `server/data` | `:server:data` | Exposed ORM, Flyway migrations, HikariCP pool |
| `sandbox-runner` | `:sandbox-runner:app` | Ktor service — only component with Docker socket |
| `app/webApp` | `:app:webApp` | Kilua (Kotlin/JS + WASM-JS) frontend |
| `app/shared` | `:app:shared` | KMP shared module — domain models, API contracts, validation |
| `core` | `:core` | Cross-cutting: `EnvConfig`, `LoggingConfig`, MDC setup |

## Boundaries

- `server/api` → `server/domain` ← `server/data` (Clean Architecture — domain has no outward deps)
- `app/webApp` → `app/shared` ← `server/api` (shared module is the contract layer)
- `sandbox-runner` ↔ `server/app` via HTTP + shared secret header (no direct DB access from sandbox-runner)
- Frontend ↔ API via `/api/v1/` REST + SSE (no direct DB/Redis access from frontend)

## Integrations

- **PostgreSQL** (via Exposed + HikariCP): primary store for users, exams, submissions
- **Redis** (via Lettuce): refresh token store + session cache
- **Docker Engine**: sandbox-runner manages container lifecycle via `docker-java`
- **Vite**: frontend build + HMR proxy (`/api` → `http://localhost:8080`)

## Risks / Complexity Hotspots

- Sandbox container lifecycle: timeout handling, cleanup on crash, resource limit enforcement
- SSE stream management: terminal state detection, reconnect behavior, auth on stream
- Exam state machine enforcement: concurrent state transitions, deadline-triggered CLOSED

## Keep Here

- Stable module boundaries and dependency directions
- Integration constraints that affect multiple features
- Known complexity hotspots

## Never Store Here

- Implementation details of individual tasks
- Stale diagrams (keep diagrams current or remove them)
