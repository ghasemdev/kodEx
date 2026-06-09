# §VIII — Authentication & Authorization

All protected API endpoints MUST require a valid **JWT access token** in the
`Authorization: Bearer <token>` header. Token issuance and validation MUST use the
`ktor-server-auth-jwt` Ktor plugin.

Token lifecycle:
- **Access token**: short-lived (15 minutes). Carries claims: `sub` (userId), `role`, `iat`, `exp`.
- **Refresh token**: longer-lived (7 days), stored server-side in **PostgreSQL as a SHA-256 hash**
  (revocable). Issued alongside the access token on login; exchanged for a new access token on
  expiry. Lookup is a single indexed query (`WHERE token_hash = $1 AND revoked_at IS NULL AND
  expires_at > now()`) — runs at most once per 15-minute access token lifetime.
- Refresh tokens MUST be invalidated on explicit logout and on password change.

Password storage:
- All user passwords MUST be hashed with **Argon2id** (`argon2-jvm` library) before
  persistence. bcrypt (`jbcrypt`) is acceptable for migration paths but Argon2id is
  the required algorithm for new accounts.
- Plaintext, MD5, SHA-1, and unsalted SHA-256 storage are strictly forbidden.
- The JWT signing secret MUST be at minimum 256 bits, loaded from an environment variable
  (never hardcoded). See `security_constitution.md`.

**Rationale**: JWT enables stateless horizontal scaling of the API. Refresh tokens in PostgreSQL
allow instant revocation (single UPDATE) without requiring Redis as mandatory infrastructure for
auth. The token lookup is infrequent (once per 15-minute access token lifetime per user) making
PG the pragmatic choice. Argon2id is the OWASP-recommended password hashing algorithm as of 2024 —
it is memory-hard and resistant to GPU-based brute force.

**Amendment history**: §VIII 1.5.0 → 1.6.0 (2026-06-08) — refresh token storage changed from
Redis to PostgreSQL SHA-256 hash. Redis remains in the stack for per-IP rate-limit counters
(spec 004) and is the opt-in upgrade path for multi-instance `QuotaTracker`.
