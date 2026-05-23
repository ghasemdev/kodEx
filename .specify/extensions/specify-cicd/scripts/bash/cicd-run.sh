#!/usr/bin/env bash
# Specify-CICD: Run local CI/CD pipeline
#
# Usage: cicd-check.sh [--verbose] [--fail-fast] [--continue] [--diff-bounded] [--step <name>]

EXTENSION_DIR="$(CDPATH="" cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(CDPATH="" cd "$EXTENSION_DIR/../../../../../" && pwd)"
cd "$REPO_ROOT"

CONFIG_DIR="$REPO_ROOT/.specify/extensions/specify-cicd"
CONFIG="$CONFIG_DIR/cicd-config.yaml"
LOG_DIR="$CONFIG_DIR/logs"
SUMMARY_FILE="$CONFIG_DIR/.last-run-summary"
mkdir -p "$LOG_DIR"

# Default values
VERBOSE=false
FAIL_FAST=true
CONTINUE=false
DIFF_BOUNDED_ONLY=false
FILTER_STEP=""

# Parse flags
while [[ $# -gt 0 ]]; do
    case "$1" in
        --verbose|-v) VERBOSE=true; shift ;;
        --fail-fast) FAIL_FAST=true; shift ;;
        --continue|-c) CONTINUE=true; FAIL_FAST=false; shift ;;
        --diff-bounded|-d) DIFF_BOUNDED_ONLY=true; shift ;;
        --step|-s) FILTER_STEP="$2"; shift 2 ;;
        --dry-run)
            exec "$EXTENSION_DIR/cicd-dry-run.sh" ;;
        *) echo "Unknown flag: $1"; exit 1 ;;
    esac
done

# Check config exists
if [ ! -f "$CONFIG" ]; then
    echo "❌ cicd-config.yaml not found."
    echo "Run setup first:"
    echo "  .specify/extensions/specify-cicd/scripts/bash/cicd-setup.sh"
    exit 1
fi

# ── Get changed files ─────────────────────────────────────────────────────────
changed_files=""
if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    changed_files=$(git diff --name-only HEAD 2>/dev/null || true)
fi

# ── Parse config with Python and execute ──────────────────────────────────────
python3 - "$CONFIG" "$LOG_DIR" "$SUMMARY_FILE" "$changed_files" "$FAIL_FAST" "$CONTINUE" "$DIFF_BOUNDED_ONLY" "$FILTER_STEP" "$VERBOSE" << 'PYEOF'
import yaml, sys, os, subprocess, time, re

config_path = sys.argv[1]
log_dir = sys.argv[2]
summary_file = sys.argv[3]
changed_files_str = sys.argv[4]
fail_fast = sys.argv[5].lower() == "true"
cont = sys.argv[6].lower() == "true"
diff_bounded_only = sys.argv[7].lower() == "true"
filter_step = sys.argv[8]
verbose = sys.argv[9].lower() == "true"

# Parse changed files
changed_files = [f.strip() for f in changed_files_str.split('\n') if f.strip()] if changed_files_str else []

def matches_pattern(path, patterns):
    """Check if a file matches any of the diff-bounded patterns."""
    if not patterns:
        return True
    for pat in patterns:
        # Simple glob matching
        if pat.startswith('**/'):
            pat = pat[3:]
        if '*' in pat:
            # Convert glob to regex
            regex = pat.replace('*', '.*')
            if re.search(regex, path, re.IGNORECASE):
                return True
        else:
            if path.endswith(pat) or pat in path:
                return True
    return False

def should_skip_diff_bounded(step):
    """Check if step should be skipped due to diff-bounded filtering."""
    if not diff_bounded_only:
        return False
    
    db = step.get('diff_bounded', {})
    if not db.get('enabled', False) and not diff_bounded_only:
        return False
    
    if not db.get('patterns'):
        return False
    
    # If no changed files, don't skip
    if not changed_files:
        return False
    
    # Check if any changed file matches the step's patterns
    for cf in changed_files:
        if matches_pattern(cf, db['patterns']):
            return False
    
    return True

# Load config
with open(config_path) as f:
    cfg = yaml.safe_load(f)

steps = cfg.get('pipeline', {}).get('steps', [])
execution = cfg.get('execution', {})
env_overrides = execution.get('env', {})

pass_count = 0
fail_count = 0
warn_count = 0
skip_count = 0
total = 0
results = []

print("=== Specify-CICD: Local Pipeline Check ===")
print()

for step in steps:
    name = step.get('name', 'unnamed')
    
    # Increment total for all steps
    total += 1
    
    # If filter_step is set, skip non-matching steps
    if filter_step and name != filter_step:
        if filter_step:  # Only show count when filtering
            skip_count += 1
        continue
    
    # Check if disabled
    if step.get('enabled', True) is False:
        skip_count += 1
        print(f"  ⏭️  {name}: DISABLED")
        continue
    
    # Check diff-bounded
    if should_skip_diff_bounded(step):
        skip_count += 1
        print(f"  ⏭️  {name}: SKIPPED (diff-bounded, no matching file changes)")
        continue
    
    # Get command
    cmd = step.get('cmd', step.get('command', ''))
    if not cmd:
        # Try to extract from workflow if no command specified
        cmd = 'echo "ERROR: No command configured for step: ' + name + '"'
    
    timeout_sec = step.get('timeout_seconds', 300)
    pass_codes = step.get('pass_codes', [0])
    warn_codes = step.get('warn_codes', [])
    
    print(f"  🏃  {name}...", end="", flush=True)
    
    # Build environment
    env = os.environ.copy()
    env.update(env_overrides)
    
    # Execute
    start = time.time()
    log_file = os.path.join(log_dir, f"{name}-{time.strftime('%Y%m%d-%H%M%S')}.log")
    
    try:
        process = subprocess.run(
            cmd,
            shell=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            timeout=timeout_sec,
            env=env
        )
        exit_code = process.returncode
    except subprocess.TimeoutExpired:
        exit_code = -1
        print(f" ⏱️  TIMEOUT (>{timeout_sec}s)")
        fail_count += 1
        duration = timeout_sec
        
        with open(log_file, 'wb') as f:
            f.write(process.stdout if 'process' in dir() else b'')
        
        results.append((name, "TIMEOUT", str(timeout_sec)))
        
        if fail_fast:
            print()
            print("Pipeline FAILED. Stopped at: " + name)
            print(f"Log: {log_file}")
            # Write summary
            with open(summary_file, 'w') as sf:
                for r in results:
                    sf.write(f"{r[0]}|{r[1]}|{r[2]}\n")
            sys.exit(1)
        continue
    except FileNotFoundError:
        exit_code = 127
        print(f" ❌ NOT FOUND (command not found)")
        fail_count += 1
        duration = 0
        
        results.append((name, "FAIL", "command not found"))
        
        if fail_fast:
            print()
            print(f"Pipeline FAILED. Stopped at: {name}")
            sys.exit(1)
        continue
    
    end = time.time()
    duration = int(end - start)
    
    # Write log
    process = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=timeout_sec, env=env)
    with open(log_file, 'wb') as f:
        f.write(process.stdout)
    
    # Determine status
    if exit_code in pass_codes:
        print(f" ✅ PASS ({duration}s)")
        pass_count += 1
        results.append((name, "PASS", str(duration)))
    elif exit_code in warn_codes:
        print(f" ⚠️  WARN ({duration}s) — exit {exit_code}")
        warn_count += 1
        results.append((name, "WARN", str(duration)))
    else:
        print(f" ❌ FAIL ({duration}s) — exit {exit_code}")
        fail_count += 1
        results.append((name, "FAIL", str(exit_code)))
        
        # Show first few lines of error
        if not cont:
            print()
            print(f"  Last lines from log ({log_file}):")
            try:
                with open(log_file) as lf:
                    lines = [l.strip() for l in lf.readlines() if l.strip()]
                    for line in lines[-5:]:
                        print(f"    {line}")
            except:
                pass
        
        if fail_fast:
            print()
            print("Pipeline FAILED. Stopped at: " + name)
            print(f"Log: {log_file}")
            break
    
# If continue mode, show all results
if not cont:
    # If fail_fast was false, we would have printed all results already
    pass

print()
print("─" * 60)

pipeline_result = "✅ PASSED"
if fail_count > 0:
    pipeline_result = "❌ FAILED"
elif warn_count > 0:
    pipeline_result = "⚠️  WARNING"

print(f"Pipeline Result: {pipeline_result} ({pass_count}/{total} passed)")
print("─" * 60)
print()
print(f"Summary: {pass_count} passed, {warn_count} warn, {fail_count} fail, {skip_count} skipped")
print()

if log_dir:
    print(f"Logs: {log_dir}/")

# Write summary file
with open(summary_file, 'w') as sf:
    for r in results:
        sf.write(f"{r[0]}|{r[1]}|{r[2]}\n")

sys.exit(1 if fail_count > 0 else 0)
PYEOF
