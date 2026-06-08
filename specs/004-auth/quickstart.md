# Quick-Start: Auth Feature (Spec 004)

**Branch**: `feature/004-auth`

---

## New Environment Variables

Add these to `.env` (and `.env.example` with placeholder values):

```dotenv
# OAuth — GitHub
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret

# OAuth — Google
GOOGLE_CLIENT_ID=your_google_client_id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your_google_client_secret

# OAuth callback base (must match registered redirect URI)
OAUTH_CALLBACK_BASE_URL=http://localhost:8080

# Cloudflare Turnstile (use test keys for local dev — always pass)
# Test site key: 1x00000000000000000000AA  Test secret: 1x0000000000000000000000000000000AA
CLOUDFLARE_TURNSTILE_SITE_KEY=1x00000000000000000000AA
CLOUDFLARE_TURNSTILE_SECRET=1x0000000000000000000000000000000AA

# Resend transactional email
RESEND_API_KEY=re_your_api_key
EMAIL_FROM=noreply@kodex.dev

# App base URL (for link construction in emails)
APP_BASE_URL=http://localhost:5173

# TOTP secret encryption key — generate with: openssl rand -base64 32
TOTP_ENCRYPTION_KEY=base64encodedAESKey=

# WebAuthn challenge cookie signing key — generate with: openssl rand -base64 32
WEBAUTHN_CHALLENGE_KEY=base64encodedAESKey=

# GeoIP database path (mounted in Docker Compose)
GEOIP_DB_PATH=/data/GeoLite2-City.mmdb

# Redis (per-IP rate limiting)
# Production: set REDIS_PASSWORD and use redis://:${REDIS_PASSWORD}@redis:6379
REDIS_URL=redis://localhost:6379
REDIS_PASSWORD=

# MinIO (avatar object storage)
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET_AVATARS=avatars
MINIO_PUBLIC_URL=http://localhost:9000
```

## Local Development Setup

### 1. Download GeoIP Database

```bash
mkdir -p infra/geoip
curl -L "https://cdn.jsdelivr.net/npm/geolite2-city/GeoLite2-City.mmdb.gz" \
  | gunzip > infra/geoip/GeoLite2-City.mmdb
```

### 2. Register OAuth Apps

**GitHub**:
- Go to GitHub → Settings → Developer settings → OAuth Apps → New OAuth App
- Authorization callback URL: `http://localhost:8080/api/v1/auth/oauth/github/callback`

**Google**:
- Go to Google Cloud Console → APIs & Services → Credentials → Create OAuth 2.0 Client ID
- Authorized redirect URI: `http://localhost:8080/api/v1/auth/oauth/google/callback`

### 3. Get Cloudflare Turnstile Keys

For local dev, use the always-pass test keys above (no Cloudflare account needed).
For production: dashboard.cloudflare.com → Turnstile → Add Site.

### 4. Start the Stack

```bash
docker compose up --build
```

The `server/data` module's Flyway migration `V3__auth_schema.sql` runs automatically on startup.

---

## New Catalog Entries (`gradle/libs.versions.toml`)

Add to `[versions]`:
```toml
ua-parser              = "1.6.1"
webauthn4j             = "0.31.6.RELEASE"
kotlin-onetimepassword = "3.0.0"
zxcvbn4j               = "1.9.0"
geoip2                 = "5.1.0"
resend                 = "4.14.1"
lettuce                = "7.6.0.RELEASE"
minio                  = "9.0.1"
```

Add to `[libraries]`:
```toml
ua-parser-java         = { module = "com.github.ua-parser:uap-java",              version.ref = "ua-parser" }
webauthn4j-core        = { module = "com.webauthn4j:webauthn4j-core",             version.ref = "webauthn4j" }
kotlin-onetimepassword-lib = { module = "dev.turingcomplete:kotlin-onetimepassword", version.ref = "kotlin-onetimepassword" }
zxcvbn4j-lib           = { module = "com.nulab-inc:zxcvbn",                       version.ref = "zxcvbn4j" }
geoip2-lib             = { module = "com.maxmind.geoip2:geoip2",                  version.ref = "geoip2" }
resend-java            = { module = "com.resend:resend-java",                      version.ref = "resend" }
lettuce-core           = { module = "io.lettuce:lettuce-core",                     version.ref = "lettuce" }
minio-sdk              = { module = "io.minio:minio",                              version.ref = "minio" }
# kotlinx-coroutines-reactive — already uses existing "coroutines" version ref:
kotlinx-coroutines-reactive = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-reactive", version.ref = "coroutines" }
```

Add npm dependency to `app/webApp/build.gradle.kts` (inside `jsMain` + `wasmJsMain`):
```kotlin
implementation(npm("zxcvbn-ts", "3.0.4"))
```

---

## API Smoke-Test (curl)

```bash
# 1. Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"Str0ng!Pass#99","turnstileToken":"XXXX.DUMMY.TOKEN.XXXX"}'

# 2. Login
curl -c cookies.txt -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Str0ng!Pass#99"}'

# 3. Refresh (uses refresh_token cookie)
curl -c cookies.txt -b cookies.txt -X POST http://localhost:8080/api/v1/auth/refresh

# 4. Get own profile
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer <access_token_from_step_2>"

# 5. Logout
curl -b cookies.txt -X POST http://localhost:8080/api/v1/auth/logout
```

---

## Running Tests

```bash
# Unit + integration tests (Testcontainers spins up PostgreSQL automatically)
./gradlew :server:api:test :server:domain:test :server:data:test

# Frontend tests
./gradlew :app:webApp:jsTest

# Screenshot tests (requires CHROME_BIN)
CHROME_BIN="/path/to/chrome" ./gradlew :app:webApp:jsBrowserTest
```

---

## Flyway Migration

New migration: `server/data/src/main/resources/db/migration/V3__auth_schema.sql`

Applied automatically on server startup. To apply manually:
```bash
./gradlew :server:data:flywayMigrate
```
