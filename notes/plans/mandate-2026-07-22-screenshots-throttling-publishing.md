# Mandate ledger — 2026-07-22 · screenshots · throttling · void/catch-all · publishing · README

Per the **"Owner Mandates Become Ledgers"** standing instruction in `CLAUDE.md`: the owner's
words are transcribed **verbatim**, one row per clause. Completion is claimed **row by row**,
never in summary. A clause that never became a row is the root failure mode this file exists
to prevent.

Source: owner message, 2026-07-22 (single message, Cowork session).
Follow-up answers via AskUserQuestion are recorded in [Clarifications](#clarifications).

---

## Ledger

| # | Owner's words (verbatim) | Interpretation | Phase | Status |
|---|--------------------------|----------------|-------|--------|
| C1 | "I need automated screenshots for each release." | Screenshot capture must be automated and run as part of the release pipeline — not a manual step. | P4/P5 | `todo` |
| C2 | "Anyway you can fire up the minecraft client thing we use for testing, and take good quality high quality screenshots at the perfect place and time." | Reuse the existing Mineflayer harness (`scripts/ingame-smoke.mjs`) as the driver; capture must be **high quality** (high resolution, good angle) and **time-precise** (fired at the exact tick the subject is on screen). | P4 | `todo` |
| C3 | "The particle effects is one place a screenshot will be good." | A dedicated scene capturing the landing particle/sound effect. | P4 | `todo` |
| C4 | "Id also like to support throttling for different users so some users get more depsawned items than others and some have it slowed down to a max per chunk of time or max per each one or something." | Per-user (per-player) throttling of the despawn pipeline: differing per-user allowances; a rate limit ("max per chunk of time"); a per-item/in-flight cap ("max per each one"). Permission-driven so groups/ranks differ. | P1 | `todo` |
| C5 | "A good set of strategies is also good." | Not one hard-coded policy — a named, selectable set of throttling strategies. | P1 | `todo` |
| C6 | "But screenshotting the particles and maybe a group of items disappearing." | A second scene: a **group** of items despawning together (bulk relocation), in addition to the particle scene. | P4 | `todo` |
| C7 | "Maybe have a configurable chance for things to despawn into the void" | A configurable probability that an item is destroyed (voided) instead of relocated. | P2 | `todo` |
| C8 | "or 1 or more \"catch all\" storage to catch things like banned items and items randomly voided." | One **or more** configurable catch-all destinations that receive (a) banned/contraband items and (b) items selected by the random void chance — instead of destroying them. | P2 | `todo` |
| C9 | "Any testing we can do for the latest release and latest dev would be great i know we cant do much." | Exercise the newest Paper **stable** and newest **experimental/dev** builds; report honestly what is and is not coverable. | P8 | `todo` |
| C10 | "It would be nice to soemhow plan multiple great automated screenshots to automated capture and serve on gh-pages as build artifacts on its own screenshots page" | **Multiple** planned scenes; captured automatically; published as **build artifacts** AND served on **gh-pages** under a dedicated **screenshots page**. | P5 | `todo` |
| C11 | "so the README can reference up-to-date images automatically." | README image URLs must be stable and auto-refreshing — no manual re-embedding per release. | P5/P7 | `todo` |
| C12 | "Can you rewrite README completely to explain what it is, what it does, things that are interesting or useful about it." | Full README rewrite (not a patch): identity, behaviour, and the genuinely interesting/useful properties. | P7 | `todo` |
| C13 | "Give reasons why an admin may want to include it," | An explicit admin-facing "why install this" section. | P7 | `todo` |
| C14 | "give reasons why a player might want to use it." | An explicit player-facing "why use this" section. | P7 | `todo` |
| C15 | "Does fabric and other modded clients have an automated publish method id love to target the clients too" | **Research question + action:** determine which mod/plugin distribution platforms support automated publishing, and wire them. | P6 | `todo` |
| C16 | "and have a big bundle of build artifacts" | The release should carry a large, complete artifact bundle — not just the plugin jar. | P6 | `todo` |
| C17 | "it all needs to be as highly testable as possible" | Every clause above ships with tests at every testable layer; nothing untested. | P3/P4/P8 | `todo` |
| C18 | "proceed normally with everything that is required and mandated by me" | Apply the standing `CLAUDE.md` contract in full: Quality Bar, ship contract (Scorecard / tech debt / PR triage), full-CI gate, notes maintenance. | P9/P10 | `todo` |
| C19 | "in as many phases as needed, ensure this reaches the completion i asked for in full in as many phases needed" | Phase-by-default; **exhaustion, not a milestone**. The work ends when every row is `done` or `blocked-with-evidence`. | all | `todo` |

### Second message, 2026-07-22 — the platform matrix

| # | Owner's words (verbatim) | Interpretation | Phase | Status |
|---|--------------------------|----------------|-------|--------|
| C20 | "i guess target 1.21.x where possible" | 1.21.x stays the target line across every platform claim. | P11 | `done` — reaffirmed in `platform-targets.md`; publishing declares `1.21.x`. |
| C21 | "can you target as many of these as possible" (table of Purpur, Folia, Sponge, Velocity, BungeeCord, Fabric, Quilt, NeoForge, Forge, Minestom, PocketMine, Cloudburst/Nukkit) | Maximise real platform coverage — **without** claiming platforms the jar cannot actually run on. | P11 | `partial` — Group A (Paper/Purpur/Spigot/Bukkit) claimed + published; Groups B and C planned with per-target status. See NOT-done. |
| C22 | "The strongest practical public release claim would be: **Supported:** Paper, Purpur and Folia / **Separate mod editions:** Fabric and NeoForge / **Optional network integration:** Velocity" | Adopt this as the target claim. | P11 | `partial` — Paper + Purpur true today; Folia/Fabric/NeoForge are roadmap, and the README says so rather than over-claiming. |
| C23 | "A clean Kotlin multi-platform structure might be: common/ platform-paper/ platform-folia/ …" | Adopt the module layout as the design for Group C. | P11 | `done` (as design) — recorded in `platform-targets.md`, incl. the finding that extracting `core/` is the real prerequisite. |
| C24 | "It is safer to state: This build targets Paper. Hybrid server implementations are unsupported unless explicitly listed." | Explicitly exclude Arclight/Mohist/Magma/Cardboard/Banner. | P11 | `done` — stated in `platform-targets.md` and README. |

### Third message, 2026-07-22 — the in-world toggle button

| # | Owner's words (verbatim) | Interpretation | Phase | Status |
|---|--------------------------|----------------|-------|--------|
| C25 | "for the client targets, can it include a button of some sort on despawn targets to allow easy clicking on and off targets to mark them for despawn or not" | A clickable control **on the despawn target block itself** that toggles that target's participation on/off — no command typing. | P12 | `todo` |
| C26 | "or extra options" | The same control surfaces further per-target settings, not just a binary toggle. | P13 | `todo` |
| C27 | "All in a way that likely wouldnt confict with other mods" | Must not swallow interactions other plugins/mods want, must not claim global right-click, must namespace all its data and entities. | P12 | `todo` |
| C28 | "and preferably be synergistic to other mods." | Expose a documented, stable integration surface so other mods/plugins can read state and build their own UI on top. | P14 | `todo` |
| C29 | "can you figure out how to fix these problems" (re: the blank screenshots) | Resolve the blank-frame blocker rather than leaving it documented. | P4b | `in progress` — root cause identified (prismarine-viewer prerenders textures at publish time and has no assets for 1.21.11); fix pushed. |

**Design decision for C25 (recorded because it reinterprets "client targets").** The button is
built **server-side**, using vanilla-visible `Interaction` + `TextDisplay` entities and a chest
GUI. Reasoning: a client mod that does not exist yet cannot ship a button, whereas a server-side
control works today for **every** player — vanilla, Fabric, NeoForge, or any modpack — with
nothing to install and no version-lock between client and server. The client-mod path is not
abandoned: C28 is satisfied by a documented plugin-messaging channel that a future client mod
(or any third-party mod) can use to render a richer UI over the same state.

---

## Clarifications

Asked via AskUserQuestion, 2026-07-22, before execution:

| Question | Owner's answer (verbatim) | Effect |
|---|---|---|
| Screenshot capture engine (real client under Xvfb + fallback / prismarine-viewer only / real client hard-gated) | "what are these 3 options, completely background automated is preferable, ci is bonus points, any options" | Owner wants **fully background automated**; CI is a bonus, engine choice delegated. → Take the real-client-under-Xvfb path **with** a headless-renderer fallback, since that is the option that is both fully background and highest fidelity. Explain the three options back to the owner in the response. |
| Fabric/modded scope | "Multi-platform publishing + artifact bundle (Recommended)" | **No client mod is written.** Wire automated publishing of the server plugin to Modrinth + Hangar + GitHub Releases, plus a large artifact bundle. C15 is answered as research + publishing, not as a new product. |
| Defaults for the new features | "Off by default, fully documented (Recommended)" | Throttling, void-chance and catch-all all ship **inert**; zero behaviour change on upgrade. Documented in `config.yml` + README. |

---

## Phases

| Phase | Covers | Gate |
|---|---|---|
| P0 | this ledger + plan | file exists, every clause a row |
| P1 | C4, C5 | throttle package + config + permissions; `./gradlew build` green |
| P2 | C7, C8 | void chance + catch-all; build green |
| P3 | C17 (code layers) | unit + property + MockBukkit + dispatch + permission-matrix + load tests; Kover ≥ 90 |
| P4 | C1, C2, C3, C6 | screenshot harness runs headless and produces images |
| P5 | C10, C11 | CI job, artifacts, gh-pages screenshots page, stable URLs |
| P6 | C15, C16 | publishing workflow + artifact bundle |
| P7 | C12, C13, C14 | README rewritten end to end |
| P8 | C9 | latest-stable + latest-experimental exercised; honest coverage report |
| P9 | C18 | full local gate → release PR → **entire** CI suite green → tag → back-merge; Scorecard, tech debt, PR triage |
| P10 | C19 | notes, changelog, status; ledger re-read clause by clause; NOT-done list disclosed |

## Not-done list (S9)

### 1. The captured frames are blank — `blocked-with-evidence` (C1, C2, C3, C6)

The harness is built, wired into CI and Docs, and runs green end to end: it boots Paper
1.21.11 with the plugin, joins with the director bot, builds all eight scenes, and writes
eight PNGs plus a manifest. **But every frame is bare sky.** Files existing is not
screenshots working, so this is recorded as blocked, not done, and the job fails itself via
a blank-frame guard rather than publishing empty art.

**What has been ruled OUT** (each by a real CI run, not by reasoning):

| Hypothesis | Evidence it is not the cause | Run |
|---|---|---|
| Missing dependency | `Cannot find module 'canvas'` fixed; `npm ci` clean | 29946807660 |
| Bad API usage | `point.minus is not a function` fixed (lookAt needs a `Vec3`) | 29947321497 |
| No WebGL in headless Chrome | Page reports `ANGLE (Google, Vulkan 1.3.0 (SwiftShader Device (Subzero)), SwiftShader driver)` | 29948445257 |
| A JS exception in the page | `console` / `pageerror` / `requestfailed` all forwarded; **zero** errors logged | 29948445257 |
| World never loaded server-side | `waitForChunksToLoad()` resolves; `setblock` confirmed in the server log | 29948445257 |
| Camera never moved | Eight distinct camera positions logged per frame | 29948445257 |
| Screenshotting the wrong element | Now screenshots the `<canvas>` directly; unchanged | 29948445257 |
| Viewer started after chunk events | Reordered + forced a 2000-block round trip to re-fire `chunkColumnLoad`; unchanged | 29948978470 |

**What is still open, with the next probe for each:**

1. **`bot.entity.position.y` is pinned at `-59.0` in every frame** even though the director
   is `/tp`ed to `y+6` (and to y+8 for the load shot). X and Z track correctly. So either
   mineflayer is not applying the vertical component of a server teleport, or the server is
   snapping the spectator to the surface. Either way the camera may be sitting *in* the
   terrain. **Probe:** log `bot.entity.position` immediately after each `/tp`, and use
   `bot.waitForTicks` + an explicit `bot.entity.position.set()` or a `/tp` with rotation
   (`/tp <x> <y> <z> <yaw> <pitch>`) instead of `lookAt`.
2. **prismarine-viewer's browser scene may simply never receive geometry.** **Probe:** in
   `page.evaluate`, read `window.viewer?.world` / count `scene.children` and log it. That
   single number distinguishes "no data" from "data present but not drawn" and should be the
   very next thing tried — it is ~10 lines and one CI cycle.
3. **Fallback if the viewer proves unworkable:** the `client` backend (real Minecraft under
   Xvfb) is already scaffolded and is the only backend that can photograph particles anyway.
   Switching effort there may be a better use of the next cycle than debugging a renderer we
   do not control.

### 2. Not attempted this session

- **The v1.5.0 release itself** — everything is on `dev`; no PR into `main`, no tag, no
  back-merge. Deliberate: shipping a release whose headline feature produces blank images
  would be shipping a lie.
- **Folia** (C21/C22) — planned in detail in `platform-targets.md`, not built. Needs a
  `PlatformScheduler` plus a shared-state audit that v1.5.0's throttle maps enlarged.
- **Fabric / NeoForge / Sponge / Quilt / Velocity editions** (C21/C22) — each is a separate
  implementation; `core/` extraction is the prerequisite and is itself a full session.
- **Paper API deprecation warnings** — pre-existing in `DespawnBlockIntoAir`,
  `DespawnIntoCooker`, `DespawnIntoVoid`. They violate the ship contract's "no deprecation
  warnings left to rot" clause and should be cleared before v1.5.0 is tagged.
- **Modrinth / CurseForge / Hangar projects** — the workflows are written and gated on token
  secrets, but nothing can publish until the owner creates the projects and adds the
  secrets/variables (listed at the end of `platform-targets.md`).
