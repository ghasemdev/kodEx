#!/usr/bin/env bash
# Specify-CICD: Parse CI workflows and generate cicd-config.yaml
#
# Usage: cicd-setup.sh [--provider github-actions|gitlab-ci|circleci|bitbucket]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT=$(cd "$SCRIPT_DIR/../../../../../" && pwd)
cd "$REPO_ROOT"

CONFIG_DIR="$REPO_ROOT/.specify/extensions/specify-cicd"
CONFIG="$CONFIG_DIR/cicd-config.yaml"
mkdir -p "$CONFIG_DIR/logs"

print_usage() {
    echo "Usage: cicd-setup.sh [--provider github-actions|gitlab-ci|circleci|bitbucket]"
    echo ""
    echo "Providers auto-detected from:"
    echo "  .github/workflows/*.yml     (GitHub Actions)"
    echo "  .gitlab-ci.yml              (GitLab CI)"
    echo "  .circleci/config.yml        (CircleCI)"
    echo "  bitbucket-pipelines.yml     (Bitbucket Pipelines)"
    exit 1
}

PROVIDER=""
while [[ $# -gt 0 ]]; do
    case "$1" in
        --provider) PROVIDER="$2"; shift 2 ;;
        --help|-h) print_usage ;;
        *) echo "Unknown option: $1"; print_usage ;;
    esac
done

# Auto-detect
if ls "$REPO_ROOT/.github/workflows/"*.yml 2>/dev/null | grep -qv '\.action\.' 2>/dev/null; then
    [ -z "$PROVIDER" ] && PROVIDER="github-actions"
fi
[ -z "$PROVIDER" ] && [ -f "$REPO_ROOT/.gitlab-ci.yml" ] && PROVIDER="gitlab-ci"
[ -z "$PROVIDER" ] && [ -f "$REPO_ROOT/.circleci/config.yml" ] && PROVIDER="circleci"
[ -z "$PROVIDER" ] && [ -f "$REPO_ROOT/bitbucket-pipelines.yml" ] && PROVIDER="bitbucket"

if [ -z "$PROVIDER" ]; then
    echo "❌ No CI workflow files detected."
    print_usage
fi

echo "=== Specify-CICD Setup ==="
echo "Provider: $PROVIDER"

# Use Python to parse YAML and generate config
python3 - "$PROVIDER" "$CONFIG" "$REPO_ROOT" << 'PYEOF'
import yaml, sys, os, glob, re

provider = sys.argv[1]
# Normalize dashes to underscores
provider = provider.replace('-', '_')
config_path = sys.argv[2]
repo_root = sys.argv[3]

def detect_sources(repo):
    """Detect which CI providers have config files."""
    sources = {}
    
    gha_files = glob.glob(os.path.join(repo, ".github/workflows", "*.yml"))
    gha_files = [f for f in gha_files if ".action." not in os.path.basename(f)]
    if gha_files:
        sources["github_actions"] = {"enabled": True, "paths": [os.path.join(repo, ".github/workflows", "*.yml")], "files": gha_files}
    
    gl_file = os.path.join(repo, ".gitlab-ci.yml")
    if os.path.exists(gl_file):
        sources["gitlab_ci"] = {"enabled": True, "paths": [gl_file]}
    
    cc_file = os.path.join(repo, ".circleci", "config.yml")
    if os.path.exists(cc_file):
        sources["circleci"] = {"enabled": True, "paths": [cc_file]}
    
    bb_file = os.path.join(repo, "bitbucket-pipelines.yml")
    if os.path.exists(bb_file):
        sources["bitbucket"] = {"enabled": True, "paths": [bb_file]}
    
    return sources

def parse_github_actions(files):
    """Extract job commands from GitHub Actions workflow files."""
    steps = []
    
    for wf_path in files:
        with open(wf_path) as f:
            wf = yaml.safe_load(f)
        
        wf_name = os.path.splitext(os.path.basename(wf_path))[0]
        jobs = wf.get("jobs", {})
        
        for job_name, job_def in jobs.items():
            if not isinstance(job_def, dict):
                continue
            
            # Determine timeout
            timeout = job_def.get("timeout-minutes", 20) * 60
            
            # Get steps
            job_steps = job_def.get("steps", [])
            current_run = []
            
            for step in job_steps:
                if not isinstance(step, dict):
                    continue
                
                run_cmd = step.get("run", "")
                if not run_cmd:
                    continue
                
                # Handle multiline commands
                if ">>>" in run_cmd:
                    # Already a multiline block - extract commands
                    for line in run_cmd.strip().split('\n'):
                        line = line.strip()
                        if line and not any(line.startswith(x) for x in ['#', 'if ', 'then', 'fi', 'else', 'done', '"$']):
                            current_run.append(line)
                    continue
                
                # Single command
                current_run.append(run_cmd.strip())
            
            if current_run:
                # Map job name to step name
                step_name_map = {
                    "assemble": "assemble",
                    "test": "test & coverage",
                    "coverage": "test & coverage",
                    "detekt": "lint",
                    "benchmark": "benchmark",
                    "fast-benchmark": "benchmark",
                    "secret-scan": "secret scan",
                    "dependency-check": "dependency check",
                }
                
                mapped_name = step_name_map.get(job_name, job_name)
                
                # Build command string
                cmd_str = "; ".join(current_run)
                
                steps.append({
                    "name": mapped_name,
                    "enabled": True,
                    "cmd": cmd_str.strip(),
                    "timeout_seconds": timeout,
                    "pass_codes": [0],
                    "warn_codes": [],
                    "diff_bounded": {
                        "enabled": True,
                        "patterns": [
                            "**/*.kt",
                            "**/*.java",
                            "**/*.kts",
                            "build.gradle*",
                        ],
                    },
                })
    
    return steps

# Detect sources
sources = detect_sources(repo_root)
if not sources:
    print("Error: No CI sources found", file=sys.stderr)
    sys.exit(1)

# Check if requested provider is available
normalized_provider = provider.replace('-', '_')
if normalized_provider not in sources:
    print(f"Error: Provider '{provider}' not found in {sources.keys()}", file=sys.stderr)
    sys.exit(1)

# Get the relevant workflow files
if provider == "github_actions":
    wf_files = sources[provider]["files"]
elif provider == "gitlab_ci":
    wf_files = [s["paths"][0] for s in sources.values() if s.get("paths")]
elif provider == "circleci":
    wf_files = [s["paths"][0] for s in sources.values() if s.get("paths")]
else:
    wf_files = [s["paths"][0] for s in sources.values() if s.get("paths")]

# Parse steps from CI workflows
steps = []
if provider == "github_actions":
    steps = parse_github_actions(wf_files)

# Map sources in config
source_config = {"github_actions": {}, "gitlab_ci": {}, "circleci": {}, "bitbucket": {}}
for key, val in sources.items():
    source_config[key].update(val)

# Determine which source is enabled
enabled_keys = [k for k, v in source_config.items() if v.get("enabled")]

config = {
    "sources": source_config,
    "pipeline": {
        "steps": steps,
    },
    "execution": {
        "fail_fast": True,
        "verbose": True,
        "env": {"GRADLE_OPTS": "-Dorg.gradle.daemon=false"},
        "working_dir": ".",
        "retry": {"max_attempts": 1, "delay_seconds": 0},
    },
    "report": {
        "path": "docs/cicd",
        "include_logs": True,
        "include_timing": True,
        "include_diff_analysis": True,
    },
}

# Write config
with open(config_path, 'w') as f:
    yaml.dump(config, f, default_flow_style=False, sort_keys=False, width=120)

print(f"✓ Config written to: {config_path}")
print(f"  Sources: {enabled_keys}")
print(f"  Steps: {len(steps)} parsed from {len(wf_files)} workflow file(s)")

# Provide guidance on next steps
print()
print("Next steps:")
print("  1. Edit cicd-config.yaml to refine step order and patterns")
print("  2. Run local CI: specify.cicd.run")
print("  3. See results: specify.cicd.report")
PYEOF

echo ""
echo "Setup complete."
