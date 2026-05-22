# Data Model: Project Foundation & Developer Experience Setup

**Feature**: Project Base Setup | **Phase**: 1 | **Date**: 2026-05-18

---

## Scope Note

This feature introduces **no business entities** and **no database tables**. The PostgreSQL
and Redis instances are provisioned in Docker Compose and connection settings are prepared
as environment variables, but no schema is applied and no data is stored.

The first Flyway migration (`V1__init.sql`) is intentionally left for the next feature
that introduces actual domain data.

---

## Infrastructure Concepts (non-persistent)

These are operational concepts used by the skeleton — they are not database models.

### Service (runtime concept)

Represents a runnable component of the system.

| Attribute | Type | Notes |
|---|---|---|
| `name` | String | Human-readable name (e.g., `kodex-api`) |
| `status` | Enum: `UP` / `STARTING` / `DOWN` | Reported by the health endpoint |
| `version` | String | Application version (from build metadata) |
| `startedAt` | ISO-8601 timestamp | When the JVM process started |

Exposed only through the `/api/v1/health` response. Not persisted.

### QualityGate (build-time concept)

| Gate | Tool | Threshold | Failure behaviour |
|---|---|---|---|
| Static analysis | Detekt 1.23.8 | Zero violations | Build fails; PR blocked |
| Test coverage | Kover 0.9.8 | ≥ **90%** (configurable) | Build fails; PR blocked |
| Compilation | Gradle 9.5.1 | Zero errors/warnings | Build fails; PR blocked |

---

## Environment Configuration Schema

All configuration is supplied via environment variables (Constitution §XI). The following
lists every variable the skeleton requires; real values come from `.env` (never committed).

| Variable | Required | Default (dev) | Description |
|---|---|---|---|
| `SERVER_PORT` | No | `8080` | Port the Ktor server listens on |
| `SERVER_HOST` | No | `0.0.0.0` | Bind address |
| `WEBAPP_ORIGIN` | Yes (prod) | `http://localhost:5173` | Allowed CORS origin |
| `DB_HOST` | No | `localhost` | PostgreSQL host |
| `DB_PORT` | No | `5432` | PostgreSQL port |
| `DB_NAME` | No | `kodex` | PostgreSQL database name |
| `DB_USER` | Yes | — | PostgreSQL username |
| `DB_PASSWORD` | Yes | — | PostgreSQL password |
| `REDIS_HOST` | No | `localhost` | Redis host |
| `REDIS_PORT` | No | `6379` | Redis port |
| `REDIS_PASSWORD` | No | — | Redis password (empty = no auth) |
| `JWT_SECRET` | Yes (prod) | — | JWT signing secret (≥ 256 bits) |
| `SANDBOX_SHARED_SECRET` | Yes (prod) | — | Shared secret for server ↔ sandbox-runner |
| `LOG_LEVEL` | No | `INFO` | Root log level |

> **Skeleton note**: `DB_*`, `REDIS_*`, `JWT_SECRET`, and `SANDBOX_SHARED_SECRET` are
> wired into environment config helpers in this feature but **not actively used** until
> the features that need them are implemented. Missing secrets in development do NOT
> crash the server — only missing secrets required by the current feature's active code paths.

---

## State Transitions

None in this feature. The only lifecycle tracked is the health status of the running
service (UP/DOWN), which is derived from the JVM process state — not stored.
