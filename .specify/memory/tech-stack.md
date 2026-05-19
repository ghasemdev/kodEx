# Technology Stack Requirements

## Library Selection Policy

Prefer Kotlin libraries in this order:
1. Official JetBrains / Kotlin Foundation libraries (`kotlinx-*`)
2. High-star, actively maintained Kotlin-native libraries
3. Java/JVM libraries only when no suitable Kotlin alternative exists

## Backend (`server/`)

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

## Frontend (`app/webApp/`)

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

## Shared Data Module (`core/data/`) — KMP

`core:data` is the **cross-server-and-client KMP module**. It MUST contain all domain models
(data classes, enums, sealed classes), validation logic, and API contract types used by
**both the backend and any client** (web, Android, iOS, desktop).
This module MUST NOT depend on any platform-specific library. Logging is NOT permitted here.

**Dependency rule**: `server:domain` → `core:data` ← `app:shared` ← `app:webApp`

## Client Shared Module (`app/shared/`) — KMP

`app:shared` is the **client-side shared layer**. It re-exports `core:data` via `api()` and
adds any client-specific shared models or logic. It MUST NOT be imported by server modules.
It may later be split into `app:shared:ui`, `app:shared:data`, `app:shared:domain`.

## Sandbox Runner (`sandbox-runner/`)

| Concern | Library |
|---|---|
| HTTP server | [**Ktor**](https://github.com/ktorio/ktor) (Kotlin JVM) |
| Docker client | [**docker-java**](https://github.com/docker-java/docker-java) (Java lib — no Kotlin-native alternative) |
| Serialization | [**kotlinx.serialization**](https://github.com/Kotlin/kotlinx.serialization) |
| Logging | [**kotlin-logging**](https://github.com/oshai/kotlin-logging) + [**Logback**](https://github.com/qos-ch/logback) JSON |

## Future Client Targets (out of scope for v1)

The codebase SHOULD be structured to allow future addition of Android, iOS, and desktop
clients via Kotlin Multiplatform + Compose Multiplatform. No KMP Compose code should be
written in v1; the `shared/` module must remain KMP-compatible to keep this path open.

## Infrastructure

- **Sandbox**: Docker. Dockerfile templates per exam mode MUST be versioned under `sandbox/`.
- **Container Orchestration**: Docker Engine (single-host) for v1. Kubernetes is out of scope unless amended.
- **Build Tool**: Gradle (Kotlin DSL) for all modules.
- **Code Quality**: Detekt (see §VII). Configuration in `config/detekt/detekt.yml`.
  CI pipeline MUST run `./gradlew detekt` and fail on violations.
