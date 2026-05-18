# Security & Execution Constraints

- Submitted code MUST be scanned for known dangerous patterns (e.g., `Runtime.exec`,
  `ProcessBuilder`, reflection-based class loading) before entering the sandbox. Suspicious
  submissions MUST be rejected with a clear error, not silently failed.
- The sandbox image MUST be rebuilt and re-audited whenever the base JDK or Android build
  tools version changes.
- Password storage: see §VIII (Argon2id required).
- JWT signing: see §VIII (256-bit minimum secret, env-var loaded).
- All inter-service communication (backend ↔ sandbox orchestrator) MUST occur over
  authenticated channels (shared secret header), never open HTTP.
- Rate limiting MUST be applied to the submission endpoint and the auth endpoints
  (login, token refresh). Limits are configurable per exam/environment.
- **CORS**: The API server MUST configure CORS to allow requests only from the known webapp
  origin. In development: `http://localhost:5173`. In production: the deployed webapp domain
  (injected via environment variable `WEBAPP_ORIGIN`). All other origins MUST be rejected.
  Credentials (cookies/auth headers) MUST be allowed on permitted origins.

## Secrets & Environment Policy

- Every secret (database passwords, JWT signing secret, Redis password, sandbox shared
  secret) MUST be supplied via **environment variables**. No secret may appear in source
  code, Gradle build files, or committed configuration files.
- A `.env.example` file MUST be committed to the repository listing every required
  environment variable with a placeholder value and a comment describing its purpose.
  The actual `.env` file MUST be in `.gitignore`.
- Production code MUST NOT provide default/fallback values for secrets. Missing secrets
  at startup MUST cause immediate application termination with a clear error message.
- Secrets MUST NOT be logged, even at DEBUG level.
