# Memory Index

Compact routing map for durable project memory (`docs/memory/`). Keep it short.

> [!NOTE]
> Governance layer (constitution, security, principles) is at `.specify/memory/`.
> Read `.specify/memory/workflow.md` for the memory-first workflow.

## Architecture

| ID | Title | File | Status |
|----|-------|------|--------|
| A1 | Type-safe project accessors mandatory — `project(":x")` string literals prohibited | [ARCHITECTURE.md](ARCHITECTURE.md) | Active |
| A2 | `devMain` source set — dev-only code excluded from production `installDist` | [ARCHITECTURE.md](ARCHITECTURE.md) | Active |
| A3 | Convention plugin composition — never add build config directly to module files | [ARCHITECTURE.md](ARCHITECTURE.md) | Active |
| A4 | Frontend XSS boundary — text-node-only rendering; `innerHTML` banned in design system components | [ARCHITECTURE.md](ARCHITECTURE.md) | Active |
| A5 | Ktor `HttpClient` must not have hardcoded base URL — Vite proxy `proxy("/api", "http://localhost:8080")` is required | [ARCHITECTURE.md](ARCHITECTURE.md) | Active |

## Bugs

| ID | Title | File | Status |
|----|-------|------|--------|
| B1 | Wrong docker-java catalog alias: use `libs.bundles.docker`, not `libs.docker.java` | [BUGS.md](BUGS.md) | Active |
| B2 | Kilua DOM event dispatch needs 80ms recomposition delay in tests — 10ms causes flaky null assertions | [BUGS.md](BUGS.md) | Active |

## Decisions

| ID | Title | File | Status |
|----|-------|------|--------|
| D1 | Use `kotlin.time.Instant` in Kotlin 2.3+ — `kotlinx.datetime.Instant` is deprecated | [DECISIONS.md](DECISIONS.md) | Active |
| D2 | kotlin-logging version must be `8.0.03`, not `8.0.0` (unpublished) | [DECISIONS.md](DECISIONS.md) | Active |
| D3 | CI benchmarks use `benchmarkFast` + `-PbenchmarkConfig=fast` — never `benchmark` in CI | [DECISIONS.md](DECISIONS.md) | Active |
| D4 | `detekt-convention` is the only correct way to wire Detekt — direct plugin application prohibited | [DECISIONS.md](DECISIONS.md) | Active |
| D7 | CSP must live in both `index.html` meta tag AND Ktor `DefaultHeaders` — Vite bypasses Ktor | [DECISIONS.md](DECISIONS.md) | Active |
| D10 | `ApiEnvelope<T>` / `ApiMeta` / `ApiErrorEnvelope` live in `core:models` — server keeps only builders | [DECISIONS.md](DECISIONS.md) | Active |
| D11 | Koin Annotations `@Single`/`@Factory` + Compiler Plugin (`io.insert-koin.compiler.plugin:1.0.0`) for all DI | [DECISIONS.md](DECISIONS.md) | Active |
| D12 | Repository → RemoteDataSource separation: repo selects source + maps domain; DS handles transport | [DECISIONS.md](DECISIONS.md) | Active |
| D13 | `_field`/`val field` StateFlow double-property is idiomatic in Kotlin 2.3.21; KEEP-0068 not stable | [DECISIONS.md](DECISIONS.md) | Active |
| D14 | API route constants in `object ApiRoutes` — no inline path strings in call sites | [DECISIONS.md](DECISIONS.md) | Active |
| D15 | `sanitizeRequestId()` utility is the only correct X-Request-Id reflection pattern — inline `?: Uuid.random()` is the vulnerable form | [DECISIONS.md](DECISIONS.md) | Active |

## Security Reviews

| File | Type | Date | Risk | Counts | OWASP |
|------|------|------|------|--------|-------|
| [docs/security-reviews/2026-05-28-feature-002-design-system-branch.md](../security-reviews/2026-05-28-feature-002-design-system-branch.md) | branch | 2026-05-28 | MODERATE | C:0 H:0 M:1 L:2 | A03,A05,A06 |
| [docs/security-reviews/2026-05-28-feature-002-design-system-followup.md](../security-reviews/2026-05-28-feature-002-design-system-followup.md) | followup | 2026-05-28 | MODERATE | C:0 H:0 M:1 L:2 | A03,A05,A06 |
| [docs/security-reviews/2026-05-29-feature-002-design-system-whitebox.md](../security-reviews/2026-05-29-feature-002-design-system-whitebox.md) | whitebox | 2026-05-29 | LOW | C:0 H:0 M:1 L:2 I:2 resolved:3 deferred:2 | A03,A05,A06 |
| [docs/security-reviews/2026-06-07-feature-003-landing-page.md](../security-reviews/2026-06-07-feature-003-landing-page.md) | branch | 2026-06-07 | MODERATE | C:0 H:0 M:3 L:1 | A03,A05 |
| [docs/security-reviews/2026-06-07-feature-003-landing-page-followup.md](../security-reviews/2026-06-07-feature-003-landing-page-followup.md) | followup | 2026-06-07 | MODERATE | C:0 H:0 M:3 L:1 | A03,A05 |
| [docs/security-reviews/2026-06-07-feature-003-landing-page-whitebox.md](../security-reviews/2026-06-07-feature-003-landing-page-whitebox.md) | whitebox | 2026-06-07 | LOW | C:0 H:0 M:3 L:1 resolved:3 deferred:1 | A03,A05 |
| [docs/security-reviews/2026-06-09-feature-004-auth-plan.md](../security-reviews/2026-06-09-feature-004-auth-plan.md) | plan | 2026-06-09 | HIGH | C:0 H:2 M:5 L:2 I:2 | A01,A02,A05,A07 |

## Workflow

| ID | Title | File | Status |
|----|-------|------|--------|
| W4 | Feature 003 complete — landing page, tests, security hardening, Vite proxy fix | [WORKLOG.md](WORKLOG.md) | Active |
| W3 | Feature 002 complete — design system, i18n, screenshot tests, security hardening | [WORKLOG.md](WORKLOG.md) | Active |
| W1 | Feature 001 complete — KodEx project foundation | [WORKLOG.md](WORKLOG.md) | Active |
| W2 | Build system & CI/CD refactor — convention composition + benchmark fast mode | [WORKLOG.md](WORKLOG.md) | Active |
