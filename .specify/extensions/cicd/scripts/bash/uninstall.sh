#!/usr/bin/env bash
# Specify-CICD: Uninstall — remove CICD skills from .claude/skills/

set -e

SCRIPT_DIR="$(CDPATH="" cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EXTENSION_DIR="$(CDPATH="" cd "$SCRIPT_DIR/../.." && pwd)"

REPO_ROOT="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel 2>/dev/null)" \
  || REPO_ROOT="$(CDPATH="" cd "$SCRIPT_DIR/../../../../../" && pwd)"

SKILLS_SRC="$EXTENSION_DIR/skills"
SKILLS_DST="$REPO_ROOT/.claude/skills"

removed=0
for skill_dir in "$SKILLS_SRC"/*/; do
  skill_name="$(basename "$skill_dir")"
  dst_dir="$SKILLS_DST/$skill_name"

  if [ -d "$dst_dir" ]; then
    rm -rf "$dst_dir"
    echo "  🗑️  removed: $skill_name"
    removed=$((removed + 1))
  fi
done

echo ""
echo "Specify-CICD: $removed skill(s) removed from $SKILLS_DST"
