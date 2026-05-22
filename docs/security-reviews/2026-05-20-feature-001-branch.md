---
document_type: security-review
review_type: branch
assessment_date: 2026-05-20
codebase_analyzed: KodEx / feature/001-project-base-setup
total_files_analyzed: 10
total_findings: 9
overall_risk: HIGH
critical_count: 0
high_count: 2
medium_count: 3
low_count: 2
informational_count: 2
owasp_categories: [A01, A05, A09]
cwe_ids: [CWE-200, CWE-284, CWE-306, CWE-16]
field_summaries:
  document_type: "Always 'security-review'. Allows indexers to skip non-review documents."
  review_type: "Which command generated this document: audit, branch, staged, plan, tasks, or followup."
  assessment_date: "ISO 8601 date the review was performed (YYYY-MM-DD)."
  overall_risk: "Highest severity tier with active findings (CRITICAL, HIGH, MODERATE, LOW, INFORMATIONAL)."
  critical_count: "Number of Critical findings (CVSS 9.0-10.0)."
  high_count: "Number of High findings (CVSS 7.0-8.9)."
  medium_count: "Number of Medium findings (CVSS 4.0-6.9)."
  low_count: "Number of Low findings (CVSS 0.1-3.9)."
  informational_count: "Number of Informational findings."
  owasp_categories: "OWASP Top 10 2021 categories that have at least one finding."
  cwe_ids: "CWE identifiers referenced in this document."
  finding_id: "Unique finding identifier (SEC-NNN) for cross-referencing and task linkage."
  location: "File path and line number of the vulnerable code (path/to/file.ext:line)."
  owasp_category: "OWASP Top 10 2021 category for this finding."
  cwe: "Common Weakness Enumeration identifier with short name."
  cvss_score: "CVSS v3.1 base score (0.0-10.0)."
  spec_kit_task: "Spec-Kit task ID for backlog tracking and remediation follow-up."
---

# SECURITY REVIEW REPORT — BRANCH: feature/001-project-base-setup vs develop

## Executive Summary

The branch introduces the complete project foundation: Gradle build system, Ktor server skeleton, Kilua frontend scaffold, Docker Compose infrastructure, and CI pipeline. The codebase is well-structured and the secrets management pattern (`EnvConfig.env()`) is correctly implemented.

**Two HIGH findings require remediation before merging to `develop`**: the `sandbox-runner` service is reachable from the host network without any authentication, violating the security constitution's isolation requirement. All other findings are medium or lower and may be addressed in follow-up tasks.

No hardcoded secrets were found. CORS is correctly restricted to `WEBAPP_ORIGIN`. Cache-Control headers are set on the health endpoint.

---

## Branch Diff Reviewed

- **Target**: `feature/001-project-base-setup`
- **Base**: `develop`
- **Files audited** (security-relevant subset):

| File | Purpose |
|------|---------|
| `core/src/main/kotlin/dev/kodex/core/env/EnvConfig.kt` | Env var loading |
| `server/app/src/main/kotlin/dev/kodex/server/Application.kt` | Ktor entry point, CORS, error handling |
| `server/api/src/main/kotlin/dev/kodex/server/api/routes/HealthRoutes.kt` | Health endpoint |
| `server/app/src/dev/kotlin/dev/kodex/server/dev/DevModule.kt` | Dev-only routes |
| `sandbox-runner/app/src/main/kotlin/dev/kodex/sandbox/Application.kt` | Sandbox-runner entry point |
| `sandbox/kotlin/Dockerfile` | Kotlin sandbox image |
| `docker/docker-compose.yml` | Service orchestration |
| `docker/server.Dockerfile` | Server image |
| `.env.example` | Env var template |
| `.github/workflows/ci.yml` | CI pipeline |

---

## Vulnerability Findings

---

### [HIGH] SEC-001 — sandbox-runner port exposed to host network

**Location**: `docker/docker-compose.yml:55`
**OWASP Category**: A01:2021 — Broken Access Control
**CWE**: CWE-284 — Improper Access Control
**CVSS Score**: 8.1

**Description**:
The `sandbox-runner` service binds port `8081` to the host (`"8081:8081"`). The security constitution explicitly states: *"sandbox-runner HTTP — Trusted (shared secret header required) — Internal only — never exposed publicly."* Any process on the host machine (or any networked machine if Docker is configured to expose interfaces) can call the sandbox-runner API directly, bypassing the shared secret check.

```yaml
# docker/docker-compose.yml:53-56
sandbox-runner:
  ports:
    - "8081:8081"   # ← HOST:CONTAINER — exposes to host
```

**Remediation**:
Remove the `ports` mapping from `sandbox-runner`. Docker Compose services on the same network can communicate by service name without port exposure:

```yaml
sandbox-runner:
  # no ports: mapping — reachable by server at http://sandbox-runner:8081
  expose:
    - "8081"   # optional: documents the internal port
```

**Spec-Kit Task**: TASK-SEC-001

---

### [HIGH] SEC-002 — sandbox-runner has no shared-secret authentication

**Location**: `sandbox-runner/app/src/main/kotlin/dev/kodex/sandbox/Application.kt:1-21`
**OWASP Category**: A01:2021 — Broken Access Control
**CWE**: CWE-306 — Missing Authentication for Critical Function
**CVSS Score**: 8.6

**Description**:
The sandbox-runner Ktor server accepts all requests without verifying the `SANDBOX_SHARED_SECRET` header. The security constitution mandates: *"All requests from `server/app` to `sandbox-runner` MUST include the `SANDBOX_SHARED_SECRET` in a dedicated header. `sandbox-runner` MUST reject any request missing or having an invalid secret with HTTP 401."*

The current skeleton has no auth middleware at all:

```kotlin
// sandbox-runner/app/src/main/kotlin/dev/kodex/sandbox/Application.kt
routing {
    get("/health") {
        call.respond(HttpStatusCode.OK)   // no auth check
    }
}
```

**Remediation**:
Add a Ktor plugin (or `intercept` on the route pipeline) that validates the shared secret header before any route executes:

```kotlin
install(createRouteScopedPlugin("SharedSecretAuth") {
    onCall { call ->
        val secret = env("SANDBOX_SHARED_SECRET")
        val header = call.request.headers["X-Sandbox-Secret"]
        if (header != secret) {
            call.respond(HttpStatusCode.Unauthorized)
            finish()
        }
    }
})
```

Exempt the `/health` endpoint from the secret check (health checks from Docker come from localhost without the header).

**Spec-Kit Task**: TASK-SEC-002

---

### [MEDIUM] SEC-003 — PostgreSQL and Redis ports exposed to host

**Location**: `docker/docker-compose.yml:9` (postgres), `docker/docker-compose.yml:20` (redis)
**OWASP Category**: A05:2021 — Security Misconfiguration
**CWE**: CWE-16 — Configuration
**CVSS Score**: 5.3

**Description**:
Both `postgres` (5432) and `redis` (6379) are bound to the host via `ports` mappings. The security constitution states these are *"Internal only — never exposed publicly."* On a developer machine this is tolerable, but the same compose file is used in any shared/staging environment.

```yaml
postgres:
  ports:
    - "${DB_PORT:-5432}:5432"   # ← exposed to host
redis:
  ports:
    - "${REDIS_PORT:-6379}:6379"  # ← exposed to host
```

**Remediation**:
Replace `ports` with `expose` for both services in the production-oriented compose file. For local developer convenience, add an override file `docker/docker-compose.dev.yml` that adds the port bindings:

```yaml
# docker/docker-compose.yml (base)
postgres:
  expose:
    - "5432"
redis:
  expose:
    - "6379"
```

```yaml
# docker/docker-compose.dev.yml (override — never used in CI/staging)
services:
  postgres:
    ports:
      - "${DB_PORT:-5432}:5432"
  redis:
    ports:
      - "${REDIS_PORT:-6379}:6379"
```

**Spec-Kit Task**: TASK-SEC-003

---

### [MEDIUM] SEC-004 — Internal error details leaked in HTTP response body

**Location**: `server/app/src/main/kotlin/dev/kodex/server/Application.kt:40-45`
**OWASP Category**: A05:2021 — Security Misconfiguration
**CWE**: CWE-200 — Exposure of Sensitive Information to an Unauthorized Actor
**CVSS Score**: 4.3

**Description**:
The `StatusPages` exception handler returns `cause.message` directly in the response body. Internal stack traces, database driver messages, and env var names can surface to the client.

```kotlin
exception<Throwable> { call, cause ->
    call.respondText(
        "Internal Server Error: ${cause.message}",   // ← leaks internal details
        status = HttpStatusCode.InternalServerError,
    )
}
```

**Remediation**:
Log the full cause server-side and return a generic message to the client. Use the API envelope format:

```kotlin
exception<Throwable> { call, cause ->
    call.application.log.error("Unhandled exception", cause)
    call.respond(
        HttpStatusCode.InternalServerError,
        buildEnvelope(
            data = null,
            error = "An unexpected error occurred.",
            requestId = call.request.headers["X-Request-Id"] ?: UUID.randomUUID().toString(),
            service = "kodex-api",
            version = BuildConfig.VERSION,
        )
    )
}
```

**Spec-Kit Task**: TASK-SEC-004

---

### [MEDIUM] SEC-005 — Redis running without password authentication

**Location**: `docker/docker-compose.yml:17-22`
**OWASP Category**: A05:2021 — Security Misconfiguration
**CWE**: CWE-306 — Missing Authentication for Critical Function
**CVSS Score**: 5.9

**Description**:
The `redis` service is started without the `--requirepass` flag and without passing `REDIS_PASSWORD` as a command argument. Any process that can reach Redis (including any container on the Docker Compose network) can read or write tokens without authentication.

```yaml
redis:
  image: redis:7-alpine
  # no command: --requirepass ...
  # no REDIS_PASSWORD env var passed
```

**Remediation**:
Pass the password via the Redis `command` directive:

```yaml
redis:
  image: redis:7-alpine
  command: >
    sh -c '[ -n "$$REDIS_PASSWORD" ] && exec redis-server --requirepass "$$REDIS_PASSWORD" || exec redis-server'
  environment:
    REDIS_PASSWORD: ${REDIS_PASSWORD}
```

Note: An empty `REDIS_PASSWORD` (acceptable in local dev) bypasses auth. Document this trade-off in `.env.example`.

**Spec-Kit Task**: TASK-SEC-005

---

### [LOW] SEC-006 — Sandbox Dockerfile missing enforced resource limits

**Location**: `sandbox/kotlin/Dockerfile:1-14`
**OWASP Category**: A05:2021 — Security Misconfiguration
**CWE**: CWE-16 — Configuration
**CVSS Score**: 3.1

**Description**:
The security constitution requires: *"Sandbox container MUST have hard CPU limit, hard memory limit, and hard wall-clock timeout."* The current Dockerfile only documents these constraints in comments. Enforcement depends entirely on the executor calling `docker run --cpus=2 --memory=512m --network=none`. If the executor omits these flags, the sandbox runs unrestricted.

**Remediation**:
Enforcement MUST be in `sandbox-runner/executor` code (not in the Dockerfile, which cannot enforce runtime flags). When implementing the executor, validate that `--network=none`, `--cpus`, `--memory`, and `--stop-timeout` are always set and fail fast if configuration is absent. Add a test that verifies these flags are present in any container start call.

**Spec-Kit Task**: TASK-SEC-006

---

### [LOW] SEC-007 — Missing global security response headers

**Location**: `server/app/src/main/kotlin/dev/kodex/server/Application.kt`
**OWASP Category**: A05:2021 — Security Misconfiguration
**CWE**: CWE-16 — Configuration
**CVSS Score**: 3.7

**Description**:
The Ktor server does not set standard browser security headers globally:
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Referrer-Policy: strict-origin-when-cross-origin`
- `Permissions-Policy: ...`

While these primarily protect browser clients, the API is consumed by a browser frontend.

**Remediation**:
Add a global response intercept in `Application.kt` or install the `ktor-server-default-headers` plugin:

```kotlin
install(DefaultHeaders) {
    header("X-Content-Type-Options", "nosniff")
    header("X-Frame-Options", "DENY")
    header("Referrer-Policy", "strict-origin-when-cross-origin")
}
```

**Spec-Kit Task**: TASK-SEC-007

---

### [INFORMATIONAL] SEC-008 — `/dev/ping` route has no authentication

**Location**: `server/app/src/dev/kotlin/dev/kodex/server/dev/DevModule.kt:12-15`
**OWASP Category**: A01:2021 — Broken Access Control
**CWE**: CWE-306 — Missing Authentication for Critical Function
**CVSS Score**: 0.0 (dev-only, not in production artifact)

**Description**:
The `/dev/ping` route is unauthenticated. This is acceptable because `devMain` is excluded from `installDist` and will never reach production. However, if a developer accidentally adds business logic to a dev route, they may assume it is protected.

**Recommendation**:
Add a comment stating that dev routes are intentionally unauthenticated and must never contain sensitive logic.

**Spec-Kit Task**: N/A (informational)

---

### [INFORMATIONAL] SEC-009 — CI pipeline has no secret scanning step

**Location**: `.github/workflows/ci.yml`
**OWASP Category**: A09:2021 — Security Logging and Monitoring Failures
**CWE**: CWE-200 — Exposure of Sensitive Information
**CVSS Score**: 0.0

**Description**:
The CI pipeline runs build, test, and quality (Detekt) jobs but does not include a secret scanning step (e.g., `trufflehog`, `gitleaks`, or GitHub's built-in secret scanning). A committed credential would not be caught by the pipeline.

**Recommendation**:
Add `gitleaks/gitleaks-action` or enable GitHub Advanced Security secret scanning on the repository.

**Spec-Kit Task**: TASK-SEC-009 (optional backlog item)

---

## Confirmed Secure Patterns

| Pattern | Location | Notes |
|---------|----------|-------|
| `env()` throws on missing required secrets | `EnvConfig.kt` | Startup fails fast — secrets cannot be empty |
| No secrets in source code | Entire diff | All secrets use env vars; `.env` is gitignored |
| CORS restricted to `WEBAPP_ORIGIN` | `Application.kt:28-38` | `allowOrigins { it == webAppOrigin }` — exact match, no wildcard |
| `Cache-Control: no-store` on health endpoint | `HealthRoutes.kt:14` | Correctly set |
| Multi-stage Docker build | `docker/server.Dockerfile` | Build artifacts not in final image |
| Non-root user in sandbox | `sandbox/kotlin/Dockerfile:11-12` | `useradd sandbox` + `USER sandbox` |
| No hardcoded JWT or DB credentials | Entire diff | `.env.example` uses obvious placeholders |
| `WEBAPP_ORIGIN` loaded via `env()` (required) | `Application.kt:22` | Cannot start without this var |

---

## Prioritized Action Plan

| Priority | Finding | Effort | Before Merge? |
|----------|---------|--------|---------------|
| 1 | SEC-001: Unexpose sandbox-runner port | XS (2 lines) | **YES** |
| 2 | SEC-002: Add shared-secret auth to sandbox-runner | S (1 plugin) | **YES** |
| 3 | SEC-004: Fix error detail leak in StatusPages | S (refactor handler) | Recommended |
| 4 | SEC-005: Redis password enforcement in Compose | S (1 command line) | Recommended |
| 5 | SEC-003: Unexpose postgres/redis ports | XS + compose override | Next sprint |
| 6 | SEC-007: Add global security headers | XS (plugin install) | Next sprint |
| 7 | SEC-006: Enforce sandbox limits in executor | M (executor implementation) | When T027 executor is implemented |
| 8 | SEC-009: Add secret scanning to CI | XS (action) | Backlog |
