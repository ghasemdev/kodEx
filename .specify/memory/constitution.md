<!--
SYNC IMPACT REPORT
==================
Version change: 1.4.0 → 1.5.0
Type of bump: MINOR (unified API response envelope, bilingual error messages, DELETE behavior change)

Modified principles:
  - §IX (API Design Conventions): replaced flat error schema + "no envelope" rule
    with a unified success/error envelope; changed DELETE from 204 to 200 + data:null;
    added bilingual userMessage (en/fa) in errors; moved pagination into meta.pagination

Structural change (1.5.0):
  - Split monolithic constitution.md into per-principle files under principles/
  - This file is now the index + governance only; read individual files for full detail

Remaining TODOs:
  - TODO(RATIFICATION_DATE): Confirm exact project start date if different from 2026-05-17
  - TODO(REDIS_USE_CASE): Redis confirmed for refresh tokens + session cache; decide if also used for submission queue/leaderboard before data-model spec
-->

# KodEx Constitution

**Version**: 1.5.0 | **Ratified**: 2026-05-17 | **Last Amended**: 2026-05-19

---

## Quick Reference

| § | Principle | Key Rule | File |
|---|---|---|---|
| I | Kotlin-First Stack | All code in Kotlin. Ktor + Kilua + KMP shared module. | [i-kotlin-first.md](principles/i-kotlin-first.md) |
| II | Dual Exam Modes | Kotlin Mode + Android Mode only. Static + dynamic test cases. | [ii-dual-exam-modes.md](principles/ii-dual-exam-modes.md) |
| III | Secure Sandbox | Isolated Docker container. `sandbox-runner` is the only service with Docker socket access. | [iii-secure-sandbox.md](principles/iii-secure-sandbox.md) |
| IV | Test-Injection Grading | Grading server-side only. Injected tests hidden from participant. Scoring never in sandbox. | [iv-test-injection-grading.md](principles/iv-test-injection-grading.md) |
| V | Role-Based Domain Model | Admin + Participant roles. Exam states: DRAFT → PUBLISHED → CLOSED (terminal). | [v-role-based-domain-model.md](principles/v-role-based-domain-model.md) |
| VI | Auditability | Every submission event logged with correlation ID. Audit logs immutable in prod. | [vi-auditability.md](principles/vi-auditability.md) |
| VII | Architecture | Clean Arch backend (`:api` → `:domain` ← `:data`). MVI frontend. Detekt on all modules. | [vii-architecture.md](principles/vii-architecture.md) |
| VIII | Auth & AuthZ | JWT (15 min access / 7 day refresh in Redis). Argon2id for passwords. 401/403 enforced at API layer. | [viii-authentication.md](principles/viii-authentication.md) |
| IX | API Design | `/api/v1/` prefix. Unified `{data, meta}` envelope on every response. DELETE = 200 + `data:null`. Bilingual `userMessage`. SSE for async grading. | [ix-api-design.md](principles/ix-api-design.md) |
| X | Testing Policy | Kotest (JVM) + kotlin.test (shared). No cross-layer mocks. Testcontainers for DB/Redis. | [x-testing-policy.md](principles/x-testing-policy.md) |

**Supporting documents**:
- [tech-stack.md](tech-stack.md) — Library choices with links for all modules
- [security.md](security.md) — Execution constraints, CORS, secrets & environment policy

---

## Governance

This constitution supersedes all informal decisions, README notes, and verbal agreements.
Any principle in this document takes precedence over implementation convenience.

**Amendment procedure**:
1. Open a pull request with the proposed change and a written rationale.
2. Increment the version according to semantic versioning (see below).
3. Update `Last Amended` date to the merge date.
4. The PR description MUST include a Sync Impact Report covering affected templates and
   follow-up TODOs.
5. Merging the PR constitutes ratification; no separate approval step is required for a
   single-maintainer project (revisit when the team grows).

**Compliance review**: Every feature plan (`plan.md`) MUST include a "Constitution Check"
section that explicitly gates the plan against all active principles before implementation begins.
A feature plan that skips or voids this gate MUST NOT be merged.

**Versioning policy**:
- MAJOR: Removing or fundamentally redefining an existing principle.
- MINOR: Adding a new principle or materially expanding guidance.
- PATCH: Clarifications, wording fixes, non-semantic refinements.
