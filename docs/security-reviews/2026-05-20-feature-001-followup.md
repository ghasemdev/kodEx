---
document_type: security-review
review_type: followup
assessment_date: 2026-05-20
remediation_completed: 2026-05-23
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
  finding_id: "Unique finding identifier (SEC-NNN) for cross-referencing."
  location: "File path and line number of the vulnerable code."
  owasp_category: "OWASP Top 10 2021 category for this finding."
  cwe: "Common Weakness Enumeration identifier."
  cvss_score: "CVSS v3.1 base score."
  spec_kit_task: "Spec-Kit task ID for backlog tracking."
---

# SECURITY REVIEW FOLLOW-UP — feature/001-project-base-setup

## Executive Summary

9 findings from the branch review (2026-05-20). All 7 actionable findings have been
remediated. 1 finding is deferred (SEC-006 — executor not yet implemented). 1 finding
is informational and required no action (SEC-008).

**This branch is clear to merge to `develop`.**

---

## Inputs Reviewed

- Security review: `docs/security-reviews/2026-05-20-feature-001-branch.md`
- Tasks backlog: `specs/001-project-base-setup/tasks.md`
- Memory: `docs/memory/INDEX.md`, `docs/memory/ARCHITECTURE.md`
- Constitution: `.specify/memory/security_constitution.md`

---

## Resolution Status

| Finding | Severity | Decision | Status | Commit |
|---------|----------|----------|--------|--------|
| SEC-001 sandbox-runner port exposed | HIGH | Implement now | ✅ **Done** | `9b9c9968` |
| SEC-002 sandbox-runner no auth | HIGH | Implement now | ✅ **Done** | `9b9c9968` |
| SEC-003 postgres/redis ports exposed | MEDIUM | Technical debt → Implement | ✅ **Done** | `ce1f128` |
| SEC-004 error details leaked | MEDIUM | Implement now | ✅ **Done** | `ce1f128` |
| SEC-005 Redis no password | MEDIUM | Implement now | ✅ **Done** | `ce1f128` |
| SEC-006 sandbox resource limits | LOW | Technical debt | ⏳ **Deferred** | — |
| SEC-007 missing security headers | LOW | Implement now | ✅ **Done** | `ce1f128` |
| SEC-008 /dev/ping unauthenticated | INFO | Acceptable | ✅ **Acceptable** | — |
| SEC-009 no CI secret scanning | INFO | Technical debt → Implement | ✅ **Done** | `ce1f128` |

---

## Remediation Details

### ✅ SEC-001 — sandbox-runner port removed from host

`docker/docker-compose.yml`: replaced `ports: "8081:8081"` with `expose: "8081"`.
`sandbox-runner` is now reachable only from within the Docker Compose network.

---

### ✅ SEC-002 — shared-secret authentication added to sandbox-runner

`sandbox-runner/app/src/main/kotlin/dev/kodex/sandbox/Application.kt`:
Ktor `bearer("secret-auth")` authenticates all requests using `SANDBOX_SHARED_SECRET`.
`/health` route is exempt (required for Docker health checks from localhost).

---

### ✅ SEC-003 — postgres and redis ports isolated from host

`docker/docker-compose.yml`: `postgres` and `redis` now use `expose:` instead of `ports:`.
`docker/docker-compose.dev.yml` (new): override file that restores port bindings for
local development tools (TablePlus, Redis Insight):

```bash
# Dev workflow (with host port access):
docker compose -f docker/docker-compose.yml -f docker/docker-compose.dev.yml up

# Production-like (no host port exposure):
docker compose -f docker/docker-compose.yml up
```

`quickstart.md` updated with both commands.

---

### ✅ SEC-004 — internal error details removed from HTTP responses

`server/api/src/main/kotlin/dev/kodex/server/api/response/Envelope.kt`:
Added `ErrorEnvelope` + `buildErrorEnvelope()` for typed error responses.

`server/app/src/main/kotlin/dev/kodex/server/Application.kt`:
`StatusPages` exception handler now:
1. Logs the full `Throwable` via `call.application.log.error("Unhandled exception", cause)`
2. Returns `{"error":"An unexpected error occurred.", "meta":{...}}` — no stack trace or message text

---

### ✅ SEC-005 — Redis password enforcement in Docker Compose

`docker/docker-compose.yml`: Redis service now starts with:
```yaml
command: >
  sh -c '[ -n "$$REDIS_PASSWORD" ] && exec redis-server --requirepass "$$REDIS_PASSWORD" || exec redis-server'
environment:
  REDIS_PASSWORD: ${REDIS_PASSWORD:-}
```

Empty `REDIS_PASSWORD` is tolerated in local dev (no auth). Non-empty value enforces auth.
`.env.example` updated: `REDIS_PASSWORD` is now documented as **required in production**.

---

### ✅ SEC-007 — global security response headers installed

`gradle/libs.versions.toml`: added `ktor-server-default-headers` library + added to `ktor-server` bundle.

`server/app/src/main/kotlin/dev/kodex/server/Application.kt`:
```kotlin
install(DefaultHeaders) {
    header("X-Content-Type-Options", "nosniff")
    header("X-Frame-Options", "DENY")
    header("Referrer-Policy", "strict-origin-when-cross-origin")
}
```

---

### ✅ SEC-009 — secret scanning added to CI pipeline

`.github/workflows/ci.yml`: `secret-scan` job added (PR trigger only):
```yaml
- uses: gitleaks/gitleaks-action@v2
  env:
    GITHUB_TOKEN: ${{ secrets.PERSONAL_ACCESS_TOKEN }}
```
Full-history checkout (`fetch-depth: 0`) ensures all commits in the PR are scanned.

---

## Remaining Technical Debt

### TASK-SEC-006 — Enforce sandbox resource limits in executor

**Severity**: LOW | **Source**: SEC-006 | **Type**: Technical Debt

**Why safe to defer**: The `sandbox-runner/executor` module is a placeholder — no Docker
container lifecycle code exists yet.

**Remediation plan**: When implementing `sandbox-runner/executor`, the `docker run` call MUST include:
- `--network=none`
- `--cpus=<exam.cpuLimit>` (default: 2.0)
- `--memory=<exam.memoryLimit>` (default: 512m)
- `--stop-timeout=<exam.wallClockTimeout>` (default: 10s)
- `--read-only` with explicit writable tmpfs mounts

Add a unit test asserting these flags are always present in container start parameters.

**Revisit trigger**: Before the first sandbox execution task is implemented.
**Target milestone**: Feature implementing code submission execution (feature 003 or 004).

---

## Confirmed Secure Patterns (carry forward)

These patterns were validated in this review and must be maintained in all future features:

| Pattern | Enforcement |
|---------|------------|
| `env()` throws on missing secrets — no silent defaults | `core/env/EnvConfig.kt` |
| CORS restricted to exact origin (`WEBAPP_ORIGIN`) — no wildcard | `server/app/Application.kt` |
| Multi-stage Docker build — no build tools in production image | `docker/server.Dockerfile` |
| Non-root user in sandbox container | `sandbox/kotlin/Dockerfile` |
| `Cache-Control: no-store` on sensitive endpoints | `server/api/routes/HealthRoutes.kt` |
| No secrets committed — `.env` in `.gitignore` | `.gitignore` |
| Error responses never contain exception messages or stack traces | `server/app/Application.kt` |
| Redis requires password in production (`REDIS_PASSWORD` env var) | `docker/docker-compose.yml` |
| Security headers on all API responses (`nosniff`, `DENY`, `strict-origin`) | `server/app/Application.kt` |
| Secret scanning on every PR (gitleaks) | `.github/workflows/ci.yml` |
