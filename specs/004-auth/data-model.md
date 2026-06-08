# Data Model: Authentication & User Identity (Spec 004)

**Branch**: `feature/004-auth` | **Date**: 2026-06-08

---

## PostgreSQL Schema (Flyway V3__auth_schema.sql)

### Table: `users`

Primary identity record for every account.

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | `BIGSERIAL` | PK | |
| `username` | `VARCHAR(30)` | NOT NULL UNIQUE | 3–30 chars, `[a-z0-9_-]` only |
| `email` | `VARCHAR(255)` | NOT NULL UNIQUE | Lowercased before storage |
| `email_verified` | `BOOLEAN` | NOT NULL DEFAULT false | |
| `password_hash` | `VARCHAR(255)` | NULLABLE | Null for OAuth-only accounts |
| `role` | `VARCHAR(20)` | NOT NULL DEFAULT 'PARTICIPANT' | PARTICIPANT \| EXAM_CREATOR \| ADMIN |
| `failed_login_count` | `INTEGER` | NOT NULL DEFAULT 0 | Reset on successful login |
| `locked_until` | `TIMESTAMPTZ` | NULLABLE | Null = not locked |
| `created_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT now() | |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT now() | |

**Indexes**: `users_username_idx UNIQUE`, `users_email_idx UNIQUE`

---

### Table: `user_profiles`

Editable public/private profile metadata. One row per user.

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | `BIGSERIAL` | PK | |
| `user_id` | `BIGINT` | FK → users(id) UNIQUE | CASCADE DELETE |
| `display_name` | `VARCHAR(100)` | NULLABLE | Falls back to username in UI |
| `first_name` | `VARCHAR(100)` | NULLABLE | |
| `last_name` | `VARCHAR(100)` | NULLABLE | |
| `birthdate` | `DATE` | NULLABLE | YYYY-MM-DD; age computed at render time |
| `avatar_url` | `VARCHAR(512)` | NULLABLE | Object storage URL |
| `location` | `VARCHAR(100)` | NULLABLE | Free text |
| `github_url` | `VARCHAR(512)` | NULLABLE | Validated URL |
| `linkedin_url` | `VARCHAR(512)` | NULLABLE | Validated URL |
| `twitter_url` | `VARCHAR(512)` | NULLABLE | Validated URL |
| `website_url` | `VARCHAR(512)` | NULLABLE | Validated URL |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT now() | |

Profile row is created (all nulls) immediately after user registration.

---

### Table: `oauth_identities`

Links external provider identities to a user account.

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | `BIGSERIAL` | PK | |
| `user_id` | `BIGINT` | FK → users(id) | CASCADE DELETE |
| `provider` | `VARCHAR(20)` | NOT NULL | GITHUB \| GOOGLE |
| `provider_user_id` | `VARCHAR(255)` | NOT NULL | Provider-assigned user ID |
| `linked_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT now() | |

**Indexes**: `oauth_identities_provider_user_idx UNIQUE (provider, provider_user_id)`, `oauth_identities_user_idx (user_id)`

---

### Table: `refresh_tokens`

Server-side refresh token registry. Tokens are SHA-256 hashed before storage.

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | `BIGSERIAL` | PK | |
| `user_id` | `BIGINT` | FK → users(id) | CASCADE DELETE |
| `token_hash` | `CHAR(64)` | NOT NULL UNIQUE | Hex-encoded SHA-256 of raw token |
| `issued_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT now() | |
| `expires_at` | `TIMESTAMPTZ` | NOT NULL | issued_at + 7 days |
| `revoked_at` | `TIMESTAMPTZ` | NULLABLE | Null = active |
| `device_hint` | `VARCHAR(255)` | NULLABLE | Parsed from User-Agent: `"Chrome 124 / macOS 14"` |
| `ip_address` | `VARCHAR(45)` | NULLABLE | Raw IP (IPv4 or IPv6) for sessions list display |

**`device_hint` population**: Parsed at token issuance time using `uap-java` from the `User-Agent` request header. Format: `"{browser} {majorVersion} / {os} {osMajorVersion}"`. Examples:
- `Chrome 124 / macOS 14`
- `Firefox 125 / Windows 11`
- `Safari 17 / iPhone iOS 17`
- `Unknown` — if `User-Agent` header is absent

**Session revocation by user**: `DELETE /api/v1/users/me/sessions/{id}` sets `revoked_at = now()` for that row. The user sees the `device_hint` + `ip_address` in the sessions list and can revoke any session including those on other devices.

**Active token query**: `WHERE user_id = ? AND revoked_at IS NULL AND expires_at > now()`

**Indexes**: `refresh_tokens_hash_idx UNIQUE`, `refresh_tokens_user_active_idx (user_id, revoked_at, expires_at)`

---

### Table: `email_verification_tokens`

Pending email verification records (registration + re-send).

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | `BIGSERIAL` | PK | |
| `user_id` | `BIGINT` | FK → users(id) | CASCADE DELETE |
| `token_hash` | `CHAR(64)` | NOT NULL | SHA-256 of raw token (magic link) or OTP code |
| `delivery_mode` | `VARCHAR(20)` | NOT NULL | MAGIC_LINK \| OTP |
| `expires_at` | `TIMESTAMPTZ` | NOT NULL | 24 h (magic link) \| 15 min (OTP) |
| `used_at` | `TIMESTAMPTZ` | NULLABLE | Null = unused |

**OTP storage note**: For 6-digit OTP, store SHA-256(`userId:otp`) as `token_hash` so that the raw 6 digits are never stored.

---

### Table: `pending_email_changes`

In-flight email address change requests.

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | `BIGSERIAL` | PK | |
| `user_id` | `BIGINT` | FK → users(id) UNIQUE | One pending change per user |
| `new_email` | `VARCHAR(255)` | NOT NULL | The requested new email |
| `token_hash` | `CHAR(64)` | NOT NULL | SHA-256 of verification token |
| `requested_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT now() | |
| `expires_at` | `TIMESTAMPTZ` | NOT NULL | requested_at + 24 h |
| `used_at` | `TIMESTAMPTZ` | NULLABLE | |

---

### Table: `password_reset_tokens`

One-use password reset links (Forgot Password flow).

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | `BIGSERIAL` | PK | |
| `user_id` | `BIGINT` | FK → users(id) | CASCADE DELETE |
| `token_hash` | `CHAR(64)` | NOT NULL UNIQUE | SHA-256 of raw reset token |
| `expires_at` | `TIMESTAMPTZ` | NOT NULL | issued + 1 h |
| `used_at` | `TIMESTAMPTZ` | NULLABLE | |

---

### Table: `emergency_revoke_tokens`

Single-use tokens embedded in new-device alert emails; clicking revokes all sessions without requiring login.

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | `BIGSERIAL` | PK | |
| `user_id` | `BIGINT` | FK → users(id) | CASCADE DELETE |
| `token_hash` | `CHAR(64)` | NOT NULL UNIQUE | SHA-256 of raw token |
| `expires_at` | `TIMESTAMPTZ` | NOT NULL | issued + 24 h |
| `used_at` | `TIMESTAMPTZ` | NULLABLE | Null = unused |

**Index**: `emergency_revoke_tokens_hash_idx UNIQUE`

---

### Table: `webauthn_credentials`

Registered FIDO2/WebAuthn passkeys.

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | `BIGSERIAL` | PK | |
| `user_id` | `BIGINT` | FK → users(id) | CASCADE DELETE |
| `credential_id` | `BYTEA` | NOT NULL UNIQUE | Raw credential ID bytes |
| `public_key_cose` | `BYTEA` | NOT NULL | COSE-encoded public key |
| `sign_count` | `BIGINT` | NOT NULL DEFAULT 0 | Counter for clone detection |
| `aaguid` | `VARCHAR(36)` | NULLABLE | Authenticator AAGUID (UUID) |
| `friendly_name` | `VARCHAR(100)` | NULLABLE | User-assigned name |
| `created_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT now() | |

**Index**: `webauthn_credentials_user_idx (user_id)`

---

### Table: `totp_configs`

Per-user TOTP 2FA configuration.

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | `BIGSERIAL` | PK | |
| `user_id` | `BIGINT` | FK → users(id) UNIQUE | CASCADE DELETE |
| `secret_encrypted` | `TEXT` | NOT NULL | AES-256-GCM ciphertext (base64); IV prepended |
| `enabled` | `BOOLEAN` | NOT NULL DEFAULT false | false = setup in progress |
| `backup_codes_hashes` | `TEXT` | NOT NULL DEFAULT '[]' | JSON array of SHA-256 hashes of one-time codes |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT now() | |

**Recovery code lifecycle**: 8 codes generated on setup; each hash removed from array when used. Array empty → all codes consumed (warn user).

---

### Table: `known_login_ips`

IP addresses seen for each user — used for new-device login alerts.

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | `BIGSERIAL` | PK | |
| `user_id` | `BIGINT` | FK → users(id) | CASCADE DELETE |
| `ip_hash` | `CHAR(64)` | NOT NULL | SHA-256 of raw IP address (privacy) |
| `country_code` | `CHAR(2)` | NULLABLE | ISO 3166-1 alpha-2 |
| `city` | `VARCHAR(100)` | NULLABLE | GeoIP city name |
| `first_seen_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT now() | |
| `last_seen_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT now() | |

**Index**: `known_login_ips_user_ip_idx UNIQUE (user_id, ip_hash)`

---

## Exposed Table Definitions (Kotlin)

```kotlin
// server/data module — dev.kodex.server.data.db.tables

object UsersTable : LongIdTable("users") {
    val username       = varchar("username", 30).uniqueIndex()
    val email          = varchar("email", 255).uniqueIndex()
    val emailVerified  = bool("email_verified").default(false)
    val passwordHash   = varchar("password_hash", 255).nullable()
    val role           = enumerationByName<Role>("role", 20).default(Role.PARTICIPANT)
    val failedLoginCount = integer("failed_login_count").default(0)
    val lockedUntil    = timestamp("locked_until").nullable()
    val createdAt      = timestamp("created_at")
    val updatedAt      = timestamp("updated_at")
}

object UserProfilesTable : LongIdTable("user_profiles") {
    val userId       = reference("user_id", UsersTable).uniqueIndex()
    val displayName  = varchar("display_name", 100).nullable()
    val firstName    = varchar("first_name", 100).nullable()
    val lastName     = varchar("last_name", 100).nullable()
    val birthdate    = date("birthdate").nullable()
    val avatarUrl    = varchar("avatar_url", 512).nullable()
    val location     = varchar("location", 100).nullable()
    val githubUrl    = varchar("github_url", 512).nullable()
    val linkedinUrl  = varchar("linkedin_url", 512).nullable()
    val twitterUrl   = varchar("twitter_url", 512).nullable()
    val websiteUrl   = varchar("website_url", 512).nullable()
    val updatedAt    = timestamp("updated_at")
}

object OAuthIdentitiesTable : LongIdTable("oauth_identities") {
    val userId         = reference("user_id", UsersTable)
    val provider       = enumerationByName<OAuthProvider>("provider", 20)
    val providerUserId = varchar("provider_user_id", 255)
    val linkedAt       = timestamp("linked_at")
    init { uniqueIndex(provider, providerUserId) }
}

object RefreshTokensTable : LongIdTable("refresh_tokens") {
    val userId      = reference("user_id", UsersTable)
    val tokenHash   = char("token_hash", 64).uniqueIndex()
    val issuedAt    = timestamp("issued_at")
    val expiresAt   = timestamp("expires_at")
    val revokedAt   = timestamp("revoked_at").nullable()
    val deviceHint  = varchar("device_hint", 255).nullable()
    val ipAddress   = varchar("ip_address", 45).nullable()
}

object EmailVerificationTokensTable : LongIdTable("email_verification_tokens") {
    val userId       = reference("user_id", UsersTable)
    val tokenHash    = char("token_hash", 64)
    val deliveryMode = enumerationByName<DeliveryMode>("delivery_mode", 20)
    val expiresAt    = timestamp("expires_at")
    val usedAt       = timestamp("used_at").nullable()
}

object PendingEmailChangesTable : LongIdTable("pending_email_changes") {
    val userId      = reference("user_id", UsersTable).uniqueIndex()
    val newEmail    = varchar("new_email", 255)
    val tokenHash   = char("token_hash", 64)
    val requestedAt = timestamp("requested_at")
    val expiresAt   = timestamp("expires_at")
    val usedAt      = timestamp("used_at").nullable()
}

object PasswordResetTokensTable : LongIdTable("password_reset_tokens") {
    val userId    = reference("user_id", UsersTable)
    val tokenHash = char("token_hash", 64).uniqueIndex()
    val expiresAt = timestamp("expires_at")
    val usedAt    = timestamp("used_at").nullable()
}

object EmergencyRevokeTokensTable : LongIdTable("emergency_revoke_tokens") {
    val userId    = reference("user_id", UsersTable)
    val tokenHash = char("token_hash", 64).uniqueIndex()
    val expiresAt = timestamp("expires_at")
    val usedAt    = timestamp("used_at").nullable()
}

object WebAuthnCredentialsTable : LongIdTable("webauthn_credentials") {
    val userId        = reference("user_id", UsersTable)
    val credentialId  = binary("credential_id", 256).uniqueIndex()
    val publicKeyCose = binary("public_key_cose", 512)
    val signCount     = long("sign_count").default(0)
    val aaguid        = varchar("aaguid", 36).nullable()
    val friendlyName  = varchar("friendly_name", 100).nullable()
    val createdAt     = timestamp("created_at")
}

object TotpConfigsTable : LongIdTable("totp_configs") {
    val userId            = reference("user_id", UsersTable).uniqueIndex()
    val secretEncrypted   = text("secret_encrypted")
    val enabled           = bool("enabled").default(false)
    val backupCodesHashes = text("backup_codes_hashes").default("[]")
    val updatedAt         = timestamp("updated_at")
}

object KnownLoginIpsTable : LongIdTable("known_login_ips") {
    val userId      = reference("user_id", UsersTable)
    val ipHash      = char("ip_hash", 64)
    val countryCode = char("country_code", 2).nullable()
    val city        = varchar("city", 100).nullable()
    val firstSeenAt = timestamp("first_seen_at")
    val lastSeenAt  = timestamp("last_seen_at")
    init { uniqueIndex(userId, ipHash) }
}
```

---

## Redis Key Schema

All keys use SHA-256 of the raw IP address so raw IPs never leave the application tier.

| Key pattern | Type | TTL | Purpose |
|-------------|------|-----|---------|
| `login:attempts:<sha256(ip)>` | String (integer) | 600 s | Per-IP failed login counter. `INCR` on each failure; `EXPIRE 600` on first write. Read before each login attempt: ≥ 3 → require Turnstile token. Reset (`DEL`) on successful login from that IP. |

**Counter lifecycle**:
1. Login attempt arrives → `GET login:attempts:<ip_hash>`
2. If value ≥ 3 and no Turnstile token in request → return `400 TURNSTILE_REQUIRED`
3. If credentials are wrong → `INCR login:attempts:<ip_hash>`, set `EXPIRE 600` (only if key is new via `SET ... NX EX 600` idiom)
4. If credentials are correct → `DEL login:attempts:<ip_hash>`; proceed to issue tokens

Account lockout (10 consecutive per-account failures, FR-008a) is tracked in `users.failed_login_count` + `users.locked_until` (PostgreSQL — permanent record, not ephemeral).

---

## KMP Shared Models (`core/models`)

```
core/models/src/commonMain/kotlin/dev/kodex/core/models/auth/
├── Role.kt                    # enum: PARTICIPANT, EXAM_CREATOR, ADMIN
├── OAuthProvider.kt           # enum: GITHUB, GOOGLE
├── AuthTokensResponse.kt      # access_token: String, expires_in: Int (seconds)
├── RegisterRequest.kt         # username, email, password, turnstileToken
├── LoginRequest.kt            # email, password, turnstileToken (nullable)
├── TotpLoginRequest.kt        # sessionToken (temp), totpCode
├── UserDto.kt                 # id, username, email, role, emailVerified, profile: UserProfileDto
├── UserProfileDto.kt          # displayName, firstName, lastName, birthdate, avatarUrl, location, social links
├── SessionDto.kt              # id, deviceHint (e.g. "Chrome 124 / macOS 14"), ipAddress, issuedAt, expiresAt, isCurrent
├── OAuthProviderDto.kt        # provider: OAuthProvider, linkedAt
└── PasskeyDto.kt              # id, friendlyName, aaguid, createdAt
```

---

## State Transitions

### User Account States
```
UNVERIFIED ──(verify email)──► ACTIVE
ACTIVE ──(10 failed logins in 15 min)──► LOCKED (30 min auto-unlock)
LOCKED ──(30 min passes OR unlock link clicked)──► ACTIVE
```

### 2FA Setup States
```
NOT_CONFIGURED ──(setup initiated)──► PENDING_CONFIRMATION
PENDING_CONFIRMATION ──(correct TOTP code submitted)──► ENABLED
ENABLED ──(disable with password + TOTP)──► NOT_CONFIGURED
```

### Email Change States
```
NO_PENDING ──(change email requested)──► PENDING_VERIFICATION (24 h TTL)
PENDING_VERIFICATION ──(new email verified)──► NO_PENDING (email updated)
PENDING_VERIFICATION ──(24 h elapsed)──► NO_PENDING (auto-cancelled)
```
