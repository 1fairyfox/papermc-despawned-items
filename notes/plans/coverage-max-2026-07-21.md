# Plan — coverage maximization + full-layer test pass (2026-07-21)

**Goal (owner mandate).** Codecov ≥90% (aim near 100%); real tests on every feature at
every testable layer — permissions, data backends, commands, management, config,
high-load, performance; Scorecard as high as a single-collaborator repo can get.

**Baseline (measured).** Kover line coverage **44.2%** (535/1211), branch 20.7%.
Zero/near-zero classes: `RemoveMaterials` 0/61, `DespawnProcess` 0/54,
`DespawnBlockIntoAir$Companion` 0/49, `DespawnEffect` 0/36, `RecycleAction` 0/19,
`DespawnItemIntoEntity` 1/87, `DespawnIntoCooker` 1/61, `DespiActions` 4/127,
`DespawnIntoStorage` 1/23, `StorageFactory` 3/41, `CommandFeedback` 1/9.

## Phases

1. **Command + permission layer** — MockBukkit Brigadier dispatch for every `/despi`
   subcommand + `/recycle`; permission matrix (none / `despi.use` / `despi.elevated` /
   `recycle.use`), player-vs-console gating, invalid-input paths; `DespiActions` direct
   tests; `CommandFeedback` incl. `targetBlock` failure path.
2. **Despawn pipeline** — `DespawnProcess` (placement, partial, contraband, no-locations,
   unloaded world, exhaustion), `DespawnEffect` (loops, disable paths, force-destroy),
   all five `into/*` strategies against real MockBukkit worlds/blocks/entities,
   `OnItemDespawnEvent` incl. cancelled-event skip.
3. **Management** — `RemoveMaterials`: owner-filtered/global walks, both-null error,
   empty-store, progress messages, per-sender exclusivity, force-destroy.
4. **Data backends** — `StorageFactory` selection (yaml/aliases/sqlite/unknown),
   YAML→SQLite migration, sqlite pool behaviour; `LocationManager` debounced flush,
   `reload`/`shutdown`/`saveNow` paths. MySQL connect is N/A without a server
   (Testcontainers deferred — CI-only Docker dependency).
5. **Config + packaging** — config defaults/invalid/reload; `plugin.yml` validity test
   (main class, api-version, permissions match code constants).
6. **High-load + performance** — queue cap with both drop policies, per-tick budget,
   max-concurrent bound, flood (10k+) guardrails, existing store benches kept.
7. **Coverage gate** — Kover verify rule: min 90% line coverage, gating `check`/`build`.
8. **Scorecard** — repo-side re-audit: token permissions on all workflows, pinned
   actions, dangerous-workflow scan; note externals that stay owner-side.
9. **Release** — green build → notes/changelog/VERSION → commit `dev` → PR to `main`.

## Checklist enumeration (notes/plans/testing.md items this pass touches)

Each item's outcome is recorded individually in testing.md at completion (S8/S9 apply):

- §1–7 boundary/equivalence/negative/parameterized expansion — commands + config
- §3 parameterized: reward materials, command matrix, permission matrix
- §5 duplication invariant (items in = out + consumed) — storage strategy tests
- §8 lifecycle disable; PDC (`/recycle` progress); scheduler cancel-on-disable
- §9–10 `ItemDespawnEvent` handling + ignoreCancelled
- §11–12 `plugin.yml` validity; config invalid-value warnings
- §13–15 migration (YAML→DB); pool behaviour
- §23 command matrix; §59 permission matrix at dispatch level
- §59–64 authorization, input validation (bad material names), DoS flood test
- §65–73 performance guardrails (kept + extended)
- §47–52 jar-content — deferred to a Gradle task follow-up if time runs out (disclose)
- §87 regression pins — kept; no new bugs fixed this pass unless found

## Not in scope (disclosed up front)

Real-server headless smoke in CI, Mineflayer in-game bot, Testcontainers MySQL, JMH,
Pitest — all remain ⏳/⛔ in testing.md with reasons. External badge activations
(Codecov app/token, Sonar, CodeFactor, SCORECARD_TOKEN PAT) remain owner-side.
