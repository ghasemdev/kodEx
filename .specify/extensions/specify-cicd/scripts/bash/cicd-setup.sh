#!/usr/bin/env bash
# Specify-CICD: Local CI/CD Setup
# Parses CI workflow files and generates cicd-config.yaml
#
# Usage: cicd-setup.sh [--provider github-actions|gitlab-ci|circleci|bitbucket]

set -e

EXTENSION_DIR="$(CDPATH="" cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT=$(CDPATH="" cd "$EXTENSION_DIR/../../../.." && pwd)
cd "$REPO_ROOT"

CONFIG_DIR="$REPO_ROOT/.specify/extensions/specify-cicd"
CONFIG_FILE="$CONFIG_DIR/cicd-config.yaml"
LOG_FILE="$CONFIG_DIR/logs/setup-$(date +%Y%m%d-%H%M%S).log"
mkdir -p "$CONFIG_DIR/logs"

log() { echo "[cicd-setup] $*" | tee -a "$LOG_FILE" >&2; }
warn() { echo "[cicd-setup] ⚠ $*" | tee -a "$LOG_FILE" >&2; }

# ── Auto-detect provider ─────────────────────────────────────────────────────
PROVIDER=""
GHA_COUNT=0
GL_COUNT=0
CC_COUNT=0
BB_COUNT=0

if ls "$REPO_ROOT/.github/workflows/"*.yml 2>/dev/null | grep -qv '\.action\.' 2>/dev/null; then
    GHA_COUNT=$(ls "$REPO_ROOT/.github/workflows/"*.yml | grep -cv '\.action\.' 2>/dev/null || echo 0)
    PROVIDER="github-actions"
fi
if [ -f "$REPO_ROOT/.gitlab-ci.yml" ]; then
    GL_COUNT=1
    [ -z "$PROVIDER" ] && PROVIDER="gitlab-ci"
fi
if [ -f "$REPO_ROOT/.circleci/config.yml" ]; then
    CC_COUNT=1
    [ -z "$PROVIDER" ] && PROVIDER="circleci"
fi
if [ -f "$REPO_ROOT/bitbucket-pipelines.yml" ]; then
    BB_COUNT=1
    [ -z "$PROVIDER" ] && PROVIDER="bitbucket"
fi

# Override via --provider flag
while [[ $# -gt 0 ]]; do
    case "$1" in
        --provider) PROVIDER="$2"; shift 2 ;;
        *) log "Unknown option: $1"; exit 1 ;;
    esac
done

if [ -z "$PROVIDER" ]; then
    log "No CI workflow files detected."
    log "Create one of:"
    log "  .github/workflows/*.yml    (GitHub Actions)"
    log "  .gitlab-ci.yml              (GitLab CI)"
    log "  .circleci/config.yml        (CircleCI)"
    log "  bitbucket-pipelines.yml     (Bitbucket)"
    exit 1
fi

log "Detected provider: $PROVIDER"

# ── Parse GitHub Actions ─────────────────────────────────────────────────────
parse_github_actions() {
    local workflow_dir="$REPO_ROOT/.github/workflows"
    
    for wf in "$workflow_dir"/*.yml; do
        [ -f "$wf" ] || continue
        local wf_name
        wf_name=$(basename "$wf" .yml)
        [ "$wf_name" = "*.action" ] && continue
        
        log "Parsing workflow: $wf_name"
        
        # Extract job names
        local in_job=false
        local indent=0
        local job_stack=""
        
        while IFS= read -r line; do
            # Detect 'jobs:' key
            if echo "$line" | grep -qE '^jobs:'; then
                in_job=true
                continue
            fi
            
            $in_job || continue
            
            # Detect job name (2-space indent, ends with:)
            if echo "$line" | grep -qE '^  [a-zA-Z_][a-zA-Z0-9_-]*:'; then
                local job_name
                job_name=$(echo "$line" | sed 's/^  \([a-zA-Z_][a-zA-Z0-9_-]*\):.*/\1/')
                job_stack="$job_stack JOB:$job_name"
                log "  Found job: $job_name"
                continue
            fi
            
            # Detect step name (6-space indent, 'name:' or step without name)
            if echo "$line" | grep -qE '^      name:'; then
                continue  # We track by run command below
            fi
            
            # Extract 'run:' command
            if echo "$line" | grep -qE '^        run:'; then
                local run_cmd
                run_cmd=$(echo "$line" | sed 's/^        run: //' | sed 's/^"//' | sed 's/"$//' | sed "s/^'//" | sed "s/'$//")
                
                # Check for multiline run (>>>)
                if echo "$line" | grep -q '>'; then
                    run_cmd=""
                    while IFS= read -r inner_line; do
                        if echo "$inner_line" | grep -qE '^        -? '?; then
                            local step_cmd
                            step_cmd=$(echo "$inner_line" | sed 's/^        -*//' | sed 's/^ *//' | sed 's/^"//' | sed 's/"$//')
                            if [ -n "$step_cmd" ] && [ "$step_cmd" != "|" ] && [ "$step_cmd" != ">" ] && [ "$step_cmd" != ">>" ]; then
                                [ -n "$run_cmd" ] && run_cmd="$run_cmd; "
                                run_cmd="$run_cmd$step_cmd"
                            fi
                        elif echo "$inner_line" | grep -qE '^[^ ]'; then
                            break
                        fi
                    done
                fi
                
                if [ -n "$run_cmd" ]; then
                    # Extract timeout
                    local timeout=300
                    local timeout_minutes
                    timeout_minutes=$(echo "$line" | grep -oE 'timeout-minutes: *[0-9]+' | grep -oE '[0-9]+' || echo "")
                    [ -n "$timeout_minutes" ] && timeout=$((timeout_minutes * 60))
                    
                    # Extract condition
                    local condition=""
                    local condition_line
                    condition_line=$(grep -B5 "run:" "$wf" | grep 'if:' | tail -1 || echo "")
                    if [ -n "$condition_line" ]; then
                        condition=$(echo "$condition_line" | sed 's/.*if: *//' | sed 's/^"//' | sed 's/"$//')
                    fi
                    
                    # Extract needs (dependencies)
                    local needs=""
                    local needs_line
                    needs_line=$(grep -B10 "run:" "$wf" | grep 'needs:' | tail -1 || echo "")
                    if [ -n "$needs_line" ]; then
                        needs=$(echo "$needs_line" | sed 's/.*needs: *//')
                    fi
                    
                    log "    Step: $run_cmd (timeout: ${timeout}s)"
                fi
            fi
        done < "$wf"
    done
}

# ── Simple GitHub Actions → YAML config generator ───────────────────────────
generate_github_actions_config() {
    local workflow_dir="$REPO_ROOT/.github/workflows"
    local steps_yaml=""
    local step_idx=0
    
    for wf in "$workflow_dir"/*.yml; do
        [ -f "$wf" ] || continue
        local wf_name
        wf_name=$(basename "$wf" .yml)
        case "$wf_name" in *.action) continue ;; esac
        
        log "Generating config from workflow: $wf_name"
        
        local current_job=""
        local in_jobs=false
        local in_job=false
        local in_steps=false
        local job_indent=0
        
        while IFS= read -r line; do
            # Detect 'jobs:' top-level key
            if echo "$line" | grep -qE '^jobs:'; then
                in_jobs=true
                continue
            fi
            
            $in_jobs || continue
            
            # Detect job name (2 spaces indentation)
            local job_match
            job_match=$(echo "$line" | grep -oE '^  [a-zA-Z_][a-zA-Z0-9_-]*:' | head -1 || echo "")
            if [ -n "$job_match" ]; then
                current_job=$(echo "$job_match" | sed 's/^  //; s/:$//')
                job_indent=2
                in_job=true
                in_steps=false
                
                # Check if job has conditions
                local job_condition=""
                local after_line
                after_line=$(sed -n "/^  ${current_job}:/,/^  [a-zA-Z_]/p" "$wf" | head -10)
                if echo "$after_line" | grep -q 'if:'; then
                    job_condition=$(echo "$after_line" | grep 'if:' | head -1 | sed 's/.*if: *//; s/^"//; s/"$//')
                fi
                
                # Check 'needs'
                local job_needs=""
                job_needs=$(echo "$after_line" | grep 'needs:' | head -1 | sed 's/.*needs: *//')
                
                # Check timeout
                local timeout=300
                local tm
                tm=$(echo "$after_line" | grep 'timeout-minutes:' | head -1 | grep -oE '[0-9]+' || echo "")
                [ -n "$tm" ] && timeout=$((tm * 60))
                
                log "  Job: $current_job (needs: ${job_needs:-none}, timeout: ${timeout}s)"
                continue
            fi
            
            $in_job || continue
            
            # Detect 'steps:' key (6 spaces)
            if echo "$line" | grep -qE '^      steps:'; then
                in_steps=true
                continue
            fi
            
            $in_steps || continue
            
            # Each step starts with '- uses:' or '- name:' (8 spaces)
            if echo "$line" | grep -qE '^        - '; then
                continue  # Just a step marker
            fi
            
            # Extract 'run:' command from step
            if echo "$line" | grep -qE '^          run:'; then
                local run_cmd
                # Handle single-line run
                if ! echo "$line" | grep -q '>>>'; then
                    run_cmd=$(echo "$line" | sed 's/^          run: *//' | sed 's/^"//' | sed 's/"$//' | sed "s/^'//" | sed "s/'$//")
                    
                    # Build the step yaml block
                    step_idx=$((step_idx + 1))
                    local step_name="${current_job}_step_${step_idx}"
                    
                    # Try to extract a meaningful name from 'name:' on the line before
                    local prev_context
                    prev_context=$(sed -n "/run:/p" "$wf" | head -2 | head -1)
                    # If the step above has a 'name:', use the run command as step name
                    # Otherwise use run command as-is
                    
                    steps_yaml="${steps_yaml}
    - name: \"$current_job\"
      enabled: true
      cmd: \"$run_cmd\"
      timeout_seconds: $timeout"
                    
                    # Store parsed step for later
                    if [ ! -f "$CONFIG_DIR/.steps.json" ]; then
                        echo "[]" > "$CONFIG_DIR/.steps.json"
                    fi
                    
                    log "    Parsed step: $run_cmd"
                fi
            fi
        done < "$wf"
    done
    
    # Write final config
    cat > "$CONFIG_FILE" << 'HEADER'
# Specify-CICD Configuration — auto-generated
# Edit this file to refine step order, diff-bounded patterns, and enabled states.

# ── Source CI providers ──────────────────────────────────────────────────────
sources:
  github_actions:
    enabled: true
    paths:
      - ".github/workflows/*.yml"
    parse_rules:
      include:
        - "*.yml"
      exclude:
        - "*.action.yml"
  gitlab_ci:
    enabled: false
    paths:
      - ".gitlab-ci.yml"
  circleci:
    enabled: false
    paths:
      - ".circleci/config.yml"
  bitbucket:
    enabled: false
    paths:
      - "bitbucket-pipelines.yml"
HEADER

    cat >> "$CONFIG_FILE" << 'PIPELINE_HEADER'

# ── Pipeline execution ──────────────────────────────────────────────────────
# Manually edit to reorder, rename, or add steps.
# Diff-bounded: set enabled:true + patterns to run only on matching file changes.

PIPELINE_HEADER

    # Use a different approach - directly extract run commands from workflows
    local steps_written=""
    
    for wf in "$workflow_dir"/*.yml; do
        [ -f "$wf" ] || continue
        local wf_name
        wf_name=$(basename "$wf" .yml)
        case "$wf_name" in *.action) continue ;; esac
        
        # Extract all 'run:' commands with context
        local parsed_steps=()
        local in_run_block=false
        local current_run=""
        local current_job=""
        
        while IFS= read -r line; do
            # Track current job
            if echo "$line" | grep -qE '^  [a-zA-Z_][a-zA-Z0-9_-]*:'; then
                current_job=$(echo "$line" | sed 's/^  //; s/:$//')
            fi
            
            # Extract single-line run commands
            if echo "$line" | grep -qE '^          run: "; then
                local cmd
                cmd=$(echo "$line" | sed 's/^          run: *//' | sed 's/^"//' | sed 's/"$//')
                parsed_steps+=("${current_job}|||${cmd}")
            fi
            
            # Extract multiline run commands
            if echo "$line" | grep -qE '^          run: \'>\|>>'; then
                in_run_block=true
                continue
            fi
            
            if $in_run_block; then
                if echo "$line" | grep -qE '^          '; then
                    local cmd
                    cmd=$(echo "$line" | sed 's/^          //' | sed 's/^ *//')
                    if [ -n "$cmd" ] && ! echo "$cmd" | grep -qE '^(if|fi|then|else|done|"$|"$)'; then
                        current_run="${current_run:+${current_run}; }${cmd}"
                    fi
                else
                    $in_run_block || continue
                    if [ -n "$current_run" ]; then
                        parsed_steps+=("${current_job}|||${current_run}")
                        current_run=""
                    fi
                    in_run_block=false
                fi
            fi
        done < "$wf"
    done
    
    # Write parsed steps as YAML
    local prev_job=""
    local write_steps_from_context "$wf"
                parsed_steps+=("${current_job}|||${current_run}")
                    current_run=""
                fi
                in_run_block=false
            esac
        done < "$wf"
        
        # Deduplicate steps
        local unique_steps=()
        local seen_steps=""
        for s in "${parsed_steps[@]}"; do
            local cmd="${s##*|||}"
            if [[ ! "$seen_steps" =~ "$cmd" ]]; then
                seen_steps="${seen_steps}${cmd}"
                unique_steps+=("$s")
            fi
        done
        
        for us in "${unique_steps[@]}"; do
            local job_name="${us%%|||*}"
            local cmd="${us##*|||}"
            
            # Map common job names to step names
            local step_name="$job_name"
            case "$step_name" in
                assemble) step_name="assemble" ;;
                test) step_name="test & coverage" ;;
                detekt) step_name="lint" ;;
                benchmark|fast-benchmark) step_name="benchmark" ;;
            esac
            
            steps_written="${steps_written}
    - name: \"$step_name\"
      enabled: true
      cmd: \"$cmd\"
      timeout_seconds: 600
      pass_codes:
        - 0
      warn_codes: []
      diff_bounded:
        enabled: true
        patterns:
          - \"**/*.kt\"
          - \"**/*.java\"
          - \"**/*.kts\"
          - \"build.gradle*\"
"
        done
        
    echo "$steps_written" >> "$CONFIG_FILE"
    
    # Write remaining config
    cat >> "$CONFIG_FILE" << 'FOOTER'

# ── Execution behavior ──────────────────────────────────────────────────────
execution:
  fail_fast: true
  verbose: true
  env:
    GRADLE_OPTS: "-Dorg.gradle.daemon=false"
  working_dir: "."
  retry:
    max_attempts: 1
    delay_seconds: 0

# ── Report output ───────────────────────────────────────────────────────────
report:
  path: "reports/cicd/local-report.md"
  include_logs: true
  include_timing: true
  include_diff_analysis: true
FOOTER
    
    log "Config written to: $CONFIG_FILE"
    log "Parse complete."
}

# ── Main ─────────────────────────────────────────────────────────────────────
log "=== Specify-CICD Setup ==="
log "Repo root: $REPO_ROOT"

case "$PROVIDER" in
    github-actions)
        generate_github_actions_config
        ;;
    gitlab-ci)
        warn "GitLab CI support planned for next version"
        log "Creating minimal config for GitLab CI"
        cat > "$CONFIG_FILE" << 'EOF'
# Specify-CICD: Auto-generated from .gitlab-ci.yml
# TODO: Full GitLab CI parser in development.

sources:
  gitlab_ci:
    enabled: true
    paths:
      - ".gitlab-ci.yml"

pipeline:
  steps: []

execution:
  fail_fast: true
  verbose: true
  working_dir: "."

report:
  path: "reports/cicd/local-report.md"
  include_logs: true
FOOTER
        ;;
    circleci)
        warn "CircleCI support planned for next version"
        cat > "$CONFIG_FILE" << 'EOF'
# Specify-CICD: Auto-generated from .circleci/config.yml
# TODO: Full CircleCI parser in development.

sources:
  circleci:
    enabled: true
    paths:
      - ".circleci/config.yml"

pipeline:
  steps: []

execution:
  fail_fast: true
  verbose: true
  working_dir: "."

report:
  path: "reports/cicd/local-report.md"
  include_logs: true
FOOTER
        ;;
    bitbucket)
        warn "Bitbucket Pipelines support planned for next version"
        cat > "$CONFIG_FILE" << 'EOF'
# Specify-CICD: Auto-generated from bitbucket-pipelines.yml
# TODO: Full Bitbucket parser in development.

sources:
  bitbucket:
    enabled: true
    paths:
      - "bitbucket-pipelines.yml"

pipeline:
  steps: []

execution:
  fail_fast: true
  verbose: true
  working_dir: "."

report:
  path: "reports/cicd/local-report.md"
  include_logs: true
FOOTER
        ;;
    *)
        log "Unknown provider: $PROVIDER"
        exit 1
        ;;
esac

log "Setup complete. Config: $CONFIG_FILE"
log "Edit the file to refine step order, patterns, and enabled states."

cat << 'DONE'

Specify-CICD setup complete.

Next steps:
  1. Edit .specify/extensions/specify-cicd/cicd-config.yaml
     → adjust step order, patterns, and enabled states
  2. Run local CI: specify.cicd.check
  3. See results: specify.cicd.report
DONE
