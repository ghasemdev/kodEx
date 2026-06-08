# Research: Authentication & User Identity (Spec 004)

**Date**: 2026-06-08
**Branch**: `feature/004-auth`

---

## D1 — WebAuthn Server Library

**Decision**: `com.webauthn4j:webauthn4j-core:0.31.6.RELEASE`

**Rationale**:
- Pure Java with excellent Kotlin null-safety annotations (`@NotNull`/`@Nullable` on all public members)
- Minimal dependency tree (SLF4J + Jackson only) — no transitive surprises
- Framework-agnostic: plug-in to any Ktor route handler manually
- Active maintenance; 0.31.x series tracks WebAuthn Level 3 spec

**Alternatives considered**:
- `com.yubico:webauthn-server-core:2.9.0` — enterprise-grade but heavier dependency tree and slightly more complex API for our use case
- Custom CBOR parsing — rejected (reinvents the wheel, audit risk)

**Catalog entries to add**:
```toml
webauthn4j = "0.31.6.RELEASE"
webauthn4j-core = { module = "com.webauthn4j:webauthn4j-core", version.ref = "webauthn4j" }
```

**Challenge state storage**: WebAuthn requires server to remember the challenge until the assertion response arrives (~30 s). Approach chosen: signed, AES-GCM-encrypted HttpOnly cookie containing the challenge bytes. Avoids any server-side state (no Redis, no PG row). Cookie is set on `begin` and validated + deleted on `complete`.

---

## D2 — TOTP Library

**Decision**: `dev.turingcomplete:kotlin-onetimepassword:3.0.0`

**Rationale**:
- Native Kotlin (not a Java wrapper), KMP-compatible
- RFC 6238 compliant; `GoogleAuthenticatorOneTimePasswordGenerator` class covers QR setup and verification in one API
- Actively maintained; 3.0.0 released 2026

**Alternatives considered**:
- `com.warrenstrange:googleauth:1.5.0` — Java, unmaintained since 2022
- Manual HMAC-SHA1 implementation — rejected (audit risk)

**Catalog entries to add**:
```toml
kotlin-onetimepassword = "3.0.0"
kotlin-onetimepassword-lib = { module = "dev.turingcomplete:kotlin-onetimepassword", version.ref = "kotlin-onetimepassword" }
```

**TOTP secret storage**: Secret is AES-256-GCM encrypted with `TOTP_ENCRYPTION_KEY` env var before insertion into `totp_configs.secret_encrypted`. Key is 256-bit, base64-encoded. IV is stored as prefix in the ciphertext column.

---

## D3 — Password Strength Estimation

**Decision**:
- **Client-side (npm / Kotlin/JS)**: `zxcvbn-ts:3.0.4` (actively maintained TypeScript rewrite)
- **Server-side (JVM)**: `com.nulab-inc:zxcvbn:1.9.0`

**Rationale**: Both libraries implement the same algorithm, so client-side (real-time feedback) and server-side (validation gate) produce consistent scores. Score ≥ 2 ("Fair") is the acceptance threshold.

**Score mapping**:
| Score | Label | Accept |
|-------|-------|--------|
| 0 | Too weak | ❌ |
| 1 | Weak | ❌ |
| 2 | Fair | ✅ minimum |
| 3 | Strong | ✅ |
| 4 | Very Strong | ✅ |

**Client-side integration**:
```kotlin
// In webApp build.gradle.kts (jsMain + wasmJsMain)
implementation(npm("zxcvbn-ts", "3.0.4"))
```

**Catalog entries to add**:
```toml
zxcvbn4j = "1.9.0"
zxcvbn4j-lib = { module = "com.nulab-inc:zxcvbn", version.ref = "zxcvbn4j" }
```

---

## D4 — GeoIP for New-Device Detection

**Decision**: `com.maxmind.geoip2:geoip2:5.1.0` + GeoLite2-City.mmdb

**Rationale**:
- Free (CC BY-SA 4.0), no API key required
- Local database lookup (~50 MB file, ~0.3 ms/query) — zero network latency at login time
- Standard JVM library with full Kotlin interop

**Database update strategy**: Docker Compose mounts `infra/geoip/GeoLite2-City.mmdb`. CI/CD cron updates it weekly via jsDelivr mirror (no MaxMind account needed):
```
https://cdn.jsdelivr.net/npm/geolite2-city/GeoLite2-City.mmdb.gz
```

**Catalog entries to add**:
```toml
geoip2 = "5.1.0"
geoip2-lib = { module = "com.maxmind.geoip2:geoip2", version.ref = "geoip2" }
```

**New-device detection logic**: IP is hashed with SHA-256 before any storage (`KnownLoginIpsTable.ipHash`). On login, compute SHA-256 of `X-Forwarded-For` (sanitized) or `remoteHost`, look up in `known_login_ips` for this user. If absent → new device → send alert, insert row, record GeoIP result. If present → update `lastSeenAt`.

---

## D5 — Transactional Email

**Decision**: Resend REST API via `com.resend:resend-java:4.14.1` (official JVM SDK)

**Rationale**:
- Official SDK available on Maven Central (avoids hand-rolling HTTP client calls)
- Free tier: 3,000 emails/month — sufficient for v1
- Simple API: `resend.emails().send(SendEmailRequest(...))`
- Alternative path: raw Ktor `HttpClient` POST to `https://api.resend.com/emails` if SDK adds unwanted deps

**Catalog entries to add**:
```toml
resend = "4.14.1"
resend-java = { module = "com.resend:resend-java", version.ref = "resend" }
```

**New env vars**: `RESEND_API_KEY`, `EMAIL_FROM` (e.g. `noreply@kodex.dev`), `APP_BASE_URL` (for link construction).

---

## D6 — Token Delivery Strategy

**Decision**: Refresh token as HttpOnly cookie; access token in response body (JS memory only)

**Rationale**:
- HttpOnly cookie for refresh token → inaccessible to JavaScript → XSS cannot steal it
- `SameSite=Lax` (not `Strict`): OAuth callbacks are cross-site GET redirects; `Strict` would drop the cookie, breaking the OAuth flow
- `Secure` flag: enabled in production (HTTPS only); disabled in local Docker dev
- Access token stays in JS memory (Kilua `AuthStore`) — not in `localStorage` (XSS-resistant)
- Future mobile clients: refresh token delivered in response body + stored in Keychain/Keystore

**Cookie spec**:
```
Set-Cookie: refresh_token=<opaque_token>; HttpOnly; Secure (prod); SameSite=Lax; Path=/api/v1/auth; Max-Age=604800
```
Path is scoped to `/api/v1/auth` so the cookie is not sent on every API call — only on refresh/logout.

---

## D7 — Cloudflare Turnstile Integration

**Decision**: No SDK — direct HTTP POST to Cloudflare's verification endpoint

**Endpoint**: `POST https://challenges.cloudflare.com/turnstile/v0/siteverify`
**Payload**: `{ "secret": "<CLOUDFLARE_TURNSTILE_SECRET>", "response": "<token>", "remoteip": "<ip>" }`
**Check**: `response.success == true`

Implementation: `TurnstileVerifier` in `server:api` — a thin Ktor `HttpClient` wrapper called from `AuthRoutes` before credential processing.

**New env vars**: `CLOUDFLARE_TURNSTILE_SITE_KEY` (frontend), `CLOUDFLARE_TURNSTILE_SECRET` (backend).
**Trigger points**: registration form submit, login after 3 failed attempts, forgot-password form.

---

## D8 — OAuth2 Provider Configuration

**Decision**: Ktor `ktor-server-auth` OAuth2 plugin (already in catalog at 3.5.0)

**GitHub scopes**: `read:user user:email`
**Google scopes**: `openid email profile`

**Profile fetch after OAuth callback**:
- GitHub: `GET https://api.github.com/user` + `GET https://api.github.com/user/emails` (for primary verified email)
- Google: ID token (JWT) decoded directly — contains `sub`, `email`, `name`, `picture`

**New env vars**: `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `OAUTH_CALLBACK_BASE_URL`.

---

## D9 — Argon2id Parameters

**Decision**: iterations=2, memory=65536 KiB (64 MiB), parallelism=4

Per OWASP Password Storage Cheat Sheet 2025/2026. Library `de.mkammerer:argon2-jvm` is already in catalog at version `2.12`. No version change needed — parameters are passed at call site.

---

## D10 — Constitution Amendment Required (§VIII)

**Current §VIII**: "Refresh token: longer-lived (7 days), stored server-side in **Redis** (revocable)."
**Proposed change**: Replace "Redis" with "PostgreSQL" throughout §VIII and security_constitution.md §2.

**Justification**:
- Behavior is identical: server-side, revocable, hashed before storage
- PostgreSQL lookup for refresh token (~0.5 ms) is fast enough — this query occurs at most once per 15-minute access token lifetime
- Eliminates Redis as a required infrastructure component for auth, simplifying Docker Compose and ops
- Redis may be re-introduced later for rate limiting (SEC-004) or leaderboard caching (spec 010) — it shouldn't be mandated by the auth spec
- Token rotation means each refresh produces a new token + invalidates the old one; the PG update is a single indexed UPDATE

**Constitution version bump**: 1.5.0 → 1.6.0 (MINOR — tech selection change in a named principle)

**Affected files**:
- `.specify/memory/principles/viii-authentication.md`
- `.specify/memory/security_constitution.md` (§2 JWT lifecycle section)
- `.specify/memory/constitution.md` (quick reference row for §VIII)
- `docs/memory/DECISIONS.md` (add entry D16)
- Remove `REDIS_PASSWORD` from required env vars in security_constitution.md §4 (for auth spec; keep if reintroduced later)

---

## D11 — JWT Library

**Decision**: `io.ktor:ktor-server-auth-jwt:3.5.0` (already in catalog)

**Rationale**: Official Ktor plugin — handles JWT issuance, signature verification, and claim extraction natively. No additional library needed. Configured via `install(Authentication) { jwt("auth-jwt") { ... } }` in `server:app`.

**Signing algorithm**: HS256 (HMAC-SHA256) with a ≥256-bit secret loaded from `JWT_SECRET` env var (already required by §VIII).

---

## D12 — Password Hashing

**Decision**: Argon2id via `de.mkammerer:argon2-jvm:2.12` (already in catalog)

**Parameters** (OWASP Password Storage Cheat Sheet 2025):
```
type        = Argon2id
iterations  = 2
memory      = 65536 KiB  (64 MiB)
parallelism = 4
```

**Rationale**: Memory-hard algorithm — GPU brute-force attacks are economically infeasible. bcrypt and plain SHA-256 are explicitly forbidden by §VIII for new accounts.

**Wrapper**: `server/data/src/main/kotlin/.../crypto/PasswordHasher.kt` — thin Kotlin wrapper with `hash(password)` and `verify(hash, password)` functions. Never called outside `server:data`.

---

## D13 — User-Agent Parsing for Session Device Hints

**Decision**: `com.github.ua-parser:uap-java:1.6.1`

**Rationale**:
- Local parsing (no network call at login time)
- Produces structured `browser name + OS` from raw `User-Agent` header
- Output stored as `device_hint` in `refresh_tokens` table (e.g. `"Chrome 124 / macOS 14"`, `"Safari / iPhone iOS 17"`)
- Used in the Sessions list UI so users can identify and revoke specific sessions

**Integration**: Called in `AuthRoutes` when issuing a refresh token:
```kotlin
val userAgent = call.request.headers[HttpHeaders.UserAgent] ?: "Unknown"
val parsed = uaParser.parse(userAgent)
val deviceHint = "${parsed.userAgent.family} ${parsed.userAgent.major} / ${parsed.os.family} ${parsed.os.major}".trim()
```

**Catalog entries to add**:
```toml
ua-parser = "1.6.1"
ua-parser-java = { module = "com.github.ua-parser:uap-java", version.ref = "ua-parser" }
```

---

## D14 — Redis Client (Rate Limiting)

**Decision**: `io.lettuce:lettuce-core:6.3.2.RELEASE`

**Rationale**:
- Lettuce is the de facto standard async Java/Kotlin Redis client; fully non-blocking, coroutine-friendly via `await()` on `RedisFuture`
- No Redis-specific Kotlin library mature enough for production; lettuce + coroutine wrappers is the established pattern in the Ktor ecosystem
- Minimal API surface needed: only `INCR`, `EXPIRE`, `GET`, `DEL` for the rate-limit counter use case

**Alternatives considered**:
- `jedis` — synchronous, thread-per-connection; not suitable for Ktor's coroutine model
- `kreds` (Kotlin-native) — promising but not production-proven; small community
- In-memory `ConcurrentHashMap` — single-host only; lost on restart; no TTL native support

**Use cases in this spec**:
1. **Per-IP failed login counter** (`login:attempts:<sha256(ip)>`) — `INCR` + `EXPIRE 600` on each failed attempt; if count ≥ 3, require Turnstile; if count ≥ 10, trigger account lockout check
2. **Infrastructure foundation** — sets up Redis in Docker Compose for future rate-limiting (SEC-004) and leaderboard caching (spec 010) without additional infra changes

**Redis key schema**:
```
login:attempts:<sha256(ip)>   String  TTL=600s  # per-IP failed login counter
```

**Connection**: Single `RedisClient` created at startup; `StatefulRedisConnection<String, String>` shared via Koin `@Single`. URI from `REDIS_URL` env var (e.g. `redis://localhost:6379`).

**Catalog entries to add**:
```toml
lettuce = "6.3.2.RELEASE"
lettuce-core = { module = "io.lettuce:lettuce-core", version.ref = "lettuce" }
```

---

## D15 — Avatar Object Storage

**Decision**: MinIO (self-hosted S3-compatible) via `io.minio:minio:8.5.11`

**Rationale**:
- MinIO runs as a single Docker container; zero external cloud dependency in local dev and CI
- S3-compatible API means a production switch to AWS S3 / R2 / GCS requires only env var changes, not code changes
- Official Java SDK (`io.minio:minio`) covers all needed operations: `putObject`, `presignedGetObjectUrl`
- File upload flow: server receives `multipart/form-data`, validates MIME type + magic bytes (first 16 bytes), streams directly to MinIO, stores resulting URL in `user_profiles.avatar_url`

**Accepted file types**: `image/jpeg`, `image/png`, `image/webp` — validated by magic bytes:
- JPEG: `FF D8 FF`
- PNG: `89 50 4E 47 0D 0A 1A 0A`
- WebP: `52 49 46 46 … 57 45 42 50`

**Max size**: 5 MB (FR-025). Enforced before streaming to MinIO.

**URL format**: `http(s)://<MINIO_PUBLIC_URL>/<MINIO_BUCKET_AVATARS>/<userId>.<ext>`

**Catalog entries to add**:
```toml
minio = "8.5.11"
minio-sdk = { module = "io.minio:minio", version.ref = "minio" }
```

**New env vars**: `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET_AVATARS`, `MINIO_PUBLIC_URL`.

---

## New Environment Variables Summary

**Already in catalog (no new entries needed)**:
- `ktor-server-auth-jwt` — JWT plugin
- `argon2-jvm` — Argon2id password hashing

**New catalog entries**:
```toml
# versions
ua-parser            = "1.6.1"
webauthn4j           = "0.31.6.RELEASE"
kotlin-onetimepassword = "3.0.0"
zxcvbn4j             = "1.9.0"
geoip2               = "5.1.0"
resend               = "4.14.1"
lettuce              = "6.3.2.RELEASE"
minio                = "8.5.11"

# libraries
ua-parser-java       = { module = "com.github.ua-parser:uap-java",            version.ref = "ua-parser" }
webauthn4j-core      = { module = "com.webauthn4j:webauthn4j-core",            version.ref = "webauthn4j" }
kotlin-onetimepassword-lib = { module = "dev.turingcomplete:kotlin-onetimepassword", version.ref = "kotlin-onetimepassword" }
zxcvbn4j-lib         = { module = "com.nulab-inc:zxcvbn",                     version.ref = "zxcvbn4j" }
geoip2-lib           = { module = "com.maxmind.geoip2:geoip2",                version.ref = "geoip2" }
resend-java          = { module = "com.resend:resend-java",                    version.ref = "resend" }
lettuce-core         = { module = "io.lettuce:lettuce-core",                   version.ref = "lettuce" }
minio-sdk            = { module = "io.minio:minio",                            version.ref = "minio" }
```

---

## New Environment Variables

| Variable | Purpose | Module |
|----------|---------|--------|
| `GITHUB_CLIENT_ID` | GitHub OAuth app client ID | server:app |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth app client secret | server:app |
| `GOOGLE_CLIENT_ID` | Google OAuth 2.0 client ID | server:app |
| `GOOGLE_CLIENT_SECRET` | Google OAuth 2.0 client secret | server:app |
| `OAUTH_CALLBACK_BASE_URL` | Base URL for OAuth callback routes | server:app |
| `CLOUDFLARE_TURNSTILE_SITE_KEY` | Turnstile public key (frontend) | webApp |
| `CLOUDFLARE_TURNSTILE_SECRET` | Turnstile secret (backend verify) | server:app |
| `RESEND_API_KEY` | Resend transactional email key | server:app |
| `EMAIL_FROM` | Sender address (e.g. `noreply@kodex.dev`) | server:app |
| `APP_BASE_URL` | Base URL for link generation (e.g. `https://kodex.dev`) | server:app |
| `TOTP_ENCRYPTION_KEY` | AES-256 key (base64, 32 bytes) for TOTP secret encryption | server:app |
| `GEOIP_DB_PATH` | Absolute path to GeoLite2-City.mmdb | server:app |
| `REDIS_URL` | Redis connection URI (e.g. `redis://localhost:6379`) | server:app |
| `MINIO_ENDPOINT` | MinIO server URL (e.g. `http://minio:9000`) | server:app |
| `MINIO_ACCESS_KEY` | MinIO access key | server:app |
| `MINIO_SECRET_KEY` | MinIO secret key | server:app |
| `MINIO_BUCKET_AVATARS` | Bucket name for avatar storage (e.g. `avatars`) | server:app |
| `MINIO_PUBLIC_URL` | Public-facing base URL for serving stored objects | server:app |
