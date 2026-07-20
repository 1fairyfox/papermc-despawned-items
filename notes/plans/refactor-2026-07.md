# Plan: Full Refactor, Hardening, Testing & Scale-Up (2026-07)

Owner: Fairy Fox · Started: 2026-07-20 · Branch model: git-flow (work on `dev`,
feature branches for big phases, PATCH/MINOR releases to `main`).

Goal: take the working 1.0.0 Kotlin port and turn it into a **solid, modern,
well-tested, well-documented plugin that scales to large servers** with no
performance loss — better APIs, smaller focused files, real config + permissions
+ admin flexibility, and a proper test suite. Nothing publicly released yet
(1.0.0 not cut to `main`), so breaking changes are cheap and welcome.

## Target decision (2026-07-20)

**Retarget from Paper 26.1 / Java 25 → Paper 1.21.x (latest patch, 1.21.11) / Java
21.** Rationale: MockBukkit supports 1.21.x but not the 26.x line yet, so 1.21.x
unlocks the full integration/e2e test tier; 1.21.11 (Dec 2025) is one feature drop
and ~3.5 months behind 26.1 (Mar 2026), covers most live servers today, and also
unblocks Mineflayer in-game bot testing (caps at 1.21.11). Must **verify the built
plugin still loads on a 26.1 server** (Paper forward-compat; watch the 26.x registry
changes). Update `CLAUDE.md`, `status.md`, `plugin.yml` (`api-version`) accordingly.

## Success criteria

- Builds green against real Paper 1.21.x; enables cleanly on a headless server; and
  is verified to still load on a 26.1 server.
- Despawn pipeline is throttled/batched — bounded work per tick regardless of how
  many items despawn at once (large-server safe).
- O(1)/O(log n) location lookups (spatial + owner indexes), no full-list copies on
  the hot path; saves are incremental, off the main thread where possible.
- Granular permissions, per-user self-managed storage with settable limits, and
  group/tier limits (config- and permission-driven).
- Balanced, refined, versioned config with migration; modern Brigadier commands.
- Categorized JUnit 5 suite (unit/logic/integrity/roundtrip/fuzz/bench/regression)
  + headless acceptance smoke; CI `tests.yml`. Modeled on pokered-save-editor-2's
  test taxonomy.
- KDoc throughout, Dokka site wired to fairyfox chrome, professional README,
  filled-in `notes/context/*`.

## Findings from the audit (what's wrong / weak today)

Correctness / bugs:
1. `DespawnProcess.despawnIntos` is a lazily-built **static** list capturing the
   **first** plugin instance's strategies — stale after `/reload`; also global
   mutable state shared with `RemoveMaterials`.
2. Container writes mutate an inventory obtained from one block-state **snapshot**,
   then call `.update()` on a **freshly fetched** snapshot — persistence may no-op.
   Verify against Paper 26.1 semantics; switch to mutating-then-updating the *same*
   captured state (or the live tile-entity inventory).
3. `/recycle` rewards depend on scoreboard objectives (`recycleCount*`) the plugin
   **never creates** — silently does nothing. Move to `PersistentDataContainer`.
4. Particles that require data (DUST, etc.) would throw at `spawnParticle` — no
   data-parameter handling.
5. `BlacklistedItems` actually holds the **allowed** reward pool (misnomer) and is
   a global mutable `object`.

Performance / scale (the "large server" mandate):
6. One full `DespawnProcess` per despawning item, each doing async chunk loads over
   random locations with per-loop `runTaskLater` — unbounded task/IO storm under
   load. Needs a central, throttled scheduler with a per-tick budget, chunk-load
   coalescing, and de-dup.
7. `FileLocations.save()` **clears and rewrites every player file on every
   add/remove** — O(n) main-thread disk I/O. Needs incremental, per-owner, async
   persistence (and/or a single store / SQLite option).
8. Every lookup does `ArrayList(locationEntries).firstOrNull{…}` — full-list copy +
   linear scan. Needs a spatial index (world + packed chunk/pos key) and an
   owner→entries index.
9. `DespawnBlockIntoAir.doesApply` / entity strategy call `getNearbyEntities` on
   every attempt — expensive; cache/limit.
10. No Folia awareness — evaluate `RegionScheduler`/`GlobalRegionScheduler` for
    region-safe scheduling (stretch; at minimum don't assume single main thread).

Design / modernization:
11. Legacy `plugin.yml` commands + brittle manual arg parsing → Paper **Brigadier**
    `Commands` API (typed args, suggestions, subcommands, help).
12. Hand-rolled config getters, no validation, no versioning/migration.
13. No permission granularity, no admin limits, no self-service, no groups.
14. Big files: `DespiSubcommandsB.kt` (490), `DespiSubcommandsA.kt` (230),
    `FileLocations.kt` (178) — split into focused units.
15. `notes/context/*` are unfilled templates; no tests; README could be stronger.

## Phased execution (commit on `dev` each phase; check in between)

### Phase 1 — Core refactor, correctness & scale  (feature/core-refactor)
- Data model: `DespawnLocation` value/data class; block-pos + world key packing;
  `LocationStore` with spatial index (world→chunk→entries) + owner index; O(1)
  add/remove/contains; incremental dirty-tracking persistence (async flush,
  save-on-shutdown, debounced).
- Central `DespawnScheduler`: bounded per-tick budget, coalesced async chunk loads,
  de-dup, backpressure; replaces per-item task spawning. Configurable caps.
- Fix bugs #1–#5 (instance-scoped strategy registry, correct container updates,
  PDC-based recycle, particle-data handling, rename reward pool).
- Split large files; extract pure logic (stack math, key packing, parsing,
  contraband/hazard/blacklist predicates) into server-independent units for tests.
- Modern Kotlin: sealed `DespawnResult`, data classes, `@JvmInline` value classes,
  `ThreadLocalRandom`/`kotlin.random`, Adventure `MiniMessage` for messages.
- Evaluate Folia scheduling; abstract scheduling behind an interface either way.
- Gate: `./gradlew build` green + headless enable.

### Phase 2 — Test suite + CI  (feature/test-suite)
- JUnit 5 + `src/test`; taxonomy after pokered: `unit/` (getters, config parse,
  clamping), `logic/` (index draw-without-replacement, stack splitting, recipe
  predicates, limits), `integrity/` (no dup locations, index covers store,
  owner-index consistency), `roundtrip/` (location ↔ string, config save/load),
  `fuzz/` (random configs & location strings parse safely), `bench/` (lookup /
  index / scheduler at large N — asserts scaling bounds), `regression/` (each bug
  above pinned). Helpers/fixtures + synthetic data generator.
- Headless acceptance: boot Paper 26.1, enable, run a scripted smoke (documented;
  MockBukkit unavailable for 26.x).
- `.github/workflows/tests.yml`; write `notes/plans/testing.md`.
- Gate: suite green in CI.

### Phase 3 — Config, commands, permissions, admin/UX  (feature/config-commands)
- Balanced versioned config w/ migration; sections for effects, performance/scaling
  caps, storage backend, and limits/groups. Validation + friendly errors.
- Permissions: granular nodes; **per-user self-managed storage with settable
  limits**; **group/tier limits** via permission nodes (`despi.limit.<n>`) and/or
  config-defined tiers; admin overrides. Meet the average-admin quality bar.
- Rewrite commands on Brigadier: `/despi` subcommands + `/recycle`, typed args,
  suggestions, permission-scoped help.
- Gate: build green + headless command smoke.

### Phase 4 — Docs, Dokka, README  (feature/docs)
- KDoc throughout; fill `notes/context/{project,architecture,principles}.md`,
  `systems/overview.md`, `decisions/architecture.md`.
- Wire Dokka (already themed) to publish; verify `dokkaGenerate`.
- Professional README: features, install, config, permissions/limits, commands,
  performance/scaling notes, build/test, badges.
- Gate: `dokkaGenerate` clean; README reviewed.

## Notes / open questions
- SQLite vs YAML store: default YAML (incremental), optional SQLite for large
  servers — decide in Phase 1 by measuring.
- Folia: aim for region-safe scheduling; if too costly, ship Paper-only but keep the
  scheduling seam. Record decision in `decisions/architecture.md`.
- Version bumps: Phase 1–3 are MINOR (new behavior/config) on the way to a cut
  `v1.x` once stable; keep `VERSION` current per commit.
