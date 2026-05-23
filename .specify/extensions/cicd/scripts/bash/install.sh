#!/usr/bin/env bash
# Specify-CICD: Install — register CICD skills into .claude/skills/
#
# Usage: install.sh [--copy]
#   --copy   copy SKILL.md instead of symlinking (useful for CI environments)

set -e

SCRIPT_DIR="$(CDPATH="" cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EXTENSION_DIR="$(CDPATH="" cd "$SCRIPT_DIR/../.." && pwd)"

# Find repo root via git, fall back to relative path
REPO_ROOT="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel 2>/dev/null)" \
  || REPO_ROOT="$(CDPATH="" cd "$SCRIPT_DIR/../../../../../" && pwd)"

SKILLS_SRC="$EXTENSION_DIR/skills"
SKILLS_DST="$REPO_ROOT/.claude/skills"

USE_COPY=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --copy) USE_COPY=true; shift ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

if [ ! -d "$SKILLS_SRC" ]; then
  echo "❌ Skills directory not found: $SKILLS_SRC"
  exit 1
fi

mkdir -p "$SKILLS_DST"

installed=0
for skill_dir in "$SKILLS_SRC"/*/; do
  skill_name="$(basename "$skill_dir")"
  src="$skill_dir/SKILL.md"
  dst_dir="$SKILLS_DST/$skill_name"
  dst="$dst_dir/SKILL.md"

  [ -f "$src" ] || continue
  mkdir -p "$dst_dir"

  if $USE_COPY; then
    cp "$src" "$dst"
    echo "  ✅ installed (copy):    $skill_name"
  else
    # Relative symlink from .claude/skills/<name>/SKILL.md → extension
    rel="$(python3 -c "import os; print(os.path.relpath('$src', '$dst_dir'))")"
    ln -sf "$rel" "$dst"
    echo "  ✅ installed (symlink): $skill_name → $rel"
  fi
  installed=$((installed + 1))
done

echo ""
echo "Specify-CICD: $installed skill(s) registered in $SKILLS_DST"
echo "Commands available: /speckit-cicd-setup  /speckit-cicd-run  /speckit-cicd-dry-run  /speckit-cicd-report"
