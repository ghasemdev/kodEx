---
description: "Run local CI/CD pipeline and report results step-by-step"
---

# Run Local CI/CD Check

## Behavior

Executes the locally extracted CI/CD pipeline step-by-step, captures output, and reports pass/fail per step.

## Execution

Run the script:

```bash
.specify/extensions/specify-cicd/scripts/bash/cicd-run.sh [--verbose] [--fail-fast] [--diff-bounded]

## Flags

- `--verbose` — show real-time output for each step
- `--fail-fast` — stop on first failure (default; set `--continue` to override)
- `--continue` — continue after failure to collect all results
- `--diff-bounded` — only run steps whose file patterns match changed files
- `--step <name>` — run only the named step (e.g., `--step lint`)
- `--dry-run` — show what would execute without running

## Config

Reads `.specify/extensions/specify-cicd/cicd-config.yaml`.
If the config does not exist, runs `speckit-cicd-setup` automatically.

## Output

For each step:
- `✅ <step_name>: PASS (<duration>)`
- `❌ <step_name>: FAIL (exit <code>)`
- `⚠️ <step_name>: WARN (exit <code>)`
- `⏭️ <step_name>: SKIPPED (diff-bounded, no matching files)`

On failure, prints a summary:
```
Pipeline: FAILED (2/5 passed, 3 skipped)
────────────────────────────────────────────────────────
Step          Status    Exit  Duration    Diff-Bounded
────────────────────────────────────────────────────────
assemble      ✅ PASS   0     45s           ✅ matched
lint          ✅ PASS   0     22s           ✅ matched
test          ❌ FAIL   1     120s          ⏭️ skipped
coverage      ⏭️ SKIP   —     —             ⏭️ not run
benchmark     ⏭️ SKIP   —     —             ⏭️ not run
────────────────────────────────────────────────────────
Failure: test exited with code 1
Log: .specify/extensions/specify-cicd/logs/test-<timestamp>.log
```

## Graceful Degradation

- If no CI config exists → runs `cicd-setup` automatically
- If a step's `cmd` depends on tools not installed (gradle, make, cargo) → SKIPPED with note
- If `cicd-config.yaml` specifies steps but no `sources` are configured → uses steps as-is
- Step timeout kills the process and reports TIMEOUT

## Logging

Each step's full stdout/stderr is written to:
```
.specify/extensions/specify-cicd/logs/<step_name>-<YYYYMMDD-HHMMSS>.log
```

Base directory for this skill: file:///Users/part/Projects/Kotlin/kodEx/.claude/skills/speckit-cicd-run
Relative paths in this skill (e.g., scripts/, reference/) are relative to this base directory.
