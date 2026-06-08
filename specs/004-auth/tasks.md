# Tasks: Authentication & User Identity (Spec 004)

**Branch**: `feature/004-auth` | **Spec**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)

**Input**: `plan.md`, `spec.md`, `data-model.md`, `contracts/api-auth.md`, `research.md`, `quickstart.md`

**Format**: `[ID] [P?] [Story?] Description — file path`
- **[P]**: parallelizable (different files, no shared dependencies)
- **[USN]**: user story label (maps to spec.md user stories)
- Tests: collected in final phase (not TDD — spec does not request test-first)

---

## Phase 1: Setup (Catalog, Env Vars, Gradle)

**Purpose**: Wire new dependencies before any domain code is written.

- [ ] T001 Apply §VIII constitution amendment — update `.specify/memory/principles/viii-authentication.md` (Redis → PG for refresh tokens, MINOR bump 1.5.0→1.6.0)
- [ ] T002 [P] Update `.specify/memory/security_constitution.md` §2 (refresh token storage change) and `docs/memory/DECISIONS.md` (add D16)
- [ ] T003 Add all new `[versions]` + `[libraries]` catalog entries to `gradle/libs.versions.toml` — webauthn4j, kotlin-onetimepassword, zxcvbn4j, geoip2, resend, ua-parser, lettuce, minio, (kotlinx-coroutines-reactive already added)
- [ ] T004 [P] Add all new auth env vars to `.env.example` — GITHUB_CLIENT_ID/SECRET, GOOGLE_CLIENT_ID/SECRET, OAUTH_CALLBACK_BASE_URL, CLOUDFLARE_TURNSTILE_SITE_KEY/SECRET, RESEND_API_KEY, EMAIL_FROM, APP_BASE_URL, TOTP_ENCRYPTION_KEY, WEBAUTHN_CHALLENGE_KEY, GEOIP_DB_PATH, REDIS_URL, REDIS_PASSWORD, MINIO_ENDPOINT/ACCESS_KEY/SECRET_KEY/BUCKET_AVATARS/PUBLIC_URL
- [ ] T005 [P] Add new library dependencies to `server/data/build.gradle.kts` — webauthn4j-core, kotlin-onetimepassword, zxcvbn4j-lib, geoip2-lib, resend-java, ua-parser-java, lettuce-core, kotlinx-coroutines-reactive, minio-sdk, argon2-jvm (verify already present)
- [ ] T006 [P] Add new library dependencies to `server/api/build.gradle.kts` — ktor-server-rate-limit, ktor-server-forwarded-header (verify both already in catalog)
- [ ] T007 [P] Add npm dependency to `app/webApp/build.gradle.kts` inside `jsMain` + `wasmJsMain` blocks: `implementation(npm("zxcvbn-ts", "3.0.4"))`
- [ ] T008 [P] Add all new env vars to `EnvConfig` object in `server/app/src/main/kotlin/dev/kodex/server/app/config/EnvConfig.kt` (add `env("WEBAUTHN_CHALLENGE_KEY")`, `env("REDIS_URL")`, `env("REDIS_PASSWORD")`, `env("MINIO_ENDPOINT")`, etc.)

**Checkpoint**: `./gradlew dependencies` resolves without error on all modules.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Database schema, crypto, services, repositories, JWT, and base API infrastructure. MUST be complete before any user story.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

### Database

- [ ] T009 Create Flyway migration `server/data/src/main/resources/db/migration/V3__auth_schema.sql` — all 11 tables: `users`, `user_profiles`, `oauth_identities`, `refresh_tokens`, `email_verification_tokens`, `pending_email_changes`, `password_reset_tokens`, `emergency_revoke_tokens`, `webauthn_credentials`, `totp_configs`, `known_login_ips` — with all indexes per `data-model.md`
- [ ] T010 [P] Create `server/data/src/main/kotlin/dev/kodex/server/data/db/tables/UsersTable.kt` + `UserProfilesTable.kt` per data-model.md Exposed definitions
- [ ] T011 [P] Create `server/data/.../db/tables/OAuthIdentitiesTable.kt` + `RefreshTokensTable.kt`
- [ ] T012 [P] Create `server/data/.../db/tables/EmailVerificationTokensTable.kt` + `PendingEmailChangesTable.kt`
- [ ] T013 [P] Create `server/data/.../db/tables/PasswordResetTokensTable.kt` + `EmergencyRevokeTokensTable.kt`
- [ ] T014 [P] Create `server/data/.../db/tables/WebAuthnCredentialsTable.kt` + `TotpConfigsTable.kt` + `KnownLoginIpsTable.kt`

### Crypto & Infrastructure Services

- [ ] T015 Create `server/data/src/main/kotlin/dev/kodex/server/data/crypto/PasswordHasher.kt` — Argon2id (iter=2, mem=65536, par=4) wrap `de.mkammerer:argon2-jvm`; expose `hash(plain: String): String` and `verify(hash: String, plain: String): Boolean`
- [ ] T016 [P] Create `server/data/.../crypto/TokenHasher.kt` — `SecureRandom.nextBytes(32).toHexString()` for raw token + SHA-256 hex for stored hash; all 6 token tables use this
- [ ] T017 [P] Create `server/data/.../crypto/TotpCrypto.kt` — AES-256-GCM encrypt/decrypt using `TOTP_ENCRYPTION_KEY`; prepend random 12-byte IV to ciphertext; base64-encode for storage
- [ ] T018 [P] Create `server/data/.../crypto/WebAuthnChallengeCrypto.kt` — AES-256-GCM sign/verify WebAuthn challenge bytes using `WEBAUTHN_CHALLENGE_KEY`; output as base64 for cookie value
- [ ] T019 Create `server/data/.../email/EmailService.kt` (interface) + `ResendEmailService.kt` (impl via `com.resend:resend-java`) in `server/data/src/main/kotlin/dev/kodex/server/data/email/`
- [ ] T020 Create `server/data/.../geoip/GeoIpService.kt` — `DatabaseReader` from `geoip2-lib`; `lookup(ip: String): GeoResult?` returns `(countryCode, city)`; load from `GEOIP_DB_PATH`
- [ ] T021 Create `server/data/.../ratelimit/RateLimitService.kt` — Lettuce `RedisClient` Koin `@Single`; suspending `incrementAndGet(key: String, ttlSeconds: Long): Long` using `kotlinx.coroutines.reactive.awaitSingle()`; suspending `reset(key: String)`; Redis key pattern: `login:attempts:<sha256(ip)>` (TTL=600s)
- [ ] T022 Create `server/data/.../storage/AvatarStorageService.kt` — `MinioClient` Koin `@Single`; `upload(userId: Long, bytes: ByteArray, mimeType: String): String`; validate magic bytes (JPEG `FF D8 FF`, PNG `89 50 4E 47`, WebP `52 49 46 46`); store as `<userId>.<server-derived-ext>`; return public URL; reject if size > 5 MB

### Domain — Repository Interfaces

- [ ] T023 [P] Create repository interfaces in `server/domain/src/main/kotlin/dev/kodex/server/domain/auth/repository/` — `UserRepository.kt`, `TokenRepository.kt`, `OAuthIdentityRepository.kt`, `EmergencyRevokeTokenRepository.kt`
- [ ] T024 [P] Create `server/domain/.../users/repository/ProfileRepository.kt` + `SessionRepository.kt`
- [ ] T025 [P] Create `server/domain/.../passkey/repository/PasskeyRepository.kt`
- [ ] T026 [P] Create `server/domain/.../totp/repository/TotpRepository.kt`

### Domain — Repository Implementations

- [ ] T027 Implement `server/data/.../repository/UserRepositoryImpl.kt` — CRUD for `users` + auto-UUID username generation (email prefix, strip non-`[a-z0-9_-]`, truncate 30, numeric suffix if taken); `@Single`
- [ ] T028 [P] Implement `server/data/.../repository/TokenRepositoryImpl.kt` — refresh token CRUD; active query (`revoked_at IS NULL AND expires_at > now()`); rotate (insert new, revoke old in transaction)
- [ ] T029 [P] Implement `server/data/.../repository/OAuthIdentityRepositoryImpl.kt` + `EmergencyRevokeTokenRepositoryImpl.kt`
- [ ] T030 [P] Implement `server/data/.../repository/ProfileRepositoryImpl.kt` + `SessionRepositoryImpl.kt`
- [ ] T031 [P] Implement `server/data/.../repository/PasskeyRepositoryImpl.kt`
- [ ] T032 [P] Implement `server/data/.../repository/TotpRepositoryImpl.kt`

### Shared Models (core:models)

- [ ] T033 [P] Create `core/models/src/commonMain/kotlin/dev/kodex/core/models/auth/Role.kt` + `OAuthProvider.kt` enums
- [ ] T034 [P] Create `core/models/.../auth/AuthTokensResponse.kt` + `TotpChallengeResponse.kt` + `UsernameAvailabilityResponse.kt`
- [ ] T035 [P] Create `core/models/.../auth/RegisterRequest.kt` + `LoginRequest.kt` + `TotpLoginRequest.kt` + `ChangeUsernameRequest.kt`
- [ ] T036 [P] Create `core/models/.../auth/UserDto.kt` + `UserProfileDto.kt` + `UpdateProfileRequest.kt` + `SessionDto.kt` + `OAuthProviderDto.kt` + `PasskeyDto.kt`

### API Base Infrastructure

- [ ] T037 Install `Authentication { jwt("auth-jwt") { ... } }` plugin in `server/app/src/main/kotlin/dev/kodex/server/app/Application.kt` — configure HS256, `JWT_SECRET`, realm; validate `sub`, `role`, `exp` claims; reject tokens where `role` claim is absent
- [ ] T038 [P] Install `ktor-server-forwarded-header` plugin in `Application.kt` — enables `call.request.origin.remoteHost` to return the correct client IP after normalisation
- [ ] T039 Create `server/api/.../auth/middleware/RoleGuard.kt` — `fun Route.requireRole(vararg roles: Role)` extension; responds 403 if JWT role not in set
- [ ] T040 [P] Create `server/api/.../auth/middleware/TurnstileVerifier.kt` — suspending `verify(token: String, ip: String): Boolean`; POST to `https://challenges.cloudflare.com/turnstile/v0/siteverify` via Ktor `HttpClient`; reads `CLOUDFLARE_TURNSTILE_SECRET`
- [ ] T041 Install `ktor-server-rate-limit` plugin for all `POST /api/v1/auth/*` routes in route registration — limit: 20 requests / 10 s per IP (defense-in-depth beside Redis counter)
- [ ] T042 Add `Cache-Control: no-store` response header to all `/api/v1/auth/*` route handlers via a Ktor `createApplicationPlugin` call-plugin or per-route helper — per `security_constitution.md §6`
- [ ] T043 Register all new Koin `@Single` / `@Factory` modules in the Koin `startKoin` block: all new services (Email, GeoIp, RateLimit, AvatarStorage) and all repository impls
- [ ] T044 Update `docker-compose.yml` — add `redis:7-alpine` service (port 6379, no password local dev); add `minio/minio:latest` service (ports 9000/9001, env MINIO_ROOT_USER/PASSWORD); add `GEOIP_DB_PATH` volume mount (`./infra/geoip:/data`); add all new env vars to `server` service
- [ ] T045 Update `app/webApp/src/webMain/kotlin/dev/kodex/webapp/network/auth/AuthRemoteDataSource.kt` — scaffold interface + `AuthRemoteDataSourceImpl.kt`; add `object ApiRoutes` constants for all new endpoints in the appropriate constants file

**Checkpoint**: `./gradlew :server:data:flywayMigrate` succeeds; `./gradlew :server:app:run` starts without errors; all 11 tables visible in PG.

---

## Phase 3: User Story 1 — Email Registration & Verification (Priority: P1) 🎯 MVP Start

**Goal**: New users can register with username + email + password, receive a verification email, and activate their account.

**Independent Test**: `curl` POST `/auth/register` with valid payload → 201; check inbox for magic link; `GET /api/v1/users/me` with returned access token → 200 with user data.

- [ ] T046 [US1] Implement `server/domain/.../auth/usecase/RegisterUseCase.kt` — validate username (`[a-z0-9_-]`, 3–30, unique), validate email unique, validate password strength via `com.nulab-inc:zxcvbn` score ≥ 2, call `TurnstileVerifier`, hash password via `PasswordHasher`, insert `users` row + `user_profiles` row (empty), dispatch verification email (magic link for web / OTP for mobile via `X-Client-Platform` header), return `AuthTokensResponse`
- [ ] T047 [US1] Implement `server/domain/.../auth/usecase/VerifyEmailUseCase.kt` — lookup `email_verification_tokens` by `SHA-256(token)`, check not used + not expired, set `users.email_verified=true`, mark token used; issue access + refresh tokens on success
- [ ] T048 [US1] Implement `server/domain/.../auth/usecase/ResendVerificationUseCase.kt` — look up unverified account by email, invalidate old tokens, generate + dispatch new token; generic response (no enumeration)
- [ ] T049 [US1] Add `POST /api/v1/auth/register` in `server/api/.../auth/AuthRoutes.kt` — call `TurnstileVerifier` + `RegisterUseCase`; set `refresh_token` HttpOnly SameSite=Lax cookie; return `AuthTokensResponse`; mobile path: return refresh token in body instead of cookie
- [ ] T050 [P] [US1] Add `GET /api/v1/auth/username/check?username=` in `AuthRoutes.kt` — call `UserRepository.isUsernameTaken`; return `UsernameAvailabilityResponse`
- [ ] T051 [US1] Add `POST /api/v1/auth/verify-email` + `POST /api/v1/auth/verify-email/resend` in `AuthRoutes.kt` — call respective use cases; set refresh cookie on verify success
- [ ] T052 [P] [US1] Create email HTML templates: `VerificationMagicLinkEmail` + `VerificationOtpEmail` in `server/data/.../email/templates/` — inline HTML strings, no template engine dependency
- [ ] T053 [US1] Create `app/webApp/src/webMain/kotlin/dev/kodex/webapp/pages/auth/SignUpPage.kt` + `AuthViewModel.kt` + `AuthUiState.kt` — form with username (real-time availability check via debounced `GET /username/check`), email, password (zxcvbn-ts strength bar, score ≥ 2 gate), Turnstile widget; submit → `AuthRemoteDataSource.register()`
- [ ] T054 [US1] Create `app/webApp/.../pages/auth/VerifyEmailPage.kt` — display "check your inbox" state; handle magic-link callback (`?token=`); show "resend" button; update `AuthStore` on verification success
- [ ] T055 [US1] Add `register`, `verifyEmail`, `resendVerification`, `usernameCheck` methods to `AuthRemoteDataSourceImpl.kt`

**Checkpoint**: Full registration + verification flow works end-to-end via `curl` smoke tests in `quickstart.md`.

---

## Phase 4: User Story 2 — Email/Password Login (Priority: P1)

**Goal**: Verified users can log in, receive JWT access + refresh tokens, access protected routes, and silently refresh on token expiry.

**Independent Test**: Login with registered account → receive `accessToken`; `GET /users/me` succeeds; wait 15 min or mock expiry → `POST /auth/refresh` silently issues new pair.

- [ ] T056 [US2] Implement `server/domain/.../auth/usecase/LoginUseCase.kt` — check account lockout (`users.locked_until`), check per-IP Redis counter (if ≥ 3 require Turnstile), `PasswordHasher.verify()`, increment Redis counter on failure + reset on success, increment `users.failed_login_count` (lock at 10 + dispatch lockout email), issue access JWT + refresh token (set cookie or body per platform)
- [ ] T057 [US2] Implement `server/domain/.../auth/usecase/RefreshTokenUseCase.kt` — look up token by SHA-256 hash, check not revoked + not expired, rotate (insert new + revoke old in DB transaction), return new `AuthTokensResponse`
- [ ] T058 [US2] Add `POST /api/v1/auth/login` in `AuthRoutes.kt` — call `LoginUseCase`; on TOTP-enabled result return `TotpChallengeResponse` with `totpSessionToken` JWT (`{ type:"totp-session", sub:"<userId>", exp:iat+300 }`); otherwise set cookie + return tokens
- [ ] T059 [US2] Add `POST /api/v1/auth/refresh` in `AuthRoutes.kt` — read `refresh_token` cookie, call `RefreshTokenUseCase`, set new cookie + return new `accessToken`
- [ ] T060 [US2] Add `GET /api/v1/users/me` in `server/api/.../users/UserRoutes.kt` — protected by `requireRole(PARTICIPANT, EXAM_CREATOR, ADMIN)`; return `UserDto` with full profile
- [ ] T061 [US2] Create `app/webApp/.../auth/AuthState.kt` — sealed: `LoggedOut | LoggingIn | LoggedIn(user: UserDto, accessToken: String, expiresAt: Instant) | RequiresTotp(totpSessionToken: String)`
- [ ] T062 [US2] Create `app/webApp/.../auth/AuthStore.kt` — MVI store; holds `AuthState`; schedules coroutine timer to refresh token 60 s before `expiresAt`; exposes `login()`, `setLoggedIn()`, `logout()`, `refreshNow()`
- [ ] T063 [US2] Create `app/webApp/.../auth/TokenInterceptor.kt` — Ktor Client plugin; injects `Authorization: Bearer <accessToken>` on every request; intercepts 401, calls `AuthStore.refreshNow()`, retries once
- [ ] T064 [US2] Create `app/webApp/.../pages/auth/SignInPage.kt` + extend `AuthViewModel.kt` — email + password form; autocomplete attributes; on `RequiresTotp` state → show inline TOTP prompt with `totpSessionToken`; wire Turnstile widget (show after 3 failures)
- [ ] T065 [P] [US2] Create lockout notification email template `AccountLockedEmail` in `server/data/.../email/templates/`
- [ ] T066 [US2] Add `login`, `refresh`, `getMe` methods to `AuthRemoteDataSourceImpl.kt`; wire `TokenInterceptor` into app Ktor Client; add `UserRemoteDataSource.kt` + `UserRemoteDataSourceImpl.kt`

**Checkpoint**: Login → `accessToken` → `GET /users/me` → 200 with user object. Logout and re-login confirmed working.

---

## Phase 5: User Story 7 — Logout (Priority: P1)

**Goal**: Users can end their session; the refresh token is revoked server-side and cannot be reused.

**Independent Test**: Login → capture cookie → logout → attempt refresh with same cookie → 401.

- [ ] T067 [US7] Implement `server/domain/.../auth/usecase/LogoutUseCase.kt` — look up refresh token by hash, set `revoked_at = now()`; if no valid token found, return success silently
- [ ] T068 [US7] Add `POST /api/v1/auth/logout` in `AuthRoutes.kt` — call `LogoutUseCase`; clear `refresh_token` cookie (`Max-Age=0`); return `data: null`
- [ ] T069 [US7] Wire logout to navbar profile dropdown in `app/webApp` — call `AuthRemoteDataSource.logout()` → dispatch to `AuthStore` → navigate to `SignInPage`; add `logout` method to `AuthRemoteDataSourceImpl.kt`

**Checkpoint**: Full login → logout → refresh attempted → 401 cycle verified.

---

## Phase 6: User Story 3 — OAuth Login/Register: GitHub & Google (Priority: P1)

**Goal**: Users can sign in or register with their GitHub or Google account; profile is pre-populated; new accounts get an auto-generated username.

**Independent Test**: Click "Continue with GitHub" → complete OAuth → land on dashboard authenticated; `GET /users/me` returns user with GitHub avatar URL.

- [ ] T070 [US3] Implement `server/domain/.../auth/usecase/OAuthLoginUseCase.kt` — look up `oauth_identities` by (provider, providerUserId); if not found: check if email matches existing user (link) else create new user (auto-generate username from email prefix per FR-040, create empty profile, insert oauth identity); fetch GitHub: `GET /api.github.com/user` + `/user/emails`; fetch Google: decode ID token; populate `user_profiles` on first OAuth login; issue tokens
- [ ] T071 [US3] Configure `ktor-server-auth` OAuth2 plugin in `Application.kt` — GitHub provider (scopes `read:user user:email`, callback URL from `OAUTH_CALLBACK_BASE_URL`) + Google provider (scopes `openid email profile`)
- [ ] T072 [US3] Create `server/api/.../auth/OAuthRoutes.kt` — `GET /auth/oauth/{provider}` triggers redirect; `GET /auth/oauth/{provider}/callback` calls `OAuthLoginUseCase`, sets cookie, redirects to `APP_BASE_URL/auth/oauth-success?access_token=...`; on error redirect to `APP_BASE_URL/sign-in?error=oauth_failed`; MUST use plugin (not manual redirect) for CSRF state protection per SEC-007
- [ ] T073 [US3] Create `app/webApp/.../pages/auth/OAuthSuccessPage.kt` — handles `/auth/oauth-success?access_token=...&expires_in=...` route; extracts params (never touches query string on subsequent navigations); dispatches to `AuthStore.setLoggedIn()`; redirects to dashboard
- [ ] T074 [P] [US3] Add "Continue with GitHub" + "Continue with Google" buttons to `SignInPage.kt` and `SignUpPage.kt`

**Checkpoint**: GitHub OAuth end-to-end works; new account auto-username visible in `GET /users/me`.

---

## Phase 7: User Story 6 — Forgot Password & Change Password (Priority: P2)

**Goal**: Users can recover a forgotten password via email link; logged-in users can change their password from security settings.

**Independent Test**: Request reset for test email → receive link → set new password → old password rejected → login with new password succeeds.

- [ ] T075 [US6] Implement `server/domain/.../auth/usecase/ForgotPasswordUseCase.kt` — look up user by email (silent if not found), generate 32-byte `TokenHasher.generate()` token, hash + store in `password_reset_tokens` (1 h TTL), dispatch `PasswordResetEmail`; always return 200 (no enumeration)
- [ ] T076 [US6] Implement `server/domain/.../auth/usecase/ResetPasswordUseCase.kt` — look up by SHA-256 hash, check not used + not expired, validate new password strength (zxcvbn ≥ 2), `PasswordHasher.hash()`, update `users.password_hash`, mark token used, revoke all refresh tokens for account
- [ ] T077 [US6] Implement `server/domain/.../users/usecase/ChangePasswordUseCase.kt` — verify current password, validate new password strength, hash + update, revoke all other refresh tokens; handle OAuth-only account (no current password required for first password set)
- [ ] T078 [US6] Add `POST /api/v1/auth/forgot-password` + `POST /api/v1/auth/reset-password` in `AuthRoutes.kt` — apply `Cache-Control: no-store`; Turnstile on forgot-password
- [ ] T079 [US6] Add `PATCH /api/v1/users/me/password` in `UserRoutes.kt` — `requireRole(...)`, call `ChangePasswordUseCase`
- [ ] T080 [P] [US6] Create password reset email template `PasswordResetEmail` in `server/data/.../email/templates/`
- [ ] T081 [US6] Create `app/webApp/.../pages/auth/ForgotPasswordPage.kt` + `ResetPasswordPage.kt` — forgot: email input + Turnstile widget; reset: new password field with zxcvbn-ts strength meter
- [ ] T082 [US6] Add change-password section to `SecuritySettingsPage.kt` (scaffold if not yet created) — current password + new password fields; POST to `PATCH /users/me/password`

**Checkpoint**: Full forgot-password → email → reset → login cycle verified with curl.

---

## Phase 8: User Story 5 — TOTP Two-Factor Authentication (Priority: P2)

**Goal**: Users can enable TOTP 2FA from security settings; subsequent logins require the 6-digit code after password verification.

**Independent Test**: Enable 2FA with TOTP app → logout → login → TOTP prompt appears → submit correct code → authenticated; submit wrong code → 401.

- [ ] T083 [US5] Implement `server/domain/.../totp/usecase/SetupTotpUseCase.kt` — generate 20-byte base32 secret via `kotlin-onetimepassword`, encrypt via `TotpCrypto`, insert `totp_configs` row (`enabled=false`), generate QR code data URI, return `otpAuthUri` + `qrCodeDataUri` + plaintext `secret`
- [ ] T084 [US5] Implement `server/domain/.../totp/usecase/ConfirmTotpUseCase.kt` — verify submitted TOTP code (±1 period drift), set `enabled=true`, generate 8 recovery codes (`16 bytes SecureRandom`, formatted `XXXXXXXX-XXXXXXXX`, store SHA-256 hashes in `backup_codes_hashes` JSON array); return plaintext codes (shown once only)
- [ ] T085 [US5] Implement `server/domain/.../totp/usecase/VerifyTotpCodeUseCase.kt` — check 6-digit TOTP (via `kotlin-onetimepassword`) OR 16-char recovery code (SHA-256 match in array, remove used entry); return `userId` on success
- [ ] T086 [US5] Implement `server/domain/.../totp/usecase/DisableTotpUseCase.kt` — verify current password + TOTP code, delete `totp_configs` row
- [ ] T087 [US5] Create `server/api/.../auth/TotpLoginRoutes.kt` — `POST /api/v1/auth/login/totp`: validate `totpSessionToken` JWT claims MUST include `type == "totp-session"` (reject if absent or different type), extract `sub`, call `VerifyTotpCodeUseCase`, issue access + refresh tokens
- [ ] T088 [US5] Create `server/api/.../users/TotpRoutes.kt` — `POST /users/me/2fa/setup`, `POST /users/me/2fa/confirm`, `DELETE /users/me/2fa`; all require `requireRole(...)`
- [ ] T089 [US5] Handle `TotpChallengeResponse` in `SignInPage.kt` — when `AuthStore` enters `RequiresTotp` state, show inline TOTP input field; submit `totpSessionToken + code` to `POST /auth/login/totp`; on success dispatch `setLoggedIn()`
- [ ] T090 [US5] Add 2FA setup section to `SecuritySettingsPage.kt` — "Enable 2FA" button → QR code display + secret text → confirmation input → recovery codes show-once modal (acknowledge before dismissing)
- [ ] T091 [US5] Add "Disable 2FA" UI to `SecuritySettingsPage.kt` — password + current TOTP code confirmation
- [ ] T092 [US5] Add dismissible "Enable 2FA" nudge banner to dashboard/home page — persisted via `localStorage` key `kodex_2fa_nudge_dismissed`; shown once per session if 2FA not enabled

**Checkpoint**: Login with 2FA-enabled account → TOTP prompt → correct code → access granted; recovery code path also tested.

---

## Phase 9: User Story 4 — WebAuthn / FIDO2 Passkeys (Priority: P2)

**Goal**: Logged-in users can register a passkey; on subsequent visits they can authenticate via biometric/PIN without a password.

**Independent Test**: Register passkey in security settings → logout → `POST /auth/passkeys/authenticate/begin` → submit browser assertion → access granted.

- [ ] T093 [US4] Implement `server/domain/.../passkey/usecase/RegisterPasskeyUseCase.kt` — generate challenge (`SecureRandom.nextBytes(32)`), sign + encrypt via `WebAuthnChallengeCrypto`, set in HttpOnly cookie; on complete: validate attestation via `webauthn4j-core`, store `WebAuthnCredential` row
- [ ] T094 [US4] Implement `server/domain/.../passkey/usecase/AuthenticatePasskeyUseCase.kt` — generate challenge cookie; on complete: validate assertion via `webauthn4j-core`, enforce signCount monotonicity (if `assertion.signCount ≤ stored.signCount && stored.signCount > 0` → log security event + return 401 per SEC-009), update `sign_count`, issue tokens
- [ ] T095 [US4] Implement `server/domain/.../passkey/usecase/RemovePasskeyUseCase.kt` — verify last-auth-method guard (must have password or another provider/passkey), delete `webauthn_credentials` row
- [ ] T096 [US4] Create `server/api/.../auth/PasskeyAuthRoutes.kt` — `POST /api/v1/auth/passkeys/authenticate/begin` + `POST .../complete` (unauthenticated flows); challenge cookie: `HttpOnly; SameSite=Lax; Path=/api/v1/auth; Max-Age=120`
- [ ] T097 [US4] Create `server/api/.../users/PasskeyManageRoutes.kt` — `POST /users/me/passkeys/register/begin`, `POST .../complete`, `GET /users/me/passkeys`, `DELETE /users/me/passkeys/{passkeyId}`; all protected
- [ ] T098 [US4] Add passkey registration UI to `SecuritySettingsPage.kt` — call WebAuthn JS API (`navigator.credentials.create()`), POST assertion to `register/complete`; display registered passkeys list with friendly name + delete button
- [ ] T099 [US4] Add "Sign in with passkey" button to `SignInPage.kt` — call `navigator.credentials.get()`, POST to `authenticate/complete`; hide button if `PublicKeyCredential` not supported in browser

**Checkpoint**: Chrome + Touch ID / Windows Hello: full passkey register → logout → passkey login cycle confirmed.

---

## Phase 10: User Story 8 — New Device Login Alert (Priority: P2)

**Goal**: A successful login from a new IP triggers an instant security email with a single-use link that revokes all sessions without requiring re-authentication.

**Independent Test**: Login from known IP → no email; simulate new IP (modify test) → email received; click revoke link → all refresh tokens revoked → subsequent refresh attempt returns 401.

- [ ] T100 [US8] Add new-device detection to `LoginUseCase.kt` — after successful auth: SHA-256 hash client IP (from `call.request.origin.remoteHost`), check `known_login_ips` by `(userId, ipHash)`; if absent: GeoIP lookup, generate `EmergencyRevokeToken` (32-byte TokenHasher, 24 h TTL), store hash, dispatch `NewDeviceAlertEmail`, insert `known_login_ips` row; if present: update `last_seen_at`
- [ ] T101 [US8] Implement `server/domain/.../auth/usecase/EmergencyRevokeAllSessionsUseCase.kt` — look up `emergency_revoke_tokens` by SHA-256 hash, check not used + not expired, revoke ALL `refresh_tokens` for user (`revoked_at = now()`), mark emergency token used
- [ ] T102 [US8] Add `GET /api/v1/auth/emergency-revoke?token=` to `AuthRoutes.kt` — no auth required; call `EmergencyRevokeAllSessionsUseCase`; on success redirect to `APP_BASE_URL/security/sessions-revoked`; on invalid token → `400 TOKEN_INVALID_OR_EXPIRED`
- [ ] T103 [P] [US8] Create `NewDeviceAlertEmail` template in `server/data/.../email/templates/` — shows timestamp, approximate location (city, country), device hint, includes emergency revoke link (`APP_BASE_URL/api/v1/auth/emergency-revoke?token=<raw>`)
- [ ] T104 [P] [US8] Create `app/webApp/.../pages/auth/SessionsRevokedPage.kt` — static confirmation page shown after emergency revoke link is clicked; message: "All sessions have been revoked"

**Checkpoint**: Simulate new IP login → email received → click revoke link → all sessions gone.

---

## Phase 11: User Story 9 — Active Sessions Management (Priority: P2)

**Goal**: Users can view all active sessions with device/IP info and revoke any individual session or all others at once.

**Independent Test**: Login from two browsers → sessions list in settings shows two entries; revoke one → second browser's refresh attempt returns 401.

- [ ] T105 [US9] Implement `server/domain/.../users/usecase/GetSessionsUseCase.kt` — query active refresh tokens for user, mark which `tokenHash` matches current request's cookie, map to `SessionDto` list (newest first)
- [ ] T106 [US9] Implement `server/domain/.../users/usecase/RevokeSessionUseCase.kt` — single: set `revoked_at` for given session ID (owned by requesting user only); all-except-current: bulk revoke with exclusion of current token hash
- [ ] T107 [US9] Create `server/api/.../users/SessionRoutes.kt` — `GET /users/me/sessions`, `DELETE /users/me/sessions/{sessionId}`, `DELETE /users/me/sessions` (all-except-current); all protected
- [ ] T108 [US9] Add sessions list section to `SecuritySettingsPage.kt` — table with `deviceHint`, `ipAddress`, `issuedAt`, `isCurrent` badge; per-row "Revoke" button; "Revoke all other sessions" button with confirmation dialog

**Checkpoint**: Two-browser revocation test passes; current session badge displayed correctly.

---

## Phase 12: User Story 10 — Account Linking & Email Change (Priority: P2)

**Goal**: Users can link/unlink OAuth providers and change their registered email address with verification.

**Independent Test**: Email-only account → link GitHub → `GET /users/me/oauth` shows GitHub linked; unlink → removed (only if password remains); request email change → verify → email updated.

- [ ] T109 [US10] Create `server/api/.../users/OAuthLinkRoutes.kt` — `GET /users/me/oauth` (list linked providers), `DELETE /users/me/oauth/{provider}` (unlink; enforce last-auth-method guard: 409 if only auth method); protected
- [ ] T110 [US10] Implement `server/domain/.../users/usecase/ChangeEmailUseCase.kt` — verify current password (or OAuth if no password), check new email uniqueness, generate verification token, upsert `pending_email_changes` row, dispatch `EmailChangeVerificationEmail` to new address
- [ ] T111 [US10] Implement pending email change completion — validate `POST /users/me/email/verify` token, update `users.email`, delete `pending_email_changes` row, dispatch `EmailChangedNotificationEmail` to old address, revoke all refresh tokens
- [ ] T112 [US10] Implement `server/domain/.../users/usecase/ChangeUsernameUseCase.kt` — validate new username format (`[a-z0-9_-]`, 3–30), check uniqueness, update `users.username`
- [ ] T113 [US10] Add `PATCH /api/v1/users/me/email` + `POST /api/v1/users/me/email/verify` to `UserRoutes.kt` — protected
- [ ] T114 [US10] Add `PATCH /api/v1/users/me/username` to `UserRoutes.kt` — protected; 409 on taken, 422 on invalid format
- [ ] T115 [P] [US10] Create `EmailChangeVerificationEmail` + `EmailChangedNotificationEmail` templates in `server/data/.../email/templates/`
- [ ] T116 [US10] Add account-linking + email-change sections to `SecuritySettingsPage.kt` — linked OAuth providers list with link/unlink buttons; change email form; change username form

**Checkpoint**: Email change end-to-end: request → verify → new email active; old email receives notification.

---

## Phase 13: User Story 11 — Profile Edit (Priority: P2)

**Goal**: Users can update their public/private profile (avatar, display name, real name, birthdate, location, social links).

**Independent Test**: PATCH `/users/me/profile` with all fields → 200; `GET /api/v1/users/{username}` returns updated public fields. `POST /users/me/avatar` with JPEG file → 200 with URL; re-fetch profile shows new avatar.

- [ ] T117 [US11] Implement `server/domain/.../users/usecase/UpdateProfileUseCase.kt` — validate social link URLs (well-formed `https://` URLs), sanitize birthdate, update `user_profiles` row
- [ ] T118 [US11] Implement `server/domain/.../users/usecase/UploadAvatarUseCase.kt` — receive `ByteArray` + content-type; enforce 5 MB limit; validate magic bytes via `AvatarStorageService.upload()`; update `user_profiles.avatar_url`
- [ ] T119 [US11] Add `PATCH /api/v1/users/me/profile` to `UserRoutes.kt` — call `UpdateProfileUseCase`; return updated `UserProfileDto`
- [ ] T120 [US11] Add `POST /api/v1/users/me/avatar` to `UserRoutes.kt` — `receiveMultipart()`, extract file part, stream to `UploadAvatarUseCase`; return `{ avatarUrl: "..." }`; 413 if > 5 MB; 415 if invalid MIME/magic
- [ ] T121 [US11] Add `GET /api/v1/users/{username}` public profile endpoint to `UserRoutes.kt` — no auth required; return public subset: `username`, `displayName`, `avatarUrl`, `location`, social links, `role`, `createdAt`; 404 if not found
- [ ] T122 [US11] Create `app/webApp/.../pages/settings/ProfileSettingsPage.kt` + `ProfileViewModel.kt` — avatar upload via `<input type="file">` bridge, all profile fields, social link fields with URL format validation (client-side), save button → `PATCH /users/me/profile` + optionally `POST /users/me/avatar`
- [ ] T123 [P] [US11] Update `UserRemoteDataSourceImpl.kt` — add `updateProfile()`, `uploadAvatar()`, `getPublicProfile()` methods
- [ ] T124 [US11] Update navbar in `app/webApp` — authenticated state shows user avatar (from `AuthStore.state.user.profile.avatarUrl`) + username dropdown with links to Profile Settings, Security Settings, Logout

**Checkpoint**: Avatar upload → navbar immediately shows new avatar; public profile URL `/u/<username>` reflects all updated fields.

---

## Phase 14: Tests (Phase F)

**Purpose**: Kotest integration + unit + frontend + screenshot tests for all 11 user stories.

- [ ] T125 [P] Create `server/data/src/test/kotlin/.../crypto/PasswordHasherTest.kt` + `TokenHasherTest.kt` + `TotpCryptoTest.kt` + `WebAuthnChallengeCryptoTest.kt` — unit tests (no DB); verify hash/verify round-trips, entropy guarantees
- [ ] T126 [P] Create `server/api/src/test/kotlin/.../middleware/TurnstileVerifierTest.kt` — mock Ktor `HttpClient`; test success + failure responses
- [ ] T127 [P] Kotest integration test `RegisterUseCaseTest.kt` in `server/domain/src/test/kotlin/` — Testcontainers PG; test successful register, duplicate email, duplicate username, weak password rejection
- [ ] T128 [P] Kotest integration test `LoginUseCaseTest.kt` — test success, wrong password (counter increments), lockout at 10 failures, Turnstile required at 3 per-IP failures
- [ ] T129 [P] Kotest integration test `RefreshTokenUseCaseTest.kt` — test rotation, revoked token rejection, expired token rejection
- [ ] T130 [P] Kotest integration test `VerifyEmailUseCaseTest.kt` — magic link + OTP paths; expired token; already-used token
- [ ] T131 [P] Kotest integration test `ForgotPasswordUseCaseTest.kt` + `ResetPasswordUseCaseTest.kt` — full cycle; expired link; used link
- [ ] T132 [P] Kotest integration test `OAuthLoginUseCaseTest.kt` — mock provider responses; new user created; existing email linked; auto-username generation
- [ ] T133 [P] Kotest integration test `SetupTotpUseCaseTest.kt` + `ConfirmTotpUseCaseTest.kt` + `VerifyTotpCodeUseCaseTest.kt` — secret generation, confirmation, TOTP code validation, recovery code usage
- [ ] T134 [P] Kotest integration test `RegisterPasskeyUseCaseTest.kt` + `AuthenticatePasskeyUseCaseTest.kt` — mock `webauthn4j-core`; signCount monotonicity failure case
- [ ] T135 [P] Kotest API-level test — route-level test via `testApplication { }`: `POST /auth/register` → verify → `GET /users/me`; `POST /auth/login` → `POST /auth/refresh` → `POST /auth/logout` full cycle
- [ ] T136 [P] Frontend `kotlin.test` — `AuthStoreTest.kt`: state transitions LoggedOut → LoggedIn → Refresh → LoggedOut; timer scheduling
- [ ] T137 [P] Frontend `kotlin.test` — `TokenInterceptorTest.kt`: mock HTTP; verify 401 triggers refresh + retry; verify Bearer header injected
- [ ] T138 Screenshot test `SignInPage` in `app/webApp/src/test/` via `jsBrowserTest` — baseline + 2FA prompt variant
- [ ] T139 [P] Screenshot test `SignUpPage` — baseline + password strength bar states (Weak / Fair / Strong)
- [ ] T140 [P] Screenshot test `SecuritySettingsPage` — sessions list + 2FA section + passkeys section
- [ ] T141 [P] Screenshot test `ProfileSettingsPage` — filled profile + empty profile states

**Checkpoint**: `./gradlew :server:api:test :server:domain:test :server:data:test` green; `./gradlew :app:webApp:jsTest` green; screenshot baselines generated.

---

## Phase 15: Polish & Cross-Cutting Concerns

- [ ] T142 Run `./gradlew detekt` on all new Kotlin files; fix all violations (min identifier length, naming, complexity)
- [ ] T143 [P] Update `.github/workflows/ci.yml` — add all new auth secrets to CI environment (`CLOUDFLARE_TURNSTILE_SECRET`, `JWT_SECRET`, `TOTP_ENCRYPTION_KEY`, `WEBAUTHN_CHALLENGE_KEY`, `REDIS_URL`, `MINIO_ENDPOINT`, etc.); add `CHROME_BIN` for screenshot tests; add `GEOIP_DB_PATH` download step
- [ ] T144 [P] Update `SecuritySettingsPage.kt` + `ProfileSettingsPage.kt` — add `SecurityViewModel.kt` + `SecurityUiState.kt` for unified state management across all security-settings sections (TOTP + sessions + passkeys + account linking)
- [ ] T145 Run quickstart.md smoke tests — `curl` register + login + refresh + avatar upload; confirm all pass against local docker-compose stack
- [ ] T146 [P] Add `CHANGELOG.md` entry for spec 004 auth feature (via `/speckit-changelog-generate` or manual entry)

---

## Dependencies & Execution Order

### Phase Dependencies

```
Phase 1 (Setup) ──► Phase 2 (Foundational) ──┬──► Phase 3 (US1) ──► Phase 4 (US2) ──► Phase 5 (US7) ──► Phase 6 (US3)
                                               │
                                               └── [US1+US2+US3 complete] ──► Phase 7–13 (US4–US11) can proceed in parallel
                                                                          └──► Phase 14 (Tests) per story
                                                                          └──► Phase 15 (Polish) last
```

### User Story Dependencies

| Story | Priority | Can Start After | Notes |
|-------|---------|-----------------|-------|
| US1 Registration | P1 | Phase 2 | First P1 story |
| US2 Login | P1 | US1 complete | Needs verified users |
| US7 Logout | P1 | US2 complete | Needs refresh tokens |
| US3 OAuth | P1 | Phase 2 | Can parallel US1 if staffed |
| US6 Password | P2 | US2 complete | Password management |
| US5 TOTP | P2 | US2 complete | Modifies login flow |
| US4 WebAuthn | P2 | Phase 2 | Mostly independent |
| US8 New Device | P2 | US2 complete | Extends login |
| US9 Sessions | P2 | US2 complete | Uses refresh tokens |
| US10 Account Linking | P2 | US3 complete | Needs OAuth |
| US11 Profile Edit | P2 | US1 complete | Needs users table |

### Within Each Phase: Execution Order

1. `[P]`-marked tasks within a phase run in parallel
2. Non-`[P]` tasks within a phase run sequentially
3. Repository interfaces (T023–T026) before impls (T027–T032)
4. Impls before use cases; use cases before API routes; routes before frontend

### Parallel Opportunities Per Phase

```bash
# Phase 2 DB tables (T010-T014) — all 5 parallel:
T010 V3__auth_schema.sql | T011 UsersTable | T012 OAuthTable | T013 EmailTable | T014 PasswordResetTable

# Phase 2 Crypto + Services (T015-T022) — T016/T017/T018 parallel, T019/T020/T021/T022 parallel:
T016 TokenHasher | T017 TotpCrypto | T018 WebAuthnChallengeCrypto
T019 EmailService | T020 GeoIpService | T021 RateLimitService | T022 AvatarStorageService

# Phase 14 Tests — all marked [P] can run in parallel:
T127-T141 — all independent Kotest/kotlinx.test/screenshot suites
```

---

## Implementation Strategy

### MVP (User Stories 1 + 2 + 7 only — P1 core)

1. Phase 1: Setup (T001–T008)
2. Phase 2: Foundational (T009–T045)
3. Phase 3: US1 Registration (T046–T055)
4. Phase 4: US2 Login (T056–T066)
5. Phase 5: US7 Logout (T067–T069)
6. **STOP & VALIDATE**: full register → login → refresh → logout cycle
7. Deploy / demo MVP — real auth system working

### Incremental Delivery

- Add US3 (OAuth) → demo social login
- Add US6+US5 (Password Recovery + TOTP) → demo 2FA
- Add US4 (WebAuthn) → demo passkeys
- Add US8+US9 (Device Alerts + Sessions) → demo security dashboard
- Add US10+US11 (Account Linking + Profile) → feature complete

---

## Summary

| Metric | Count |
|--------|-------|
| Total tasks | **146** |
| Phase 1 (Setup) | 8 |
| Phase 2 (Foundational) | 37 |
| Phase 3 US1 Registration | 10 |
| Phase 4 US2 Login | 11 |
| Phase 5 US7 Logout | 3 |
| Phase 6 US3 OAuth | 5 |
| Phase 7 US6 Password | 8 |
| Phase 8 US5 TOTP | 10 |
| Phase 9 US4 WebAuthn | 7 |
| Phase 10 US8 New Device | 5 |
| Phase 11 US9 Sessions | 4 |
| Phase 12 US10 Account Linking | 8 |
| Phase 13 US11 Profile Edit | 8 |
| Phase 14 Tests | 17 |
| Phase 15 Polish | 5 |
| Parallelizable tasks [P] | **79** |
| User story phases | **11** |
| P1 stories | 4 (US1, US2, US7, US3) |
| P2 stories | 7 (US4, US5, US6, US8, US9, US10, US11) |
