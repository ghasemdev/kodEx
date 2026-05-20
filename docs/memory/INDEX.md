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

## Bugs

| ID | Title | File | Status |
|----|-------|------|--------|
| B1 | Wrong docker-java catalog alias: use `libs.bundles.docker`, not `libs.docker.java` | [BUGS.md](BUGS.md) | Active |

## Decisions

| ID | Title | File | Status |
|----|-------|------|--------|
| D1 | Use `kotlin.time.Instant` in Kotlin 2.3+ — `kotlinx.datetime.Instant` is deprecated | [DECISIONS.md](DECISIONS.md) | Active |
| D2 | kotlin-logging version must be `8.0.03`, not `8.0.0` (unpublished) | [DECISIONS.md](DECISIONS.md) | Active |

## Workflow

| ID | Title | File | Status |
|----|-------|------|--------|
| W1 | Feature 001 complete — KodEx project foundation | [WORKLOG.md](WORKLOG.md) | Active |
