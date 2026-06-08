# Implementation Plan: Authentication & User Identity

**Branch**: `feature/004-auth` | **Date**: 2026-06-08 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/004-auth/spec.md`

---

## Summary

Build the complete authentication and identity system for KodEx: email+password registration with strength validation and CAPTCHA, OAuth via GitHub and Google (server-side code flow), WebAuthn/FIDO2 passkeys, TOTP 2FA with recovery codes, JWT access tokens (15 min) + rotating refresh tokens (7 days, SHA-256 hashed in PostgreSQL), email verification (magic link / OTP), forgot/change password, profile editing, active sessions management, new-device email alerts, account linking, and RBAC middleware. Frontend: Kilua MVI pages for Sign In, Sign Up, Forgot/Reset Password, Email Verify, Profile Settings, Security Settings.

---

## Technical Context

**Language/Version**: Kotlin 2.3.21 (backend JVM + frontend Kotlin/JS)

**Primary Dependencies**:
- Backend: Ktor 3.5.0, Exposed 1.3.0, Flyway 12.6.1, Koin 4.2.1 (Annotations), argon2-jvm 2.12
- Auth-specific additions: webauthn4j-core 0.31.6.RELEASE, kotlin-onetimepassword 3.0.0, zxcvbn4j 1.9.0, geoip2 5.1.0, resend-java 4.14.1, uap-java 1.6.1, lettuce-core 6.3.2.RELEASE (Redis), minio 8.5.11 (avatar storage)
- Frontend: Kilua (latest via Gradle plugin), Ktor Client JS, zxcvbn-ts 3.0.4 (npm)

**Storage**: PostgreSQL (primary DB — all token, session, user data). Redis (per-IP failed login counter for Turnstile/lockout trigger).

**Testing**: Kotest (JVM), kotlin.test (KMP/JS), Testcontainers (PostgreSQL for integration tests)

**Target Platform**: Linux server (Ktor Netty JVM), Browser (Kotlin/JS via Vite)

**Project Type**: Web service (API) + Single-Page Application (SPA)

**Performance Goals**: Auth endpoints ≤ 500 ms p99. Token refresh ≤ 100 ms p95 (single indexed PG query).

**Constraints**: HttpOnly cookie for refresh token. Redis used only for ephemeral rate-limit counters (not for session/token storage). Access token in JS memory only (not localStorage). OWASP Argon2id parameters: 2 iterations / 64 MiB memory / 4 threads.

**Scale/Scope**: v1 single-host Docker Compose deployment. All 11 user stories from spec.

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| § | Principle | Status | Notes |
|---|-----------|--------|-------|
| I | Kotlin-First | ✅ PASS | All code Kotlin JVM + Kotlin/JS. Java interop only for argon2-jvm, webauthn4j-core, geoip2, resend-java (no Kotlin-native alternatives). |
| II | Dual Exam Modes | ✅ N/A | Not affected by auth feature. |
| III | Secure Sandbox | ✅ N/A | Not affected. |
| IV | Test-Injection Grading | ✅ N/A | Not affected. |
| V | Role-Based Domain Model | ✅ PASS | PARTICIPANT default, EXAM_CREATOR admin-granted, ADMIN. Role stored in JWT claims. RBAC enforced at `server:api` layer. |
| VI | Auditability | ✅ PASS | All auth events (login success/failure, password change, 2FA toggle, passkey register/remove, lockout) logged with requestId, userId, timestamp. Failed attempts log without credential. |
| VII | Architecture | ✅ PASS | Clean Arch backend (`:api` → `:domain` ← `:data`). MVI frontend. Koin Annotations DI. Detekt on all new files. |
| VIII | Auth & AuthZ | ⚠️ AMENDMENT REQUIRED | JWT 15 min + Argon2id + role enforcement: ✅. Refresh token storage: **VIOLATION** — constitution says Redis; spec decision is PostgreSQL (SHA-256 hash). See amendment proposal below. |
| IX | API Design | ✅ PASS | All endpoints under `/api/v1/`. Unified `ApiEnvelope<T>` on all responses. `ApiEnvelope` types in `core:models`. `sanitizeRequestId()` on all new routes. |
| X | Testing Policy | ✅ PASS | Kotest JVM integration tests with Testcontainers (PG). No cross-layer mocks. kotlin.test for shared models. |

### §VIII Constitution Amendment — Refresh Token Storage

**Amendment**: §VIII + security_constitution.md §2 + tech-stack.md

**Current text**: "Refresh token: longer-lived (7 days), stored server-side in **Redis** (revocable)."
**Proposed text**: "Refresh token: longer-lived (7 days), stored server-side in **PostgreSQL** as a SHA-256 hash (revocable)."

**Version bump**: 1.5.0 → 1.6.0 (MINOR)

**Justification**:
1. Behavior is identical — server-side, single-table lookup, revocable via indexed UPDATE
2. PG refresh token lookup (~0.5 ms) occurs at most once per 15-minute access token lifetime — negligible overhead
3. Eliminates Redis as required infrastructure for auth (Docker Compose simplification)
4. Redis may be introduced later for rate limiting (SEC-004) or leaderboard (spec 010) on its own merit
5. WebAuthn challenges: stateless signed HttpOnly cookie (no store needed)

**Files to update as part of this spec**: `.specify/memory/principles/viii-authentication.md`, `.specify/memory/security_constitution.md`, `.specify/memory/constitution.md`, `docs/memory/DECISIONS.md` (add D16).

---

## Project Structure

### Documentation (this feature)

```
specs/004-auth/
├── plan.md              ← this file
├── research.md          ← library decisions, env vars
├── data-model.md        ← Exposed table definitions, SQL schema
├── quickstart.md        ← local setup, curl tests
├── contracts/
│   └── api-auth.md      ← full API endpoint contracts
└── tasks.md             ← generated by /speckit-tasks
```

### Source Code — Backend (`server/`)

```
server/
├── api/src/main/kotlin/dev/kodex/server/api/
│   ├── auth/
│   │   ├── AuthRoutes.kt           ← register, login, logout, refresh, verify-email, forgot/reset-password
│   │   ├── OAuthRoutes.kt          ← /oauth/{provider} + /oauth/{provider}/callback
│   │   ├── PasskeyAuthRoutes.kt    ← unauthenticated passkey auth begin/complete
│   │   ├── TotpLoginRoutes.kt      ← /login/totp (2FA completion step)
│   │   └── middleware/
│   │       ├── RoleGuard.kt        ← requireRole(Role) extension on Route
│   │       └── TurnstileVerifier.kt ← HTTP call to Cloudflare verify endpoint
│   └── users/
│       ├── UserRoutes.kt           ← /users/me (GET, PATCH profile, PATCH password, PATCH email)
│       ├── SessionRoutes.kt        ← /users/me/sessions (GET, DELETE single, DELETE all)
│       ├── OAuthLinkRoutes.kt      ← /users/me/oauth (GET, DELETE provider)
│       ├── TotpRoutes.kt           ← /users/me/2fa (setup, confirm, DELETE)
│       └── PasskeyManageRoutes.kt  ← /users/me/passkeys (register begin/complete, GET, DELETE)
│
├── domain/src/main/kotlin/dev/kodex/server/domain/
│   ├── auth/
│   │   ├── usecase/
│   │   │   ├── RegisterUseCase.kt
│   │   │   ├── LoginUseCase.kt
│   │   │   ├── LogoutUseCase.kt
│   │   │   ├── RefreshTokenUseCase.kt
│   │   │   ├── VerifyEmailUseCase.kt
│   │   │   ├── ResendVerificationUseCase.kt
│   │   │   ├── ForgotPasswordUseCase.kt
│   │   │   ├── ResetPasswordUseCase.kt
│   │   │   ├── OAuthLoginUseCase.kt
│   │   │   └── EmergencyRevokeAllSessionsUseCase.kt
│   │   └── repository/
│   │       ├── UserRepository.kt
│   │       ├── TokenRepository.kt
│   │       ├── OAuthIdentityRepository.kt
│   │       └── EmergencyRevokeTokenRepository.kt
│   ├── users/
│   │   ├── usecase/
│   │   │   ├── ChangePasswordUseCase.kt
│   │   │   ├── ChangeEmailUseCase.kt
│   │   │   ├── ChangeUsernameUseCase.kt
│   │   │   ├── UpdateProfileUseCase.kt
│   │   │   ├── UploadAvatarUseCase.kt
│   │   │   ├── GetSessionsUseCase.kt
│   │   │   └── RevokeSessionUseCase.kt
│   │   └── repository/
│   │       ├── ProfileRepository.kt
│   │       └── SessionRepository.kt
│   ├── passkey/
│   │   ├── usecase/
│   │   │   ├── RegisterPasskeyUseCase.kt
│   │   │   ├── AuthenticatePasskeyUseCase.kt
│   │   │   └── RemovePasskeyUseCase.kt
│   │   └── repository/
│   │       └── PasskeyRepository.kt
│   └── totp/
│       ├── usecase/
│       │   ├── SetupTotpUseCase.kt
│       │   ├── ConfirmTotpUseCase.kt
│       │   ├── VerifyTotpCodeUseCase.kt
│       │   └── DisableTotpUseCase.kt
│       └── repository/
│           └── TotpRepository.kt
│
├── data/src/main/kotlin/dev/kodex/server/data/
│   ├── db/
│   │   └── tables/
│   │       ├── UsersTable.kt
│   │       ├── UserProfilesTable.kt
│   │       ├── OAuthIdentitiesTable.kt
│   │       ├── RefreshTokensTable.kt
│   │       ├── EmailVerificationTokensTable.kt
│   │       ├── PendingEmailChangesTable.kt
│   │       ├── PasswordResetTokensTable.kt
│   │       ├── EmergencyRevokeTokensTable.kt
│   │       ├── WebAuthnCredentialsTable.kt
│   │       ├── TotpConfigsTable.kt
│   │       └── KnownLoginIpsTable.kt
│   ├── migrations/
│   │   └── V3__auth_schema.sql
│   ├── repository/
│   │   ├── UserRepositoryImpl.kt
│   │   ├── TokenRepositoryImpl.kt
│   │   ├── OAuthIdentityRepositoryImpl.kt
│   │   ├── EmergencyRevokeTokenRepositoryImpl.kt
│   │   ├── ProfileRepositoryImpl.kt
│   │   ├── SessionRepositoryImpl.kt
│   │   ├── PasskeyRepositoryImpl.kt
│   │   └── TotpRepositoryImpl.kt
│   ├── email/
│   │   ├── EmailService.kt             ← interface: send(to, subject, html)
│   │   └── ResendEmailService.kt       ← Resend SDK impl
│   ├── geoip/
│   │   └── GeoIpService.kt             ← MaxMind GeoLite2 lookup
│   ├── ratelimit/
│   │   └── RateLimitService.kt         ← Redis INCR/EXPIRE for per-IP attempt counter
│   ├── storage/
│   │   └── AvatarStorageService.kt     ← MinIO putObject; validates MIME + magic bytes; returns public URL
│   └── crypto/
│       ├── PasswordHasher.kt           ← Argon2id wrapper
│       ├── TokenHasher.kt              ← SHA-256 hex utility
│       └── TotpCrypto.kt               ← AES-256-GCM encrypt/decrypt for TOTP secrets
```

### Source Code — Shared Models (`core/models`)

```
core/models/src/commonMain/kotlin/dev/kodex/core/models/
└── auth/
    ├── Role.kt                  ← enum: PARTICIPANT, EXAM_CREATOR, ADMIN
    ├── OAuthProvider.kt         ← enum: GITHUB, GOOGLE
    ├── AuthTokensResponse.kt    ← accessToken: String, expiresIn: Int
    ├── TotpChallengeResponse.kt ← requiresTotp: Boolean, totpSessionToken: String
    ├── RegisterRequest.kt
    ├── LoginRequest.kt
    ├── TotpLoginRequest.kt
    ├── UserDto.kt
    ├── UserProfileDto.kt
    ├── UpdateProfileRequest.kt
    ├── ChangeUsernameRequest.kt
    ├── SessionDto.kt
    ├── OAuthProviderDto.kt
    ├── PasskeyDto.kt
    └── UsernameAvailabilityResponse.kt
```

### Source Code — Frontend (`app/webApp`)

```
app/webApp/src/webMain/kotlin/dev/kodex/webapp/
├── auth/
│   ├── AuthState.kt            ← sealed: LoggedOut | LoggedIn(user, accessToken, expiresAt)
│   ├── AuthStore.kt            ← global state; token refresh scheduler (coroutine timer)
│   └── TokenInterceptor.kt     ← Ktor Client plugin: injects Bearer token, auto-refreshes on 401
├── pages/
│   ├── auth/
│   │   ├── SignInPage.kt
│   │   ├── SignUpPage.kt
│   │   ├── ForgotPasswordPage.kt
│   │   ├── ResetPasswordPage.kt
│   │   ├── VerifyEmailPage.kt
│   │   ├── OAuthSuccessPage.kt ← handles /auth/oauth-success?access_token=...
│   │   ├── AuthViewModel.kt
│   │   └── AuthUiState.kt
│   └── settings/
│       ├── ProfileSettingsPage.kt
│       ├── SecuritySettingsPage.kt
│       ├── ProfileViewModel.kt
│       ├── SecurityViewModel.kt
│       └── SecurityUiState.kt
└── network/
    └── auth/
        ├── AuthRemoteDataSource.kt
        ├── AuthRemoteDataSourceImpl.kt
        ├── UserRemoteDataSource.kt
        └── UserRemoteDataSourceImpl.kt
```

---

## Complexity Tracking

| Deviation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|--------------------------------------|
| PostgreSQL for refresh tokens (§VIII amendment) | Eliminates Redis infra dependency for auth; token lookup is infrequent (once/15 min per user) | Redis adds infrastructure complexity and mandatory ops work before any auth is usable |
| WebAuthn challenge in signed cookie | Avoids server-side ephemeral store for 30 s challenge data | PG row with 30 s TTL requires cleanup job; Redis not in this spec |
| TOTP secret encrypted at rest | TOTP secret is long-lived; if DB is compromised without key, secrets are useless | Plaintext storage violates security constitution §2 |
| `totpSessionToken` (temp token after password step) | Two-step 2FA flow needs to carry password-verified proof to TOTP step | Storing state in session/Redis not viable; JWT-signed temp token is stateless and short-lived |
| OAuth username auto-generation | First-time OAuth users have no username; onboarding step adds a screen and ViewModel | Auto-generate from email prefix (strip non-`[a-z0-9_-]`, truncate, suffix if taken); user can change later via PATCH |
| `EmergencyRevokeTokensTable` for new-device alert | Revoke-all link in alert email must work without login (user may have lost account access if credentials were stolen) | Linking to `/account/security` requires login; a single-use token satisfies FR-034 with no UX friction |

---

## Implementation Phases

> Tasks are generated by `/speckit-tasks`. This section shows the logical grouping.

### Phase A — Database & Config Foundation
1. Constitution amendment: update `viii-authentication.md`, `security_constitution.md`, `constitution.md`
2. Add new env vars to `.env.example` and `EnvConfig`
3. Add new catalog entries (`libs.versions.toml`)
4. Flyway migration `V3__auth_schema.sql` — all 11 tables (includes `emergency_revoke_tokens`)
5. Exposed table definitions (all 11 tables, includes `EmergencyRevokeTokensTable`)
6. Crypto utilities: `PasswordHasher`, `TokenHasher`, `TotpCrypto`
7. `EmailService` interface + `ResendEmailService` impl
8. `GeoIpService` impl
8a. `RateLimitService` impl — Lettuce `RedisClient` Koin `@Single`; `incrementAndGet(key, ttlSeconds)` + `reset(key)` coroutine wrappers
8b. `AvatarStorageService` impl — MinIO `MinioClient` Koin `@Single`; `upload(userId, bytes, mimeType): String`; validates magic bytes; returns public URL

### Phase B — Domain Layer
9. Repository interfaces: `UserRepository`, `TokenRepository`, `OAuthIdentityRepository`, `ProfileRepository`, `SessionRepository`, `PasskeyRepository`, `TotpRepository`
10. Repository impls in `server:data`
11. Use cases: Register, Login, Logout, Refresh, VerifyEmail, ResendVerification, ForgotPassword, ResetPassword
11a. Use case: `EmergencyRevokeAllSessionsUseCase` — validates token, revokes all refresh tokens, marks token used
12. Use cases: OAuth login + identity linking (includes auto-username generation from email prefix)
13. Use cases: ChangePassword, ChangeEmail, ChangeUsername, UpdateProfile, UploadAvatar, GetSessions, RevokeSession
14. Use cases: RegisterPasskey, AuthenticatePasskey, RemovePasskey
15. Use cases: SetupTotp, ConfirmTotp, VerifyTotpCode, DisableTotp

### Phase C — API Layer
16. `RoleGuard.kt` middleware — `requireRole(Role)` route extension
17. `TurnstileVerifier.kt` — HTTP call to Cloudflare
18. `AuthRoutes.kt` — register, login, logout, refresh, verify-email, resend, forgot/reset-password, username-check, emergency-revoke
19. `OAuthRoutes.kt` — GitHub + Google OAuth2 code flow (Ktor `ktor-server-auth` OAuth plugin); includes auto-username generation
20. `TotpLoginRoutes.kt` — `/login/totp`
21. `PasskeyAuthRoutes.kt` — unauthenticated passkey begin/complete
22. `UserRoutes.kt` (includes `PATCH /me/username`, `POST /me/avatar`), `SessionRoutes.kt`, `OAuthLinkRoutes.kt`, `TotpRoutes.kt`, `PasskeyManageRoutes.kt`
23. New-device login alert logic — called from `LoginUseCase` after successful auth
24. `sanitizeRequestId()` applied to all new routes

### Phase D — Shared Models (`core:models`)
25. All auth DTOs and request/response types in `core/models/src/commonMain/kotlin/dev/kodex/core/models/auth/`

### Phase E — Frontend
26. `AuthStore.kt` + `TokenInterceptor.kt` — global auth state + auto-refresh
27. `ApiRoutes.kt` updates — add all auth and user endpoint constants
28. `AuthRemoteDataSource` + `UserRemoteDataSource` — all API calls
29. `SignInPage`, `SignUpPage` — with password strength meter (zxcvbn-ts), Turnstile widget
30. `ForgotPasswordPage`, `ResetPasswordPage`, `VerifyEmailPage`, `OAuthSuccessPage`
31. `ProfileSettingsPage` — avatar upload, all profile fields, social links
32. `SecuritySettingsPage` — sessions list, 2FA setup/disable, passkey list, OAuth provider linking
33. Navbar update — authenticated state: avatar + username dropdown

### Phase F — Tests
34. Kotest integration tests for all use cases (Testcontainers PG)
35. Unit tests: `PasswordHasher`, `TokenHasher`, `TotpCrypto`, `TurnstileVerifier` (mock HTTP)
36. Kotest API-level tests: register, login, OAuth callback (mock provider), passkey flow (mock webauthn4j)
37. Frontend kotlin.test: `AuthStore`, `TokenInterceptor`
38. Screenshot tests: `SignInPage`, `SignUpPage`, `SecuritySettingsPage`, `ProfileSettingsPage`

### Phase G — Config Updates
39. `docker-compose.yml` — add `GEOIP_DB_PATH` volume mount; add Redis service (`redis:7-alpine`, port 6379); add MinIO service (`minio/minio:latest`, ports 9000/9001, create `avatars` bucket on startup)
40. `.github/workflows/ci.yml` — add `CLOUDFLARE_TURNSTILE_SECRET` + other auth secrets to CI environment
41. `Application.kt` — install `Authentication` plugin with JWT + OAuth2 config; add new `DefaultHeaders` (no change needed — already in place)
