# Skills — install / update reference

Two distribution mechanisms exist; they behave differently in **Claude Code cloud (web)** vs **local**:

- **`npx skills add …`** (vercel-labs/agent-skills CLI) → writes skill files **into this repo's
  `.claude/skills/`**. **.gitignored — installed on demand, never committed** (see `.gitignore`).
- **`/plugin marketplace add …` + `/plugin install …`** → installs to the **user-global**
  `~/.claude/plugins/`, NOT into the repo. These are interactive slash commands for **local** Claude
  Code; in cloud they don't populate the repo. For cloud, rely on the committed copies in
  `.claude/skills/` (already present) — re-run the marketplace install only when working locally.

So: the `npx skills add` skills can be (re)installed by the script; the `/plugin` skills stay as the
committed copies for cloud and are refreshed via slash commands when local.

| Skill | Source | Install command | Mechanism |
|-------|--------|-----------------|-----------|
| ui-ux-pro-max | `ui-ux-pro-max-cli` (npm) | `npm install -g ui-ux-pro-max-cli` then `uipro init -a claude` | `uipro` CLI → repo (gitignored) |
| taste (design) | `Leonxlnx/taste-skill` | `npx skills add Leonxlnx/taste-skill --skill "design-taste-frontend" --copy` | `skills add` → repo (gitignored) |
| taste (minimalist) | `Leonxlnx/taste-skill` | `npx skills add Leonxlnx/taste-skill --skill "minimalist-ui" --copy` | `skills add` → repo (gitignored) |
| frontend-design | `ui-ux/frontend-design.md` (committed) | manual / Anthropic official plugin — verify exact string | committed copy |
| shadcn | `shadcn-ui/ui` (committed) | manual / shadcn registry — verify exact string | committed copy |
| superpowers | `obra/superpowers` | `/plugin marketplace add obra/superpowers-marketplace` then `/plugin install superpowers@superpowers-marketplace` | `/plugin` → global |
| ponytail | `DietrichGebert/ponytail` | `/plugin marketplace add DietrichGebert/ponytail` then `/plugin install ponytail@ponytail` | `/plugin` → global |
| caveman | `JuliusBrussee/caveman` | `curl -fsSL https://raw.githubusercontent.com/JuliusBrussee/caveman/main/install.sh \| bash` | installer → global |

Notes:
- `npx skills add` produces the canonical layout `.claude/skills/<name>/SKILL.md`. The current committed
  copies use a flat hand-made layout; both work, but a fresh `skills add` will create its own folder —
  delete the old flat copy of that skill if you switch to the installed one to avoid duplicates.
- `--agent claude-code` targets Claude Code specifically. Drop it to install for all detected agents.
- caveman / ponytail / superpowers run session hooks; in cloud those hooks may not fire — the committed
  `.claude/skills/` copies still provide the skill text, which is what matters here.
- gsap removed (JS-only, no Jetpack Compose target). Use native Compose animation primitives instead.

Run `bash .claude/setup-skills.sh` to install the install-required skills (ui-ux-pro-max, taste,
impeccable); the `/plugin`/curl skills stay as committed copies (refresh via slash commands when local).
