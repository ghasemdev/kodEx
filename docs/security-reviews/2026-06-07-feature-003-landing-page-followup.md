---
document_type: security-review
review_type: followup
assessment_date: 2026-06-07
codebase_analyzed: kodex / feature/003-landing-page
total_files_analyzed: 4
total_findings: 4
overall_risk: MODERATE
critical_count: 0
high_count: 0
medium_count: 3
low_count: 1
informational_count: 0
owasp_categories: [A03, A05]
cwe_ids: [CWE-116, CWE-693, CWE-441, CWE-20]
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
  owasp_categories: "OWASP Top 10 2025 categories (A01-A10) that have at least one finding."
  cwe_ids: "CWE identifiers referenced in this document."
  finding_id: "Unique finding identifier (SEC-NNN) for cross-referencing and task linkage."
  location: "File path and line number of the vulnerable code (path/to/file.ext:line)."
  owasp_category: "OWASP Top 10 2025 category for this finding (AXX:2025-Name)."
  cwe: "Common Weakness Enumeration identifier with short name (CWE-NNN: Name)."
  cvss_score: "CVSS v3.1 base score (0.0-10.0). 9.0+=Critical, 7.0-8.9=High, 4.0-6.9=Medium, 0.1-3.9=Low."
  spec_kit_task: "Spec-Kit task ID for backlog tracking and remediation follow-up (TASK-SEC-NNN)."
---

# SECURITY REVIEW FOLLOW-UP PLAN — feature/003-landing-page

## Executive Summary

4 findings from the 2026-06-07 branch review. **3 implement now** (trivial effort, constitution
violations, compound risk when SEC-002 + SEC-003 are both present). **1 track as technical debt**
(low severity, blocked on infrastructure decision, implementation diverged from original task
intent).

Total remediation effort: ~30 lines of code across 4 files. No blocking architectural change
required.

---

## Inputs Reviewed

| Artifact | Path |
|---|---|
| Branch security review | `docs/security-reviews/2026-06-07-feature-003-landing-page.md` |
| Task backlog | `specs/003-landing-page/tasks.md` |
| Security constitution | `.specify/memory/security_constitution.md` |

---

## Resolution Decisions

| Finding | Severity | Decision | Rationale |
|---|---|---|---|
| SEC-001 X-Request-Id reflection | Medium | **Implement now** | Explicit constitution rule §6; affects 3 call sites; ~20 lines |
| SEC-002 CSP localhost in index.html | Medium | **Implement now** | 1-line fix; constitution sync rule; compounds with SEC-003 |
| SEC-003 NetworkKoinModule base URL | Medium | **Implement now** | 3-line deletion; removes local-network probe surface; prerequisite for SEC-002 to be meaningful |
| SEC-004 Rate-limit key collapses behind proxy | Low | **Track as technical debt** | Low severity; depends on infra (reverse proxy decision not made); task T037 already specified `origin.remoteHost` but implementation diverged — implementation bug, not design gap |

---

## Immediate Remediation Tasks

### Backlog Table

| Task ID | Title | Severity | Type | Source Finding | Depends On | Acceptance Criteria |
|---|---|---|---|---|---|---|
| TASK-SEC-001 | Add `sanitizeRequestId()` utility and apply at all server call sites | Medium | Implement | SEC-001 | — | All three call sites use `sanitizeRequestId()`; unit test covers valid UUID passthrough + invalid string → fresh UUID; `koverVerify` still passes |
| TASK-SEC-002 | Remove `http://localhost:8080` from `index.html` CSP `connect-src` | Medium | Implement | SEC-002 | TASK-SEC-003 | `connect-src 'self'` only in `index.html`; in sync with Ktor `DefaultHeaders` CSP; dev proxy still routes `/api/*` correctly |
| TASK-SEC-003 | Remove hardcoded base URL from `NetworkKoinModule` | Medium | Implement | SEC-003 | — | `DefaultRequest { url(...) }` block deleted; API calls use relative paths; `jsBrowserTest` passes; no localhost:8080 in compiled JS |

---

### TASK-SEC-001 — `sanitizeRequestId()` utility

**Source:** SEC-001 — `LandingRoutes.kt:17`, `HealthRoutes.kt:18`, `Application.kt` (StatusPages)

Create `server/api/src/main/kotlin/dev/kodex/server/api/util/RequestId.kt`:

```kotlin
package dev.kodex.server.api.util

import kotlin.uuid.Uuid

private val UUID_REGEX = Regex(
    "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
    RegexOption.IGNORE_CASE,
)

fun sanitizeRequestId(raw: String?): String =
    if (raw != null && raw.matches(UUID_REGEX)) raw else Uuid.random().toString()
```

Replace at all 3 call sites:

```kotlin
// Before (all 3 files):
val requestId = call.request.headers["X-Request-Id"] ?: Uuid.random().toString()

// After:
val requestId = sanitizeRequestId(call.request.headers["X-Request-Id"])
```

Add a test to `server/api/src/test/kotlin/dev/kodex/server/api/util/RequestIdTest.kt`:

```kotlin
class RequestIdTest : FunSpec({
    test("valid UUID passthrough") {
        val id = "550e8400-e29b-41d4-a716-446655440000"
        sanitizeRequestId(id) shouldBe id
    }
    test("null returns fresh UUID") {
        sanitizeRequestId(null).length shouldBe 36
    }
    test("script injection replaced with UUID") {
        val result = sanitizeRequestId("<script>alert(1)</script>")
        result shouldNotBe "<script>alert(1)</script>"
        result.length shouldBe 36
    }
    test("oversized string replaced") {
        sanitizeRequestId("a".repeat(500)).length shouldBe 36
    }
})
```

---

### TASK-SEC-002 — Remove localhost from CSP

**Source:** SEC-002 — `app/webApp/src/webMain/resources/index.html:12`
**Depends on:** TASK-SEC-003 must be completed first.

One-line change in `index.html`:

```diff
- connect-src 'self' http://localhost:8080;">
+ connect-src 'self';">
```

Vite's dev proxy in `vite.config.ts` routes `/api/*` → `localhost:8080`. All `ApiRoutes` constants
use relative paths (`/api/v1/...`), so they resolve as same-origin through Vite and satisfy
`connect-src 'self'` without an explicit localhost allowance.

---

### TASK-SEC-003 — Remove hardcoded base URL from NetworkKoinModule

**Source:** SEC-003 — `app/webApp/src/webMain/kotlin/dev/kodex/webapp/di/NetworkKoinModule.kt:17`

Delete the `DefaultRequest` block (~3 lines):

```diff
  @Single
  fun httpClient(): HttpClient = HttpClient(Js) {
-     install(DefaultRequest) {
-         url("http://localhost:8080")
-     }
      install(ContentNegotiation) {
```

---

## Technical Debt Backlog

### TASK-SEC-004 — Fix rate-limit key to use real client IP via ForwardedHeaders

**Source:** SEC-004 — `server/app/src/main/kotlin/dev/kodex/server/Application.kt:99`
**Type:** Technical Debt
**Severity:** Low
**OWASP:** A05:2021

**Why safe to defer:**
The landing stats endpoint (`/api/v1/stats/landing`) is unauthenticated, read-only, and returns
static data. The impact of a collapsed rate-limit key (all clients sharing one bucket) is bounded:
at worst, a noisy client causes brief HTTP 429s for others.

**Note on T037 divergence:** Task T037 in `tasks.md` correctly specified
`requestKey { call -> call.request.origin.remoteHost }`. The implementation landed on
`call.request.local.remoteHost` instead. This is an implementation bug vs. design intent.

**Remaining risk:** Behind a reverse proxy or load balancer, all users share one rate-limit
bucket. One misbehaving client can trigger 429 for everyone, or receive unlimited requests if
the proxy IP is never exhausted.

**Revisit trigger:** Either of:
1. A reverse proxy / Nginx / AWS ALB is introduced in front of the Ktor server
2. Auth or submission rate limits are added (higher stakes)
3. A new feature spec adds endpoints where per-client limiting is security-critical

**Target milestone:** `feature/005-auth` or `infra/001-production-deploy` — whichever comes first

**What to implement when triggered:**

```kotlin
// Application.kt — add before install(RateLimit):
install(XForwardedHeaders)   // trust X-Forwarded-For from known proxy

// Update requestKey:
requestKey { call -> call.request.origin.remoteHost }
```

Restrict `XForwardedHeaders` to trusted proxy CIDRs to prevent client-supplied header spoofing.

---

## Already Covered Items

None. All 4 findings are new — not previously tracked in `tasks.md`.

*(T037 specified the correct rate-limit key accessor but was an implementation task; the security
gap requiring ForwardedHeaders for proxy environments was not explicitly captured as a security
concern.)*

---

## Confirmed Secure Patterns (Unchanged)

All patterns confirmed in the branch review remain intact: Kilua text-node rendering,
`BadgeDefinition.init` validation, `ALLOWED_LOCALES` filtering, CORS/DefaultHeaders configuration.

---

## Execution Order

```
TASK-SEC-003  (delete DefaultRequest base URL — no JS calls to localhost)
      ↓
TASK-SEC-002  (remove localhost from CSP — safe now that JS no longer references it)
      ↓
TASK-SEC-001  (add sanitizeRequestId util + unit test)
      ↓
verify: ./gradlew :app:webApp:jsBrowserTest :server:api:test koverVerify
      ↓
TASK-SEC-004  (defer → feature/005-auth or infra/001-production-deploy)
```
