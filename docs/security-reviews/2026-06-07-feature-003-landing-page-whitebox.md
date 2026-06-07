---
document_type: security-review
review_type: whitebox-assessment
assessment_date: 2026-06-07
export_date: 2026-06-07
codebase_analyzed: KodEx / feature/003-landing-page
total_files_analyzed: 18
total_findings: 4
residual_risk: LOW
critical_count: 0
high_count: 0
medium_count: 3
low_count: 1
informational_count: 0
findings_resolved: 3
findings_deferred: 1
owasp_categories: [A03, A05]
cwe_ids: [CWE-20, CWE-116, CWE-441, CWE-693]
---

# KODEX — WHITEBOX SECURITY ASSESSMENT REPORT

**Feature Branch**: `feature/003-landing-page`
**Assessment Date**: 2026-06-07
**Remediation Completed**: 2026-06-07
**Exported**: 2026-06-07
**Assessor**: AI Security Auditor (Spec-Kit Security Review Extension — Whitebox mode)

---

## 1. EXECUTIVE SUMMARY

### 1.1 Assessment Overview

This report consolidates the findings from a Whitebox Security Assessment conducted on
2026-06-07 against the `feature/003-landing-page` branch of the KodEx platform, together
with full remediation evidence collected on the same date. The assessment was performed with
complete access to source code, architectural decision records, the KodEx Security Constitution
(`.specify/memory/security_constitution.md` v1.1.0), the prior feature-002 whitebox assessment,
and the durable repository memory hub (`docs/memory/`). The scope covered the public-facing
landing page (8 Kilua/JS sections), the new `/api/v1/stats/landing` Ktor endpoint, rate
limiting, and all new shared models and networking infrastructure introduced by the branch.

### 1.2 Risk Posture

**Overall Risk Rating at Assessment: MODERATE**
**Residual Risk Rating at Export: LOW**

| Severity      | Count | Status          | Primary OWASP Categories       |
|---------------|-------|-----------------|-------------------------------|
| Critical      | 0     | —               | —                             |
| High          | 0     | —               | —                             |
| Medium        | 3     | ✅ Resolved     | A03, A05 — Injection, Misconfig |
| Low           | 1     | ⏳ Deferred     | A05 — Security Misconfiguration |
| Informational | 0     | —               | —                             |

All 3 MEDIUM findings were remediated in a single security fix commit. The LOW finding is
formally deferred as technical debt tracked as `TASK-SEC-004`.

### 1.3 Key Findings and Strategic Impact

The highest-severity finding (SEC-002, CVSS 5.3) was the presence of
`http://localhost:8080` in the `connect-src` directive of `index.html`. Alone, this is a
misconfiguration that allows production browsers to connect to services on the user's local
machine. Combined with SEC-003 — where `NetworkKoinModule` unconditionally set
`http://localhost:8080` as the Ktor `HttpClient` base URL — the compound effect is a
local-network probing vector: every API call from a production visitor's browser was directed
to `localhost:8080` on their own machine, and the CSP explicitly permitted those connections.

SEC-001 (CVSS 4.3) covers the direct violation of Security Constitution §6: client-supplied
`X-Request-Id` header values were reflected verbatim into JSON response bodies at all three
server call sites, with no UUID format validation. While JSON serialization prevents XSS, the
path enables log injection (CWE-117) and violates an explicit constitution rule.

The low finding (SEC-004, CVSS 3.1) concerns the rate-limit key using `local.remoteHost`
instead of `origin.remoteHost`. Behind a load balancer, this collapses all clients into one
bucket. This is safe to defer because the affected endpoint (`/api/v1/stats/landing`) is
unauthenticated and read-only, and because task T037 already specified the correct accessor
(`origin.remoteHost`) — the gap is an implementation divergence, not a design failure.

No XSS surface was found in the 8 landing page sections. All text rendering uses Kilua's
`+` operator throughout.

### 1.4 Remediation Roadmap

The 3 MEDIUM findings were remediated in a single commit in the order required by their
dependency:

```
TASK-SEC-003 → TASK-SEC-002 → TASK-SEC-001
```

SEC-003 was implemented first because removing the `HttpClient` base URL makes the CSP
`connect-src 'self'` change in SEC-002 semantically correct. SEC-001 is independent and
was applied last. SEC-004 is deferred to `feature/005-auth` or `infra/001-production-deploy`,
whichever arrives first.

---

## 2. ASSESSMENT METHODOLOGY

### 2.1 Scope of Work

| Asset | Coverage |
|-------|----------|
| `app/webApp/src/webMain/resources/index.html` | CSP, meta headers — full review |
| `app/webApp/src/webMain/kotlin/.../di/NetworkKoinModule.kt` | HttpClient config, base URL — full review |
| `app/webApp/src/webMain/kotlin/.../di/ApiRoutes.kt` | Route constants, relative-path enforcement — full review |
| `app/webApp/src/webMain/kotlin/.../network/LandingStatsRemoteDataSourceImpl.kt` | Network call, header usage — full review |
| `server/api/src/main/kotlin/.../routes/LandingRoutes.kt` | X-Request-Id, response headers — full review |
| `server/api/src/main/kotlin/.../routes/HealthRoutes.kt` | X-Request-Id, cache headers — full review |
| `server/app/src/main/kotlin/.../Application.kt` | DefaultHeaders, CORS, RateLimit, StatusPages — full review |
| `server/api/src/main/kotlin/.../util/RequestId.kt` | Remediation utility — full review |
| `app/webApp/src/webMain/kotlin/.../sections/HeroSection.kt` | Rendering, stat display — XSS review |
| `app/webApp/src/webMain/kotlin/.../sections/GlobalNavBar.kt` | Username rendering, aria attrs — XSS review |
| `app/webApp/src/webMain/kotlin/.../sections/Footer.kt` | Link rendering, social icons — XSS review |
| `app/webApp/src/webMain/kotlin/.../sections/ExamTypesSection.kt` | Card rendering — XSS review |
| `app/webApp/src/webMain/kotlin/.../sections/GamificationSection.kt` | Badge rendering — XSS review |
| `app/webApp/src/webMain/kotlin/.../sections/LeaderboardSection.kt` | Username/rank display — XSS review |
| `app/webApp/src/webMain/kotlin/.../sections/ProblemsSection.kt` | Problem title display — XSS review |
| `app/webApp/src/webMain/kotlin/.../sections/CreateExamSection.kt` | CTA text, checklist — XSS review |
| `core/models/src/.../landing/LandingStatsResponse.kt` | API model, serialization — full review |
| `core/models/src/.../api/BadgeDefinition.kt` | Validation in init block — full review |

### 2.2 Testing Approach

This was a **Whitebox Security Assessment**. The assessor had full access to:

- Source code logic and internal data flows.
- The KodEx Security Constitution (v1.1.0, last amended 2026-05-29), the authoritative
  contract for all security requirements.
- The prior feature-002 whitebox assessment
  (`docs/security-reviews/2026-05-29-feature-002-design-system-whitebox.md`) and its set of
  confirmed secure patterns, used as the baseline for carry-forward analysis.
- Architectural decision records and design intent in `docs/memory/` and `specs/`.

Findings were evaluated against the **OWASP Top 10 (2021)** and the **CWE/SANS** weakness
taxonomy. All findings were cross-referenced against the Security Constitution to determine
whether they represent a violation of an explicit rule or a defence-in-depth gap.

---

## 3. TECHNICAL FINDINGS

---

### SEC-001 — X-Request-Id Reflected into Response Body Without UUID Validation

**Severity**: MEDIUM
**OWASP Category**: A03:2021 — Injection
**CWE**: CWE-116 — Improper Encoding or Escaping of Output / CWE-20 — Improper Input Validation
**CVSS v3.1**: 4.3 (AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:L/A:N)
**Status**: ✅ Resolved

#### 3.1.1 Description

All three server endpoints (`/api/v1/stats/landing`, `/api/v1/health`, and the `StatusPages`
500-handler) accepted the `X-Request-Id` request header from the client and reflected it
verbatim into the `meta.requestId` field of every JSON response envelope, with no UUID
format validation:

```kotlin
// Identical pattern at all 3 call sites
val requestId = call.request.headers["X-Request-Id"] ?: Uuid.random().toString()
```

This directly violates Security Constitution §6:

> *"X-Request-Id header values supplied by clients MUST be validated as UUID format before
> being reflected into response bodies or logs. Non-UUID values MUST be replaced with a
> fresh `Uuid.random()`."*

While JSON serialization prevents direct XSS in the response body, the unvalidated path
enables log injection (CWE-117): a caller who sends
`X-Request-Id: INJECTION\nERROR: fake log line` causes the injected text to appear in
structured server logs alongside the real request metadata.

#### 3.1.2 Evidence (Vulnerable State)

**File**: `server/api/src/main/kotlin/dev/kodex/server/api/routes/LandingRoutes.kt:19`

```kotlin
val requestId = call.request.headers["X-Request-Id"] ?: Uuid.random().toString()
```

**File**: `server/api/src/main/kotlin/dev/kodex/server/api/routes/HealthRoutes.kt:16`

```kotlin
val requestId = call.request.headers["X-Request-Id"] ?: Uuid.random().toString()
```

**File**: `server/app/src/main/kotlin/dev/kodex/server/Application.kt:78`

```kotlin
val requestId = call.request.headers["X-Request-Id"] ?: Uuid.random().toString()
```

#### 3.1.3 Exploit Scenario

1. Attacker sends: `GET /api/v1/stats/landing` with
   `X-Request-Id: <script>alert(1)</script>` or
   `X-Request-Id: normal-id\n[ERROR] fake-server-event severity=CRITICAL`.
2. The raw string is placed in `meta.requestId` of the response and written to server logs.
3. Log consumers (e.g., Grafana, alerting pipelines) may parse the injected line as a
   legitimate log event, suppressing or distorting real alerts.

#### 3.1.4 Impact

Log injection on all three endpoints. JSON body reflection is safe from browser XSS
because the content is serialized, but the log path is exploitable with any log parser
that trusts `requestId` as a trusted field. Severity is MEDIUM (not HIGH) because the
current logging stack has no confirmed automated parsing that would be weaponized.

#### 3.1.5 Remediation Applied

New utility file `server/api/src/main/kotlin/dev/kodex/server/api/util/RequestId.kt`:

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

Applied at all 3 call sites:

```kotlin
// All three files — after:
val requestId = sanitizeRequestId(call.request.headers["X-Request-Id"])
```

New test `server/api/src/test/kotlin/dev/kodex/server/api/util/RequestIdTest.kt` covers:
valid UUID passthrough (lowercase + uppercase), null → fresh UUID, script injection →
replaced, oversized string → replaced, empty string → replaced, plain text → replaced.

The existing `HealthRouteTest` test `"X-Request-Id header is forwarded as requestId"` was
updated: the test value `"test-request-id-123"` was replaced with a real UUID
`"550e8400-e29b-41d4-a716-446655440000"`, and a second test was added to verify that
non-UUID values are replaced by the server. `koverVerify` passes after the changes.

---

### SEC-002 — CSP `connect-src` Hardcodes `http://localhost:8080` in `index.html` — Out of Sync With Ktor Header

**Severity**: MEDIUM
**OWASP Category**: A05:2021 — Security Misconfiguration
**CWE**: CWE-693 — Protection Mechanism Failure
**CVSS v3.1**: 5.3 (AV:N/AC:L/PR:N/UI:R/S:C/C:L/I:L/A:N)
**Status**: ✅ Resolved

#### 3.2.1 Description

The `index.html` CSP was modified from `connect-src 'self'` to
`connect-src 'self' http://localhost:8080`, while the Ktor `DefaultHeaders` CSP retained
`connect-src 'self'`. This created two problems simultaneously:

**Problem 1 — CSP sync violation.** Security Constitution §6 states:
> *"Both declarations MUST stay in sync. Rationale: `index.html` is served by Vite/webapp
> container, not by the Ktor API — Ktor headers do not protect the HTML shell."*

The HTML shell's CSP governs what the browser permits on the initial page load. API-level
headers apply only to API responses. Having divergent CSPs means the effective policy
depends on which resource is loaded — a maintenance trap that will cause future audits to
miss the looser declaration.

**Problem 2 — Localhost exposure in production.** The `http://localhost:8080` allowance is
a development convenience that was accidentally committed. In production, every visitor's
browser is explicitly permitted (by the CSP) to make HTTP connections to port 8080 on their
own machine. This enables local-network probing: an attacker who can influence the
application's fetch calls can probe internal services on the visitor's machine.

The risk is further compounded by SEC-003, which caused the JS client to always target
`http://localhost:8080` as its base URL. Together, the two findings form a complete
local-network probe: the JS client requests `localhost:8080`, and the CSP allows it.

#### 3.2.2 Evidence (Vulnerable State)

**File**: `app/webApp/src/webMain/resources/index.html:14`

```html
connect-src 'self' http://localhost:8080;">
```

**File**: `server/app/src/main/kotlin/dev/kodex/server/Application.kt:58`

```kotlin
"connect-src 'self'; " +   // ← correct, but out of sync with index.html
```

#### 3.2.3 Exploit Scenario

1. Production user visits `https://kodex.dev`.
2. Browser downloads `index.html` with `connect-src 'self' http://localhost:8080`.
3. JS bundle (after SEC-003) sets base URL to `http://localhost:8080` — API calls target the
   visitor's own local port 8080.
4. If the visitor runs any service on port 8080 (a local dev server, a Docker container, an
   IoT management UI), the API calls reach it.
5. Even without auth, the timing and response pattern of those calls leaks whether the port
   is open, creating a local-port oracle.

#### 3.2.4 Impact

Local-network probing via CSP misconfiguration, compounded with SEC-003. CVSS 5.3 because
the browser will silently connect to the victim's localhost — no user interaction required
beyond page load (CVSS `UI:R` accounts for the initial visit).

#### 3.2.5 Remediation Applied

One-line change to `app/webApp/src/webMain/resources/index.html`:

```diff
- connect-src 'self' http://localhost:8080;">
+ connect-src 'self';">
```

The Ktor `DefaultHeaders` CSP already had `connect-src 'self'`. Both declarations are now
in sync. The Vite dev proxy (defined in `vite.config.ts`) routes `/api/*` →
`http://localhost:8080`, so all `ApiRoutes` constants (which use `/api/v1/...` relative
paths) continue to work in development under `connect-src 'self'` without any explicit
localhost allowance.

This fix was applied **after** SEC-003 so that there is no window during which the CSP is
tightened but the JS client still targets `localhost:8080`.

---

### SEC-003 — `NetworkKoinModule` Hardcodes `http://localhost:8080` as API Base URL

**Severity**: MEDIUM
**OWASP Category**: A05:2021 — Security Misconfiguration
**CWE**: CWE-693 — Protection Mechanism Failure
**CVSS v3.1**: 4.0 (AV:N/AC:L/PR:N/UI:R/S:U/C:N/I:L/A:N)
**Status**: ✅ Resolved

#### 3.3.1 Description

The Ktor `HttpClient` singleton was configured with a `DefaultRequest` block that set
`http://localhost:8080` as the base URL unconditionally:

```kotlin
install(DefaultRequest) {
    url("http://localhost:8080")   // applied in all environments, including production
}
```

The inline comment `// Prod: same-origin — no base URL override needed` documents the
correct production intent but provides no conditional code to implement it. The
`DefaultRequest` block wins, overriding every relative `ApiRoutes` constant with an
absolute localhost target.

The consequence is that in production:
- Every API call targets the **visitor's** localhost port 8080, not the backend server.
- Combined with SEC-002, the browser is explicitly permitted to make these connections.
- The production backend at `https://kodex.dev/api/v1/...` receives zero traffic from
  any relative-path call site.

#### 3.3.2 Evidence (Vulnerable State)

**File**: `app/webApp/src/webMain/kotlin/dev/kodex/webapp/di/NetworkKoinModule.kt:15-18`

```kotlin
fun httpClient(): HttpClient = HttpClient(Js) {
    install(DefaultRequest) {
        url("http://localhost:8080")   // ← unconditional; applies in production
    }
    install(ContentNegotiation) { ... }
}
```

#### 3.3.3 Exploit Scenario

1. Production visitor loads `https://kodex.dev`.
2. `LandingViewModel` calls `LandingStatsRepository.fetchStats()`.
3. `LandingStatsRemoteDataSourceImpl` calls the Ktor `HttpClient` with
   route `/api/v1/stats/landing`.
4. `DefaultRequest` rewrites the URL to `http://localhost:8080/api/v1/stats/landing`.
5. The request never reaches the production API. Landing stats never load.
6. If any service runs on the visitor's port 8080, it receives the request.

#### 3.3.4 Impact

Functional regression in production (all API calls misdirected) plus local-network probing
vector when combined with SEC-002. Severity is MEDIUM because the production endpoint is
unauthenticated and there is no data leakage from a 200 response.

#### 3.3.5 Remediation Applied

Removed the `DefaultRequest` block from
`app/webApp/src/webMain/kotlin/dev/kodex/webapp/di/NetworkKoinModule.kt`:

```diff
  fun httpClient(): HttpClient = HttpClient(Js) {
-     install(DefaultRequest) {
-         url("http://localhost:8080")
-     }
      install(ContentNegotiation) {
```

The `import io.ktor.client.plugins.DefaultRequest` import was also removed. All `ApiRoutes`
constants use `/api/v1/...` relative paths. In development, Vite's proxy forwards these to
`localhost:8080`. In production, they resolve to the same origin as the webapp. No explicit
base URL is needed in either environment.

`jsBrowserTest` was re-run after this change and passes.

---

### SEC-004 — Rate-Limit Key Uses `local.remoteHost` — Collapses to Proxy IP Behind Load Balancer

**Severity**: LOW
**OWASP Category**: A05:2021 — Security Misconfiguration
**CWE**: CWE-441 — Unintended Proxy or Intermediary
**CVSS v3.1**: 3.1 (AV:N/AC:H/PR:N/UI:N/S:U/C:N/I:L/A:L)
**Status**: ⏳ Deferred — `TASK-SEC-004`

#### 3.4.1 Description

The rate-limit key for the `"public"` `RateLimitName` uses
`call.request.local.remoteHost`:

```kotlin
requestKey { call -> call.request.local.remoteHost }
```

`local.remoteHost` is the IP address of the direct TCP peer — the server that established
the connection. Behind a load balancer or reverse proxy, this is always the proxy's IP
address. All visitors share a single rate-limit bucket keyed to the proxy IP, which means:

- A single misbehaving client can exhaust the bucket and trigger HTTP 429 for everyone.
- An attacker can bypass per-client rate limiting entirely, since the bucket is shared.

The original task T037 in `specs/003-landing-page/tasks.md` correctly specified
`call.request.origin.remoteHost` as the rate-limit key. The implementation diverged to
`local.remoteHost`. This is an implementation bug against a correctly stated design intent.

#### 3.4.2 Evidence

**File**: `server/app/src/main/kotlin/dev/kodex/server/Application.kt:94`

```kotlin
requestKey { call -> call.request.local.remoteHost }   // ← should be origin.remoteHost
```

#### 3.4.3 Accepted Risk and Remediation Plan

**Why deferred**: The `/api/v1/stats/landing` endpoint is unauthenticated, read-only, and
returns static compile-time constants. The impact of a collapsed rate-limit bucket is bounded
to occasional 429s for legitimate users — not a data breach or authorization bypass.
Whether the production infrastructure will use a reverse proxy has not been decided.
Installing `ForwardedHeaders` without restricting it to trusted proxy CIDRs introduces a
worse vulnerability (client-supplied IP spoofing), so the fix should not be rushed.

**Remaining risk**: Behind any proxy or load balancer, all clients share one 60-req/min
bucket. A scraper or scanner consuming 60 requests/min will throttle the bucket for all
other users.

**Revisit trigger**: Either (1) a reverse proxy / Nginx / AWS ALB is placed in front of the
Ktor server, (2) auth or submission rate limits are added, or (3) a new feature requires
per-client enforcement as a security control.

**Target milestone**: `feature/005-auth` or `infra/001-production-deploy`.

**Remediation plan** (when triggered):

```kotlin
// Application.kt — before install(RateLimit):
install(XForwardedHeaders)   // or ForwardedHeaders — restrict to trusted proxy CIDRs

// Update requestKey:
requestKey { call -> call.request.origin.remoteHost }
```

---

## 4. ARCHITECTURAL DRIFT AND SYSTEMIC RISKS

### 4.1 XSS Attack Surface Remains Zero — All 8 Landing Sections Use Text-Node Rendering

Every section introduced in this feature renders user-facing content exclusively through
Kilua's `+` operator. The `username` field in `GlobalNavBar.kt` is rendered via `+`
(text node). Badge `name` and `description` fields in `GamificationSection.kt` use `+`.
Problem titles in `ProblemsSection.kt` use `+`. No `innerHTML`, `outerHTML`, or
`js("...innerHTML...")` calls appear anywhere in the landing page code.

This is the most important structural invariant to maintain as the platform grows. The
CSP `script-src 'self'` is a defence-in-depth layer, not a substitute for text-node
discipline. Both controls MUST remain in place.

### 4.2 `BadgeDefinition` Establishes the Init-Block Validation Pattern for API-Bound Data

`BadgeDefinition.kt` validates `id` (alphanumeric + dash + underscore, ≤ 64 chars) and
`iconPath` (allowlist of `svg`, `png`, `webp`, `avif` extensions; no `..` traversal) in
its `init` block. This follows the Security Constitution §5 pattern established for
`NavItem` in feature-002 and MUST be applied to all future data classes whose fields
are used in DOM attributes or served from an API.

The landing page uses only static compile-time `BadgeDefinition` instances. When badges
are loaded from the API in a future feature, the `init` block will enforce integrity at
deserialization time — before any field value touches the DOM.

### 4.3 `sanitizeRequestId()` Is the Authoritative Pattern for All Future Header Reflections

`server/api/src/main/kotlin/dev/kodex/server/api/util/RequestId.kt` is now the single
utility for safe `X-Request-Id` handling across all server modules. Any new endpoint
that reads and reflects a client header MUST use `sanitizeRequestId()`. Do not re-inline
the `?:` fallback pattern — it is the vulnerable pattern that was removed.

The `UUID_REGEX` in this file uses `Regex(pattern, RegexOption.IGNORE_CASE)`, which is
correct: UUIDs may arrive in either case from HTTP clients. Both forms pass through
unchanged. Anything that does not match is silently replaced with a fresh `Uuid.random()`.

### 4.4 `ApiRoutes` Relative-Path Discipline Is the Production-Safety Guarantee

All route constants in `ApiRoutes` use paths of the form `/api/v1/...` — no scheme, no
host. This is the correct pattern for a Kotlin/JS frontend served by Vite in dev and
as a same-origin static bundle in production. The `DefaultRequest { url(...) }` pattern
MUST NOT be reintroduced as a base-URL shortcut. If environment-specific base URLs are
ever needed (e.g., for non-Vite test environments), they MUST be supplied via a
`BuildConfig` constant populated from an env var, not hardcoded.

### 4.5 Rate Limiting Is Present — Key Strategy Needs Revisit Before Auth Endpoints

`RateLimit` is correctly installed in `Application.kt` and applied to the public landing
endpoint. The bucket parameters (60 req/min) are reasonable for a static stats endpoint.
The remaining gap (SEC-004) is limited to the key strategy under proxy conditions. The
plugin installation itself, the limit value, and the `rateLimit {}` wrapper in routing
are all correct and should not be changed as part of any SEC-004 remediation.

---

## 5. APPENDICES

### 5.1 Confirmed Secure Patterns (New, This Feature)

These patterns were validated in this assessment and MUST be maintained in all future
features that extend the server routes, API client, or landing page sections.

| Pattern | Enforcement Location |
|---------|---------------------|
| `sanitizeRequestId()` for all X-Request-Id reflections | `server/api/src/.../util/RequestId.kt` — all server endpoints |
| `ApiRoutes` constants use relative paths only — no base URL in `HttpClient` | `app/webApp/src/.../di/NetworkKoinModule.kt` — no `DefaultRequest { url(...) }` |
| CSP `connect-src 'self'` in `index.html` — no localhost allowances | `app/webApp/src/webMain/resources/index.html` |
| `BadgeDefinition.id` and `.iconPath` validated in `init` block | `core/models/src/.../api/BadgeDefinition.kt` |
| `Cache-Control: public, max-age=300` on static landing stats | `server/api/src/.../routes/LandingRoutes.kt` |
| Rate limiting on all public endpoints | `server/app/src/.../Application.kt` — `RateLimit` plugin |
| Text-node rendering (`+` operator) in all 8 landing sections | All `*Section.kt` files — no `innerHTML` |
| Username rendered via `+` operator (not innerHTML) | `app/webApp/src/.../sections/GlobalNavBar.kt` |

### 5.2 Confirmed Secure Patterns (Carry-Forward from Feature 002)

| Pattern | Enforcement Location |
|---------|---------------------|
| Text-node rendering only — no `innerHTML` in design system | All 13 design system components |
| CSP meta tag in HTML shell — in sync with Ktor `DefaultHeaders` | `index.html` + `Application.kt` |
| Locale code validated against `ALLOWED_LOCALES` before use | `app/webApp/src/.../design/i18n/I18nSetup.kt` |
| `NavItem.key` and `NavItem.icon` validated in `init` block | `app/webApp/src/.../design/components/NavBar.kt` |
| Clipboard accessed via Web API only | `app/webApp/src/.../design/components/CodeBlock.kt` |
| Modal `keydown` listener removed on unmount via `DisposableEffect` | `app/webApp/src/.../design/components/Modal.kt` |

### 5.3 Confirmed Secure Patterns (Carry-Forward from Feature 001)

| Pattern | Enforcement Location |
|---------|---------------------|
| `env()` throws on missing secrets — no silent defaults | `core/env/EnvConfig.kt` |
| CORS restricted to exact `WEBAPP_ORIGIN` — no wildcard | `server/app/Application.kt` |
| Multi-stage Docker build — no build tools in production image | `docker/server.Dockerfile` |
| Non-root user in all containers | `docker/server.Dockerfile`, `sandbox-runner.Dockerfile` |
| Error responses never expose exception messages or stack traces | `server/app/Application.kt` — StatusPages handler |
| Redis requires password in production | `docker/docker-compose.yml` |
| Full security header suite on all API responses | `server/app/Application.kt` — `DefaultHeaders` |
| Secret scanning on every PR — full history | `.github/workflows/ci.yml` |
| postgres/redis not bound to host ports | `docker/docker-compose.yml` |
| Shared secret required on all sandbox-runner routes except `/health` | `sandbox-runner/app/Application.kt` |

### 5.4 CVSS Scoring Reference

| Score Range | Severity      |
|-------------|---------------|
| 9.0 – 10.0  | Critical      |
| 7.0 – 8.9   | High          |
| 4.0 – 6.9   | Medium        |
| 0.1 – 3.9   | Low           |
| 0.0         | Informational |

### 5.5 Finding Cross-Reference

| Finding | Severity | CVSS | OWASP | CWE | Status |
|---------|----------|------|-------|-----|--------|
| SEC-001 — X-Request-Id reflected without UUID validation | MEDIUM | 4.3 | A03 | CWE-116, CWE-20 | ✅ Resolved |
| SEC-002 — CSP localhost in index.html, out of sync with Ktor | MEDIUM | 5.3 | A05 | CWE-693 | ✅ Resolved |
| SEC-003 — NetworkKoinModule hardcoded base URL | MEDIUM | 4.0 | A05 | CWE-693 | ✅ Resolved |
| SEC-004 — Rate-limit key collapses to proxy IP | LOW | 3.1 | A05 | CWE-441 | ⏳ Deferred — TASK-SEC-004 |

### 5.6 Tooling Context

- **Assessment Framework**: Spec-Kit Security Review Extension (Whitebox mode)
- **Memory Access**: Markdown-only (`.specify/memory/`, `docs/memory/`)
- **Prior Assessment**: `docs/security-reviews/2026-05-29-feature-002-design-system-whitebox.md`
- **Standards**: OWASP Top 10 (2021), CWE/SANS Top 25
- **Security Constitution**: `.specify/memory/security_constitution.md` v1.1.0
- **Branch Review**: `docs/security-reviews/2026-06-07-feature-003-landing-page.md`
- **Follow-Up Plan**: `docs/security-reviews/2026-06-07-feature-003-landing-page-followup.md`
- **Assessment Date**: 2026-06-07
- **Remediation Completed**: 2026-06-07
- **Export Date**: 2026-06-07
