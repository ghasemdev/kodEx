---
document_type: security-review
review_type: plan
assessment_date: 2026-06-09
codebase_analyzed: KodEx / specs/004-auth
total_files_analyzed: 6
total_findings: 11
overall_risk: HIGH
critical_count: 0
high_count: 2
medium_count: 5
low_count: 2
informational_count: 2
owasp_categories: [A01, A02, A05, A07]
cwe_ids: [CWE-321, CWE-331, CWE-287, CWE-352, CWE-434, CWE-525, CWE-307]
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

# Security Review — Plan: Auth & User Identity (Spec 004)

**Reviewer**: Claude Sonnet 4.6 | **Date**: 2026-06-09 | **Branch**: `feature/004-auth`

---

## Executive Summary

The auth plan covers a comprehensive identity system (email+password, OAuth, WebAuthn, TOTP, JWT, sessions, new-device alerts). The overall security design is sound — Argon2id, HttpOnly cookies, SHA-256 token hashing, and stateless WebAuthn challenges are all correct choices. However, **two HIGH findings** must be resolved before implementation begins: the WebAuthn challenge cookie signing key has no defined source, and raw token entropy is unspecified for all opaque tokens. Five MEDIUM findings are gaps the plan creates that implementors will resolve inconsistently without explicit guidance.

---

## Artifacts Reviewed

| File | Purpose |
|------|---------|
| `specs/004-auth/plan.md` | Implementation plan — phases, source structure, technical context |
| `specs/004-auth/spec.md` | Feature spec — FRs, user stories, entities |
| `specs/004-auth/research.md` | Library decisions D1–D15 |
| `specs/004-auth/data-model.md` | Exposed table definitions, Redis key schema |
| `specs/004-auth/contracts/api-auth.md` | All API endpoint contracts |
| `specs/004-auth/quickstart.md` | Env vars, catalog entries, local setup |
| `.specify/memory/security_constitution.md` | Project security rules (authoritative) |
| `.specify/memory/principles/viii-authentication.md` | Auth principle |

---

## Findings

### SEC-001 — WebAuthn Challenge Cookie Key Has No Defined Source

| Field | Value |
|-------|-------|
| **Severity** | HIGH |
| **CVSS** | 7.5 |
| **OWASP** | A02:2025 — Cryptographic Failures |
| **CWE** | CWE-321: Use of Hard-coded Cryptographic Key |
| **Location** | `specs/004-auth/research.md` §D1, `specs/004-auth/quickstart.md` (env vars) |
| **Task** | TASK-SEC-001 |

**Description**: The plan specifies that WebAuthn challenges are stored in a signed AES-GCM HttpOnly cookie. This cookie must be signed/encrypted with a secret key — but the plan defines no env var for this key and gives no guidance on how it is derived. An implementor may:
- Hardcode a static key (breaks all security guarantees)
- Derive it from `JWT_SECRET` (acceptable, but undocumented and fragile if JWT_SECRET rotates)
- Generate a random per-startup key (acceptable, but challenges issued before a restart become invalid)

**Required fix**: Add `WEBAUTHN_CHALLENGE_KEY` env var (base64, ≥256 bits, `openssl rand -base64 32`) to `quickstart.md` and `security_constitution.md §4`. Document explicitly in `research.md` §D1 that this key — not a derivation — is used for AES-GCM of the challenge cookie.

---

### SEC-002 — Raw Token Entropy Unspecified

| Field | Value |
|-------|-------|
| **Severity** | HIGH |
| **CVSS** | 7.3 |
| **OWASP** | A02:2025 — Cryptographic Failures |
| **CWE** | CWE-331: Insufficient Entropy |
| **Location** | `specs/004-auth/data-model.md` — all `token_hash CHAR(64)` columns |
| **Task** | TASK-SEC-002 |

**Description**: The plan defines six tables that store SHA-256 hashes of opaque tokens (refresh tokens, email verification tokens, password reset tokens, pending email change tokens, emergency revoke tokens) and the TOTP backup codes. None of the documents specify:
- The byte length of the raw token before hashing
- That `SecureRandom` (or equivalent CSPRNG) is the source

If an implementor uses UUID v4 (122 bits), that is acceptable. If they use a 6-digit code (unintentionally), that is not. OWASP requires ≥ 160 bits for server-side tokens.

**Required fix**: Add to `data-model.md` a single line on token generation: `Raw token: 32 bytes from SecureRandom, hex-encoded (64 chars). SHA-256(hex) → token_hash column.` Apply to all six token tables.

---

### SEC-003 — `REDIS_PASSWORD` Missing from Required Env Vars

| Field | Value |
|-------|-------|
| **Severity** | MEDIUM |
| **CVSS** | 5.9 |
| **OWASP** | A05:2025 — Security Misconfiguration |
| **CWE** | CWE-287: Improper Authentication |
| **Location** | `specs/004-auth/quickstart.md` (env vars), `security_constitution.md §4` |
| **Task** | TASK-SEC-003 |

**Description**: `security_constitution.md §4` lists `REDIS_PASSWORD` as a required env var for the project. The auth plan introduces Redis as a new service but only defines `REDIS_URL=redis://localhost:6379` (no password). Production Redis without authentication is an exposed data store. The `REDIS_URL` should embed credentials (`redis://:password@host:port`) or a separate `REDIS_PASSWORD` should be required.

**Required fix**: Add `REDIS_PASSWORD` to `quickstart.md` env vars and to the plan's Phase A env config task. For local dev, note that `redis:7-alpine` can run without a password, but production must set one. Update `REDIS_URL` example: `redis://:${REDIS_PASSWORD}@redis:6379`.

---

### SEC-004 — `Cache-Control: no-store` Not Planned for Auth Routes

| Field | Value |
|-------|-------|
| **Severity** | MEDIUM |
| **CVSS** | 4.3 |
| **OWASP** | A05:2025 — Security Misconfiguration |
| **CWE** | CWE-525: Use of Web Browser Cache Containing Sensitive Information |
| **Location** | `specs/004-auth/plan.md` Phase C, `security_constitution.md §6` |
| **Task** | TASK-SEC-004 |

**Description**: `security_constitution.md §6` mandates `Cache-Control: no-store` on all auth and sensitive endpoints. Phase C of the plan defines `AuthRoutes.kt` and related files but doesn't mention this header. Without explicit planning, the implementor may forget to add it, leaving access tokens and sensitive responses cached in proxies or browsers.

**Required fix**: Add to plan Phase C item 24: "`Cache-Control: no-store` MUST be set on all auth route responses via a `CallPlugin` or per-route `response.header()` call."

---

### SEC-005 — `totpSessionToken` Needs Explicit `type` Claim

| Field | Value |
|-------|-------|
| **Severity** | MEDIUM |
| **CVSS** | 5.3 |
| **OWASP** | A07:2025 — Identification and Authentication Failures |
| **CWE** | CWE-287: Improper Authentication |
| **Location** | `specs/004-auth/contracts/api-auth.md` — `POST /auth/login/totp`, `specs/004-auth/plan.md` Complexity Tracking |
| **Task** | TASK-SEC-005 |

**Description**: The plan describes `totpSessionToken` as "a short-lived JWT signed with `JWT_SECRET`" with payload `{ userId, passwordVerified: true }`. If the TOTP step validator checks only `exp` and signature — not a `type: "totp-session"` claim — then a valid access token (also signed with `JWT_SECRET`) could be submitted at `POST /auth/login/totp` and accepted, bypassing TOTP for any already-authenticated user. Conversely, a TOTP session token submitted to the JWT auth guard must not grant access.

**Required fix**: Add to `contracts/api-auth.md` and plan complexity tracking: `totpSessionToken` JWT payload MUST include `{ type: "totp-session", sub: "<userId>", iat, exp: iat+300 }`. The TOTP route MUST reject tokens where `type != "totp-session"`. The JWT auth guard MUST reject tokens where `type == "totp-session"` (or equivalently, where the `role` claim is absent).

---

### SEC-006 — Avatar File Extension Must Derive from MIME, Not User Input

| Field | Value |
|-------|-------|
| **Severity** | MEDIUM |
| **CVSS** | 5.0 |
| **OWASP** | A03:2025 — Injection |
| **CWE** | CWE-434: Unrestricted Upload of File with Dangerous Type |
| **Location** | `specs/004-auth/contracts/api-auth.md` — `POST /api/v1/users/me/avatar`, `specs/004-auth/research.md` §D15 |
| **Task** | TASK-SEC-006 |

**Description**: `research.md §D15` describes MIME + magic byte validation but says the stored URL format is `<userId>.<ext>` without specifying that the file extension is derived from the server-validated MIME type, not from the client-supplied filename. If the implementation extracts the extension from the multipart `filename` field, a malicious user could upload a valid JPEG bytes with filename `exploit.html` and have it served with a `.html` extension — bypassing MinIO's content-type serving and potentially executing as HTML in some browser configurations.

**Required fix**: Add to `research.md §D15`: "The stored filename MUST be `<userId>.<server-derived-ext>` where ext is derived from the validated MIME type (`jpeg`, `png`, `webp`) — never from the multipart filename field."

---

### SEC-007 — OAuth CSRF State Parameter Not Documented

| Field | Value |
|-------|-------|
| **Severity** | MEDIUM |
| **CVSS** | 4.8 |
| **OWASP** | A01:2025 — Broken Access Control |
| **CWE** | CWE-352: Cross-Site Request Forgery |
| **Location** | `specs/004-auth/contracts/api-auth.md` — OAuth endpoints, `specs/004-auth/plan.md` Phase C item 19 |
| **Task** | TASK-SEC-007 |

**Description**: The plan uses Ktor's `ktor-server-auth` OAuth2 plugin, which generates and validates the `state` parameter automatically. However, the plan does not document this protection or explicitly require it. An implementor who bypasses the plugin or implements OAuth manually would omit CSRF protection. The plan should document this as a non-negotiable requirement.

**Required fix**: Add to plan Phase C item 19: "Ktor `ktor-server-auth` OAuth plugin MUST be used — never a manual redirect. The plugin generates and validates the `state` parameter automatically (CSRF protection). Do not disable or bypass this mechanism."

---

### SEC-008 — `ktor-server-rate-limit` Not Wired to Auth Routes

| Field | Value |
|-------|-------|
| **Severity** | LOW |
| **CVSS** | 3.7 |
| **OWASP** | A07:2025 — Identification and Authentication Failures |
| **CWE** | CWE-307: Improper Restriction of Excessive Authentication Attempts |
| **Location** | `specs/004-auth/plan.md` Phase C, `gradle/libs.versions.toml` |
| **Task** | TASK-SEC-008 |

**Description**: `gradle/libs.versions.toml` already includes `ktor-server-rate-limit`. The plan uses Redis-based per-IP INCR for Turnstile triggering and PG-based per-account lockout for 10 failures. However, neither the Redis counter nor the PG lockout prevents a burst of hundreds of requests/second to `/auth/login` before Redis can respond. The built-in Ktor rate-limit plugin can cap request throughput cheaply as a first defense layer (e.g., max 20 requests/10s per IP), independent of Redis.

**Required fix**: Add to plan Phase C: "Install `ktor-server-rate-limit` with a global rate limit on all `POST /api/v1/auth/*` routes (e.g., 20 requests per 10 s per IP). This complements the Redis-based Turnstile trigger and the PG-based lockout."

---

### SEC-009 — WebAuthn `signCount` Monotonicity Check Not Specified

| Field | Value |
|-------|-------|
| **Severity** | LOW |
| **CVSS** | 3.1 |
| **OWASP** | A07:2025 — Identification and Authentication Failures |
| **CWE** | CWE-287: Improper Authentication |
| **Location** | `specs/004-auth/data-model.md` — `webauthn_credentials.sign_count` |
| **Task** | TASK-SEC-009 |

**Description**: The data model stores `sign_count` ("Counter for clone detection") but the plan doesn't specify the required behavior when an incoming assertion carries a `signCount` ≤ the stored value. Per WebAuthn Level 2 §6.1, if the new `signCount` is less than or equal to the stored value and the stored value is non-zero, the authenticator may be cloned and the server SHOULD refuse the assertion or flag the credential.

**Required fix**: Add to plan Phase B `AuthenticatePasskeyUseCase`: "If `assertion.signCount ≤ storedCredential.signCount` AND `storedCredential.signCount > 0`, log a security event and return `401 PASSKEY_ASSERTION_FAILED`."

---

### SEC-010 — X-Forwarded-For Extraction Strategy Not Defined (Informational)

| Field | Value |
|-------|-------|
| **Severity** | INFORMATIONAL |
| **CVSS** | 0.0 |
| **OWASP** | — |
| **CWE** | — |
| **Location** | `specs/004-auth/data-model.md` — Redis key schema, `KnownLoginIpsTable` |

**Description**: The plan hashes `X-Forwarded-For` or `remoteHost` for both the Redis rate-limit key and `known_login_ips`. It doesn't specify which header wins or how to handle spoofed `X-Forwarded-For` values (e.g., a client sending `X-Forwarded-For: 1.2.3.4, attacker-ip`). Ktor's `ForwardedHeaders` plugin already handles this — but the plan should note that `ktor-server-forwarded-header` (already in catalog) MUST be installed and the first IP in the chain (leftmost) used after stripping client-supplied headers.

**Recommended note**: Add to plan Phase G: "Install `ktor-server-forwarded-header` plugin to canonicalize `X-Forwarded-For`. Use `call.request.origin.remoteHost` after plugin normalisation — never concatenate the raw header."

---

### SEC-011 — TOTP Encryption Key Rotation Not Addressed (Informational)

| Field | Value |
|-------|-------|
| **Severity** | INFORMATIONAL |
| **CVSS** | 0.0 |
| **OWASP** | — |
| **CWE** | — |
| **Location** | `specs/004-auth/research.md` §D2, `specs/004-auth/data-model.md` — `totp_configs.secret_encrypted` |

**Description**: TOTP secrets are AES-256-GCM encrypted with `TOTP_ENCRYPTION_KEY`. The plan does not address what happens if this key must be rotated: all existing encrypted secrets become unreadable until re-encrypted with the new key. For v1 this is acceptable, but a migration path should be noted for the future.

**Recommended note**: Add to `research.md §D2`: "Key rotation: if `TOTP_ENCRYPTION_KEY` must be rotated, a background job must decrypt all `secret_encrypted` values with the old key and re-encrypt with the new key. Deferred to a future ops runbook."

---

## Confirmed Secure Patterns

| Pattern | Where | Assessment |
|---------|-------|------------|
| Argon2id (2/65536/4) | `research.md §D9` | OWASP 2025 compliant |
| Refresh tokens as SHA-256 hashes in PG | `data-model.md` | Correct: stolen DB row ≠ valid token |
| Refresh token rotation | `contracts/api-auth.md` | Correct: old token invalidated on use |
| HttpOnly + SameSite=Lax refresh cookie | `research.md §D6` | Correct SameSite choice for OAuth compatibility |
| Access token in JS memory only | `research.md §D6` | XSS-resistant |
| WebAuthn challenge in signed HttpOnly cookie (stateless) | `research.md §D1` | Eliminates need for Redis/DB ephemeral storage |
| TOTP secret AES-256-GCM at rest | `research.md §D2`, `data-model.md` | Correct: compromised DB alone cannot reconstruct TOTP |
| IP hashed before storage | `data-model.md` | Privacy-preserving; GeoIP still possible |
| Emergency revoke token hashed before storage | `data-model.md` | Consistent with other token patterns |
| OAuth using Ktor server-side code flow (no implicit) | `research.md §D8` | Correct: no tokens exposed in URL fragment |
| No enumeration on register / forgot-password / verify | `contracts/api-auth.md` | Generic responses throughout |
| `sanitizeRequestId()` on all new routes | `plan.md §Phase C` | Consistent with D15 |
| All queries use Exposed ORM DSL | `data-model.md` | Injection-safe by design |
| Magic bytes + MIME validation on avatar upload | `research.md §D15` | Correct dual-validation approach |
| Recovery codes are single-use SHA-256 hashes | `data-model.md §totp_configs` | Correct: used code cannot be replayed |
| Per-account lockout in PG, per-IP counter in Redis | `data-model.md` | Correct separation of concerns |

---

## Action Plan

### Immediate (before implementation starts)

| Finding | Action |
|---------|--------|
| SEC-001 | Add `WEBAUTHN_CHALLENGE_KEY` env var to `quickstart.md`, `security_constitution.md §4`, and `research.md §D1` |
| SEC-002 | Add token generation spec ("32 bytes SecureRandom, hex-encoded") to `data-model.md` for all 6 token tables |
| SEC-003 | Add `REDIS_PASSWORD` to required env vars; update `REDIS_URL` example format |
| SEC-004 | Add `Cache-Control: no-store` requirement to Phase C of `plan.md` |
| SEC-005 | Add `type: "totp-session"` claim spec to `contracts/api-auth.md` and plan complexity tracking |
| SEC-006 | Clarify avatar stored filename derivation in `research.md §D15` |
| SEC-007 | Document OAuth state parameter requirement in plan Phase C item 19 |

### Before Phase C (API implementation)

| Finding | Action |
|---------|--------|
| SEC-008 | Wire `ktor-server-rate-limit` to all `POST /api/v1/auth/*` routes |
| SEC-009 | Specify signCount monotonicity check in `AuthenticatePasskeyUseCase` |
| SEC-010 | Install `ktor-server-forwarded-header` plugin in Phase G |

### Deferred

| Finding | Deferral |
|---------|---------|
| SEC-011 | Document TOTP key rotation as ops runbook for future; no action needed now |

---

## Memory Hub INDEX.md Row

```text
| docs/security-reviews/2026-06-09-feature-004-auth-plan.md | plan | 2026-06-09 | HIGH | C:0 H:2 M:5 L:2 I:2 | A01,A02,A05,A07 |
```
