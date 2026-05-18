# §VIII — Authentication & Authorization

All protected API endpoints MUST require a valid **JWT access token** in the
`Authorization: Bearer <token>` header. Token issuance and validation MUST use the
`ktor-server-auth-jwt` Ktor plugin.

Token lifecycle:
- **Access token**: short-lived (15 minutes). Carries claims: `sub` (userId), `role`, `iat`, `exp`.
- **Refresh token**: longer-lived (7 days), stored server-side in Redis (revocable).
  Issued alongside the access token on login; exchanged for a new access token on expiry.
- Refresh tokens MUST be invalidated on explicit logout and on password change.

Password storage:
- All user passwords MUST be hashed with **Argon2id** (`argon2-jvm` library) before
  persistence. bcrypt (`jbcrypt`) is acceptable for migration paths but Argon2id is
  the required algorithm for new accounts.
- Plaintext, MD5, SHA-1, and unsalted SHA-256 storage are strictly forbidden.
- The JWT signing secret MUST be at minimum 256 bits, loaded from an environment variable
  (never hardcoded). See `security.md`.

**Rationale**: JWT enables stateless horizontal scaling of the API while the refresh-token
Redis store allows instant revocation without full statefulness. Argon2id is the
OWASP-recommended password hashing algorithm as of 2024 — it is memory-hard and resistant
to GPU-based brute force.
