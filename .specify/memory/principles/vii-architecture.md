# §VII — Architecture & Module Conventions

## Directory Layout

The repository MUST follow this top-level structure:

```
root/
├── core/
│   ├── :core            # Cross-cutting JVM utilities: EnvConfig, LoggingConfig (MDC)
│   └── :core:models       # KMP shared models & API contracts — used by server AND all clients
├── app/
│   ├── shared/          # KMP client-shared layer — re-exports core:models; client-only models
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
- `core:models` is the ONLY module that defines shared domain models, enums, and API contract
  types. `app:shared` and `server:domain` MUST import from `core:models`, not from each other.
- `app:shared` is the client-side shared layer. It may later be split into
  `app:shared:ui`, `app:shared:data`, `app:shared:domain` as the frontend grows.
- Future client modules (`androidApp/`, `iosApp/`, `desktopApp/`) depend on `core:models`
  directly and/or on `app:shared`.
- `core:models` MUST remain KMP-compatible at all times (no JVM-only imports).

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

### Frontend Security Constraints (design system)

- Components MUST render all user-controlled content via Kilua's `+` operator (text nodes).
  `innerHTML` / `outerHTML` / raw DOM writes are **prohibited** without a security review. See §A4.
- Data class fields used in DOM attributes MUST be validated in `init` blocks (`require()` + allowlist).
  Pattern: `NavItem.key` and `NavItem.icon` in `NavBar.kt`.
- CSP lives in both `index.html` (meta tag) and Ktor `DefaultHeaders` (API responses). Both must
  stay in sync. See D7.
- Dev playground code is gated by `js("import.meta.env.DEV")` — Vite tree-shakes it from
  the production bundle. No `devMain` source set equivalent exists for JS/WASM targets. See D6.

## Gradle Conventions

All inter-module dependencies MUST use **Type-Safe Project Accessors** (enabled via
`enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")` in `settings.gradle.kts`).
Using `project(":path:to:module")` string literals is PROHIBITED — use `projects.path.to.module` instead.

**Rationale**: Type-safe accessors are compile-time checked, IDE-navigable, and refactor-safe.
String-based `project(...)` calls fail silently at configuration time and are not refactor-aware.

### Convention Plugin Map

All build config lives in `build-logic/src/main/kotlin/`. Conventions MUST be composed —
never add plugins or config directly to module `build.gradle.kts` files if a convention covers it.

```
benchmark-convention          ← allOpen(@State) + JMH configs: main (5s) and fast (1iter/500ms/1fork)
├── kotlin-kmp-convention     ← KMP target "jvmBenchmark", compilations, sourceSets, kover, detekt
└── kotlin-jvm-convention     ← JVM target "jvm", toolchain 21, kover, detekt

detekt-convention             ← detekt plugin + config/detekt/detekt.yml + all report formats + detekt-formatting dep
├── kotlin-kmp-convention
└── kotlin-jvm-convention

ktor-service-convention       ← applies kotlin-jvm-convention + adds ktor-server/koin/logging bundles

kover-report-convention       ← aggregate coverage report: 90% threshold, xml+html, excludes @State classes
                                Applied to root project only.

dependency-check-convention   ← OWASP plugin, NVD key via Config.get(), failBuildOnCVSS=7, autoUpdate=false
                                Applied to root project only.

benchmark-aggregation-convention ← root tasks: benchmark (all main), benchmarkFast (all fast, CI), benchmarkMerge
                                   Applied to root project only.
```

**Key rules:**
- `type-safe project accessors` are MANDATORY everywhere — `project(":x")` is prohibited
- `detekt-convention` is the single source of truth for detekt; do NOT apply the detekt plugin directly in module files
- `benchmark-convention` owns the JMH configurations (main + fast); module conventions only register their target name
- Convention plugins that use `project(":x:y")` string paths (e.g., kover module inclusions) MUST stay in root `build.gradle.kts` — typesafe accessors (`projects.*`) are not available in `build-logic`

## Code Style

All Kotlin modules MUST use [**Detekt**](https://github.com/detekt/detekt) for static analysis.
Configuration lives in `config/detekt/detekt.yml` at the repository root. CI MUST fail on any
Detekt rule violation. The Detekt configuration integrates `detekt-formatting` (ktlint rules)
via `detekt-convention`, eliminating any need for a separate ktlint pass.

**Rationale**: A single language + clean layering boundary prevents the codebase from
becoming a tangle of cross-cutting concerns as features are added. MVI aligns frontend
architecture with patterns familiar from Android/Compose, reducing context-switching cost.
