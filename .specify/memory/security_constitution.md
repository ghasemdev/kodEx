# KodEx Security Constitution

**Version**: 1.1.0 | **Ratified**: 2026-05-19 | **Last Amended**: 2026-05-29 | **Source**: Compiled from §III, §V, §VI, §VII, §VIII, §IX

> This file is the single source of truth for all security audits.
> All rules are actionable: specific enough for an AI auditor to verify against code.
> Conflicts between this file and other docs resolve in favor of this file.

---

## 1. Trust Boundaries

### Public Entry Points (untrusted by default)
| Entry Point | Description | Trust Level |
|---|---|---|
| `app/webApp/` (port 5173 dev / `WEBAPP_ORIGIN` prod) | Browser frontend | Untrusted |
| `server/app/` HTTP (port 8080) | Public REST API | Untrusted until JWT verified |
| User-submitted code | Kotlin/Android submissions | **Always untrusted** |

### Internal / Protected Entry Points
| Entry Point | Description | Trust Level |
|---|---|---|
| `sandbox-runner/` HTTP | Sandbox orchestration API | Trusted (shared secret header required) |
| PostgreSQL (port 5432) | Primary database | Internal only — never exposed publicly |
| Redis (port 6379) | Session/token store | Internal only — never exposed publicly |

### Strict Isolation Rules
- `sandbox-runner` is the **only** service with Docker socket access. The main API MUST NOT have Docker socket access.
- Communication between `server/app` and `sandbox-runner` MUST use an authenticated HTTP channel (shared secret header). Plain HTTP without the secret MUST be rejected.
- User-submitted code MUST NEVER be executed in the main API process or on the host machine.

---

## 2. Authentication & Authorization Standards

### JWT Token Lifecycle
```
Access token:   15 minutes | claims: sub (userId), role, iat, exp
Refresh token:  7 days     | stored server-side in Redis (revocable)
```
- **Library**: `ktor-server-auth-jwt` (official Ktor plugin). No custom JWT parsing.
- Refresh tokens MUST be invalidated on explicit logout and on password change.
- JWT signing secret MUST be at minimum 256 bits, loaded from env var `JWT_SECRET`. Never hardcoded.

### Password Storage
- **Required algorithm**: Argon2id via `argon2-jvm` library.
- Forbidden: plaintext, MD5, SHA-1, unsalted SHA-256, bcrypt (permitted only for legacy migration paths).

### Role-Based Access Control
| Role | Permissions |
|---|---|
| `ADMIN` | Create/update/publish exams; view all submissions and results |
| `PARTICIPANT` | Browse/join published exams; submit solutions; view **own** submissions only |

- **Enforcement location**: API layer (`server/api/`), NOT frontend only.
- Unauthenticated request to protected endpoint → **HTTP 401**.
- Authenticated but insufficient role → **HTTP 403**.
- Every endpoint MUST declare its required role in code.

### Exam State Access Matrix
| State | Admin | Participant |
|---|---|---|
| `DRAFT` | Full read + edit | **No access** |
| `PUBLISHED` | Read only (no edit) | Read + submit |
| `CLOSED` | Read only | Read only (if result visibility configured) |

- `CLOSED` is terminal — no state transitions out.
- Editing test cases or questions in `PUBLISHED` state is **forbidden for all roles**.

---

## 3. Data Isolation & Privacy Rules

- Participants MUST NOT see other participants' submissions, scores, or injected test cases.
- Injected test cases are server-side only — MUST NOT be returned in any API response to participants.
- Grading logic and scoring MUST execute server-side (in `sandbox-runner`), never client-side.
- Audit logs and submission records MUST NOT be deleted in production.
- Result visibility to participants is controlled by admin and takes effect only after exam reaches `CLOSED`.

---

## 4. Secrets Management Policy

- Every secret MUST be supplied via **environment variables**. Secrets in source code, Gradle files, or committed config files are strictly forbidden.
- `.env.example` MUST be committed listing all 14 required env vars with placeholder values and comments.
- `.env` MUST be in `.gitignore`.
- Production code MUST NOT provide default/fallback values for secrets. A missing secret at startup MUST cause immediate termination with a clear error message (via `EnvConfig.env(key)`).
- Secrets MUST NOT appear in logs at any level (DEBUG, INFO, ERROR, etc.).

### Required Environment Variables (security-relevant)
| Variable | Purpose |
|---|---|
| `JWT_SECRET` | JWT signing secret (≥256 bits) |
| `DB_PASSWORD` | PostgreSQL password |
| `REDIS_PASSWORD` | Redis password (required in production; auth spec adds Redis) |
| `TOTP_ENCRYPTION_KEY` | AES-256-GCM key for TOTP secret encryption — spec 004 (≥256 bits, base64) |
| `WEBAUTHN_CHALLENGE_KEY` | AES-256-GCM key for WebAuthn challenge cookie signing — spec 004 (≥256 bits, base64) |
| `SANDBOX_SHARED_SECRET` | Shared secret for `server` ↔ `sandbox-runner` auth |
| `WEBAPP_ORIGIN` | Allowed CORS origin for production |

---

## 5. Secure-by-Design Patterns

### Code Submission Security
- All user-submitted code MUST be scanned for dangerous patterns before sandbox entry:
  - `Runtime.exec`, `ProcessBuilder`, reflection-based class loading, file system access
- Suspicious submissions MUST be **rejected with a clear error** — not silently failed.
- Sandbox container MUST:
  - Have **no network access** (`--network=none`)
  - Have hard CPU limit (default: 2 CPUs, configurable per exam)
  - Have hard memory limit (default: 512 MB, configurable per exam)
  - Have hard wall-clock timeout (default: 10 seconds, configurable per exam)
  - Mount only submission files + injected tests (no host filesystem access)
  - Be **destroyed immediately** after execution (no container reuse)
- Sandbox image MUST be rebuilt and re-audited whenever the base JDK or Android build tools version changes.

### Frontend XSS Controls

- All design system components in `app/webApp/` MUST render user-controlled content via Kilua's
  `+` operator (text nodes — equivalent to `textContent`). Using `innerHTML`, `outerHTML`, or
  `js("…innerHTML…")` in any component is **prohibited** without an explicit security review and
  CSP hash update.
- Data class fields used in DOM attributes (`className`, `id`, `href`, `src`) MUST be validated
  in the class `init` block via `require()` against an allowlist or regex before any instance is
  constructed. **Pattern**: `NavItem.key` (alphanumeric/dash/underscore) and `NavItem.icon`
  (valid CSS class chars) in `NavBar.kt`.
- `localStorage` values used as locale codes or other control inputs MUST be filtered through a
  hardcoded `ALLOWED_*` set before being passed to any API or library. **Pattern**: `ALLOWED_LOCALES`
  in `I18nSetup.kt`.

### Database Security
- All database queries MUST use **Exposed ORM's type-safe DSL** or parameterized queries. Raw string interpolation in SQL is forbidden.
- Database migrations are managed exclusively via Flyway (`server/data/src/resources/db/migration/`). No ad-hoc schema changes.

### Rate Limiting
- Rate limiting MUST be applied to:
  - Submission endpoint
  - Auth endpoints (login, token refresh)
- Limits are configurable per exam/environment.

### Input Validation
- All user input MUST be validated at the API boundary (`server/api/` layer).
- The `app/shared/` KMP module MUST contain shared validation logic used by both backend and frontend.

---

## 6. API & Integration Security

### CORS Policy
- The API MUST configure CORS to allow requests **only** from the known webapp origin:
  - Development: `http://localhost:5173`
  - Production: value of `WEBAPP_ORIGIN` env var
- All other origins MUST be rejected.
- Credentials (cookies/auth headers) MUST be allowed on permitted origins.
- **Implementation**: Ktor `CORS` plugin in `server/app/Application.kt`.

### Response Security Headers
- `Cache-Control: no-store` MUST be set on all auth and sensitive endpoints (e.g., `/api/v1/health`, all auth routes).
- The following headers MUST be set globally via Ktor `DefaultHeaders` in `server/app/Application.kt`:
  `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`,
  `Permissions-Policy: camera=(), microphone=(), geolocation=()`, and `Content-Security-Policy`.
- CSP MUST be declared in **two places** (see §5 Frontend XSS Controls):
  1. `<meta http-equiv="Content-Security-Policy">` in every `index.html` (jsMain + wasmJsMain)
  2. `Content-Security-Policy` header in Ktor `DefaultHeaders` for all API responses
  Both declarations MUST stay in sync. Rationale: `index.html` is served by Vite/webapp container,
  not by the Ktor API — Ktor headers do not protect the HTML shell.
- `X-Request-Id` header values supplied by clients MUST be validated as UUID format before being
  reflected into response bodies or logs. Non-UUID values MUST be replaced with a fresh `Uuid.random()`.

### Internal Service Authentication
- All requests from `server/app` to `sandbox-runner` MUST include the `SANDBOX_SHARED_SECRET` in a dedicated header.
- `sandbox-runner` MUST reject any request missing or having an invalid secret with HTTP 401.

### SSE Security
- The `/api/v1/submissions/{id}/status` SSE stream MUST require a valid JWT.
- A participant MUST only be able to subscribe to their own submission's status stream (not other participants').
- The stream MUST close automatically when a terminal state (`SCORED` or `FAILED`) is reached.

### API Versioning
- All endpoints live under `/api/v1/`. This allows future breaking changes to be introduced under `/api/v2/` without disrupting existing clients.

---

## 7. Audit, Logging & Monitoring

- All logs MUST be **structured JSON** (via `logstash-logback-encoder`).
- Every HTTP request MUST generate a log entry containing a `requestId` (UUID-v4). This `requestId` MUST match the `meta.requestId` field in the API response envelope.
- The following submission events MUST be logged with `requestId`, `timestamp`, `userId`, `examId`, and `outcome`:
  - Submission received
  - Sandbox started
  - Sandbox finished (with CPU time, peak memory, exit code)
  - Scoring completed
- Security events that MUST be logged:
  - Failed authentication attempts (do not log the credential itself)
  - Role authorization failures (403 responses)
  - Rejected code submissions (dangerous pattern detected)
  - Sandbox timeout or resource limit exceeded
- Audit logs and submission records MUST NOT be deleted in production (`PROHIBITED`).
- Secrets MUST NOT appear in log output at any level.

---

## 8. Compliance Mapping

| Rule | OWASP Top 10 (2021) |
|---|---|
| Argon2id password hashing | A02 — Cryptographic Failures |
| JWT signing secret ≥256 bits via env var | A02 — Cryptographic Failures |
| Parameterized queries (Exposed ORM) | A03 — Injection |
| Input validation at API boundary | A03 — Injection |
| Code scan before sandbox execution | A03 — Injection |
| Role enforcement at API layer (401/403) | A01 — Broken Access Control |
| Participant isolation (own data only) | A01 — Broken Access Control |
| No secrets in source code or logs | A02 — Cryptographic Failures |
| Rate limiting on auth/submission | A07 — Identification & Authentication Failures |
| Structured audit logging | A09 — Security Logging & Monitoring Failures |
| CORS restricted to known origin | A05 — Security Misconfiguration |
| Sandbox network isolation | A10 — Server-Side Request Forgery (SSRF) |
| Sandbox destroyed after execution | A05 — Security Misconfiguration |
| Kilua `+` text-node rendering (no innerHTML) | A03 — Injection (XSS) |
| CSP meta tag in index.html + Ktor response headers | A05 — Security Misconfiguration |
| Component field allowlist validation in `init` block | A03 — Injection |
| localStorage values filtered through ALLOWED_* set | A03 — Injection |
