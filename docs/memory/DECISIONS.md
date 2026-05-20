# Technical Decisions (`docs/memory/`)

For governance-level decisions and project standards, see `.specify/memory/constitution.md`.

## Entry Lifecycle

```
Active → Needs Review → Superseded → (pruned)
```

---

### 2026-05-20 - D1: Use `kotlin.time.Instant` in Kotlin 2.3+, not `kotlinx.datetime.Instant`

**Status**: Active

**Why this is durable**
Every new module that works with `Instant` must use the correct import — the mistake only surfaces as a deprecation warning at compile time, not an error.

**Decision**
In Kotlin 2.3+, use `kotlin.time.Instant` (stdlib). Do not use `kotlinx.datetime.Instant`, which is deprecated. No extra dependency on `kotlinx-datetime` is required unless you need calendar-aware operations or `LocalDateTime` conversion.

**Evidence**
`HealthResponse.kt` in `core:models` and all date-aware models use `kotlin.time.Instant`.

**Tradeoffs**
- Gained: no extra dependency, stable stdlib API
- Made harder: converting to `LocalDateTime` still requires `kotlinx-datetime`

---

### 2026-05-20 - D2: kotlin-logging version must be `8.0.03`, not `8.0.0`

**Status**: Active

**Why this is durable**
Version `8.0.0` of `io.github.oshai:kotlin-logging-jvm` is not published to Maven Central. Gradle fails with a `MISSING` artifact error, and the error message is not obvious.

**Decision**
Pin kotlin-logging to `8.0.03` in `gradle/libs.versions.toml`. Before upgrading, verify the artifact exists on Maven Central before changing the version.

**Evidence**
`libs.versions.toml`: `kotlin-logging = "8.0.03"` (discovered after `8.0.0` caused a build failure).

**Tradeoffs**
- Made harder: the unusual patch version may cause confusion in future upgrades
