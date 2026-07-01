# Web Novel — Build Plan (Revision 3)

Personal Android web-novel reader. Built from scratch (QuickNovel referenced for features only, not a
fork). `applicationId: com.eclipse.webnovel`. Target device: iQOO Neo 10 (Android 16, AMOLED, 12 GB
RAM, Snapdragon). Personal use only — keep the repo/README neutral; don't enumerate scraped sites.

**What changed in Revision 3** (plan review): the on-device LLM (the heaviest, riskiest piece) is
demoted off the critical path — the cheap CPU embedding model is separated from the 2.5 GB GPU LLM,
and the whole grammar/translate engine moves to the LAST phase (Grammar-Polish first, Translate
experimental). Phases are re-sequenced so the app becomes a daily driver sooner (Library + offline
downloads + Updates pulled forward). The metaball-merge glass effect is cut from the core build
(unused by the design). Dedup leads with title+author matching before embeddings. TTS ships
System+Neural2 first. The 4 theme presets are unchanged. Scope trims: Reviews tab dropped for v1,
About section trimmed, app-icon adaptive/monochrome layers specified.

---

## 0. Session working rules (apply to every task)

- **Clarify to 95%** before writing code; ask one question at a time, never guess on real ambiguity.
- **Verify before advancing**: don't start the next to-do until the current one builds, runs, and its
  one runnable check passes. No "should work."
- **Subagents** for genuinely independent work only (e.g. per-source parsers). They cost tokens; don't
  over-spawn. `cavecrew` compresses returned output for long sessions.
- **`/init` caution**: CLAUDE.md is hand-curated — back it up before running `/init`.
- **Ponytail default**: laziest thing that works; reuse > stdlib > native > library > minimal custom.

---

## 1. Locked decisions

| # | Area | Decision |
|---|------|----------|
| 1 | Sources | Pluggable registry, REAL site names in code. Start 4: **RoyalRoad → ScribbleHub → LightNovelWorld → NovelFire**. Prove RoyalRoad first. Keep user-facing labels + repo description neutral. |
| 2 | Build order | **Vertical slice first** (1 source → Explore → Detail → Reader, real data), then fan out. |
| 3 | Glass | **Selective**: chrome/overlays only; solid surfaces behind reading text. **Backdrop blur via library** (Haze / Cloudy), NOT `Modifier.blur`. **Metaball merge cut from core** (optional Phase-7 spike). |
| 4 | Themes (4) | Emerald Green (default) · Dark (AMOLED #000000, emerald) · Light · Sand (cream + terracotta + serif headers). One token source, live switcher. **Unchanged.** |
| 5 | Typography | Playfair Display (headers) · Hanken Grotesk (UI) · Literata (reading body). |
| 6 | TTS | **4-tier auto-fallback**: Google Neural2 → Azure Neural → Google Standard → Android System TTS. Land **System (4) + Neural2 (1) first**; 2/3 later. Per-tier voice memory + monthly usage tracking. No ElevenLabs/Kokoro. |
| 7 | AI text | **On-device, demoted to last phase.** Split: **all-MiniLM-L6-v2 ONNX (~90 MB, CPU)** for embeddings/dedup/recs (available Phase 3+) vs **Qwen2.5-3B-Instruct INT4 (~2.5 GB, MediaPipe GPU)** for grammar/translate (Phase 6). Default OFF. **Grammar-Polish first; Translate experimental.** |
| 8 | Recs | **Kept.** Vector-profile, seeded from thumbs + completed. Reuses the MiniLM vectors. |
| 9 | API keys | **In-app, encrypted, local** (`EncryptedSharedPreferences`). No build-injection, no Secrets, no keys in APK/repo. |
| 10 | Backup | First-class encrypted export/restore (final phase). |
| 11 | Design review | **Build glass composables directly, screenshot on device, iterate.** No committed React/prototype artifact. |
| 12 | Platform | **Android-only, single module.** No KMP / Compose-Multiplatform-Web. |

---

## 2. Liquid-glass engineering (critical — read before any UI)

`Modifier.blur` blurs a composable's OWN content, not what's behind it — it does NOT produce
glassmorphism. Two separate effects:

- **Glass frosting** (blur the chapter list behind a toolbar): use a backdrop-blur library.
  - **Haze** (`dev.chrisbanes.haze`): scroll-aware, noise (kills AMOLED banding), materials. Picks
    scrim ≤API31 / GraphicsLayers API32 / RuntimeShader API33+ automatically.
  - **Cloudy** (`com.github.skydoves:cloudy`): has `Modifier.liquidGlass()` (SDF edges, refraction) +
    CPU fallback. Good for the "liquid glass" lens look.
- **Metaball merge** (toolbar icons fusing): **CUT from the core build.** The design never uses it, and
  the hand-rolled `RenderEffect` + `Offscreen` + AGSL-shader path (API 33+) is most of the glass risk.
  Keep `liquidMetaballContainer()` only as an optional Phase-7 spike, if ever.

**Reusable infra (build first):** `Modifier.liquidGlassSurface()` (backdrop blur + tint + 1dp
specular gradient border). Build custom layouts; avoid Material3 `TopAppBar`/`Button` where glass must
apply seamlessly. Provide a clean non-liquid glassmorphism fallback below API 31 (RenderEffect).

**Pitfalls (do not repeat):**
1. `Modifier.blur` ≠ backdrop blur — use Haze/Cloudy.
2. Blur behind a scrolling `LazyColumn` = dropped frames → blur a snapshot, not live-scrolling content.
3. AGSL `RuntimeShader` = API 33+; guard and fall back. (Mostly moot now the metaball path is cut.)
4. Text inside a `renderEffect` layer gets blurred — keep text in a sibling layer above the effect.
5. `CompositingStrategy.Offscreen` = a buffer per layer; wrap the container, not every component.
6. Don't animate blur radius per-frame (recreates RenderEffect) — animate shape transform pre-blur.
7. Touch bounds ignore visual blur/scale — keep `clickable` on the raw element, ≥48dp targets.
8. Pure-black AMOLED + blur = banding — add noise/dither.
9. Never glass behind reading text.
10. Glass = chrome/overlays only.

---

## 3. AI engine (on-device, local-first) — DEMOTED to the last phase

**Design principle for Rev 3: separate the cheap CPU model from the expensive GPU model.** The MiniLM
embedding model is tiny, instant, low-risk, and powers dedup + recs — it lands early (Phase 3). The
Qwen 3B LLM is 2.5 GB, GPU-bound, on a maintenance-only API, and its Translate output is admittedly
mediocre — the entire grammar/translate engine is deferred to Phase 6.

### 3.1 Embeddings + dedup + recs (ONNX MiniLM) — available Phase 3+
- `microsoft.onnxruntime:onnxruntime-android`. **Execution provider: XNNPACK (CPU) or QNN (Snapdragon).
  Do NOT use NNAPI — deprecated in Android 15.** MiniLM is tiny; CPU is effectively instant.
- Local WordPiece `BertTokenizer` over `vocab.txt` (not whitespace split). 384-dim normalized vector
  from `synopsis + " Tags: " + tags`. Pure-Kotlin cosine similarity. Room `@TypeConverter`:
  `FloatArray` ↔ BLOB.
- Model download managed like §3.3 (SHA-256 verify, sandbox). ~90 MB (`model.onnx` + `vocab.txt`).

### 3.2 Grammar/Translate LLM (MediaPipe Qwen) — Phase 6
- `com.google.mediapipe:tasks-genai` (re-verify current version at build time; MediaPipe LLM API is
  maintenance-only, LiteRT-LM is the forward path — acceptable to start on MediaPipe for a personal
  app). `Backend.GPU`, model at `filesDir/models/qwen2.5-3b-gpu-int4.task`.
- Model is a pre-converted `.task`/`.litertlm` bundle (NOT raw HF). Source a pre-bundled file or
  convert via AI Edge Torch (Linux + 64 GB RAM, offline step).
- **Reading modes:** Bypass (default) · **Grammar-Polish (ship first — reliable)** · **Translate
  (experimental — 3B INT4 quality is mediocre; manage expectations).** Selector in the reader Aa sheet.
  Default OFF globally + per-source.
- **Pipelines** (suspend, `Dispatchers.Default`, output-only, no preamble):
  - `fixBadMtl(raw)`: fantasy-LN editor prompt — fix grammar, pronoun mix-ups, awkward phrasing; keep
    terms/LitRPG/formatting.
  - `translateChinese(raw)`: Xianxia/Wuxia ZH→EN. Experimental. If on-device proves too weak, revisit a
    cloud call behind the user's own encrypted key — not before.
- **Glossary pre-processor:** before each paragraph, string-replace against a SQLite glossary table
  (consistent names). Per-novel glossary editor UI.
- **Safe chunking:** split on `\n\n`, ≤~500 tokens per inference call (avoid GPU OOM).

### 3.3 Model management + secure pipeline (Settings → AI & Models)
- Models are NEVER bundled in the APK. Downloaded in-app to `context.filesDir/models/`.
- Cards per model (MiniLM in Phase 3; Qwen in Phase 6): status · size · progress (% + speed) ·
  action (Download/Cancel/Delete).
- **Pre-download guards:** free storage ≥4 GB; **RAM guard** via `ActivityManager.MemoryInfo`
  (≥8 GB → 3B OK). One device here is 12 GB — guard, don't build a matrix.
- Download via `DownloadManager` → `BroadcastReceiver`; **SHA-256** verify before use; copy verified
  file into `filesDir/models/`, verify length, delete the public temp copy (app-private sandbox).

### 3.4 Recommendation system (kept) — Phase 5
- 3-tier rating on Detail — Dislike (−25% of vector), Like (+50%), Double-Like (+100%) — shifts a
  master preference vector. Seed from initial thumbs + completed list. Noisy until history
  accumulates; weights in a config object. Reuses the Phase-3 MiniLM vectors.

### 3.5 Resource protection (Qwen, Phase 6)
- **Lifecycle:** `LifecycleEventObserver` — release the engine if the reader is backgrounded >5 min.
- **Memory pressure:** `ComponentCallbacks2` — on `TRIM_MEMORY_RUNNING_CRITICAL`/`BACKGROUND`, release
  the LLM immediately; lazy-reload on resume.
- **Thermal:** `PowerManager.thermalStatus` — pause/throttle AI workers at `SEVERE`+.
- **Pre-translate queue:** WorkManager — on opening chapter N, prefetch + pre-process N+1/N+2 in the
  active mode. Gate: Wi-Fi + battery >20%.
- **Compose highlight perf:** `AnnotatedString` with stable state wrappers so per-word highlight
  updates don't trigger heavy relayout.

---

## 4. TTS (4-tier routing) — full spec

| Tier | Engine | ~Free/mo | Key | Word-sync | Notes |
|------|--------|----------|-----|-----------|-------|
| 1 | Google Cloud Neural2 | ~1M chars | Google (in-app) | SSML `<mark>` | best quality |
| 2 | Azure Neural TTS | ~500K (F0) | Azure + region (in-app) | `WordBoundary` | F0 throttles, never charges |
| 3 | Google Cloud Standard | ~4M chars | same Google key | SSML marks | lower quality, huge quota |
| 4 | Android System TTS | unlimited | none | `onRangeStart` | offline, final fallback |

**Ship order:** **Tier 4 first** (offline, free, unlimited, gives word-highlight via `onRangeStart`),
then **Tier 1** (Neural2, quality). Tiers 2/3 + the monthly-usage-counter / quota-warning infra are
optional and later — don't build quota tracking until a hard-quota tier is actually in use.

Prefer highest tier that is online + keyed + under quota; drop a tier when exhausted/keyless; fall to
Tier 4 offline. Switch only at paragraph breaks. Active-tier badge in the mini-player. Per-tier voice
memory (`tts_voice_preferences`, PK = tier) persists across monthly resets. Per-tier usage counters in
Room, reset on the 1st via WorkManager; 80%-quota warning banner (local estimates; provider count is
authoritative). Player: speed 0.5×–3×, sleep timer, MediaSession lock-screen, headphone-pause,
auto-advance. **TTS resume anchor:** persist the word/paragraph index where playback stopped.

Keys for all tiers entered in-app (Settings), stored encrypted. No build injection.

---

## 5. Sources, dedup-powered features

- `NovelSource` interface + `AdaptiveScraper` base (selector fallback chains help markup drift; they do
  NOT defeat Cloudflare/JS-rendered sites). `SourceRegistry` with runtime enable/disable. Jsoup +
  OkHttp (cookie jar, UA rotation, per-source rate limit + exponential backoff, 5-fail → temp-
  unavailable badge).
- **Reference techniques** (study, don't add as deps — Python/JS): TLS/JA3 + stealth headers for
  Cloudflare; main-text extraction heuristics. For JS/Cloudflare sites the Android options are a
  headless WebView render or skipping them — decide per source.
- **Dedup (Rev 3): lead with normalized title + author exact/fuzzy match** (free, no model). Use MiniLM
  embeddings as a **secondary** signal only; **tune the cosine threshold empirically — do NOT hardcode
  0.95** (per-source blurbs differ, so a high fixed threshold rarely fires). `processScrapedNovel(...)`:
  normalize → title+author match (append URL if found) → else embed + scan → merge source links under
  the existing record. This dedup graph powers the multi-source switcher + per-chapter fallback.
- **Multi-source switcher (Detail):** same novel deduped across sites → source picker; default to
  freshest/fastest.
- **Per-chapter source fallback:** chapter fetch 404s / Cloudflare-blocks on source A → auto-retry a
  deduped alternate before erroring.
- **Per-source health badge:** last-fetch, failure count, temp-unavailable.
- **Scraper tests = fixtures, not live network.** Record HTML per source, unit-test parsers against
  fixtures; "Test Connection" is a manual action. Keep a local rolling error log (ring buffer → file).

---

## 6. Reader

- Progress anchor = **paragraph index + intra-paragraph char offset** (survives font-size + scroll↔
  paginated changes). Not raw scroll position.
- **Tap-zone gestures** (`pointerInput`): left/right edges page; center toggles chrome; swipe between
  chapters. Volume-key paging too.
- Floating glass toolbar: progress row (`0%` … `CH n / N`) + `« · 🎧 Listen · Aa`, plus floating top
  bar (back · title/chapter · Aa). Heavy frosted backdrop so text behind is fully obscured.
- ✨ AI-clean button (Phase 6) visible only when global + current-source toggles are both ON; offers
  "View Original"; cleaned text SHA-256-cached in Room (never re-infer the same paragraph).
- Per-novel font/size/theme override; custom font import.
- **Detail screen: single canonical layout** (stats row + Continue/bookmark/download + synopsis +
  chapters). **No Reviews tab in v1** (scraping per-source reviews is fragile, low personal value).

---

## 7. Notifications / Updates (Phase 2)

- WorkManager new-chapter check (~6 h) for tracked + **completed** novels still receiving chapters.
- In-app **Updates feed/tab** backed by the same job + a `chapter_updates` Room table; notification
  deep-links to the new chapter. Reuse the job — no separate infra.

---

## 8. Phases

- **Phase 1 — Vertical slice (de-risk).** Gradle (Kotlin DSL), minSdk 31 / targetSdk 36, nav graph,
  Room (plain; SQLCipher behind a flag), `ThemeManager` + tokens (4 presets), base glass composables
  (`liquidGlassSurface`, top/bottom bars, card, button, sheet) using Haze/Cloudy. **RoyalRoad** end-to-
  end: Explore → Detail → Reader on real data, content clean, progress save/restore via the font-
  independent anchor, tap-zone gestures. **App identity:** name "Web Novel"; ship the frosted-glass
  "W + pages" launcher icon as an **adaptive icon** (separate foreground/background layers, with a
  subtle tinted background so the near-white glass doesn't vanish on a white launcher) **plus a
  monochrome layer** for Android 13+ themed icons. Gate: `./gradlew assembleDebug` clean + read a live
  RoyalRoad chapter.
- **Phase 2 — Daily-driver essentials.** Library (Reading/Completed/All), reading history; **offline
  chapter downloads** + Saved/Downloads screen (queue, per-novel size, storage meter); covers cache;
  ~6 h new-chapter check → **Updates feed + notifications**. Gate: build a library, download a novel,
  read it offline, receive an update.
- **Phase 3 — Source fan-out + dedup.** `SourceRegistry`, Source Manager UI, ScribbleHub →
  LightNovelWorld → NovelFire (fixture-backed parser tests). **MiniLM embedding infra** + dedup
  (title+author first, embeddings secondary, threshold tuned). Multi-source switcher, per-chapter
  fallback, health badges, parallel search with relevance ranking.
- **Phase 4 — TTS.** Router **Tier 4 + Tier 1 first**, then 2/3. Per-tier voice + usage, mini-player,
  word highlight + scroll sync, resume anchor.
- **Phase 5 — Recommendations.** Master preference vector + 3-tier rating (reuses MiniLM), "Because you
  read X", "Not interested" suppression, Explore sections from the cached multi-source pool (scheduled
  refresh).
- **Phase 6 — On-device grammar/translate LLM (demoted).** Model-manager card for Qwen + secure
  download/verify/sandbox, MediaPipe engine + lifecycle/memory/thermal guards, **Grammar-Polish first
  (Translate experimental)**, glossary editor, ✨ gating + SHA-256 cache, pre-translate queue.
- **Phase 7 — Polish, backup, CI/CD.** Recomposition stability (`@Immutable/@Stable`, stable LazyList
  keys), content descriptions, 48dp targets, storage audit, **encrypted export/restore**, reading
  goals/streaks, `FLAG_SECURE` toggle. `release.yml` (tag `v*.*.*` → signed APK from base64 keystore in
  Secrets — keystore only, NOT app API keys) + `update-meta.yml`. Neutral repo description. Optional
  metaball-merge spike.

---

## 9. Verification gates

- Per phase: `./gradlew assembleDebug` clean; one runnable check per non-trivial unit.
- Phase 1: read a live RoyalRoad chapter; kill/relaunch → progress restores at the right paragraph
  after a font-size change; tap-zones page correctly.
- Phase 2: download a novel → airplane mode → read offline; new-chapter job posts to Updates feed +
  notification deep-links correctly; library progress + history persist.
- Phase 3: fixture-backed JVM parser tests (`./gradlew test`); two sources of one novel merge
  (title+author, embeddings secondary); multi-source switcher + per-chapter fallback work; live Test
  Connection manual.
- Phase 4 (TTS): highlight tracks speech each tier; router falls Neural2→Azure→Standard→System as
  keys/quota/connectivity are removed; per-tier voice survives a simulated month reset; resume works.
- Phase 5 (recs): recs shift with thumbs; seeded profile isn't empty cold-start.
- Phase 6 (AI): model download → SHA-256 verify → sandbox copy; ✨ hidden unless both toggles ON;
  cleaned paragraph cached (no second inference); "View Original" round-trips; engine releases on
  memory-trim; thermal throttle triggers at SEVERE; Grammar-Polish quality acceptable; Translate flagged
  experimental.
- Phase 7 (backup): export → uninstall → reinstall → restore reproduces library + progress.
- Final: `./gradlew assembleRelease` (signed), `./gradlew lint` (zero in our code).

---

## 10. Explicitly NOT building (ponytail)

OpenRouter "optimization loop", account/cloud sync, social/comments, web ratings aggregation,
multi-user, IAP, a model-recommendation matrix, KMP/Compose-Multiplatform-Web, a throwaway React design
artifact, a **Reviews tab / review scraping (v1)**, the **metaball-merge glass effect (core build)**.
Translate is shipped **experimental**, not a headline feature. One-user app — every one of these is
scope. Revisit only after the reader spine + downloads + dedup + TTS actually ship.
