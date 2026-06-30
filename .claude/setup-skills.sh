#!/usr/bin/env bash
# Web Novel — skill setup for Claude Code
# Run ONCE from the repo root:  bash .claude/setup-skills.sh
#
# Installs the skills that ship a CLI. The rest are already bundled as plain
# SKILL.md files under .claude/skills/ (offline fallback — no network needed).
#
# NOTE: all CLI installers below write into THIS repo's .claude/skills/ (project
# scope). Claude Code reads .claude/skills/, so run from the repo root. Some
# installers default to a global ~/.agents/skills/ path that Claude Code does NOT
# read — the flags below force project scope.

set -uo pipefail
cd "$(git rev-parse --show-toplevel 2>/dev/null || echo .)"

echo "==> taste (vercel-labs 'skills' CLI)"
# main design-taste skill (repo also ships minimalist/soft/etc — add --skill to pick others)
npx -y skills add leonxlnx/taste-skill --skill taste-skill --agent claude-code --scope project \
  || echo "   taste CLI failed — bundled .claude/skills/taste/ is the fallback"

echo "==> impeccable (anti-slop design skill) — requires Node >= 24"
npx -y impeccable install \
  || echo "   impeccable failed (need Node >=24 + network) — re-run when available"

echo "==> ui-ux-pro-max CLI (global tool; bundled copy already in .claude/skills/ui-ux-pro-max/)"
npm install -g ui-ux-pro-max-cli \
  || echo "   ui-ux-pro-max-cli global install failed — bundled skill still works"

cat <<'EOF'

Bundled (no install needed, already in .claude/skills/):
  superpowers/  ponytail/  caveman/  ui-ux/

Install-required (this script): taste, impeccable, ui-ux-pro-max.
After install: reload Claude Code. For impeccable, run /impeccable init once.
EOF
