# Testing Strategy & Standard

This project adopts the **Comprehensive testing checklist for Kotlin PaperMC plugins**
as its testing standard. This file maps that checklist onto *this* plugin (a despawn-item
relocation + `/recycle` + storage plugin), records what's applicable, and tracks status.
Not every one of the 95 sections applies — this plugin has no economy, GUIs, chat,
combat, or (yet) coroutines — but a large majority do, especially the storage-integrity,
persistence, permission, config-migration, property/fuzz, and performance layers.

## Target: the testing pyramid

| Layer | Target share | Where |
|-------|-------------:|-------|
| Pure Kotlin unit + property tests | 50–65% | `unit/`, `property/`, `serialization/` |
| MockBukkit / component tests | 20–30% | `mockbukkit/` |
| Real-Paper integration + smoke | 10–15% | `integrationTest/` + CI smoke |
| End-to-end / acceptance | 5–10% | in-game bot (Mineflayer 1.21.11) + CI |

> **The rule:** MockBukkit proves the code behaves against the mocked API; a real Paper
> server proves the mock's assumptions hold. Neither replaces the other.

## Tool stack (adopted / planned)

| Purpose | Tool | Status |
|---------|------|--------|
| Runner | JUnit Jupiter | ✅ in use |
| Kotlin assertions | `kotlin.test` | ✅ in use |
| Paper/Bukkit mocks | MockBukkit (`mockbukkit-v1.21`) | ✅ in use |
| Property testing | Kotest property 5.9 | ✅ in use (`PropertyInvariantsTest`) |
| General mocking | MockK | ⛔ not needed (harness subclassing covers every seam so far) |
| DB integration | SQLite temp / Testcontainers (MariaDB) | ✅ both (containers run in CI; clean local skip without Docker) |
| Temp files | JUnit `@TempDir` | ✅ in use |
| Coverage | Kover 0.9.9 | ✅ gates `build` — **min 90% line** (`koverVerify`); suite ~95% |
| Static analysis | Detekt 1.23.8 | ✅ gates `build` (JDK-21 daemon + tuned config, **no baseline**) |
| Formatting | Ktlint 12.1.2 | ✅ gates `build`; codebase formatted |
| SAST | CodeQL (java-kotlin, traced compile) | ✅ dev pushes informational; **gates the release PR**; Kotlin pinned 2.4.0 for it |
| Microbenchmarks | JMH (me.champeau.jmh) | ✅ `./gradlew jmh` + weekly CI artifact (informational) |
| Mutation testing | Pitest 1.19 (pure core) | ✅ `./gradlew pitest` + weekly CI artifact — 84% test strength (informational) |
| Real-server profiling | spark (bundled) + JFR | ✅ numbers in every in-game CI run + downloadable .jfr artifact |
| CI | GitHub Actions | ✅ `ci.yml` (build+tests+coverage→Codecov+Sonar · `server-smoke` 1.21.11+26.1.2 · `ingame-smoke` Mineflayer) · `codeql.yml` · `scorecard.yml` |

## Source organization (target)

```
src/test/kotlin/com/popupmc/despawneditems/
  unit/           pure logic (value types, config parsing, predicates, math)
  property/       property-based + fuzz
  serialization/  (de)serialization roundtrips
  database/       repository CRUD/transaction/pool
  mockbukkit/     lifecycle, commands, permissions, events, PDC, scheduler
src/test/resources/
  fixtures/  configs/  golden/
```
(Currently tests live under `location/`; migrate to the above as the suite grows.)

## Applicability map & status

Legend: ✅ done · 🔨 in progress · ⏳ planned · ⛔ not applicable to this plugin

**Pure unit (§1–7)** — ✅ (2026-07-21 pass; coverage 44%→95%+, Kover-gated ≥90)
- Value/domain logic: `DespawnLocation`, `LocationStore`, `RewardPool` ✅
- Boundary values (limit 0/±1, max stack 64 overflow, queue cap ±1, pool bounds) ✅
- Equivalence partitions (self/other-owner; valid/invalid material; fuel/smelt/neither) ✅
- Negative & exception tests (bad UUID/material/coords, malformed config, bad command names) ✅
- Kotlin-specific: data-class equality ✅; sealed `DespawnResult` ⛔ N/A (pipeline kept
  the enum `DespawnIntoResult` — fully covered); null-safety around Paper types ✅
  (null process items, null equipment); extension fns (`sendColored`) ✅ (via feedback tests)
- Property-based & fuzz (§5): ✅ **Kotest property** adopted
  (`PropertyInvariantsTest`: generator-driven roundtrips, garbage resilience, store
  set-semantics under random op sequences, reward arithmetic) plus the original
  kotlin.random roundtrip; **duplication invariant** ✅ (storage + process + cooker)
- Parameterized (§3): reward-pool exclusion classes ✅, **full command × permission
  matrix as individual JUnit nodes** ✅ (`CommandPermissionMatrixTest` `@TestFactory`:
  41 commands × 3 levels = 123 cases, allowed AND denied asserted individually,
  plus a multi-player group scenario ending in a verified delivery)
- Mutation (§6): ✅ **Pitest 1.19** adopted, scoped to the pure core (store,
  locations, reward, command-name parsing) — 107 mutants, 67% killed, **test
  strength 84%**; informational (`./gradlew pitest`, weekly `bench.yml` job).
  MockBukkit/container suites deliberately excluded (mutating API plumbing under a
  mock times minions out without signal)

**Persistence & data (§13–15)** — ✅ (except live-MySQL)
- Serialization roundtrip + malformed/corrupt handling ✅
- File-system: temp dirs, missing/empty/corrupt file, incremental write, empty-owner
  file deletion ✅
- Database CRUD/replace/persist (SQLite) ✅; transactions ✅ + rollback-on-failure ✅;
  schema create-if-not-exists ✅; malformed-row skip ✅; **pool exhaustion ✅**
  (`SQLException` surfaces + recovers, `StorageFactoryTest`); YAML→DB migration ✅
  (one-time, no duplication on restart); backend selection incl. aliases + unknown
  fallback ✅; MySQL/MariaDB connect ✅ **real server via Testcontainers**
  (`MariaDbStorageTest`: buildMysql, CRUD roundtrip, replace semantics, YAML→MySQL
  one-time migration, full manager lifecycle) — runs in CI (Docker available);
  disables itself cleanly without Docker. Local Windows note: Testcontainers 1.21.3's
  docker-java cannot negotiate with Docker Desktop 29.5 (400 on all transports) — a
  TC-side incompat, retest on TC upgrades (see `reference/mockbukkit-harness.md`)

**MockBukkit (§8–10)** — ✅
- Lifecycle: enable ✅, disable-flushes ✅, scheduler cancel-on-disable ✅
- Player / world / inventory ✅ (harness: `TargetingPlayerMock`, `SyncChunkWorldMock`,
  `stickyContainer` — see `notes/reference/testing.md` for the MockBukkit deviations)
- Command matrix (§23) ✅ — every `/despi` branch + `/recycle` + `/despi recycle`
  dispatched through Brigadier
- Permission matrix (§8, §59) ✅ — none / `despi.use` / elevated / op / `recycle.use`
  revoked, at dispatch level
- Event-listener (§9–10): `ItemDespawnEvent` enqueue ✅, cancelled-event skip ✅
- Scheduler: throttle/budget ✅, queue caps both drop policies ✅, cancel-on-disable ✅
- PDC: `/recycle` progress key read/advance/reset ✅

**Config (§11–12)** — ✅ (migration N/A)
- `plugin.yml` validity: name, main-class loads, api-version, `${version}` expanded,
  all four permissions declared, `libraries:` complete ✅ (`PluginYmlValidityTest`)
- `config.yml` defaults ✅, invalid values coerced ✅, unknown particle fallback ✅,
  live reload picks up edits ✅, command rename/alias validation ✅
- Migration fixtures ⛔ N/A (config is not versioned; adopt when it becomes so)
- Golden-file tests ⏳ (low value while defaults are asserted directly)

**Security (§59–64)** — ✅
- Authorization: dispatch-level permission matrix ✅ + limit checks at action layer ✅
- Input validation & injection: SQL parameterized ✅ (+ malformed-row handling ✅);
  command input (bad materials, bad names) rejected with feedback ✅; command-rename
  names sanitized against a strict pattern ✅
- DoS: flood stress test ✅ — 10k-item intake <2s, bounded drain, per-tick budget and
  max-concurrent proven at dispatch (`DespawnLoadTest`)

**Performance (§65–73)** — ✅ full stack (measured 2026-07-21)
- Store scaling guardrails (100k/1M ops, O(1)) ✅ (kept)
- **Combined load** ✅ (`CombinedLoadTest`): despawn storm + 50 players commanding
  every tick → avg 0.79ms / p95 1.17ms / max 8.2ms per tick; 1000-command burst from
  100 players in 39ms; **throughput/conservation**: a 5000-item burst fully lands
  (nothing lost) in 251 ticks vs 250 theoretical; budget scaling proven at
  100/tick; persistence consistent while flushing mid-storm
- **JMH microbenchmarks** ✅ (`src/jmh`, `./gradlew jmh`, weekly `bench.yml`):
  store random-draw ~7ns, contains ~7ns, ownersAt ~15ns, add+remove ~119ns at 100k
  entries; parse ~76ns, serialize ~4ns
- **Real-server profiling** ✅: the in-game CI job storms 200×64 forced despawns,
  samples `spark tps`/`spark health` (numbers in the CI log), and uploads a full
  **JFR profile** (`server-jfr-profile` artifact) from every run
- Soak (multi-hour) ⏳ — the one remaining §65–73 item, needs a long-lived server

**Compatibility (§40–46)** — ⏳
- Version matrix in CI (1.21.11 build; forward-load on 26.1) ⏳
- Folia (§46): only claim after explicit testing; scheduling seam kept ⏳

**Build/packaging (§47–52)** — ⏳
- JAR-content test: `plugin.yml` present, main class, Kotlin runtime shaded, no test classes ⏳
- Reproducible build / dependency verification ⏳

**Real server (§19–22, §83–87)** — ✅ (automated in CI, 2026-07-21)
- Boot smoke (headless Paper) ✅ **in CI on every push/PR** — `scripts/server-smoke.sh`
  via the `server-smoke` matrix job: 1.21.11 (target, Java 21) AND 26.1.2
  (forward-compat, Java 25); asserts clean enable, no load errors, no self-disable
- In-game acceptance via Mineflayer bot ✅ **in CI, full permission story** —
  `scripts/ingame-smoke.mjs`: unprivileged denial verified from the client side,
  console `op` grant, `/despi add this` against a genuinely looked-at block,
  `locations mine`, console `give` + `/recycle` → "Done!", then a load phase (200×64
  forced despawns while the bot keeps commanding) with spark numbers and a JFR
  profile artifact; post-load responsiveness asserted (validated locally first)
- Regression test per fixed bug (§87) ✅ — see the pins section below

## Confirmed-bug regression pins (§87)

Every fixed bug gets a permanent test that fails before the fix:
- `/recycle` never rewarded (scoreboard objectives absent) → `RecycleProgress` + `RecycleProgressTest` ✅
- Data-bearing particles threw at spawn → `ParticleData` + `ParticleDataTest` ✅
- Stale static strategy list across reloads → instance-scoped `plugin.strategies` (covered by enable test) ✅
- `RemoveMaterials` IOOBE when target owner had no locations → guarded (targets.isEmpty check) ✅
- **`/despi purge` never purged containers** — `DespawnIntoVoid` applies to every block,
  removes nothing, and `break`ed the removal chain → `supportsRemoval` flag
  (`RemoveMaterialsTest`, found 2026-07-21 by the new suite) ✅
- **Reward pool could hand out non-item materials** (`DEAD_HORN_CORAL_WALL_FAN`) and
  crash the reward drop → `isItem` filter (`RecycleActionTest` threshold test +
  `PluginEnableTest` pool assertions) ✅
- **Entity purge-by-material never cleared storage-minecart stacks** (delegated to the
  exact-amount `ItemStack` overload) → amount-agnostic `Inventory.remove(Material)`
  path (`DespawnItemIntoEntityTest.removeFrom`) ✅
- (Add each new bug here.)
