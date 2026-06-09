# Feature Specification: Authentication & User Identity

**Feature Branch**: `feature/004-auth`

**Created**: 2026-06-08

**Status**: Draft

**Input**: Email+password + OAuth GitHub/Google + WebAuthn passkeys + TOTP 2FA + JWT + refresh token + email verification + profile edit + forgot/change password + logout + unique username + progressive CAPTCHA + new-device alert + active sessions UI + account linking + email change

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Email Registration & Verification (Priority: P1)

A new visitor creates a KodEx account by choosing a unique username, entering their email address, and setting a password. The username becomes their permanent public handle (e.g. `@ghasem.shirdel`, used in the URL `/u/ghasem.shirdel`). After submitting the form — which includes an invisible bot-detection challenge — they receive a verification message. On web they receive a one-click magic link; on mobile clients they receive a 6-digit OTP code. Until verification is complete the account exists but the user cannot perform authenticated actions. After verification they are fully active with the PARTICIPANT role.

**Why this priority**: The foundation of the entire auth system. Every other flow depends on a verified account existing. Without this, no other auth path is testable end-to-end.

**Independent Test**: Can be fully tested by registering a new email address, receiving the verification message, verifying, then accessing a protected resource — delivers a working sign-up funnel.

**Acceptance Scenarios**:

1. **Given** a visitor on the Sign Up page, **When** they enter a unique username, a valid email, a strong password (meets minimum strength), and pass the bot-detection challenge, **Then** the account is created with PARTICIPANT role, a verification message is sent, and the user sees a "check your inbox" screen.
1a. **Given** a username that is already taken, **When** the user types it, **Then** real-time feedback ("username unavailable") is shown before form submission.
2. **Given** a web client (no `X-Client-Platform` header or `web`), **When** registration succeeds, **Then** a magic link is emailed (single-use, expires in 24 hours).
3. **Given** a mobile client (`X-Client-Platform: android` or `ios`), **When** registration succeeds, **Then** a 6-digit OTP code is emailed (single-use, expires in 15 minutes).
4. **Given** an unverified account, **When** the user attempts a protected action, **Then** they are blocked and prompted to verify their email.
5. **Given** a valid magic link, **When** clicked, **Then** the account is marked verified and the user is redirected to the app as logged in.
6. **Given** a valid OTP code on mobile, **When** submitted, **Then** the account is marked verified and the mobile client receives a token pair.
7. **Given** an expired or already-used verification token, **When** used, **Then** an error is shown and a re-send option is offered.
8. **Given** an email that already exists, **When** someone attempts to register with it, **Then** a generic "if this email isn't registered you'll receive a message" response is shown (no user enumeration).

---

### User Story 2 — Email/Password Login (Priority: P1)

A returning user logs in with their email and password. The browser/app offers autocomplete from saved credentials. On successful authentication they receive a short-lived JWT access token (15 min) and a long-lived refresh token (7 days). The refresh token is stored server-side and can be revoked on logout.

**Why this priority**: Core login path used by users who registered with email. Must work before OAuth or passkeys can be layered on.

**Independent Test**: Can be fully tested by registering an account (Story 1), then logging in with the same credentials and accessing a route that requires authentication.

**Acceptance Scenarios**:

1. **Given** a verified account, **When** the user enters correct email and password, **Then** they receive an access token (15 min) and refresh token (7 days), and are redirected to their dashboard.
2. **Given** a login form, **When** rendered, **Then** email and password fields have correct autocomplete attributes so password managers can suggest saved credentials.
3. **Given** correct credentials, **When** the user has 2FA enabled, **Then** after password verification a TOTP prompt is shown before tokens are issued.
4. **Given** incorrect password, **When** submitted, **Then** a generic error is shown (no indication of whether email exists) and the attempt is counted toward rate-limit and lockout thresholds.
5. **Given** 3 or more failed login attempts from the same IP within 10 minutes, **When** the next attempt begins, **Then** a bot-detection challenge (Cloudflare Turnstile) is presented before the credentials are checked.
6. **Given** 10 consecutive failed attempts for the same account within 15 minutes, **When** the next attempt occurs, **Then** the account is temporarily locked (30 minutes) and the registered email receives a lockout notification with an unlock link.
6. **Given** an unverified account, **When** login is attempted, **Then** login is denied and a re-send verification option is shown.
7. **Given** a valid access token, **When** it expires, **Then** the client can exchange the refresh token for a new access token without re-entering credentials.
8. **Given** a revoked or expired refresh token, **When** a refresh is attempted, **Then** the user is logged out and must re-authenticate.

---

### User Story 3 — OAuth Login/Register: GitHub & Google (Priority: P1)

A visitor chooses to sign in with their GitHub or Google account. They are redirected to the provider, authenticate there, and are redirected back. If no KodEx account exists for that identity, one is created automatically (no email verification step needed — the provider has already verified the email). The user's avatar, display name, and email are imported from the provider.

**Why this priority**: Primary alternative to email+password; expected by most users on a developer platform. GitHub in particular is a natural identity provider for a coding platform.

**Independent Test**: Can be fully tested by clicking "Continue with GitHub/Google", completing the OAuth flow, and landing authenticated in the app with a populated profile.

**Acceptance Scenarios**:

1. **Given** a visitor clicks "Continue with GitHub", **When** they authorise on GitHub, **Then** they are redirected back and logged into a new or existing KodEx account.
2. **Given** a first-time GitHub OAuth login, **When** the callback is processed, **Then** a new PARTICIPANT account is created with avatar, name, and email imported from GitHub.
3. **Given** an existing account whose email matches the OAuth provider email, **When** they log in via OAuth, **Then** the OAuth identity is linked to the existing account (not a duplicate).
4. **Given** a successful OAuth login, **When** the user lands in the app, **Then** they have a valid access token and refresh token pair.
5. **Given** a Google OAuth login that returns a profile with a profile picture URL, **When** the account is created, **Then** the avatar URL is stored and shown in the navbar and profile page.
6. **Given** the OAuth provider returns no email (rare GitHub private-email case), **When** the callback is processed, **Then** the user is prompted to provide an email before the account is finalised.

---

### User Story 4 — WebAuthn / FIDO2 Passkey Authentication (Priority: P2)

A logged-in user registers a passkey (fingerprint, Face ID, or hardware security key) from their profile settings. On subsequent visits they can authenticate using just their passkey — no password needed. This works across all devices that support WebAuthn (browsers, future mobile app).

**Why this priority**: Significant security and UX improvement over passwords, and required to support future mobile targets with biometric auth. P2 because it requires an existing account.

**Independent Test**: Can be fully tested by enabling a passkey for an account, logging out, and then authenticating using the passkey on the same device.

**Acceptance Scenarios**:

1. **Given** a logged-in user on a WebAuthn-capable device, **When** they register a passkey in profile settings, **Then** the credential is stored server-side and the passkey appears in their registered credentials list.
2. **Given** a registered passkey, **When** the user initiates passkey login from the Sign In page, **Then** the browser/OS prompts for biometric/PIN and on success issues a token pair.
3. **Given** multiple passkeys registered (e.g., laptop + phone), **When** one is removed from profile, **Then** only the removed credential is invalidated; others still work.
4. **Given** a WebAuthn assertion with an invalid or tampered signature, **When** processed server-side, **Then** authentication is rejected.
5. **Given** a browser that does not support WebAuthn, **When** the Sign In page loads, **Then** the passkey option is hidden and other methods remain available.

---

### User Story 5 — TOTP Two-Factor Authentication (Priority: P2)

After logging in for the first time, the user sees a non-blocking banner recommending they enable 2FA. From their profile security settings, they can scan a QR code with any TOTP app (Google Authenticator, Authy), confirm with a code, and enable 2FA. Future logins require the TOTP code after password verification.

**Why this priority**: Security hardening. Does not block baseline auth but significantly reduces account takeover risk for active users.

**Independent Test**: Can be fully tested by enabling 2FA for an account, logging out, re-logging with correct password, and providing the correct TOTP code to complete authentication.

**Acceptance Scenarios**:

1. **Given** a user who has never enabled 2FA, **When** they log in successfully, **Then** a dismissible banner appears recommending they enable 2FA with a link to profile settings.
2. **Given** a user in profile security settings, **When** they click "Enable 2FA", **Then** a QR code and plaintext secret are shown for scanning with an authenticator app.
3. **Given** a valid TOTP code entered to confirm setup, **When** submitted, **Then** 2FA is activated and a set of one-time recovery codes is shown once and must be acknowledged.
4. **Given** a 2FA-enabled account, **When** the user logs in with correct password, **Then** a TOTP prompt appears before the token pair is issued.
5. **Given** a correct TOTP code (valid within ±30 second drift window), **When** submitted, **Then** authentication completes and the token pair is issued.
6. **Given** an incorrect or expired TOTP code, **When** submitted, **Then** authentication is rejected with a clear error.
7. **Given** a user who has lost their authenticator, **When** they use a one-time recovery code at the 2FA prompt, **Then** they are authenticated and the used recovery code is invalidated.
8. **Given** a logged-in user who wants to disable 2FA, **When** they confirm with their current password and a valid TOTP code, **Then** 2FA is disabled.

---

### User Story 6 — Forgot Password & Change Password (Priority: P2)

A user who has forgotten their password requests a reset. They receive a time-limited reset link via email. Following the link brings them to a form where they can set a new strong password. Separately, a logged-in user can change their password from profile security settings by confirming their current password.

**Why this priority**: Essential account recovery path. Without this, forgotten passwords mean permanent account loss for email-registered users.

**Independent Test**: Can be fully tested by requesting a reset for a known email, using the reset link, setting a new password, and logging in with it.

**Acceptance Scenarios**:

1. **Given** a visitor on the Forgot Password page, **When** they submit any email address, **Then** they always see "if this email is registered you will receive a reset link" (no user enumeration).
2. **Given** a registered email, **When** a reset is requested, **Then** a one-use reset link is emailed (expires in 1 hour).
3. **Given** a valid, unexpired reset link, **When** the user sets a new password that meets strength requirements, **Then** the password is updated, all existing refresh tokens for that account are revoked, and the user is redirected to login.
4. **Given** an expired or already-used reset link, **When** accessed, **Then** an error is shown with an option to request a new link.
5. **Given** a logged-in user in profile security settings, **When** they provide their current password and a new strong password, **Then** the password is changed and all other refresh tokens (other sessions) are revoked.
6. **Given** an OAuth-only account (no password set), **When** they access the change-password flow, **Then** they can set an initial password without providing a current password.

---

### User Story 7 — Logout (Priority: P1)

A logged-in user logs out from the profile dropdown. The server revokes the current refresh token, rendering it unusable for future token renewals. The client discards its tokens.

**Why this priority**: Required for any multi-user or shared-device scenario. Simple but security-critical.

**Independent Test**: Can be fully tested by logging in, calling logout, then attempting to use the revoked refresh token — which must fail.

**Acceptance Scenarios**:

1. **Given** a logged-in user, **When** they click "Log Out", **Then** the current refresh token is revoked server-side and the client clears its stored tokens.
2. **Given** a revoked refresh token, **When** a token refresh is attempted, **Then** a 401 response is returned.
3. **Given** a "log out all devices" action (from security settings), **When** confirmed, **Then** all refresh tokens for the account are revoked.

---

### User Story 8 — New Device Login Alert (Priority: P2)

When a user successfully logs in from an IP address or device that has never been seen before for that account, a security notification email is sent immediately. The email contains a one-click link that — if the user did not initiate the login — instantly revokes all refresh tokens for the account (force logout everywhere).

**Why this priority**: Detects credential theft without requiring the user to do anything proactive. Substantially reduces the blast radius of a stolen password.

**Independent Test**: Can be fully tested by logging in from a known IP, then simulating a login from a new IP, and verifying the security email is received and the revoke link works.

**Acceptance Scenarios**:

1. **Given** a user who has logged in before from IP A, **When** a successful login occurs from IP B (unseen for this account), **Then** a security alert email is sent immediately with details (time, approximate location, device hint) and a single-use "revoke all sessions" link.
2. **Given** the "this wasn't me" revoke link in the email, **When** clicked, **Then** all refresh tokens for the account are revoked, the user is logged out everywhere, and a confirmation page is shown. The link works without requiring login (the user may no longer have access if credentials were compromised).
3. **Given** a login from an already-seen IP, **When** authentication succeeds, **Then** no alert email is sent.
4. **Given** an OAuth login from a new device, **When** it succeeds, **Then** the same alert logic applies as for password login.

---

### User Story 9 — Active Sessions Management (Priority: P2)

A logged-in user can view all their currently active sessions in their profile security settings. Each session shows a device hint (browser/OS), IP address, approximate location, and last-used timestamp. The user can revoke any individual session, or revoke all sessions at once.

**Why this priority**: Gives users transparency and control over where their account is active. Granular revocation is better UX than the blunt "logout all devices".

**Independent Test**: Can be fully tested by logging in from two different browsers, visiting the sessions page in one, and revoking the other session — the second browser should be logged out.

**Acceptance Scenarios**:

1. **Given** a logged-in user on their security settings page, **When** the sessions list loads, **Then** all active refresh tokens are shown with device hint, IP, and last-used time — newest first.
2. **Given** the sessions list, **When** the user clicks "Revoke" on a specific session, **Then** that refresh token is invalidated and the entry disappears from the list.
3. **Given** the sessions list, **When** "Revoke all other sessions" is clicked and confirmed, **Then** all refresh tokens except the current one are revoked.
4. **Given** a session whose refresh token has naturally expired, **When** the sessions page loads, **Then** that session is not shown (expired entries are excluded).

---

### User Story 10 — Account Linking & Email Change (Priority: P2)

A logged-in user can manage which authentication methods are connected to their account from security settings. They can link additional OAuth providers (e.g., a user who registered with email can add a GitHub link), unlink providers (as long as at least one auth method remains), and change their registered email address. Changing the email triggers a re-verification flow for the new address.

**Why this priority**: Account recovery flexibility and long-term maintainability of the user's identity. Without this, losing access to a linked OAuth provider can mean losing the account.

**Independent Test**: Can be fully tested by creating an email+password account, linking a GitHub OAuth identity, verifying the GitHub login now works, then unlinking it.

**Acceptance Scenarios**:

1. **Given** a logged-in user in security settings, **When** they click "Connect GitHub", **Then** the OAuth flow runs and on success GitHub is listed as a linked provider.
2. **Given** a linked OAuth provider, **When** the user clicks "Disconnect", **Then** it is allowed only if at least one other auth method exists (password or another provider); otherwise a blocking error explains why.
3. **Given** a user who wants to change their email, **When** they enter a new email address and confirm with their current password, **Then** a verification link is sent to the new address and the change is pending until verified.
4. **Given** a pending email change, **When** the new email is verified, **Then** the account's email is updated, the old email receives a notification ("your email was changed"), and all existing refresh tokens are revoked.
5. **Given** a pending email change that is not verified within 24 hours, **When** the deadline passes, **Then** the change is cancelled and the original email remains active.

---

### User Story 11 — Profile Edit (Priority: P2)

A logged-in user can edit their public and private profile details: upload a profile picture, set their display name, real name, birthdate, location, and link their social profiles (GitHub, LinkedIn, Twitter/X, personal website). OAuth-registered users have their avatar and name pre-populated; email-registered users start with empty fields.

**Why this priority**: Personalisation that makes the platform social. Needed before spec 011 (gamification profile page) but does not block core auth.

**Independent Test**: Can be fully tested by editing profile fields, saving, then visiting the public profile page to confirm the changes are reflected.

**Acceptance Scenarios**:

1. **Given** a logged-in user on their profile edit page, **When** they upload an image file (JPEG/PNG/WebP, max 5 MB), **Then** it is stored and their avatar is updated across the navbar and profile.
2. **Given** a logged-in user, **When** they update name, surname, birthdate, location, and social links and save, **Then** the changes are persisted and reflected on their public profile page.
3. **Given** a social link field, **When** the user enters a value, **Then** it is validated as a well-formed URL before saving.
4. **Given** an OAuth-registered user, **When** they visit profile edit, **Then** avatar and name are pre-filled from the OAuth provider but can be overridden.
5. **Given** a display name field, **When** left blank, **Then** the system falls back to the username portion of the email.

---

### Edge Cases

- What happens when an OAuth provider is unavailable during the callback? → Show a user-friendly error and offer alternative sign-in methods.
- What happens if a user registers with email X via OAuth, then tries to register with email X via email+password? → The OAuth identity is the primary; the user is prompted to link (or log in with OAuth to set a password).
- What happens if a refresh token is stolen and used before legitimate use? → Token rotation on refresh means the second use of the old token invalidates both; the user's session is terminated and they must re-authenticate.
- What if the TOTP clock on the user's device is significantly off? → Allow ±1 period (±30 s) drift; beyond that reject and advise the user to sync their device clock.
- What if an avatar upload contains a malformed or malicious file? → MIME type and magic bytes are both validated server-side; the file is rejected if they don't match a permitted image type.

---

## Requirements *(mandatory)*

### Functional Requirements

**Registration & Verification**

- **FR-001**: The system MUST allow new users to register with a unique username, a unique email address, and a password that meets the minimum strength requirement.
- **FR-001a**: Usernames MUST be 3–30 characters, contain only lowercase letters, digits, underscores, and hyphens, and be unique across the platform. The public profile URL MUST be `/u/<username>`.
- **FR-001b**: The registration form MUST provide real-time username availability feedback before submission.
- **FR-002**: The system MUST reject passwords below the minimum strength threshold and display a real-time strength indicator (Weak / Fair / Strong / Very Strong) as the user types.
- **FR-003**: The system MUST send an email verification message on registration: a magic link for web clients, a 6-digit OTP for mobile clients, distinguished by the `X-Client-Platform` request header.
- **FR-004**: The system MUST block authenticated actions for unverified accounts and offer a re-send verification option.

**Login & Session**

- **FR-005**: The system MUST issue a JWT access token (15-minute expiry) and a refresh token (7-day expiry) on successful authentication.
- **FR-006**: Refresh tokens MUST be stored server-side as SHA-256 hashes and be revocable individually or all-at-once per account.
- **FR-007**: The system MUST implement refresh-token rotation: exchanging a refresh token for a new pair invalidates the old refresh token.
- **FR-008**: The system MUST present a bot-detection challenge (Cloudflare Turnstile) after 3 or more failed login attempts from the same IP within 10 minutes.
- **FR-008a**: The system MUST temporarily lock an account for 30 minutes after 10 consecutive failed login attempts within 15 minutes, and MUST send a lockout notification email.
- **FR-008b**: The bot-detection challenge MUST also appear on the registration form and the forgot-password form to prevent automated abuse.

**OAuth**

- **FR-009**: The system MUST support OAuth sign-in with GitHub and Google using a server-side authorisation code flow.
- **FR-010**: On first OAuth login the system MUST create a PARTICIPANT account and import avatar URL, display name, and email from the provider.
- **FR-011**: If an incoming OAuth email matches an existing account, the system MUST link the OAuth identity to that account rather than create a duplicate.

**WebAuthn / Passkeys**

- **FR-012**: Logged-in users MUST be able to register one or more FIDO2/WebAuthn credentials (passkeys) from their profile security settings.
- **FR-013**: Registered passkeys MUST be usable as a standalone authentication method (no password required).
- **FR-014**: Users MUST be able to remove individual registered passkeys from their profile.

**TOTP 2FA**

- **FR-015**: The system MUST support TOTP-based 2FA compatible with RFC 6238 authenticator apps.
- **FR-016**: When 2FA is enabled, login MUST require a valid TOTP code after password verification before issuing tokens.
- **FR-017**: On 2FA setup, the system MUST generate and display one-time recovery codes (minimum 8 codes) that bypass TOTP if the user loses their authenticator.
- **FR-018**: Each recovery code MUST be single-use; once consumed it is invalidated.
- **FR-019**: After the first successful login, the system MUST display a non-blocking, dismissible banner prompting the user to enable 2FA.

**Password Recovery**

- **FR-020**: The system MUST provide a Forgot Password flow that sends a one-use password reset link (1-hour expiry) to the registered email.
- **FR-021**: Using a reset link MUST invalidate all existing refresh tokens for that account.
- **FR-022**: Logged-in users MUST be able to change their password from profile security settings by confirming their current password; this MUST revoke all other active refresh tokens.

**Logout & Sessions**

- **FR-023**: The system MUST provide a logout endpoint that revokes the current refresh token.
- **FR-024**: The system MUST provide a "log out all devices" action that revokes all refresh tokens for the account.
- **FR-031**: The security settings page MUST list all active refresh tokens showing device hint, IP address, approximate location, and last-used timestamp.
- **FR-032**: Users MUST be able to revoke any individual session from the sessions list without affecting other sessions.

**New Device Alerts**

- **FR-033**: The system MUST send a security notification email when a successful login occurs from an IP address not previously seen for that account.
- **FR-034**: The security notification email MUST include a single-use "revoke all sessions" link that, when clicked, immediately invalidates all refresh tokens for the account.

**Account Linking & Email Change**

- **FR-035**: Logged-in users MUST be able to link additional OAuth providers (GitHub, Google) to their existing account from security settings.
- **FR-036**: Users MUST be able to unlink an OAuth provider only if at least one other authentication method (password or a different linked provider) remains on the account.
- **FR-037**: Users MUST be able to change their registered email address by confirming their current password; the change is pending until the new email address is verified.
- **FR-038**: On successful email change, the old email MUST receive a notification and all existing refresh tokens MUST be revoked.
- **FR-039**: A pending email change that is not verified within 24 hours MUST be automatically cancelled.

**OAuth Username & Username Change**

- **FR-040**: When a new account is created via OAuth the system MUST auto-generate a username from the OAuth provider email prefix: strip non-`[a-z0-9_-]` characters, truncate to 30 characters, and append an incrementing numeric suffix (e.g. `2`, `3`) until the name is unique.
- **FR-041**: Logged-in users MUST be able to change their username from profile settings; the new username MUST satisfy FR-001a (3–30 chars, `[a-z0-9_-]`, unique). The public profile URL `/u/<username>` updates immediately upon change.

**Profile**

- **FR-025**: Users MUST be able to upload a profile picture (JPEG, PNG, WebP; max 5 MB); the server MUST validate both MIME type and magic bytes.
- **FR-026**: Users MUST be able to set: display name, first name, last name, birthdate (YYYY-MM-DD), location (free text), and social links (GitHub URL, LinkedIn URL, Twitter/X URL, personal website URL).
- **FR-027**: Social link fields MUST validate that values are well-formed URLs before persisting.

**Roles & Access Control**

- **FR-028**: New accounts MUST be assigned the PARTICIPANT role on creation.
- **FR-029**: The system MUST enforce role-based access control via a middleware/guard that checks the JWT claims on protected routes.
- **FR-030**: The EXAM_CREATOR role MUST only be grantable by an ADMIN.

### Key Entities

- **User**: Platform identity. Attributes: id, username (unique, 3–30 chars), email, email_verified, password_hash (nullable), role (PARTICIPANT | EXAM_CREATOR | ADMIN), created_at, locked_until (nullable).
- **UserProfile**: Editable public/private metadata. Attributes: user_id (FK), display_name, first_name, last_name, birthdate, avatar_url, location, github_url, linkedin_url, twitter_url, website_url.
- **OAuthIdentity**: Links an external provider identity to a User. Attributes: id, user_id (FK), provider (GITHUB | GOOGLE), provider_user_id, linked_at.
- **RefreshToken**: Server-side refresh token record. Attributes: id, user_id (FK), token_hash (SHA-256), issued_at, expires_at, revoked_at (nullable), device_hint, ip_address.
- **EmailVerificationToken**: Pending verification record. Attributes: id, user_id (FK), token_hash, delivery_mode (MAGIC_LINK | OTP), expires_at, used_at (nullable).
- **PendingEmailChange**: In-flight email address change. Attributes: id, user_id (FK), new_email, token_hash, requested_at, expires_at, used_at (nullable).
- **PasswordResetToken**: Pending password reset record. Attributes: id, user_id (FK), token_hash, expires_at, used_at (nullable).
- **WebAuthnCredential**: Registered passkey. Attributes: id, user_id (FK), credential_id, public_key_cose, sign_count, aaguid, friendly_name, created_at.
- **TotpConfig**: TOTP configuration per user. Attributes: user_id (FK), secret_encrypted, enabled, backup_codes_hash[] (array of SHA-256 hashes of one-time codes).
- **KnownLoginIp**: IP addresses seen for a user (for new-device alert logic). Attributes: id, user_id (FK), ip_hash (SHA-256 of IP), first_seen_at, last_seen_at.
- **EmergencyRevokeToken**: Single-use token embedded in new-device alert emails. Attributes: id, user_id (FK), token_hash (SHA-256), expires_at (+24 h), used_at (nullable). Clicking the link revokes all sessions without requiring login.

---

## Non-Functional Requirements

### NFR-1 — Auth Module Extractability

The `server/domain/auth/` subtree MUST be portable to another Kotlin backend project without modification. Specifically:

- **Zero Ktor imports** in `server:domain` — no `ApplicationCall`, `Route`, or `io.ktor.*` types.
- **Zero Exposed imports** in `server:domain` — only repository interfaces; table definitions live in `server:data`.
- **No framework annotations** in use cases — Koin `@Single`/`@Inject` annotations go in `server:app` composition root only.
- Use cases depend exclusively on: `kotlinx.coroutines`, `kotlinx.datetime`, `core:models`, and domain interfaces.

To reuse in another project: copy `server/domain/auth/**` + `core/models/auth/**`, provide repository implementations for the new data layer. The Ktor routes in `server/api/auth/**` are not portable (framework-specific) and must be rewritten for the target framework.

### NFR-2 — Notification Provider Extensibility (OCP)

All transactional notification delivery (email verification, password reset, security alerts, OTP delivery) MUST be implemented behind a **provider interface** so new delivery channels can be added without modifying existing code.

- A `EmailChannel` interface and a `SmsChannel` interface are the contracts.
- Each provider is one class implementing the relevant interface.
- An `EmailRouter` (and `SmsRouter`) dispatches to available providers using:
  - **Quota awareness**: tracks daily usage per provider; skips exhausted channels.
  - **Circuit breaker**: opens after N consecutive failures; auto-recovers after a reset window.
  - **Failover order**: providers sorted by remaining capacity; first success wins.
- Adding a new provider = one new class + one line in Koin DI. Zero changes to router, use cases, or domain layer.

### NFR-3 — Horizontal Scalability Readiness

The auth backend MUST be deployable as multiple instances behind a load balancer without correctness issues:

- **Stateless request handling**: access token verification is stateless (JWT signature check only).
- **DB-backed refresh tokens**: token rotation happens in a single indexed transaction — safe under concurrent instances.
- **Redis rate-limit counter**: `INCR`/`EXPIRE` commands are atomic — correct under multiple instances.
- **QuotaTracker default**: in-process `AtomicLong` (v1 single-host). For multi-instance: swap with Redis-backed impl behind the same interface — zero domain/router code changes.
- **No sticky sessions required**: all state is in PostgreSQL + Redis; any instance can serve any request.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new user can complete the full registration and email verification flow in under 2 minutes on a standard connection.
- **SC-002**: A returning user can log in (email+password, no 2FA) in under 30 seconds from landing on the Sign In page to reaching their dashboard.
- **SC-003**: OAuth sign-in (GitHub or Google) completes in under 10 seconds from clicking the provider button to landing authenticated in the app.
- **SC-004**: All authentication endpoints respond within 500 ms at the 99th percentile under normal load.
- **SC-005**: A stolen and revoked refresh token is rejected within one token-rotation cycle (no valid access can be gained after revocation).
- **SC-006**: 95% of users who attempt 2FA setup successfully complete it on the first attempt (QR code scan + confirmation code).
- **SC-007**: Zero user enumeration: the registration, forgot-password, and login endpoints must not reveal whether a given email address is registered.
- **SC-008**: Profile edits are reflected across all surfaces (navbar avatar, public profile page) within 1 page refresh after saving.
- **SC-009**: A new-device login security alert email is delivered within 60 seconds of the triggering login event.
- **SC-010**: Bot-detection challenges on login, registration, and forgot-password add no more than 3 seconds to the user flow in the non-challenged (invisible) path.

---

## Assumptions

- Web is the only target for this spec; Android and iOS clients are future work. The `X-Client-Platform` header is introduced now to future-proof without adding mobile build complexity.
- Email sending uses an existing transactional email service (e.g., Resend, SendGrid) configured via environment variable — the provider is not specified here.
- Avatar images are stored externally (object storage); the profile record stores only the URL. Object storage integration is a deployment concern, not part of this spec.
- OAuth client credentials (GitHub App, Google OAuth 2.0 app) are pre-configured in environment variables before this spec is testable end-to-end.
- "Minimum password strength" means the password scores at least "Fair" on a standard strength estimator (e.g., zxcvbn score ≥ 2).
- The TOTP secret is stored encrypted at rest using an application-level encryption key; key management is a deployment concern.
- Infrastructure-level rate limiting (e.g., Nginx/Caddy request throttling) is deferred to SEC-004. The progressive CAPTCHA in this spec is the primary bot mitigation at the application layer.
- IP geolocation for new-device alerts uses a local GeoIP database (no external API call at login time) to determine approximate location.
- "Log out all devices" is a user-initiated action; admin-forced session revocation is out of scope for this spec (covered in spec 012).
- Birthdate is stored as a date (YYYY-MM-DD) and the displayed age is calculated at render time — it does not require a background job to stay accurate.
- The KnownLoginIp table stores a SHA-256 hash of the IP (not the raw IP) for privacy; approximate geolocation is still possible from the hash by re-hashing known IP ranges at lookup time or by storing the GeoIP result separately.
- Cloudflare Turnstile is the assumed bot-detection provider; the integration is configured via environment variables so it can be swapped.
