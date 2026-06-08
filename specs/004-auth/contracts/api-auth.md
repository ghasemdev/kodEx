# API Contract: Authentication Endpoints

All responses wrapped in `ApiEnvelope<T>` per §IX. Prefix: `/api/v1/`.

---

## Public Endpoints (no JWT required)

### `POST /api/v1/auth/register`

**Headers**: `X-Client-Platform: web|android|ios` (optional; defaults to `web`)

**Request**:
```json
{
  "username": "ghasem.shirdel",
  "email": "user@example.com",
  "password": "s3cr3tP@ss!",
  "turnstileToken": "<cloudflare-token>"
}
```

**Success 201** — sets `refresh_token` HttpOnly cookie; `X-Client-Platform: android|ios` → returns refresh token in body instead:
```json
{
  "data": {
    "accessToken": "<jwt>",
    "expiresIn": 900
  }
}
```

**Errors**:
- `422 VALIDATION_ERROR` — password too weak (score < 2), invalid email, username taken
- `409 EMAIL_ALREADY_REGISTERED` — generic message (no enumeration)
- `400 TURNSTILE_FAILED` — bot detection challenge invalid

---

### `POST /api/v1/auth/login`

**Request**:
```json
{
  "email": "user@example.com",
  "password": "s3cr3tP@ss!",
  "turnstileToken": "<token-or-null>"
}
```
`turnstileToken` is required only after ≥ 3 failed attempts from the requesting IP.

**Success 200 (no 2FA)**:
```json
{
  "data": {
    "accessToken": "<jwt>",
    "expiresIn": 900
  }
}
```

**Success 200 (2FA required)**:
```json
{
  "data": {
    "requiresTotp": true,
    "totpSessionToken": "<short-lived-jwt>"
  }
}
```
`totpSessionToken` is a short-lived JWT (TTL=5 min) signed with `JWT_SECRET`. Payload MUST be:
```json
{ "type": "totp-session", "sub": "<userId>", "iat": <unix>, "exp": <iat+300> }
```
The TOTP route MUST reject tokens where `type != "totp-session"`. The JWT auth guard MUST reject tokens where `type == "totp-session"` (i.e. missing `role` claim).

**Errors**:
- `401 INVALID_CREDENTIALS` — wrong email or password (no enumeration)
- `403 ACCOUNT_LOCKED` — with `lockedUntil` timestamp
- `403 EMAIL_NOT_VERIFIED` — account exists but email unverified
- `400 TURNSTILE_REQUIRED` — 3+ failures, challenge required but not provided

---

### `POST /api/v1/auth/login/totp`

Completes the 2FA step after successful password verification.

**Request**:
```json
{
  "totpSessionToken": "<token>",
  "code": "123456"
}
```
`code` may also be a recovery code (8 chars).

**Success 200**: Same as login success (accessToken + cookie set).

**Errors**:
- `401 INVALID_TOTP` — wrong code or expired session token

---

### `POST /api/v1/auth/refresh`

Exchanges the HttpOnly refresh token cookie for a new access + refresh token pair (rotation).

**Request**: No body. Refresh token read from `refresh_token` cookie.

**Success 200**: New `accessToken` in body + new `refresh_token` cookie.

**Errors**:
- `401 REFRESH_TOKEN_INVALID` — missing, expired, or revoked

---

### `POST /api/v1/auth/logout`

Revokes current refresh token.

**Request**: No body. Token from cookie.

**Success 200**: `data: null`. Cookie cleared (`Max-Age=0`).

---

### `POST /api/v1/auth/verify-email`

**Request (web — magic link)**:
```json
{ "token": "<raw-magic-link-token>" }
```

**Request (mobile — OTP)**:
```json
{ "userId": 42, "otp": "381920" }
```

**Success 200**: `data: null`. Account marked verified. Access + refresh token issued (user is now logged in).

**Errors**:
- `400 TOKEN_INVALID_OR_EXPIRED`
- `400 TOKEN_ALREADY_USED`

---

### `POST /api/v1/auth/verify-email/resend`

**Request**:
```json
{ "email": "user@example.com" }
```

**Success 200**: `data: null`. Generic response regardless of whether email is registered.

---

### `POST /api/v1/auth/forgot-password`

**Request**:
```json
{
  "email": "user@example.com",
  "turnstileToken": "<token>"
}
```

**Success 200**: `data: null`. Generic response (no enumeration).

---

### `POST /api/v1/auth/reset-password`

**Request**:
```json
{
  "token": "<reset-token>",
  "newPassword": "newS3cr3tP@ss!"
}
```

**Success 200**: `data: null`. All refresh tokens revoked. Redirect hint in `meta`.

**Errors**:
- `400 TOKEN_INVALID_OR_EXPIRED`
- `422 PASSWORD_TOO_WEAK`

---

### `GET /api/v1/auth/username/check`

Real-time username availability check.

**Query param**: `?username=ghasem.shirdel`

**Success 200**:
```json
{
  "data": {
    "available": true,
    "username": "ghasem.shirdel"
  }
}
```

---

### `GET /api/v1/auth/emergency-revoke`

Revokes all active sessions for the account associated with the token. Used via "this wasn't me" link in new-device alert emails. No authentication required.

**Query param**: `?token=<raw-emergency-revoke-token>`

**Success 200**: Redirect to `APP_BASE_URL/security/sessions-revoked`. All refresh tokens revoked. Token marked used.

**Errors**:
- `400 TOKEN_INVALID_OR_EXPIRED` — missing, expired, or already-used token

---

### `GET /api/v1/auth/oauth/{provider}`

Initiates OAuth code flow. `provider` = `github` | `google`.

**Response**: 302 redirect to provider authorization URL.

---

### `GET /api/v1/auth/oauth/{provider}/callback`

OAuth callback. Handled server-side; sets refresh token cookie; redirects to frontend.

**On new account**: Username is auto-generated from the OAuth email prefix (strip non-`[a-z0-9_-]`, truncate to 30 chars, numeric suffix if taken). User can change it later via `PATCH /api/v1/users/me/username`.

**On success**: 302 → `APP_BASE_URL/auth/oauth-success?access_token=<jwt>&expires_in=900`
**On error**: 302 → `APP_BASE_URL/sign-in?error=oauth_failed`

---

### `POST /api/v1/auth/passkeys/authenticate/begin`

Begin passkey authentication (no session required — unauthenticated login).

**Request**:
```json
{ "username": "ghasem.shirdel" }
```
`username` optional — omit for discoverable credential (resident key) flow.

**Success 200**: WebAuthn `PublicKeyCredentialRequestOptions` (challenge, rpId, allowCredentials, timeout).
Challenge stored in signed HttpOnly cookie (`webauthn_challenge`).

---

### `POST /api/v1/auth/passkeys/authenticate/complete`

**Request**: WebAuthn `AuthenticatorAssertionResponse` JSON.

**Success 200**: `accessToken` + refresh cookie. Same as login success.

**Errors**:
- `401 PASSKEY_ASSERTION_FAILED` — invalid signature or sign count
- `400 CHALLENGE_EXPIRED` — challenge cookie missing or expired

---

## Protected Endpoints (JWT required — `Authorization: Bearer <token>`)

### `GET /api/v1/users/me`

Returns authenticated user's full profile.

**Success 200**:
```json
{
  "data": {
    "id": 42,
    "username": "ghasem.shirdel",
    "email": "user@example.com",
    "role": "PARTICIPANT",
    "emailVerified": true,
    "totpEnabled": false,
    "profile": {
      "displayName": "Ghasem",
      "firstName": "Ghasem",
      "lastName": "Shirdel",
      "birthdate": "1995-06-15",
      "avatarUrl": "https://...",
      "location": "Tehran, Iran",
      "githubUrl": "https://github.com/ghasemdev",
      "linkedinUrl": null,
      "twitterUrl": null,
      "websiteUrl": null
    }
  }
}
```

---

### `PATCH /api/v1/users/me/profile`

Update editable profile fields.

**Request** (all fields optional):
```json
{
  "displayName": "Ghasem",
  "firstName": "Ghasem",
  "lastName": "Shirdel",
  "birthdate": "1995-06-15",
  "location": "Tehran, Iran",
  "avatarUrl": "https://cdn.kodex.dev/avatars/42.jpg",
  "githubUrl": "https://github.com/ghasemdev",
  "linkedinUrl": null,
  "twitterUrl": null,
  "websiteUrl": null
}
```

**Success 200**: Updated `UserProfileDto`.

**Errors**:
- `422 INVALID_URL` — social link not a valid URL

---

### `POST /api/v1/users/me/avatar`

Upload a profile picture. Replaces any existing avatar.

**Request**: `multipart/form-data` — field name `file`, max 5 MB, accepted types: `image/jpeg`, `image/png`, `image/webp`.

**Success 200**:
```json
{
  "data": {
    "avatarUrl": "http://minio.kodex.dev/avatars/42.jpg"
  }
}
```

**Errors**:
- `413 FILE_TOO_LARGE` — exceeds 5 MB
- `415 UNSUPPORTED_MEDIA_TYPE` — MIME type or magic bytes do not match an accepted image type

---

### `PATCH /api/v1/users/me/username`

Change the account's username.

**Request**:
```json
{ "username": "new_handle" }
```

**Success 200**: Updated `UserDto`.

**Errors**:
- `422 VALIDATION_ERROR` — invalid format (must match `[a-z0-9_-]`, 3–30 chars)
- `409 USERNAME_TAKEN` — already in use

---

### `PATCH /api/v1/users/me/password`

Change password (requires current password).

**Request**:
```json
{
  "currentPassword": "oldP@ss",
  "newPassword": "newS3cr3t!"
}
```

**Success 200**: `data: null`. All other refresh tokens revoked.

**Errors**:
- `401 WRONG_PASSWORD`
- `422 PASSWORD_TOO_WEAK`

---

### `PATCH /api/v1/users/me/email`

Request email address change.

**Request**:
```json
{
  "newEmail": "new@example.com",
  "password": "currentP@ss"
}
```

**Success 200**: `data: null`. Verification email sent to `newEmail`.

---

### `POST /api/v1/users/me/email/verify`

Complete email change after clicking verification link.

**Request**:
```json
{ "token": "<email-change-token>" }
```

**Success 200**: `data: null`. Email updated. All refresh tokens revoked.

---

### `GET /api/v1/users/me/sessions`

List all active sessions.

**Success 200**:
```json
{
  "data": [
    {
      "id": "101",
      "deviceHint": "Chrome / macOS",
      "ipAddress": "1.2.3.4",
      "issuedAt": "2026-06-01T10:00:00Z",
      "expiresAt": "2026-06-08T10:00:00Z",
      "isCurrent": true
    }
  ]
}
```

---

### `DELETE /api/v1/users/me/sessions/{sessionId}`

Revoke a specific session.

**Success 200**: `data: null`.

---

### `DELETE /api/v1/users/me/sessions`

Revoke all sessions except current.

**Success 200**: `data: { "revokedCount": 3 }`.

---

### `GET /api/v1/users/me/oauth`

List linked OAuth providers.

**Success 200**:
```json
{
  "data": [
    { "provider": "GITHUB", "linkedAt": "2026-05-01T..." }
  ]
}
```

---

### `DELETE /api/v1/users/me/oauth/{provider}`

Unlink an OAuth provider.

**Success 200**: `data: null`.

**Errors**:
- `409 LAST_AUTH_METHOD` — cannot unlink; no other auth method exists

---

### `POST /api/v1/users/me/2fa/setup`

Initiate TOTP 2FA setup.

**Success 200**:
```json
{
  "data": {
    "secret": "BASE32SECRET",
    "otpAuthUri": "otpauth://totp/KodEx:ghasem.shirdel?secret=...&issuer=KodEx",
    "qrCodeDataUri": "data:image/png;base64,..."
  }
}
```
Secret is stored encrypted immediately; `enabled=false` until confirmation.

---

### `POST /api/v1/users/me/2fa/confirm`

Confirm TOTP setup with a live code from the authenticator app.

**Request**:
```json
{ "code": "123456" }
```

**Success 200**:
```json
{
  "data": {
    "recoveryCodes": [
      "AAAA-BBBB", "CCCC-DDDD", "EEEE-FFFF", "GGGG-HHHH",
      "IIII-JJJJ", "KKKK-LLLL", "MMMM-NNNN", "OOOO-PPPP"
    ]
  }
}
```
Recovery codes shown once only. 2FA is now enabled.

---

### `DELETE /api/v1/users/me/2fa`

Disable TOTP 2FA.

**Request**:
```json
{
  "password": "currentP@ss",
  "totpCode": "123456"
}
```

**Success 200**: `data: null`.

---

### `POST /api/v1/users/me/passkeys/register/begin`

Begin passkey registration for logged-in user.

**Success 200**: `PublicKeyCredentialCreationOptions` JSON. Challenge in signed cookie.

---

### `POST /api/v1/users/me/passkeys/register/complete`

**Request**: `AuthenticatorAttestationResponse` JSON + optional `friendlyName`.

**Success 201**: `PasskeyDto` — `{ id, friendlyName, aaguid, createdAt }`.

---

### `GET /api/v1/users/me/passkeys`

List registered passkeys.

**Success 200**: `data: PasskeyDto[]`

---

### `DELETE /api/v1/users/me/passkeys/{passkeyId}`

Remove a registered passkey.

**Success 200**: `data: null`.

**Errors**:
- `409 LAST_AUTH_METHOD` — only passkey and no password/OAuth

---

## Public Profile

### `GET /api/v1/users/{username}`

Public profile (no auth required).

**Success 200**: Subset of `UserDto` — `username`, `displayName`, `avatarUrl`, `location`, social links, `role`, `joinedAt`. No email. No sensitive fields.

**Errors**:
- `404 USER_NOT_FOUND`
