#!/usr/bin/env bash
# Specify-CICD: Dry Run — Show what would execute without running
#
# Usage: cicd-dry-run.sh [--all] [--bypass <pattern>]

set -e

EXTENSION_DIR="$(CDPATH="" cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(CDPATH="" cd "$EXTENSION_DIR/../../../.." && pwd)"
cd "$REPO_ROOT"

CONFIG_DIR="$REPO_ROOT/.specify/extensions/specify-cicd"
CONFIG="$CONFIG_DIR/cicd-config.yaml"

BYPASS_ALL=false
BYPASS_PATTERN=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --all) BYPASS_ALL=true; shift ;;
        --bypass|-x) BYPASS_PATTERN="$2"; shift 2 ;;
        --step|-s) FILTER_STEP="$2"; shift 2 ;;
        *) FILTER_STEP="$1"; shift ;;
    esac
done

if [ ! -f "$CONFIG" ]; then
    echo "❌ Config not found: $CONFIG"
    echo "Run: specify-cicd-setup.sh first"
    exit 1
fi

# Use Python to parse and display
python3 << PYEOF
import yaml, sys, os, re

config_dir = "$CONFIG_DIR"
config_path = "$CONFIG"
bypass_all = "$BYPASS_ALL" == "true"
bypass_pattern = "$BYPASS_PATTERN"
filter_step = getattr(sys, '_filter_step', '') or "$FILTER_STEP"

# Read changed files for diff-bounded check
changed_files = []
try:
    import subprocess
    result = subprocess.run(
        ['git', 'diff', '--name-only', 'HEAD'],
        capture_output=True, text=True, timeout=10
    )
    changed_files = [f.strip() for f in result.stdout.strip().split('\n') if f.strip()]
except:
    pass

def matches_pattern(path, patterns):
    for pat in patterns:
        if pat.startswith('**/'):
            pat = pat[3:]
        if '*' in pat:
            # Simple glob to regex
            regex = pat.replace('*', '.*').replace('/', '(/|/)')
            if re.search(regex, path):
                return True
        else:
            if path.endswith(pat) or pat in path:
                return True
    return False

def get_status(changed_files, patterns, bypass_all):
    if bypass_all:
        return "✅ would run"
    if not patterns or not changed_files:
        return "✅ would run"
    for cf in changed_files:
        if matches_pattern(cf, patterns):
            return "✅ would run"
    return "⏭️  would skip (diff-bounded)"

with open(config_path) as f:
    cfg = yaml.safe_load(f)

steps = cfg.get('pipeline', {}).get('steps', [])

print("=== Specify-CICD: Local Pipeline Dry Run ===")
print()
print(f"{'Step':<15} | {'Command':<55} | {'Status'}")
print(f"{'-'*15}-+-{'-'*55}-+-{'-'*25}")

for s in steps:
    name = s.get('name', 'unnamed')
    cmd = s.get('cmd', s.get('command', '<no command>'))
    enabled = s.get('enabled', True)
    
    # Skip if filter step specified
    if filter_step and name != filter_step:
        continue
    
    # Skip disabled
    if enabled is False:
        print(f"  {name:<15} | (disabled)")
        continue
    
    db = s.get('diff_bounded', {}).get('enabled', False)
    patterns = s.get('diff_bounded', {}).get('patterns', [])
    
    status = get_status(changed_files, patterns, bypass_all)
    
    # Truncate command
    if len(cmd) > 55:
        cmd = cmd[:52] + "..."
    
    print(f"  {name:<15} | {cmd:<55} | {status}")

print()
would_run = sum(1 for s in steps if s.get('enabled', True) is not False)
would_skip = sum(1 for s in steps if s.get('enabled', True) is False)

print(f"Total: {len(steps)} steps, {would_run} would run, {would_skip} disabled")
PYEOF
