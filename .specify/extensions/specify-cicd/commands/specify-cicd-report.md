---
description: "Generate Markdown report of local CI/CD execution results"
---

# Generate Local CI/CD Report

## Behavior

Produces a human-readable Markdown report summarizing local CI/CD pipeline execution.

## Execution

Run the report script:

```bash
.specify/extensions/specify-cicd/scripts/bash/cicd-report.sh [--output path/to/report.md]
```

## Output format

Generates a Markdown document with:

```markdown
# Local CI/CD Report

**Project**: kodEx
**Branch**: feature/002-cicd-extension
**Timestamp**: 2026-05-23T17:30:45+03:00
**Pipeline Result**: ❌ FAILED (2/5 passed, 1 warn, 2 skipped)

## Summary

| Step    | Status  | Exit | Duration | Diff-Bounded |
|---------|---------|------|----------|--------------|
| assemble| ✅ PASS | 0    | 45s      | ✅ matched   |
| lint    | ✅ PASS | 0    | 22s      | ✅ matched   |
| test    | ❌ FAIL | 1    | 120s     | ⏭️ skipped   |
| coverage| ⏭️ SKIP | —    | —        | ⏭️ not run   |
| benchmark| ⏭️ SKIP| —    | —        | ⏭️ not run   |

## Diff-Bounded Analysis

Changed files matching step patterns:
- `src/main/kotlin/com/kodex/server/api/Envelope.kt` → assemble ✅
- `build.gradle.kts` → assemble ✅
- `src/test/kotlin/com/kodex/server/api/EnvelopeTest.kt` → test (excluded) ⏭️

## Failures

### test (FAIL)
Command: `./gradlew test jvmTest --no-daemon`
Exit code: 1
Duration: 120s

Log: [test-20260523-173045.log](../logs/test-20260523-173045.log)

**Errors**:
- `EnvelopeTest > buildErrorEnvelope_withNestedException FAILED`

## Warnings

- No warnings detected.

## Recommendations

1. Fix failing test: `EnvelopeTest > buildErrorEnvelope_withNestedException`
2. Commit fix and re-run: `./speckit.cicd.check --step test`
```

## Logging references

The report includes clickable links to per-step logs:
```
.specify/extensions/specify-cicd/logs/<step_name>-<YYYYMMDDHHmmss>.log
```

## Graceful Degradation

- If no previous run logs exist → generates empty report with instructions
- If report was already generated → appends a "Second Run" section with diff comparison

Base directory for this skill: file:///Users/part/Projects/Kotlin/kodEx/.claude/skills/speckit-cicd-report
Relative paths in this skill (e.g., scripts/, reference/) are relative to this base directory.
