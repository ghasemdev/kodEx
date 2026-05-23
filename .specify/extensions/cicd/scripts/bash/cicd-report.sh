#!/usr/bin/env bash
# Specify-CICD: Generate Markdown report from last CI check run
#
# Usage: cicd-report.sh [--output path/to/report.md]

set -e

EXTENSION_DIR="$(CDPATH="" cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(CDPATH="" cd "$EXTENSION_DIR/../../../../../" && pwd)"
cd "$REPO_ROOT"

CONFIG_DIR="$REPO_ROOT/.specify/extensions/cicd"
SUMMARY_FILE="$CONFIG_DIR/.last-run-summary"
LOG_DIR="$CONFIG_DIR/logs"
LOG_SESSION_DIR=""
if [ -f "$CONFIG_DIR/.last-run-log-dir" ]; then
    LOG_SESSION_DIR=$(tr -d '[:space:]' < "$CONFIG_DIR/.last-run-log-dir")
fi
REPORT_DIR="$REPO_ROOT/docs/cicd"
REPORT_DATE=$(date +'%Y-%m-%d_%H-%M')
OUTPUT="${REPORT_DIR}/cicd-report.${REPORT_DATE}.md"

# Override output path
while [[ $# -gt 0 ]]; do
    case "$1" in
        --output) OUTPUT="$2"; shift 2 ;;
        *) OUTPUT="$1"; shift ;;  # positional = output path
    esac
done

mkdir -p "$(dirname "$OUTPUT")"

# ── Gather data ───────────────────────────────────────────────────────────────
BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")
TIMESTAMP=$(date -Iseconds 2>/dev/null || date +"%Y-%m-%dT%H:%M:%S%z")
REPO_NAME=$(basename "$REPO_ROOT")

PASS_COUNT=0
FAIL_COUNT=0
WARN_COUNT=0
SKIP_COUNT=0
TOTAL=0

declare -a RESULTS=()

if [ -f "$SUMMARY_FILE" ]; then
    while IFS='|' read -r step_name pass_fail duration; do
        TOTAL=$((TOTAL + 1))
        case "$pass_fail" in
            PASS) PASS_COUNT=$((PASS_COUNT + 1)); RESULTS+=("$step_name|✅ PASS|${duration}s") ;;
            FAIL) FAIL_COUNT=$((FAIL_COUNT + 1));  RESULTS+=("$step_name|❌ FAIL|${duration}s") ;;
            WARN) WARN_COUNT=$((WARN_COUNT + 1));  RESULTS+=("$step_name|⚠️  WARN|${duration}s") ;;
            *)    SKIP_COUNT=$((SKIP_COUNT + 1));  RESULTS+=("$step_name|⏭️  SKIP|—") ;;
        esac
    done < "$SUMMARY_FILE"
fi

PIPELINE_RESULT="✅ PASSED"
[ "$FAIL_COUNT" -gt 0 ] && PIPELINE_RESULT="❌ FAILED"
[ -z "$FAIL_COUNT" ] || [ "$FAIL_COUNT" -eq 0 ] && [ "$WARN_COUNT" -gt 0 ] && PIPELINE_RESULT="⚠️  WARNING"

# ── Build report ──────────────────────────────────────────────────────────────
cat > "$OUTPUT" << HEADER
# Local CI/CD Report — $REPO_NAME

**Repository**: $REPO_NAME
**Branch**: $BRANCH
**Timestamp**: $TIMESTAMP
**Pipeline Result**: $PIPELINE_RESULT

## Summary

| Step | Status | Duration |
|------|--------|----------|
HEADER

for r in "${RESULTS[@]}"; do
    IFS='|' read -r sname sstatus sduration <<< "$r"
    echo "| $sname | $sstatus | $sduration |" >> "$OUTPUT"
done

cat >> "$OUTPUT" << MIDDLEDLE

## Failures

MIDDLEDLE

if [ "$FAIL_COUNT" -eq 0 ]; then
    echo "No failures detected." >> "$OUTPUT"
else
    for r in "${RESULTS[@]}"; do
        IFS='|' read -r sname sstatus _ <<< "$r"
        if echo "$sstatus" | grep -q "FAIL"; then
            cat >> "$OUTPUT" << FAIL_BLOCK
### ❌ $sname

**Exit code**: Check log below

**Log**: [${sname} log](../../.specify/extensions/cicd/logs/$(echo "$sname" | tr ' ' '-'))

FAIL_BLOCK
        fi
    done
fi

cat >> "$OUTPUT" << FOOTER
## Warnings

$(if [ "$WARN_COUNT" -eq 0 ]; then echo "No warnings detected."; else echo "$WARN_COUNT warning(s) detected. Check logs for details."; fi)

## Logs

Step logs available in: \`${LOG_SESSION_DIR:-$LOG_DIR}\`

## Recommendations

1. Review failed steps above
2. Fix issues and re-run: \`/speckit-cicd-run\`
3. Commit fix and re-push to trigger remote CI

FOOTER

echo "Report written to: $OUTPUT"
