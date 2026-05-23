# Specify Local CI/CD

> A [Spec-Kit](https://github.com/ghasemdev/spec-kit) extension that runs your CI/CD pipeline locally — before you push.

Parses your existing GitHub Actions, GitLab CI, CircleCI, or Bitbucket Pipelines config and executes each step on your machine. Supports diff-bounded execution (skip steps unaffected by your changes), per-step pass/fail reporting, timestamped logs, and Markdown reports.

---

## Features

- **Auto-extracted steps** — reads your existing CI workflow files; no duplicate config
- **Diff-bounded execution** — skips steps when no matching files changed
- **Session-based logs** — each run produces a dated folder (`logs/2026-05-23_21-40-15/`) with one file per step
- **Markdown reports** — saved to `docs/cicd/cicd-report.<date>.md` for tracking and sharing
- **Dry-run mode** — preview what would execute without running anything
- **Fail-fast or continue** — configurable pipeline behavior on failure
- **Pre-push gate** — optional hook to block `git push` until the pipeline passes

---

## Requirements

| Tool | Required |
|---|---|
| `bash` | ✅ |
| `python3` + `pyyaml` | ✅ |
| `gradle` / `make` / `cargo` / `npm` | Only if your steps use them |

---

## Installation

```bash
bash .specify/extensions/cicd/scripts/bash/install.sh
```

This registers four Claude Code skills by symlinking them into `.claude/skills/`. Use `--copy` instead of symlinking (useful in CI environments):

```bash
bash .specify/extensions/cicd/scripts/bash/install.sh --copy
```

To uninstall:

```bash
bash .specify/extensions/cicd/scripts/bash/uninstall.sh
```

---

## Commands

After installing, four slash commands are available in Claude Code:

| Command | Description |
|---|---|
| `/speckit-cicd-setup` | Scan CI workflow files and generate `cicd-config.yaml` |
| `/speckit-cicd-run` | Execute the pipeline locally, step by step |
| `/speckit-cicd-dry-run` | Preview which steps would run without executing them |
| `/speckit-cicd-report` | Generate a Markdown report from the last run |

---

## Quick Start

**1. Generate config from your existing CI workflow:**

```
/speckit-cicd-setup
```

Detects `.github/workflows/*.yml`, `.gitlab-ci.yml`, `.circleci/config.yml`, or `bitbucket-pipelines.yml` and produces `.specify/extensions/cicd/cicd-config.yaml`.

**2. Preview what would run:**

```
/speckit-cicd-dry-run
```

```
Local CI/CD Pipeline Simulation
─────────────────────────────────────────────────────────
Step       Command                              Diff-Bounded   Status
─────────────────────────────────────────────────────────
assemble   ./gradlew assemble --no-daemon       ✅ matched     would run
lint       ./gradlew detekt                     ✅ matched     would run
test       ./gradlew test jvmTest --no-daemon   ⏭️ excluded    would skip
coverage   ./gradlew koverVerify --no-daemon    ⏭️ excluded    would skip
benchmark  ./gradlew fastBenchmark --no-daemon  ⏭️ excluded    would skip
─────────────────────────────────────────────────────────
Total: 5 steps — 2 would run, 3 would skip (diff-bounded)
```

**3. Run the pipeline:**

```
/speckit-cicd-run
```

```
=== Specify-CICD: Local Pipeline Check ===

  🏃  assemble... ✅ PASS (10s)
  🏃  lint...     ✅ PASS (9s)
  🏃  test...     ✅ PASS (45s)

────────────────────────────────────────────────────────────
Pipeline Result: ✅ PASSED (3/3 passed)
────────────────────────────────────────────────────────────
Logs: .specify/extensions/cicd/logs/2026-05-23_21-40-15/
```

**4. Generate a report:**

```
/speckit-cicd-report
```

Writes `docs/cicd/cicd-report.2026-05-23_21-40.md`.

---

## Configuration

`.specify/extensions/cicd/cicd-config.yaml` controls everything:

```yaml
pipeline:
  steps:
    - name: lint
      enabled: true
      cmd: ./gradlew detekt --no-daemon
      timeout_seconds: 300
      pass_codes: [0]
      warn_codes: []
      diff_bounded:
        enabled: true
        patterns:
          - "**/*.kt"
          - "**/*.kts"

execution:
  fail_fast: true       # stop on first failure
  verbose: true         # show real-time step output
  env:
    GRADLE_OPTS: "-Dorg.gradle.daemon=false"
```

### Step fields

| Field | Default | Description |
|---|---|---|
| `name` | — | Step name (used in logs and reports) |
| `cmd` | — | Shell command to execute |
| `enabled` | `true` | Set `false` to skip permanently |
| `timeout_seconds` | `300` | Kill step after this many seconds |
| `pass_codes` | `[0]` | Exit codes treated as success |
| `warn_codes` | `[]` | Exit codes treated as warning (non-fatal) |
| `diff_bounded.enabled` | `false` | Only run if changed files match patterns |
| `diff_bounded.patterns` | `[]` | Glob patterns to match against `git diff` |

### Supported CI providers

| Provider | Config file |
|---|---|
| GitHub Actions | `.github/workflows/*.yml` |
| GitLab CI | `.gitlab-ci.yml` |
| CircleCI | `.circleci/config.yml` |
| Bitbucket Pipelines | `bitbucket-pipelines.yml` |

---

## Log Structure

Each run creates a timestamped session directory:

```
.specify/extensions/cicd/logs/
└── 2026-05-23_21-40-15/
    ├── assemble.log
    ├── lint.log
    ├── test.log
    └── benchmark.log
```

---

## Report Structure

Reports are saved as versioned Markdown files:

```
docs/cicd/
└── cicd-report.2026-05-23_21-40.md
```

Each report contains a step summary table, failure details (if any), log paths, and recommendations.

---

## `/speckit-cicd-run` Flags

You can also invoke the script directly:

```bash
.specify/extensions/cicd/scripts/bash/cicd-run.sh [flags]
```

| Flag | Description |
|---|---|
| `--verbose` | Show real-time output per step |
| `--fail-fast` | Stop on first failure (default) |
| `--continue` | Run all steps even if some fail |
| `--diff-bounded` | Only run steps matching changed files |
| `--step <name>` | Run a single named step |
| `--dry-run` | Preview without executing |

---

## Pre-push Gate (optional)

To block `git push` until the pipeline passes, add to your hooks config:

```yaml
hooks:
  before_push:
    command: "speckit.cicd.run"
    optional: true
    prompt: "Run local CI/CD checks before pushing?"
```

---

## Project Structure

```
.specify/extensions/cicd/
├── extension.yml               # Extension manifest
├── cicd-config.yaml            # Active pipeline config (generated or manual)
├── config-template.yaml        # Template for new projects
├── commands/                   # Claude Code command definitions
│   ├── specify-cicd-setup.md
│   ├── specify-cicd-run.md
│   ├── specify-cicd-dry-run.md
│   └── specify-cicd-report.md
├── skills/                     # Skill definitions (installed to .claude/skills/)
│   ├── speckit-cicd-setup/
│   ├── speckit-cicd-run/
│   ├── speckit-cicd-dry-run/
│   └── speckit-cicd-report/
└── scripts/bash/
    ├── install.sh
    ├── uninstall.sh
    ├── cicd-setup.sh
    ├── cicd-run.sh
    ├── cicd-dry-run.sh
    └── cicd-report.sh
```

---

## License

MIT © [ghasemdev](https://github.com/ghasemdev)
