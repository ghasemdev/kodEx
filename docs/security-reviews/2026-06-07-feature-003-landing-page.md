---
document_type: security-review
review_type: branch
assessment_date: 2026-06-07
codebase_analyzed: kodex / feature/003-landing-page
total_files_analyzed: 112
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

# SECURITY REVIEW REPORT — BRANCH: feature/003-landing-page vs develop

## Executive Summary

This branch introduces the KodEx landing page: a public-facing Kotlin/JS frontend, a new
`/api/v1/stats/landing` endpoint, rate limiting, and supporting shared models. Overall risk is
**MODERATE**. No critical or high findings. Three medium and one low finding require remediation
before merge — all related to CSP misconfiguration, an unvalidated header reflection, and a
rate-limit key that collapses behind a reverse proxy.

The frontend correctly uses Kilua's `+` (text-node) operator throughout. `BadgeDefinition`
validates `id` and `iconPath` in `init` blocks. `ALLOWED_LOCALES` filtering in `I18nSetup.kt` is
intact. Ktor `DefaultHeaders`, CORS, and response security headers are correctly installed.

---

## Branch Diff Reviewed

**Target:** `feature/003-landing-page`
**Base:** `develop`
**Security-relevant files analyzed:** 12 source files + `index.html`

| Area | Files |
|---|---|
| Frontend sections / layout | `HeroSection.kt`, `GlobalNavBar.kt`, `Footer.kt`, `ExamTypesSection.kt`, `GamificationSection.kt`, `LeaderboardSection.kt`, `ProblemsSection.kt`, `CreateExamSection.kt` |
| Frontend networking | `NetworkKoinModule.kt`, `ApiRoutes.kt`, `LandingStatsRemoteDataSourceImpl.kt` |
| Server routes | `LandingRoutes.kt`, `HealthRoutes.kt`, `Application.kt`, `Envelope.kt` |
| Shared models | `ApiEnvelope.kt`, `BadgeDefinition.kt` |
| CSP | `index.html` |

---

## Vulnerability Findings

---

### [MEDIUM] X-Request-Id reflected into response body without UUID validation

**Finding ID:** SEC-001
**Location:** `server/api/src/main/kotlin/dev/kodex/server/api/routes/LandingRoutes.kt:17`,
`server/app/src/main/kotlin/dev/kodex/server/Application.kt` (StatusPages handler),
`server/api/src/main/kotlin/dev/kodex/server/api/routes/HealthRoutes.kt:18`
**OWASP Category:** A03:2021 — Injection
**CWE:** CWE-116: Improper Encoding or Escaping of Output / CWE-20: Improper Input Validation
**CVSS Score:** 4.3 (Medium)

**Description:**
All three endpoints accept `X-Request-Id` from the client request and reflect it verbatim into
`meta.requestId` of every JSON response envelope with no UUID format validation:

```kotlin
// LandingRoutes.kt:17 — same pattern in Application.kt and HealthRoutes.kt
val requestId = call.request.headers["X-Request-Id"] ?: Uuid.random().toString()
```

While JSON serialization prevents direct XSS, this violates the explicit constitution rule §6:
*"X-Request-Id header values supplied by clients MUST be validated as UUID format before being
reflected into response bodies or logs. Non-UUID values MUST be replaced with a fresh
`Uuid.random()`."* A malicious caller can inject control characters into structured logs (log
injection — CWE-117) and response metadata.

**Remediation:**
Create a shared utility `sanitizeRequestId()` and apply it at all three call sites:

```kotlin
// server/api/src/main/kotlin/dev/kodex/server/api/util/RequestId.kt
private val UUID_REGEX = Regex(
    "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
    RegexOption.IGNORE_CASE,
)
fun sanitizeRequestId(raw: String?): String =
    if (raw != null && raw.matches(UUID_REGEX)) raw else Uuid.random().toString()

// All call sites:
val requestId = sanitizeRequestId(call.request.headers["X-Request-Id"])
```

**Spec-Kit Task:** TASK-SEC-001

---

### [MEDIUM] CSP `connect-src` hardcodes `http://localhost:8080` in `index.html` — out of sync with Ktor header

**Finding ID:** SEC-002
**Location:** `app/webApp/src/webMain/resources/index.html:12`,
`server/app/src/main/kotlin/dev/kodex/server/Application.kt` (DefaultHeaders CSP)
**OWASP Category:** A05:2021 — Security Misconfiguration
**CWE:** CWE-693: Protection Mechanism Failure
**CVSS Score:** 5.3 (Medium)

**Description:**
The `index.html` CSP was modified to add `http://localhost:8080` to `connect-src`:

```html
<!-- index.html — as committed -->
connect-src 'self' http://localhost:8080;
```

But the Ktor `DefaultHeaders` CSP remains:

```kotlin
// Application.kt — Ktor header
"connect-src 'self'; " +
```

Two problems:

1. **Out-of-sync declarations** — The constitution §6 requires: *"Both declarations MUST stay in sync."*
2. **Localhost exposure in production** — Production browsers trust connections to
   `http://localhost:8080`, potentially reaching internal services running on the user's own
   machine (local network probing). The development-only allowance must never be committed to
   `index.html`.

**Remediation:**
Remove the localhost allowance from `index.html` and sync both declarations to `connect-src 'self'`.
The Vite dev proxy already routes `/api/*` → `localhost:8080`, so relative-path API calls work
under `'self'` without any explicit localhost rule:

```html
<!-- index.html -->
connect-src 'self';
```

```kotlin
// Application.kt — already correct, no change needed
"connect-src 'self'; " +
```

**Spec-Kit Task:** TASK-SEC-002

---

### [MEDIUM] `NetworkKoinModule` hardcodes `http://localhost:8080` as API base URL

**Finding ID:** SEC-003
**Location:** `app/webApp/src/webMain/kotlin/dev/kodex/webapp/di/NetworkKoinModule.kt:17`
**OWASP Category:** A05:2021 — Security Misconfiguration
**CWE:** CWE-693: Protection Mechanism Failure
**CVSS Score:** 4.0 (Medium)

**Description:**
The Ktor `HttpClient` always sets its base URL to `http://localhost:8080`:

```kotlin
install(DefaultRequest) {
    url("http://localhost:8080")   // always applied, including in production builds
}
```

The inline comment states *"Prod: same-origin — no base URL override needed"* but provides no
conditional path to remove or replace this value. In production:

- Every API call targets the **user's** `localhost:8080` — not the production backend.
- Combined with SEC-002, the browser is explicitly permitted (CSP) to make these connections.
- This compounds into a local-network probing vector where the browser silently attempts HTTP
  connections to arbitrary services running on the visitor's machine.

**Remediation:**
Remove the `DefaultRequest` base URL entirely. All `ApiRoutes` constants use relative paths
(`/api/v1/...`) which the Vite dev proxy forwards to `localhost:8080` in development and resolve
to same-origin in production:

```kotlin
// NetworkKoinModule.kt — remove the DefaultRequest block entirely
// install(DefaultRequest) { url("http://localhost:8080") }  ← DELETE
```

If an explicit base URL is needed for non-Vite environments, supply it via `BuildConfig` or an
environment-injected constant — never hardcoded.

**Spec-Kit Task:** TASK-SEC-003

---

### [LOW] Rate-limit key uses `local.remoteHost` — collapses to proxy IP behind a load balancer

**Finding ID:** SEC-004
**Location:** `server/app/src/main/kotlin/dev/kodex/server/Application.kt:99`
**OWASP Category:** A05:2021 — Security Misconfiguration
**CWE:** CWE-441: Unintended Proxy or Intermediary
**CVSS Score:** 3.1 (Low)

**Description:**
```kotlin
requestKey { call -> call.request.local.remoteHost }
```

`local.remoteHost` is the IP of the direct TCP peer. Behind a load balancer or reverse proxy this
is always the proxy's IP — all clients share one rate-limit bucket. A single misbehaving client
can exhaust the bucket for all users, or an attacker can bypass per-IP limits entirely.

Low priority for the current landing-stats endpoint (unauthenticated, low value), but must be
corrected before auth and submission endpoints are added.

**Remediation:**
Install Ktor's `ForwardedHeaders` plugin and resolve the real client IP:

```kotlin
install(ForwardedHeaders)  // or XForwardedHeaders — pick one, not both

// Rate limit key:
requestKey { call -> call.request.origin.remoteHost }
```

Restrict `ForwardedHeaders` to trusted proxy CIDRs to prevent client-supplied header spoofing.

**Spec-Kit Task:** TASK-SEC-004

---

## Confirmed Secure Patterns

| Pattern | Location | Status |
|---|---|---|
| Kilua `+` text-node rendering (no `innerHTML`) | All landing sections | ✅ Compliant |
| `BadgeDefinition.id` validated with regex in `init` block | `BadgeDefinition.kt` | ✅ Compliant |
| `BadgeDefinition.iconPath` validated with allowlist regex | `BadgeDefinition.kt` | ✅ Compliant |
| `ALLOWED_LOCALES` filtering for `localStorage` locale input | `I18nSetup.kt` | ✅ Compliant |
| `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy` headers | `Application.kt` | ✅ Compliant |
| CORS restricted to `WEBAPP_ORIGIN` env var | `Application.kt` | ✅ Compliant |
| Rate limiting applied to public landing endpoint | `Application.kt` | ✅ Present (key strategy: see SEC-004) |
| No secrets or credentials hardcoded | All files | ✅ Compliant |
| `Cache-Control: public, max-age=300` on non-sensitive stats endpoint | `LandingRoutes.kt` | ✅ Appropriate |
| `Cache-Control: no-store` on health endpoint | `HealthRoutes.kt` | ✅ Compliant |
| Stats are static constants — no DB query, no injection surface | `LandingRoutes.kt` | ✅ N/A |
| `username` rendered via `+` operator and `attribute()` (not innerHTML) | `GlobalNavBar.kt` | ✅ Compliant |

---

## Prioritized Action Plan

| Priority | ID | Action | Effort |
|---|---|---|---|
| 1 | SEC-001 | Add `sanitizeRequestId()` util; apply at LandingRoutes, HealthRoutes, Application StatusPages | Small (~20 lines) |
| 2 | SEC-002 | Remove `http://localhost:8080` from `index.html` CSP `connect-src`; already synced with Ktor | Trivial (1 line) |
| 3 | SEC-003 | Remove `DefaultRequest { url(...) }` from `NetworkKoinModule`; rely on Vite proxy + relative paths | Small (~3 lines) |
| 4 | SEC-004 | Install `ForwardedHeaders` plugin; update `requestKey` before adding auth/submission endpoints | Small (defer until proxy infra confirmed) |
