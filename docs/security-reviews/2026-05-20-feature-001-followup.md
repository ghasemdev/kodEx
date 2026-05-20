---
document_type: security-review
review_type: followup
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
  finding_id: "Unique finding identifier (SEC-NNN) for cross-referencing."
  location: "File path and line number of the vulnerable code."
  owasp_category: "OWASP Top 10 2021 category for this finding."
  cwe: "Common Weakness Enumeration identifier."
  cvss_score: "CVSS v3.1 base score."
  spec_kit_task: "Spec-Kit task ID for backlog tracking."
---

# SECURITY REVIEW FOLLOW-UP — feature/001-project-base-setup

## Executive Summary

9 findings from the branch review (2026-05-20). 5 findings are scheduled for immediate remediation
within this branch before merge. 3 are deferred as technical debt with explicit revisit triggers.
1 is informational and requires no action.

**This branch MUST NOT merge to `develop` until TASK-SEC-001 and TASK-SEC-002 are resolved.**

---

## Inputs Reviewed

- Security review: `docs/security-reviews/2026-05-20-feature-001-branch.md`
- Tasks backlog: `specs/001-project-base-setup/tasks.md` (no existing TASK-SEC-* entries)
- Memory: `docs/memory/INDEX.md`, `docs/memory/ARCHITECTURE.md`
- Constitution: `.specify/memory/security_constitution.md`

---

## Resolution Decisions

| Finding | Severity | Decision | Rationale |
|---------|----------|----------|-----------|
| SEC-001 sandbox-runner port exposed | HIGH | **Implement now** | 2-line fix; merge blocker |
| SEC-002 sandbox-runner no auth | HIGH | **Implement now** | Skeleton only; merge blocker |
| SEC-003 postgres/redis ports exposed | MEDIUM | **Technical debt** | Dev convenience; low risk on localhost |
| SEC-004 error details leaked | MEDIUM | **Implement now** | 10-line fix; prevents info disclosure |
| SEC-005 Redis no password | MEDIUM | **Implement now** | 3-line fix in compose |
| SEC-006 sandbox resource limits | LOW | **Technical debt** | Executor not yet implemented |
| SEC-007 missing security headers | LOW | **Implement now** | One plugin install |
| SEC-008 /dev/ping unauthenticated | INFO | **Acceptable** | Dev-only; never in production artifact |
| SEC-009 no CI secret scanning | INFO | **Technical debt** | Low urgency; standalone action |

---

## Immediate Remediation Tasks

| Task ID | Title | Severity | Source | Depends On | Acceptance Criteria |
|---------|-------|----------|--------|------------|---------------------|
| TASK-SEC-001 | Remove sandbox-runner host port binding | HIGH | SEC-001 | — | `docker-compose.yml` has no `ports:` on `sandbox-runner`; service reachable by `server` via `http://sandbox-runner:8081` only |
| TASK-SEC-002 | Add shared-secret authentication to sandbox-runner | HIGH | SEC-002 | TASK-SEC-001 | All routes except `/health` return 401 if `X-Sandbox-Secret` header is missing or wrong; env var `SANDBOX_SHARED_SECRET` loaded via `env()` |
| TASK-SEC-004 | Remove internal error details from HTTP responses | MEDIUM | SEC-004 | — | `StatusPages` handler logs full exception server-side; client receives only `{"error":"An unexpected error occurred."}` in envelope format; no stack trace or message text in response body |
| TASK-SEC-005 | Enforce Redis password authentication in Compose | MEDIUM | SEC-005 | — | `redis` service starts with `--requirepass $REDIS_PASSWORD`; empty password is tolerated in local dev with a warning comment; updated in `.env.example` |
| TASK-SEC-007 | Install global security response headers | LOW | SEC-007 | — | Ktor `DefaultHeaders` plugin installed; responses include `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin` |

---

## Technical Debt Backlog

### TASK-SEC-003 — Unexpose PostgreSQL and Redis ports from host

**Severity**: MEDIUM
**Source**: SEC-003
**Type**: Technical Debt

**Why safe to defer**: On developer machines, bound ports are only reachable from localhost. No shared staging environment exists yet. Risk is low while the team is a single developer.

**Remaining risk**: If the Docker host is shared (e.g., a cloud dev box or CI runner with Docker-in-Docker), port exposure becomes exploitable.

**Remediation plan**:
1. In `docker/docker-compose.yml`, replace `ports:` with `expose:` for `postgres` and `redis`.
2. Create `docker/docker-compose.dev.yml` override that restores `ports:` for local tooling (TablePlus, Redis Insight).
3. Update quickstart.md: `docker compose -f docker/docker-compose.yml -f docker/docker-compose.dev.yml up`.

**Revisit trigger**: Before any shared staging environment or cloud deployment is provisioned.
**Target milestone**: Feature 002 (first feature requiring a staging environment).

---

### TASK-SEC-006 — Enforce sandbox resource limits in executor

**Severity**: LOW
**Source**: SEC-006
**Type**: Technical Debt

**Why safe to defer**: The `sandbox-runner/executor` module is a placeholder (`/.gitkeep`) — no Docker container lifecycle code exists yet. Resource limits cannot be enforced until the executor is implemented.

**Remaining risk**: Without limits, a runaway sandbox container could exhaust host CPU or memory. This risk is only realized when the executor is wired up and executes real submissions.

**Remediation plan**:
When implementing `sandbox-runner/executor`, the `docker run` call MUST include:
- `--network=none`
- `--cpus=<exam.cpuLimit>` (default: 2.0)
- `--memory=<exam.memoryLimit>` (default: 512m)
- `--stop-timeout=<exam.wallClockTimeout>` (default: 10s)
- `--read-only` with explicit writable tmpfs mounts

Add a unit test that asserts these flags are always present in the container start parameters (mock Docker client).

**Revisit trigger**: Before the first sandbox execution task is implemented.
**Target milestone**: Feature implementing code submission execution (likely feature 003 or 004).

---

### TASK-SEC-009 — Add secret scanning to CI pipeline

**Severity**: INFORMATIONAL
**Source**: SEC-009
**Type**: Technical Debt

**Why safe to defer**: GitHub's built-in push protection can be enabled at the repository level without code changes. This is a repository settings task, not a code task.

**Remaining risk**: A committed credential would not be caught by the current pipeline.

**Remediation plan**:
Add one of:
- `gitleaks/gitleaks-action@v2` as a CI job
- Enable GitHub Advanced Security + Secret Scanning on the repository settings page

**Revisit trigger**: Before inviting any external contributors or making the repository public.
**Target milestone**: Pre-launch hardening phase.

---

## Already Covered / Acceptable

| Finding | Status | Notes |
|---------|--------|-------|
| SEC-008 `/dev/ping` unauthenticated | ✅ Acceptable | `devMain` source set is excluded from `installDist`; route never ships to production. No task needed. |

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
