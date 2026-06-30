#!/usr/bin/env bash
# Install / update Web Novel skills. See SKILLS.md for the why.
# Cloud (web Claude Code): the `npx skills add` block writes into ./.claude/skills/ — commit the result.
# The `/plugin` skills install globally and are printed for you to paste into a local Claude Code session.
set -euo pipefail

echo "== npx skills add (writes into ./.claude/skills/, commit these) =="

# ui-ux-pro-max — design intelligence (jetpack-compose.csv is the primary Android trigger)
npx -y skills add nextlevelbuilder/ui-ux-pro-max-skill --skill ui-ux-pro-max --agent claude-code

# taste — anti-slop design judgment + clean/minimal aesthetic
npx -y skills add Leonxlnx/taste-skill --skill "design-taste-frontend" --agent claude-code
npx -y skills add Leonxlnx/taste-skill --skill "minimalist-ui"          --agent claude-code

echo ""
echo "== /plugin skills (run these inside a LOCAL Claude Code session — they install globally) =="
cat <<'EOF'
  superpowers:
    /plugin marketplace add obra/superpowers-marketplace
    /plugin install superpowers@superpowers-marketplace

  ponytail:
    /plugin marketplace add DietrichGebert/ponytail
    /plugin install ponytail@ponytail

  caveman (shell installer, auto-detects Claude Code):
    curl -fsSL https://raw.githubusercontent.com/JuliusBrussee/caveman/main/install.sh | bash
EOF

echo ""
echo "frontend-design + shadcn: kept as committed copies in .claude/skills/ui-ux/ (verify official"
echo "install strings before swapping). gsap intentionally NOT installed (no Compose target)."
echo ""
echo "Done. In cloud, the committed .claude/skills/ copies remain the source of truth."

# ponytail: scriptable parts run; /plugin parts can't be bash-automated (interactive slash commands).
