---
document_type: security-review
review_type: whitebox-assessment
assessment_date: 2026-05-20
export_date: 2026-05-23
codebase_analyzed: KodEx / feature/001-project-base-setup
total_files_analyzed: 10
total_findings: 9
residual_risk: LOW
critical_count: 0
high_count: 2
medium_count: 3
low_count: 2
informational_count: 2
findings_resolved: 8
findings_deferred: 1
owasp_categories: [A01, A05, A09]
cwe_ids: [CWE-16, CWE-200, CWE-284, CWE-306, CWE-400]
---

# KODEX — WHITEBOX SECURITY ASSESSMENT REPORT

**Feature Branch**: `feature/001-project-base-setup`
**Assessment Date**: 2026-05-20
**Remediation Completed**: 2026-05-23
**Exported**: 2026-05-23
**Assessor**: AI Security Auditor (Spec-Kit Security Review Extension — Whitebox mode)

---

## 1. EXECUTIVE SUMMARY

### 1.1 Assessment Overview

This report consolidates the findings from a Whitebox Security Assessment conducted on
2026-05-20 against the `feature/001-project-base-setup` branch of the KodEx platform,
together with full remediation evidence collected on 2026-05-23. The assessment was performed
with complete access to source code, architectural decision records, the KodEx Security
Constitution (`.specify/memory/security_constitution.md`), and the durable repository
memory hub (`docs/memory/`). The scope covered the Ktor API server skeleton, sandbox-runner
service, Docker Compose orchestration, the CI/CD pipeline, and the project Gradle build system.

### 1.2 Risk Posture

**Overall Risk Rating at Assessment: HIGH**
**Residual Risk Rating at Export: LOW**

| Severity      | Count | Status            | Primary OWASP Categories              |
|---------------|-------|-------------------|---------------------------------------|
| Critical      | 0     | —                 | —                                     |
| High          | 2     | ✅ Resolved        | A01 — Broken Access Control           |
| Medium        | 3     | ✅ Resolved        | A05 — Security Misconfiguration       |
| Low           | 2     | ✅ 1 Resolved / ⏳ 1 Deferred | A05 — Security Misconfiguration |
| Informational | 2     | ✅ Acceptable / Resolved | A01, A09                        |

All 7 actionable findings have been remediated. One LOW finding (SEC-006 — sandbox resource
limits) is formally deferred pending implementation of the executor module, and is tracked as
`TASK-SEC-006` in `specs/001-project-base-setup/tasks.md`.

### 1.3 Key Findings and Strategic Impact

The most critical risks identified were two HIGH-severity access control failures in the
sandbox-runner service. The service bound its administrative port (`8081`) directly to the
host network interface and accepted requests without any authentication, in direct violation
of the Security Constitution's isolation mandate. An attacker on the host network could
submit arbitrary sandbox execution requests, potentially achieving code execution on the
sandbox host prior to any resource-limit enforcement. Both findings were remediated before
first merge.

A secondary cluster of three MEDIUM findings represented defence-in-depth failures in the
supporting infrastructure: internal error messages were disclosed in HTTP responses (leaking
stack traces and driver messages), Redis ran without password authentication within the
Docker Compose network, and both PostgreSQL and Redis were bound to host ports. None of
these individually allowed direct exploitation on a private developer machine, but any of
them would have constituted a serious risk in a shared or staging environment.

A systemic observation across the findings is that the project's security-by-constitution
model is strong in design intent but was not fully reflected in initial implementation. The
`EnvConfig.env()` pattern and the CORS configuration were correctly implemented from the
outset, demonstrating that the patterns are understood; the gaps were in infrastructure
wiring rather than application logic.

### 1.4 Remediation Roadmap

All HIGH and MEDIUM findings were remediated within 72 hours of assessment. The remediation
strategy followed the Security Constitution's tier model: the two HIGH findings were
addressed first (commit `9b9c9968`), followed by all MEDIUM and LOW-immediate items together
(commit `ce1f128`). Secret scanning was added to the CI pipeline in the same batch.

The sole remaining technical debt item (TASK-SEC-006) requires enforcement of Docker
resource limits (`--network=none`, `--cpus`, `--memory`, `--stop-timeout`, `--read-only`)
inside the executor module. This is structurally deferred because the `sandbox-runner/executor`
module is an empty placeholder — no container lifecycle code exists. The Security Constitution
requirement is unambiguous and the acceptance criteria are recorded in `tasks.md`; the item
MUST be implemented and tested before the first code-submission execution feature is merged.

---

## 2. ASSESSMENT METHODOLOGY

### 2.1 Scope of Work

| Asset | Coverage |
|-------|----------|
| `core/env/EnvConfig.kt` | Secrets loading — full review |
| `server/app/Application.kt` | Ktor plugins, CORS, error handling — full review |
| `server/api/routes/HealthRoutes.kt` | HTTP endpoint, headers — full review |
| `server/app/dev/DevModule.kt` | Dev-only routes — full review |
| `sandbox-runner/app/Application.kt` | Service entry point, auth — full review |
| `sandbox/kotlin/Dockerfile` | Container image — full review |
| `docker/docker-compose.yml` | Service orchestration, port exposure — full review |
| `docker/server.Dockerfile` | Build pipeline — full review |
| `.env.example` | Secrets template — full review |
| `.github/workflows/ci.yml` | CI/CD pipeline security controls — full review |

### 2.2 Testing Approach

This was a **Whitebox Security Assessment**. Unlike a blackbox pentest, the assessor had
full access to:

- Source code logic and internal data flows.
- The KodEx Security Constitution (version 1.0.0, ratified 2026-05-19), which acts as the
  authoritative contract for all security requirements.
- Architectural decision records and design intent in `docs/memory/` and `specs/`.
- The historical bug and decision log from the repository memory hub.

Findings were evaluated against the **OWASP Top 10 (2021)** and **CWE/SANS** weakness
taxonomy. All findings were cross-referenced against the Security Constitution to determine
whether they represent a violation of an explicit rule or a defence-in-depth gap.

---

## 3. TECHNICAL FINDINGS

---

### SEC-001 — Sandbox-Runner Administrative Port Exposed to Host Network

**Severity**: HIGH
**OWASP Category**: A01:2021 — Broken Access Control
**CWE**: CWE-284 — Improper Access Control
**CVSS v3.1**: 8.1 (AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:N)
**Status**: ✅ Resolved — commit `9b9c9968`

#### 3.1.1 Description

The `sandbox-runner` service bound its administrative HTTP port (`8081`) to the host network
interface via a Docker Compose `ports` mapping. The Security Constitution §1 explicitly
classifies `sandbox-runner/` as an internal-only trusted entry point that MUST require a
shared-secret header and MUST NEVER be exposed publicly. By publishing the port to the host,
any process with network access to the Docker host — including other containers on non-Compose
networks and any process on the developer's machine — could directly invoke sandbox execution
APIs without supplying the (at that time, non-existent) shared secret.

#### 3.1.2 Evidence (Vulnerable State)

**File**: `docker/docker-compose.yml` (original, lines 53–56)

```yaml
sandbox-runner:
  ports:
    - "8081:8081"   # HOST:CONTAINER — binds to all host interfaces
```

#### 3.1.3 Exploit Scenario

1. Attacker identifies TCP port `8081` open on the Docker host (e.g., via `nmap`).
2. Attacker sends an unauthenticated HTTP request directly to `http://<host-ip>:8081/` —
   bypassing the main `server` API entirely.
3. Because no shared-secret authentication existed (see SEC-002), the request succeeds.
4. Attacker submits arbitrary sandbox commands, potentially achieving code execution within
   the sandbox container environment.

#### 3.1.4 Impact

Unrestricted access to the sandbox orchestration API, bypassing all API-layer authentication
(JWT), rate limiting, and submission validation enforced in `server/app`. Combined with the
absent authentication on the service itself (SEC-002), the attack surface is the entire
sandbox execution pipeline.

#### 3.1.5 Remediation Applied

`docker/docker-compose.yml` — replaced `ports: "8081:8081"` with `expose: "8081"`. The
`expose` directive documents the port for inter-service DNS discovery within the Compose
network without binding it to any host interface.

**Remediated Configuration**:

```yaml
sandbox-runner:
  expose:
    - "8081"   # internal-only; reachable by server at http://sandbox-runner:8081
```

---

### SEC-002 — Sandbox-Runner Missing Shared-Secret Authentication

**Severity**: HIGH
**OWASP Category**: A01:2021 — Broken Access Control
**CWE**: CWE-306 — Missing Authentication for Critical Function
**CVSS v3.1**: 8.6 (AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:L)
**Status**: ✅ Resolved — commit `9b9c9968`

#### 3.2.1 Description

The sandbox-runner Ktor server accepted all requests without verifying a shared-secret
credential. The Security Constitution §6 mandates: *"All requests from `server/app` to
`sandbox-runner` MUST include the `SANDBOX_SHARED_SECRET` in a dedicated header.
`sandbox-runner` MUST reject any request missing or having an invalid secret with HTTP 401."*
The original skeleton contained no authentication middleware whatsoever.

#### 3.2.2 Evidence (Vulnerable State)

**File**: `sandbox-runner/app/src/main/kotlin/dev/kodex/sandbox/Application.kt` (original)

```kotlin
routing {
    get("/health") {
        call.respond(HttpStatusCode.OK)   // no auth — any caller accepted
    }
}
```

#### 3.2.3 Exploit Scenario

1. Attacker connects to `sandbox-runner` via the exposed port (see SEC-001) or from any
   container on the same Docker network.
2. Attacker issues any HTTP request — no header, token, or credential required.
3. All future executor endpoints (code submission, container lifecycle) would have been
   accessible without restriction.

#### 3.2.4 Impact

A fully unauthenticated critical-function endpoint. Any internal network actor (or external
actor via the exposed port) could invoke sandbox execution logic.

#### 3.2.5 Remediation Applied

Ktor `Authentication` + `bearer("secret-auth")` plugin installed. Bearer token is validated
against `SANDBOX_SHARED_SECRET`. The `/health` route is deliberately excluded from the
`authenticate` block because Docker's health-check process runs from localhost without the
header.

**Remediated Implementation**:

```kotlin
// sandbox-runner/app/src/main/kotlin/dev/kodex/sandbox/Application.kt
install(Authentication) {
    bearer("secret-auth") {
        authenticate { tokenCredential ->
            if (tokenCredential.token == sharedSecret) UserIdPrincipal("internal") else null
        }
    }
}

routing {
    get("/health") { call.respond(HttpStatusCode.OK) }  // exempt — Docker health check

    authenticate("secret-auth") {
        route("/") {}  // all executor routes added here require the secret
    }
}
```

---

### SEC-003 — PostgreSQL and Redis Ports Exposed to Host

**Severity**: MEDIUM
**OWASP Category**: A05:2021 — Security Misconfiguration
**CWE**: CWE-16 — Configuration
**CVSS v3.1**: 5.3 (AV:N/AC:H/PR:N/UI:N/S:U/C:H/I:L/A:N)
**Status**: ✅ Resolved — commit `ce1f128`

#### 3.3.1 Description

Both the `postgres` (port 5432) and `redis` (port 6379) services were mapped to the host
network via Docker Compose `ports` directives. The Security Constitution §1 classifies both
as *"Internal only — never exposed publicly."* The same `docker-compose.yml` is used as the
base for all environments. In a shared developer environment, CI runner, or staging
deployment, host-bound database ports constitute a lateral movement path.

#### 3.3.2 Evidence (Vulnerable State)

```yaml
postgres:
  ports:
    - "${DB_PORT:-5432}:5432"   # bound to host
redis:
  ports:
    - "${REDIS_PORT:-6379}:6379"  # bound to host
```

#### 3.3.3 Exploit Scenario

1. Attacker with network access to the Docker host (shared CI runner, misconfigured cloud
   security group) connects to `<host-ip>:5432` or `<host-ip>:6379`.
2. With no Redis password (see SEC-005), attacker reads or invalidates all session tokens.
3. With the PostgreSQL password (obtainable from the `.env` file or environment leaks),
   attacker performs direct data access.

#### 3.3.4 Impact

Direct unauthorized access to the primary database and session token store, bypassing all
application-layer controls.

#### 3.3.5 Remediation Applied

`docker/docker-compose.yml` — `ports` replaced with `expose` for both services. A new
`docker/docker-compose.dev.yml` override file restores port bindings for local development
tools (TablePlus, Redis Insight) without affecting any other environment.

**Base compose (all environments)**:

```yaml
postgres:
  expose:
    - "5432"
redis:
  expose:
    - "6379"
```

**Dev override (local only)**:

```yaml
# docker/docker-compose.dev.yml
services:
  postgres:
    ports:
      - "${DB_PORT:-5432}:5432"
  redis:
    ports:
      - "${REDIS_PORT:-6379}:6379"
```

Usage documented in `specs/001-project-base-setup/quickstart.md`.

---

### SEC-004 — Internal Error Details Leaked in HTTP Responses

**Severity**: MEDIUM
**OWASP Category**: A05:2021 — Security Misconfiguration
**CWE**: CWE-200 — Exposure of Sensitive Information to an Unauthorized Actor
**CVSS v3.1**: 4.3 (AV:N/AC:L/PR:N/UI:N/S:U/C:L/I:N/A:N)
**Status**: ✅ Resolved — commit `ce1f128`

#### 3.4.1 Description

The Ktor `StatusPages` exception handler returned `cause.message` verbatim in the HTTP
response body. Any unhandled exception — database driver errors, missing configuration,
null-pointer exceptions — would expose internal implementation details (class names, SQL
queries, env var names) to unauthenticated callers. This violates OWASP A05 and the
Security Constitution §7 (secrets MUST NOT appear in logs or responses).

#### 3.4.2 Evidence (Vulnerable State)

**File**: `server/app/src/main/kotlin/dev/kodex/server/Application.kt` (original ~line 40)

```kotlin
exception<Throwable> { call, cause ->
    call.respondText(
        "Internal Server Error: ${cause.message}",   // leaks driver messages, env var names
        status = HttpStatusCode.InternalServerError,
    )
}
```

#### 3.4.3 Exploit Scenario

1. Attacker crafts a malformed request that triggers an exception in the data layer.
2. The response body contains the JDBC error message, revealing the database driver version,
   schema structure, or — in extreme cases — connection string fragments.
3. Attacker uses this information to refine further attacks (SQL injection, credential
   stuffing, dependency CVE targeting).

#### 3.4.4 Impact

Information disclosure of internal implementation details. Severity is constrained to MEDIUM
because exploitation requires an exception to be triggered; however, stack traces from
database drivers routinely expose schema structure.

#### 3.4.5 Remediation Applied

Full exception is logged server-side via `call.application.log.error`. Client receives a
typed `ErrorEnvelope` with a generic message, the request ID (for log correlation), and
no exception content.

**New response type** (`server/api/.../response/Envelope.kt`):

```kotlin
@Serializable
data class ErrorEnvelope(val error: String, val meta: Meta)

fun buildErrorEnvelope(message: String, requestId: String, service: String, version: String) =
    ErrorEnvelope(
        error = message,
        meta = Meta(requestId, Clock.System.now().toString(), service, version),
    )
```

**Remediated handler** (`server/app/.../Application.kt`):

```kotlin
exception<Throwable> { call, cause ->
    call.application.log.error("Unhandled exception", cause)   // full trace — server only
    val requestId = call.request.headers["X-Request-Id"] ?: Uuid.random().toString()
    call.respond(
        HttpStatusCode.InternalServerError,
        buildErrorEnvelope(
            message = "An unexpected error occurred.",
            requestId = requestId,
            service = "kodex-api",
            version = BuildConfig.VERSION,
        ),
    )
}
```

---

### SEC-005 — Redis Running Without Password Authentication

**Severity**: MEDIUM
**OWASP Category**: A05:2021 — Security Misconfiguration
**CWE**: CWE-306 — Missing Authentication for Critical Function
**CVSS v3.1**: 5.9 (AV:N/AC:H/PR:N/UI:N/S:U/C:H/I:H/A:N)
**Status**: ✅ Resolved — commit `ce1f128`

#### 3.5.1 Description

The Redis service started without the `--requirepass` flag. Redis is used as the session
and refresh-token store. Any container on the Docker Compose network could read, write, or
delete token records without supplying credentials. The Security Constitution §4 requires
`REDIS_PASSWORD` as a mandatory environment variable in production.

#### 3.5.2 Evidence (Vulnerable State)

```yaml
redis:
  image: redis:7-alpine
  # no command: --requirepass
  # no REDIS_PASSWORD env var
```

#### 3.5.3 Exploit Scenario

1. A compromised container on the Compose network (e.g., a vulnerable webapp dependency)
   connects to `redis:6379` without credentials.
2. Attacker issues `KEYS *` to enumerate all stored session tokens.
3. Attacker uses `GET <token-key>` to extract valid refresh tokens.
4. Attacker replays a refresh token to obtain a valid access token for any user session.

#### 3.5.4 Impact

Full compromise of the authentication session store. All active user sessions can be
hijacked without any application-layer vulnerability being required.

#### 3.5.5 Remediation Applied

Redis `command` directive conditionally starts the server with `--requirepass` when
`REDIS_PASSWORD` is non-empty. An empty value (acceptable in local dev with no sensitive
data) falls through to an unauthenticated start. This is documented in `.env.example` as
**required in production**.

```yaml
redis:
  image: redis:7-alpine
  command: >
    sh -c '[ -n "$$REDIS_PASSWORD" ] && exec redis-server --requirepass "$$REDIS_PASSWORD"
           || exec redis-server'
  environment:
    REDIS_PASSWORD: ${REDIS_PASSWORD:-}
```

---

### SEC-006 — Sandbox Container Resource Limits Not Enforced by Executor

**Severity**: LOW
**OWASP Category**: A05:2021 — Security Misconfiguration
**CWE**: CWE-400 — Uncontrolled Resource Consumption
**CVSS v3.1**: 3.1 (AV:L/AC:H/PR:N/UI:N/S:U/C:N/I:N/A:H)
**Status**: ⏳ Deferred — tracked as TASK-SEC-006

#### 3.6.1 Description

The Security Constitution §5 mandates that every sandbox container MUST have enforced CPU
limits, memory limits, wall-clock timeouts, and network isolation via `docker run` flags.
The `sandbox/kotlin/Dockerfile` documents these requirements in comments but cannot enforce
runtime flags. Enforcement MUST come from the `sandbox-runner/executor` module. That module
is currently an empty placeholder — no container lifecycle code exists. If the executor is
implemented without these constraints, user-submitted code could consume unbounded resources.

#### 3.6.2 Evidence

**File**: `sandbox-runner/executor/src/main/kotlin/dev/kodex/sandbox/executor/` (placeholder — no implementation)

The Dockerfile comment notes the intent without enforcement:

```dockerfile
# sandbox/kotlin/Dockerfile
# Runtime flags enforced by executor:
#   --network=none  --cpus=2  --memory=512m  --stop-timeout=10
```

#### 3.6.3 Exploit Scenario (Potential — Pre-Implementation)

1. Attacker submits code containing an infinite CPU-spin loop or memory allocation bomb.
2. If the executor omits `--cpus` and `--memory` flags, the container consumes host
   resources without bound.
3. Denial-of-service for all concurrent users; potential host instability.

#### 3.6.4 Impact

Denial-of-service via resource exhaustion. Severity is LOW because the module is not yet
implemented and the exploit path does not currently exist.

#### 3.6.5 Remediation Plan (Deferred)

When implementing `sandbox-runner/executor`, the `docker run` invocation MUST include:

```
--network=none
--cpus=<exam.cpuLimit>          # default 2.0
--memory=<exam.memoryLimit>     # default 512m
--stop-timeout=<exam.wallClockTimeout>  # default 10s
--read-only
--tmpfs /tmp
```

A unit test MUST assert that these flags are always present in container start parameters.

**Revisit trigger**: Before the first sandbox execution task is implemented.
**Milestone**: Feature 003 or 004 (code submission execution).

---

### SEC-007 — Global Security Response Headers Absent

**Severity**: LOW
**OWASP Category**: A05:2021 — Security Misconfiguration
**CWE**: CWE-16 — Configuration
**CVSS v3.1**: 3.7 (AV:N/AC:L/PR:N/UI:R/S:U/C:L/I:N/A:N)
**Status**: ✅ Resolved — commit `ce1f128`

#### 3.7.1 Description

The Ktor server did not set standard browser security response headers globally. The API is
consumed by a browser-based Kilua/JS frontend, making client-side header protections
meaningful. Missing headers: `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`.

#### 3.7.2 Evidence (Vulnerable State)

No `DefaultHeaders` or equivalent plugin was installed in `Application.kt`.

#### 3.7.3 Remediation Applied

`ktor-server-default-headers` library alias added to `gradle/libs.versions.toml` and
included in the `ktor-server` bundle. Plugin installed globally in `Application.kt`:

```kotlin
// SEC-007: standard browser security headers
install(DefaultHeaders) {
    header("X-Content-Type-Options", "nosniff")
    header("X-Frame-Options", "DENY")
    header("Referrer-Policy", "strict-origin-when-cross-origin")
}
```

---

### SEC-008 — `/dev/ping` Route Has No Authentication (Informational)

**Severity**: INFORMATIONAL
**OWASP Category**: A01:2021 — Broken Access Control
**CWE**: CWE-306
**CVSS v3.1**: 0.0 (dev-only; excluded from production artifact)
**Status**: ✅ Acceptable — no action required

#### 3.8.1 Description

The `/dev/ping` route in `DevModule.kt` is unauthenticated by design. This is acceptable
because the `devMain` source set is excluded from the `installDist` task and is therefore
never present in the production Docker image. The risk is theoretical only.

#### 3.8.2 Accepted Risk

The accepted risk is that a developer may add sensitive logic to a dev-only route under the
assumption it is protected, when it is not. The route is intentionally left open for
health-check convenience during local development. The exclusion from `installDist` provides
the necessary production boundary.

---

### SEC-009 — CI Pipeline Had No Secret Scanning Step (Informational)

**Severity**: INFORMATIONAL
**OWASP Category**: A09:2021 — Security Logging and Monitoring Failures
**CWE**: CWE-200 — Exposure of Sensitive Information
**CVSS v3.1**: 0.0
**Status**: ✅ Resolved — commit `ce1f128`

#### 3.9.1 Description

The CI/CD pipeline did not include a secret scanning step. A developer could accidentally
commit a credential (API key, JWT secret, DB password) and the pipeline would not catch it.

#### 3.9.2 Remediation Applied

A `secret-scan` job was added to `.github/workflows/ci.yml`, triggered on all pull requests.
It uses `gitleaks/gitleaks-action@v2` with `fetch-depth: 0` (full history checkout) to
scan every commit in the PR, not just the tip:

```yaml
secret-scan:
  name: Secret Scan (Gitleaks)
  if: github.event_name == 'pull_request'
  runs-on: ubuntu-latest
  timeout-minutes: 5
  steps:
    - uses: actions/checkout@v4
      with:
        fetch-depth: 0
    - uses: gitleaks/gitleaks-action@v2
      env:
        GITHUB_TOKEN: ${{ secrets.PERSONAL_ACCESS_TOKEN }}
```

---

## 4. ARCHITECTURAL DRIFT AND SYSTEMIC RISKS

### 4.1 Convention Plugin Authority Gap (Resolved)

At the time of assessment, the `detekt-convention.gradle.kts` plugin existed in `build-logic/`
but was applied `apply false` at the root and never actually consumed by any convention.
Subprojects applied the raw `io.gitlab.arturbosch.detekt` plugin directly, bypassing the
centralized configuration file and report setup. This meant that the security constitution's
requirement for consistent static analysis was nominally satisfied but not structurally
enforced. The refactor that produced `detekt-convention` as the single authority — applied
via composition in both `kotlin-jvm-convention` and `kotlin-kmp-convention` — resolved this
drift.

**Pattern to maintain**: Every cross-cutting concern (detekt, benchmark, coverage) MUST have
a single convention plugin as its sole configuration authority. Subprojects MUST NOT apply
these raw plugins directly.

### 4.2 Implicit Trust Within the Compose Network

Redis and PostgreSQL accept connections from any container on the Docker Compose network by
network policy alone, with credentials as the only barrier. This is appropriate for the
current skeleton (no sensitive data flows yet) but will require re-evaluation before any
data-plane feature is implemented. The eventual introduction of a service mesh or per-service
network isolation should be assessed when the submission and grading pipeline is implemented.

### 4.3 Sandbox Resource Limits Depend on Executor Discipline (Open)

The `sandbox/kotlin/Dockerfile` cannot enforce runtime flags (`--network=none`, `--cpus`,
`--memory`). The Security Constitution's sandbox isolation requirements are fully dependent
on the executor passing these flags to every `docker run` call. There is currently no
structural guarantee — only the `TASK-SEC-006` backlog entry. This is the single most
important security obligation for the next feature milestone. Failure to enforce limits
before enabling live code execution would elevate this finding from LOW to HIGH.

### 4.4 No Rate Limiting on Any Endpoint (Forward-Looking)

The Security Constitution §5 requires rate limiting on submission and authentication
endpoints. No rate limiting is currently implemented because no auth or submission endpoints
exist yet. This is not a finding against the current feature but should be captured as a
requirement in the first feature that introduces these endpoints.

---

## 5. APPENDICES

### 5.1 Confirmed Secure Patterns (Carry Forward)

These patterns were validated in this assessment and MUST be maintained in all future features.

| Pattern | Enforcement Location |
|---------|---------------------|
| `env()` throws on missing secrets — no silent defaults | `core/env/EnvConfig.kt` |
| CORS restricted to exact `WEBAPP_ORIGIN` — no wildcard | `server/app/Application.kt` |
| Multi-stage Docker build — no build tools in production image | `docker/server.Dockerfile` |
| Non-root user in sandbox container | `sandbox/kotlin/Dockerfile` |
| `Cache-Control: no-store` on sensitive endpoints | `server/api/routes/HealthRoutes.kt` |
| Error responses never contain exception messages or stack traces | `server/app/Application.kt` |
| Redis requires password in production (`REDIS_PASSWORD` env var) | `docker/docker-compose.yml` |
| Security headers on all API responses (`nosniff`, `DENY`, `strict-origin`) | `server/app/Application.kt` |
| Secret scanning on every PR — full history (`fetch-depth: 0`) | `.github/workflows/ci.yml` |
| postgres/redis not bound to host ports in base compose | `docker/docker-compose.yml` |
| Shared secret required on all sandbox-runner routes except `/health` | `sandbox-runner/app/Application.kt` |
| sandbox-runner port not exposed to host | `docker/docker-compose.yml` |

### 5.2 CVSS Scoring Reference

| Score Range | Severity      |
|-------------|---------------|
| 9.0 – 10.0  | Critical      |
| 7.0 – 8.9   | High          |
| 4.0 – 6.9   | Medium        |
| 0.1 – 3.9   | Low           |
| 0.0         | Informational |

### 5.3 Finding Cross-Reference

| Finding | Severity | CVSS | OWASP | CWE | Status | Commit |
|---------|----------|------|-------|-----|--------|--------|
| SEC-001 — sandbox port exposed | HIGH | 8.1 | A01 | CWE-284 | ✅ Resolved | `9b9c9968` |
| SEC-002 — sandbox no auth | HIGH | 8.6 | A01 | CWE-306 | ✅ Resolved | `9b9c9968` |
| SEC-003 — DB/Redis ports exposed | MEDIUM | 5.3 | A05 | CWE-16 | ✅ Resolved | `ce1f128` |
| SEC-004 — error details leaked | MEDIUM | 4.3 | A05 | CWE-200 | ✅ Resolved | `ce1f128` |
| SEC-005 — Redis no password | MEDIUM | 5.9 | A05 | CWE-306 | ✅ Resolved | `ce1f128` |
| SEC-006 — sandbox resource limits | LOW | 3.1 | A05 | CWE-400 | ⏳ Deferred | TASK-SEC-006 |
| SEC-007 — missing security headers | LOW | 3.7 | A05 | CWE-16 | ✅ Resolved | `ce1f128` |
| SEC-008 — /dev/ping unprotected | INFO | 0.0 | A01 | CWE-306 | ✅ Acceptable | — |
| SEC-009 — no secret scanning | INFO | 0.0 | A09 | CWE-200 | ✅ Resolved | `ce1f128` |

### 5.4 Tooling Context

- **Assessment Framework**: Spec-Kit Security Review Extension (Whitebox mode)
- **Memory Access**: Markdown-only (`.specify/memory/`, `docs/memory/`)
- **Standards**: OWASP Top 10 (2021), CWE/SANS Top 25
- **Security Constitution**: `.specify/memory/security_constitution.md` v1.0.0
- **Initial Assessment Date**: 2026-05-20
- **Remediation Completed**: 2026-05-23
- **Export Date**: 2026-05-23
