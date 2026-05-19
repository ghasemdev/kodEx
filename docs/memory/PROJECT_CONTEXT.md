# Project Context

Last reviewed: 2026-05-19

## Product / Service

KodEx is a competitive coding exam platform. Admins create and publish Kotlin/Android coding exams; participants submit solutions that are compiled and graded inside isolated Docker sandboxes. Results are delivered asynchronously via SSE.

## Key Constraints

- User-submitted code is **always untrusted** — executed only inside network-isolated Docker containers
- `sandbox-runner` is the only service with Docker socket access
- JWT access tokens expire in 15 min; refresh tokens are Redis-stored and revocable
- All secrets via environment variables — no defaults, no fallbacks in production
- Kotlin-only codebase: no Java source files, no non-Kotlin build scripts
- Exam state machine is irreversible: DRAFT → PUBLISHED → CLOSED (no reverse transitions)

## Important Domains

- **Exam management**: CRUD for exams, test cases, scoring rules (Admin only)
- **Submission pipeline**: receive → scan → sandbox → grade → SSE deliver
- **Auth**: JWT + Argon2id, two roles only (Admin / Participant)
- **Sandbox orchestration**: `sandbox-runner` service, Docker container lifecycle

## Current Priorities

- Phase 3 (MVP): get `docker compose up --build` working, browser shows "Hello KodEx", health endpoint returns envelope
- Gradle build system (Phase 2) complete — all convention plugins in place

## Keep Here

- Domain invariants (exam state machine, role definitions)
- Project-wide constraints that shape every feature
- Current MVP scope boundary

## Never Store Here

- Feature-specific acceptance criteria (→ specs/)
- Task lists (→ tasks.md)
- Library versions (→ .specify/memory/tech-stack.md)
