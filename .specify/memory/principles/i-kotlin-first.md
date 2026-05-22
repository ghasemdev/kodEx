# §I — Kotlin-First Stack

All application code — backend, web frontend, and evaluation tooling — MUST be written in Kotlin.
The backend MUST use Ktor. The web frontend MUST use Kotlin/JS.
No other server-side or frontend language is permitted without a formal constitution amendment.
Shared domain models (data classes, enums, sealed classes) SHOULD live in a `shared` Kotlin
Multiplatform module consumed by both backend and frontend.

**Rationale**: A single language across the full stack reduces cognitive overhead, enables code
sharing (especially domain models and validation logic), and keeps the hiring/onboarding
profile focused.
