# §VII — Architecture & Module Conventions

## Directory Layout

The repository MUST follow this top-level structure:

```
root/
├── core/
│   ├── :core            # Cross-cutting JVM utilities: EnvConfig, LoggingConfig (MDC)
│   └── :core:data       # KMP shared models & API contracts — used by server AND all clients
├── app/
│   ├── shared/          # KMP client-shared layer — re-exports core:data; client-only models
│   └── webApp/          # Kotlin/JS + Kilua frontend (v1 only client)
├── server/              # Main backend API service (Ktor JVM)
│   ├── :app             # Composition root — DI wiring (Koin), Ktor engine, plugin setup
│   ├── :api             # HTTP routes, middleware, auth, rate limiting, request/response DTOs
│   ├── :domain          # Use cases (interactors), repository interfaces, domain entities
│   └── :data            # Repository implementations (Exposed), DB schema, Flyway migrations, Redis client
├── sandbox-runner/      # Isolated code-execution service (separate Ktor process)
│   ├── :app             # Ktor entry point, authenticated HTTP API
│   └── :executor        # Docker container lifecycle, image management
```

**Module ownership rules:**
- `core:data` is the ONLY module that defines shared domain models, enums, and API contract
  types. `app:shared` and `server:domain` MUST import from `core:data`, not from each other.
- `app:shared` is the client-side shared layer. It may later be split into
  `app:shared:ui`, `app:shared:data`, `app:shared:domain` as the frontend grows.
- Future client modules (`androidApp/`, `iosApp/`, `desktopApp/`) depend on `core:data`
  directly and/or on `app:shared`.
- `core:data` MUST remain KMP-compatible at all times (no JVM-only imports).

## Backend Architecture — Clean Architecture

The `server/` service MUST follow Clean Architecture. Dependency direction is strictly
inward: `:api` → `:domain` ← `:data`. Neither `:domain` nor `shared/` may import `:data`
or `:api`.

| Module | Clean Arch Layer | Responsibilities |
|---|---|---|
| `server:api` | Presentation | Ktor routing, auth middleware, rate limiting, request validation |
| `server:domain` | Domain | Use cases, repository interfaces, domain entities (pure Kotlin) |
| `server:data` | Infrastructure | Exposed table definitions, Flyway migrations, HikariCP pool, Redis client |
| `server:app` | Composition | Koin DI modules, Ktor engine config, environment wiring |

## Frontend Architecture — MVI

The `webApp/` module MUST follow MVI (Model-View-Intent). Kilua components are pure View;
they emit Intents and render State snapshots. No business logic lives in components.

| Layer | Responsibility |
|---|---|
| View | Kilua components — renders `State`, emits `Intent` |
| ViewModel / Store | Reduces `Intent` → `Action` → new `State`; calls use cases |
| Domain | Use cases imported from `app:shared`; no duplication |
| Data | Ktor Client HTTP calls to `server:api`; browser LocalStorage via `kotlinx-browser` |

## Code Style

All Kotlin modules MUST use [**Detekt**](https://github.com/detekt/detekt) for static analysis.
Configuration lives in `config/detekt/detekt.yml` at the repository root. CI MUST fail on any
Detekt rule violation. The Detekt configuration MAY integrate `detekt-formatting` (ktlint rules)
to enforce consistent formatting without a separate ktlint pass.

**Rationale**: A single language + clean layering boundary prevents the codebase from
becoming a tangle of cross-cutting concerns as features are added. MVI aligns frontend
architecture with patterns familiar from Android/Compose, reducing context-switching cost.
