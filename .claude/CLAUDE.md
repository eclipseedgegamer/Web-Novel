# Claude Code Skills Configuration
# Repository: eclipseedgegamer/Web-Novel

## MANDATORY: Read skills before ANY task
Before responding to any request — including clarifying questions — read the relevant skill files below.

---

## 🛑 SESSION RULES (always on)

1. **Clarify to 95%.** Before writing any code, ask questions until you are ≥95% sure you understand
   the intent. Ask ONE question at a time (per `superpowers/brainstorming.md`), never a multi-question
   dump. If you can confidently infer from context, don't ask — state the assumption and proceed.
2. **Verify before advancing.** Do not move to the next to-do until you are ≥95% sure the current one
   is actually done. "Done" = it builds, it runs, and the one runnable check passes
   (per `superpowers/verification-before-completion.md`). No "should work" claims.
3. **Use subagents for independent work** (`superpowers/subagent-driven-development.md`,
   `superpowers/dispatching-parallel-agents.md`, `caveman/cavecrew.md`). ⚠️ Token caveat: each subagent
   spawns its own context — they cost more upfront, not less. Use them only for genuinely parallel or
   independent tasks (e.g. per-source scraper parsers), not for everything. `cavecrew` compresses the
   returned output (~60% smaller) which helps long sessions.
4. **`/init` caution.** This CLAUDE.md is hand-curated. Do NOT run `/init` blindly — it can overwrite
   this file. If you must regenerate, back this file up first.
5. **Ponytail by default** — laziest solution that works; reuse > stdlib > native > library > minimal
   custom code. Mark deliberate shortcuts with a `ponytail:` comment. (`ponytail/ponytail.md`)

---

## 📦 Installing / updating skills

Full reference + per-skill commands: `.claude/SKILLS.md`. Run `bash .claude/install-skills.sh` from the
repo root to (re)install. Two mechanisms:

- **`npx skills add …`** → writes into this repo's `.claude/skills/` (commit it). **Cloud-friendly.**
  Used for **ui-ux-pro-max** and **taste**.
- **`/plugin marketplace add … && /plugin install …`** → installs to global `~/.claude/plugins/`, NOT
  the repo. Local-only (slash commands). Used for **superpowers**, **ponytail**; **caveman** ships a
  `curl … | bash` installer. In cloud these don't populate the repo — the committed `.claude/skills/`
  copies are the source of truth there.

frontend-design + shadcn are kept as committed copies. **gsap is not installed** (JS-only, no Compose
target — use native Compose animation).

---

## 📱 Project Context

**App:** Web Novel — personal Android web novel reader, built from scratch for personal use only.
**Device:** iQOO Neo 10 (Android 16, AMOLED 1.5K display, 12 GB RAM, Snapdragon).
**Stack:** Jetpack Compose + Kotlin, MVVM + Clean Architecture, Room, WorkManager, Coroutines.
**Design:** Liquid Glassmorphism — glass on chrome/overlays ONLY; solid surfaces behind reading text.
**UI direction:** clean, minimal, user-friendly (selective glass, one accent, generous spacing).
**Themes:** Emerald Green (default) · Dark (AMOLED #000000, emerald accent) · Light · Sand (cream +
terracotta + serif headers).
**Typography:** Playfair Display (headers) · Hanken Grotesk (UI) · Literata (reading body).
**Min SDK:** 31 (Android 12) — `RenderEffect` blur. NOTE: AGSL `RuntimeShader` needs API 33+; guard
shader paths and fall back below 33.

Reference app for FEATURES (not design): https://github.com/LagradOst/QuickNovel — NOT a fork.

---

## 🔑 Architecture decisions locked this session

- **API keys = in-app, encrypted, local.** User enters each key in Settings → stored in
  `EncryptedSharedPreferences` (Keystore-wrapped). NO build-time injection, NO GitHub Secrets, NO keys
  in the APK or repo. (Reverses the earlier build-injection plan.)
- **On-device AI engine** (replaces cloud Gemini cleanup):
  - Translation/grammar: **Qwen2.5-3B-Instruct INT4** (~2.5 GB) via MediaPipe LLM Inference
    (`com.google.mediapipe:tasks-genai:0.10.27`, GPU backend). ⚠️ MediaPipe LLM API is now
    maintenance-only; LiteRT-LM is the forward path — fine to start on MediaPipe for a personal app.
    Model must be a pre-converted `.task`/`.litertlm` bundle (not raw HF weights). Downloaded in-app to
    `context.filesDir/models/`, SHA-256 verified, sandboxed.
  - Embeddings/dedup/recs: **all-MiniLM-L6-v2 (ONNX, ~90 MB)** + `vocab.txt` (WordPiece). 384-dim,
    cosine similarity. ⚠️ **Do NOT use NNAPI** — deprecated in Android 15. Use ONNX Runtime
    **XNNPACK (CPU)** or **QNN EP (Snapdragon)**. MiniLM is tiny; CPU is effectively instant.
  - Reading modes: Bypass (default, no inference) · Grammar-Polish · Translate. Default OFF globally +
    per-source.
  - **Recommendation system: KEPT** (user wants it). Vector-profile recs (thumbs shift master vector).
    Seed the profile from initial thumbs + completed list so cold-start isn't empty. Expect noisy recs
    until rating history accumulates.
- **Backdrop blur ≠ `Modifier.blur`.** `Modifier.blur` blurs a composable's OWN content, not what's
  behind it. For glassmorphism use a backdrop-blur library: **Haze** (`dev.chrisbanes.haze`) or
  **Cloudy** (`com.github.skydoves:cloudy`, has `Modifier.liquidGlass()` with refraction). Reserve
  hand-rolled `RenderEffect` (offscreen + alpha-threshold color matrix) for the metaball MERGE effect
  only (toolbar icons fusing), never for backdrop frosting.
- **No design artifact.** React/Vite prototype dropped (token cost). Design source of truth = the
  Compose glass composables themselves; screenshot on emulator/device and iterate.
- **No KMP / Compose-Multiplatform-Web.** Android-only, single module. Web target = scope creep.

---

## 🧊 LIQUID-GLASS ENGINEERING RULES (Compose)

Reusable infra first: `Modifier.liquidGlassSurface()` (backdrop blur + tint + 1dp specular border) and
`Modifier.liquidMetaballContainer()` (offscreen + threshold for merge). Build custom layouts; avoid
Material3 `TopAppBar`/`Button` where the glass must apply seamlessly.

**Pitfalls — do NOT make these mistakes:**
1. `Modifier.blur` blurs own content, NOT the backdrop. Use Haze/Cloudy for glass. (#1 mistake.)
2. Blur behind a scrolling `LazyColumn` = dropped frames. Blur a static/snapshot backdrop; never
   live-blur scrolling content. (Reinforces selective-glass.)
3. AGSL `RuntimeShader` = API 33+, not 31. Guard shader code; fall back on 31–32.
4. Text inside a `renderEffect` layer gets blurred too. Keep text in a SIBLING layer above the effect,
   never inside it.
5. `CompositingStrategy.Offscreen` allocates a buffer per layer — overdraw + GPU memory. Wrap the
   metaball container, not every component.
6. Never animate blur radius per-frame (recreates the `RenderEffect` each frame). Animate
   translation/scale/alpha of shapes BEFORE the blur layer; keep radius fixed.
7. Touch bounds don't follow visual blur/scale. Keep `clickable`/`pointerInput` on the raw element;
   expand hit targets to ≥48dp.
8. Pure-black AMOLED + blur = banding. Add noise/dither (Haze noise, or a faint grain overlay).
9. Never put glass behind reading text — solid high-contrast surface for body.
10. Glass = chrome/overlays only; cost scales with blurred area. Don't frost every surface.

---

## 🧠 SUPERPOWERS (workflow engine — always active)
Read `.claude/skills/superpowers/using-superpowers.md` first in every session.

| Trigger | Skill File |
|---|---|
| Starting any task | `.claude/skills/superpowers/using-superpowers.md` |
| New feature / creative work / clarifying intent | `.claude/skills/superpowers/brainstorming.md` |
| Writing implementation plan | `.claude/skills/superpowers/writing-plans.md` |
| Executing a plan | `.claude/skills/superpowers/executing-plans.md` |
| Any bug or test failure | `.claude/skills/superpowers/systematic-debugging.md` |
| Before claiming work is done (95% gate) | `.claude/skills/superpowers/verification-before-completion.md` |
| Writing or editing skills | `.claude/skills/superpowers/writing-skills.md` |
| Finishing a branch | `.claude/skills/superpowers/finishing-a-development-branch.md` |
| 2+ independent tasks | `.claude/skills/superpowers/dispatching-parallel-agents.md` |
| Implementing features | `.claude/skills/superpowers/test-driven-development.md` |
| Getting code review | `.claude/skills/superpowers/receiving-code-review.md` |
| Requesting code review | `.claude/skills/superpowers/requesting-code-review.md` |
| Need isolated workspace | `.claude/skills/superpowers/using-git-worktrees.md` |
| Independent parallel tasks (subagents) | `.claude/skills/superpowers/subagent-driven-development.md` |

---

## 🖌️ UI UX PRO MAX (install-required — always active for ANY UI/design task)
Installed via `setup-skills.sh` (`npm install -g ui-ux-pro-max-cli`), not bundled. Paths below are valid
after install. Read `.claude/skills/ui-ux-pro-max/SKILL.md` for any UI or design work.

**Python search engine (run inside Claude Code terminal):**
```bash
# Find Liquid Glass style tokens
python3 .claude/skills/ui-ux-pro-max/scripts/search.py "liquid glass" --domain style

# Generate full design system for the app
python3 .claude/skills/ui-ux-pro-max/scripts/search.py "android novel reader dark amoled" --design-system -p "Web Novel"

# Jetpack Compose guidelines (PRIMARY trigger for all Android UI work)
python3 .claude/skills/ui-ux-pro-max/scripts/search.py "glass card" --stack jetpack-compose
```

| Trigger | Skill / Data File |
|---|---|
| Any UI/design task | `.claude/skills/ui-ux-pro-max/SKILL.md` |
| **Jetpack Compose UI (Android)** | **`.claude/skills/ui-ux-pro-max/data/stacks/jetpack-compose.csv`** |
| Liquid Glass / style selection | `.claude/skills/ui-ux-pro-max/data/styles.csv` |
| Color palette / theming | `.claude/skills/ui-ux-pro-max/data/colors.csv` |
| Typography / font pairing | `.claude/skills/ui-ux-pro-max/data/typography.csv` |
| Design system reasoning (161 rules) | `.claude/skills/ui-ux-pro-max/data/ui-reasoning.csv` |
| UX guidelines / accessibility | `.claude/skills/ui-ux-pro-max/data/ux-guidelines.csv` |
| Icons (use SVG, not emoji) | `.claude/skills/ui-ux-pro-max/data/icons.csv` |
| Charts / data visualization | `.claude/skills/ui-ux-pro-max/data/charts.csv` |

---

## ⚙️ Installing / updating skills
Skills that ship a CLI are **install-required** (not bundled here) — run `bash .claude/setup-skills.sh`
from the repo root to install them into `.claude/skills/` (project scope). Skills with no CLI are
**bundled** as plain SKILL.md files (offline, no network). Re-run the script to update.

| Skill | Install command | In this repo? |
|---|---|---|
| taste | `npx skills add leonxlnx/taste-skill --skill taste-skill --agent claude-code --scope project` | install-required |
| impeccable | `npx impeccable install` (needs Node ≥24) then `/impeccable init` | install-required |
| ui-ux-pro-max | `npm install -g ui-ux-pro-max-cli` | install-required |
| superpowers / ponytail / caveman / ui-ux | (no CLI) | bundled (`.claude/skills/`) |

`npx skills add` can default to a global `~/.agents/skills/` path Claude Code does NOT read — always run
project-scoped from the repo root so files land in `.claude/skills/`.

---

## 🎨 TASTE + IMPECCABLE (install-required — anti-slop design judgment)
Installed by `setup-skills.sh`, not bundled. Read before generating UI to avoid templated output. Both
are web-oriented (scan HTML/CSS/JSX, not Kotlin) — apply the *judgment* (read-the-brief, anti-default
discipline, minimalism), map patterns to Compose. impeccable's detector won't scan `.kt`; use it for
design direction + `/impeccable init` context, not as a linter here.

| Trigger | How |
|---|---|
| Any UI / design direction (avoid slop) | `taste` → `.claude/skills/taste/` after install |
| Clean / minimal aesthetic (our UI goal) | `taste` minimalist variant |
| Establish design context, polish, critique | `impeccable`: `/impeccable init`, `/impeccable polish`, `/impeccable critique` |

---

## 🎨 UI/UX SKILLS (supplementary)
| Trigger | Skill File |
|---|---|
| Building UI / design direction | `.claude/skills/ui-ux/frontend-design.md` |
| shadcn/ui components (design reference) | `.claude/skills/ui-ux/shadcn.md` |
| Project context / AGENTS.md setup | `.claude/skills/ui-ux/intent-layer.md` |

---

## 🦣 CAVEMAN (token compression — use when asked)
| Trigger | Skill File |
|---|---|
| "caveman mode" / "be brief" / `/caveman` | `.claude/skills/caveman/caveman.md` |
| "commit message" / `/caveman-commit` | `.claude/skills/caveman/caveman-commit.md` |
| "compress memory file" / `/caveman-compress` | `.claude/skills/caveman/caveman-compress.md` |
| "caveman help" / `/caveman-help` | `.claude/skills/caveman/caveman-help.md` |
| "code review" / `/caveman-review` | `.claude/skills/caveman/caveman-review.md` |
| `/caveman-stats` | `.claude/skills/caveman/caveman-stats.md` |
| "delegate to subagent" / "use cavecrew" | `.claude/skills/caveman/cavecrew.md` |

---

## 🐴 PONYTAIL (minimal/lazy solutions — default philosophy)
| Trigger | Skill File |
|---|---|
| "ponytail" / "be lazy" / "simplest solution" | `.claude/skills/ponytail/ponytail.md` |
| "audit codebase" / `/ponytail-audit` | `.claude/skills/ponytail/ponytail-audit.md` |
| "ponytail debt" / "what did we defer" | `.claude/skills/ponytail/ponytail-debt.md` |
| "ponytail gain" / "show impact" | `.claude/skills/ponytail/ponytail-gain.md` |
| "ponytail help" / `/ponytail-help` | `.claude/skills/ponytail/ponytail-help.md` |
| "review for over-engineering" / `/ponytail-review` | `.claude/skills/ponytail/ponytail-review.md` |

---

## Slash Commands
| Command | What it does |
|---|---|
| `/init` | Generate CLAUDE.md (⚠️ back up this file first) |
| `/code-review` | Review diffs for bugs/cleanups |
| `/simplify` | Refactor for clarity/efficiency |
| `/security-review` | Security audit of pending changes |
| `/review` | Review a GitHub PR |
| `/caveman` | Switch to caveman compression mode |
| `/ponytail` | Switch to minimal solution mode |
| `/ponytail-audit` | Audit whole codebase for over-engineering |
| `/caveman-commit` | Generate compressed commit message |
