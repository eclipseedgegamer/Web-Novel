#!/usr/bin/env bash
# Web Novel — install the install-required skills for Claude Code.
# Run from the repo root:  bash .claude/setup-skills.sh
#
# These skills are installed ON DEMAND and are .gitignored — they are NEVER
# committed (see .gitignore). The other skills (superpowers, ponytail, caveman,
# ui-ux) are bundled as committed SKILL.md files and need no install.
#
# Commands below were verified against the real CLIs (their actual behaviour,
# not just cross-referenced docs):
#   * ui-ux-pro-max-cli exposes the `uipro` binary. `npm i -g` ALONE only gives
#     you the binary; you must then run `uipro init -a claude` to populate
#     .claude/skills/ (creates ui-ux-pro-max/ + sibling design skill folders,
#     including data/stacks/jetpack-compose.csv and scripts/search.py).
#   * The taste repo skills are named `design-taste-frontend` and `minimalist-ui`
#     (there is NO skill called "taste-skill"). One `--skill` per invocation;
#     `--copy` writes real files instead of symlinks.
#   * impeccable needs Node >= 24; this cloud image is Node 22, so it no-ops here.

set -uo pipefail
cd "$(git rev-parse --show-toplevel 2>/dev/null || echo .)"

echo "==> ui-ux-pro-max (global 'uipro' CLI, then project init)"
npm install -g ui-ux-pro-max-cli \
  && uipro init -a claude -f \
  || echo "   ui-ux-pro-max install failed (need network) — retry later"

echo "==> taste (design-taste-frontend + minimalist-ui via the 'skills' CLI)"
npx -y skills add Leonxlnx/taste-skill --skill design-taste-frontend --agent claude-code --copy -y \
  || echo "   taste/design-taste-frontend install failed — retry later"
npx -y skills add Leonxlnx/taste-skill --skill minimalist-ui --agent claude-code --copy -y \
  || echo "   taste/minimalist-ui install failed — retry later"

echo "==> impeccable (anti-slop design skill) — requires Node >= 24"
if [ "$(node -p 'process.versions.node.split(".")[0]' 2>/dev/null || echo 0)" -ge 24 ]; then
  npx -y impeccable install || echo "   impeccable failed — re-run when network available"
else
  echo "   SKIPPED — Node $(node --version 2>/dev/null) < 24. Run locally on Node >= 24, then /impeccable init."
fi

cat <<'EOF'

Bundled (committed, no install needed): superpowers/  ponytail/  caveman/  ui-ux/
Install-required (this script, .gitignored): ui-ux-pro-max/ (+ sibling design folders),
  design-taste-frontend/, minimalist-ui/, impeccable/.
After install: reload Claude Code. Update later with `uipro update` / re-running this script.
EOF
