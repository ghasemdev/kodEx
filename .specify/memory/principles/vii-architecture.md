# §VII — Architecture & Module Conventions

## Directory Layout

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
