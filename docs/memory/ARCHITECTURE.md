# Architecture

Last reviewed: 2026-05-19

## System Overview

KodEx is a multi-service system running inside Docker Compose (single-host, v1). Three backend services + one frontend, sharing a PostgreSQL DB and Redis instance.

## Major Components

| Component | Module | Type | Role |
|---|---|---|---|
| `core` | `:core` | JVM | `EnvConfig`, `LoggingConfig` (MDC), cross-cutting JVM utilities |
| `core/models` | `:core:models` | **KMP** | Shared domain models, API contracts, validation — used by server AND all clients |
| `app/shared` | `:app:shared` | **KMP** | Client-side shared layer; re-exports `core:models`; future: split into ui/data/domain |
| `app/webApp` | `:app:webApp` | KMP JS/WASM | Kilua frontend |
| `server/api` | `:server:api` | JVM | Ktor routes, auth middleware, DTOs |
| `server/domain` | `:server:domain` | JVM | Use cases, repository interfaces |
| `server/data` | `:server:data` | JVM | Exposed ORM, Flyway, HikariCP, Redis (Lettuce) |
| `server/app` | `:server:app` | JVM | Ktor engine, Koin DI composition root |
| `sandbox-runner` | `:sandbox-runner:app` | JVM | Ktor service — only component with Docker socket |

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
