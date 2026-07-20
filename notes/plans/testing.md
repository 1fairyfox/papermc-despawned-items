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
| Property testing | Kotest property | ⏳ adding |
| General mocking | MockK | ⏳ adding (when a collaborator needs it) |
| DB integration | SQLite temp / Testcontainers (MySQL) | ✅ SQLite · ⏳ Testcontainers |
| Temp files | JUnit `@TempDir` | ✅ in use |
| Coverage | Kover | ⏳ adding |
| Static analysis | Detekt | ⏳ (pending Kotlin 2.4 tool support) |
| Formatting | Ktlint | ⏳ (pending Kotlin 2.4 tool support) |
| Microbenchmarks | JMH | ⛔ deferred (JUnit bench guards suffice for now) |
| Mutation testing | Pitest | ⛔ deferred |
| CI | GitHub Actions | ⏳ `tests.yml` |

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

**Pure unit (§1–7)** — 🔨
- Value/domain logic: `DespawnLocation`, `LocationStore`, `RewardPool` ✅
- Boundary values (0/1/-1, max stack, world border coords, capacity ±1) ⏳ expand
- Equivalence partitions (owner/member/stranger; valid/invalid material) ⏳
- Negative & exception tests (bad UUID/material/coords, malformed config) ✅ partial
- Kotlin-specific: data-class equality ✅, sealed `DespawnResult` ⏳ (with pipeline refactor),
  null-safety/interop around Paper platform types ⏳, extension-function tests ⏳
- Property-based & fuzz (§5): serialization roundtrip ✅ (kotlin.random) → Kotest ⏳;
  **duplication invariant** (items in = items out + consumed) 🔨 core for a storage plugin
- Parameterized (§3): every reward `Material`, every command, permission matrix ⏳
- Mutation (§6): Pitest ⛔ deferred

**Persistence & data (§13–15)** — 🔨
- Serialization roundtrip + malformed/corrupt handling ✅
- File-system: temp dirs, missing/empty/corrupt file, incremental write ✅
- Database CRUD/replace/persist (SQLite) ✅; transactions ✅ (per-owner txn);
  schema create-if-not-exists ✅; MySQL/MariaDB via Testcontainers ⏳; pool exhaustion ⏳

**MockBukkit (§8–10)** — 🔨
- Lifecycle: enable/disable, command & listener registration ✅ (enable) / ⏳ (disable, dupes)
- Player / world / inventory ⏳ (with pipeline + command wiring)
- Command matrix (§23) ⏳ (with Brigadier rewrite)
- Permission matrix (§8, §59) ⏳ (with limits/permissions work)
- Event-listener (§9–10): `ItemDespawnEvent` handling, cancellation, ignoreCancelled ⏳
- Scheduler (§8 scheduler): throttle/budget, cancel-on-disable ⏳ (with scheduler)
- PDC (§8 PDC): `/recycle` progress key ⏳ (add explicit test)

**Config (§11–12)** — ⏳ (Phase 3)
- `plugin.yml` validity, commands/permissions match code, api-version, no `${}` leftovers ⏳
- `config.yml` defaults, invalid enum/material/sound/particle/range warnings ⏳ (particle ✅ logic)
- Migration fixtures (old→new), idempotent, version bump on success ⏳ (with versioned config)
- Golden-file tests for generated/migrated config ⏳

**Security (§59–64)** — ⏳
- Authorization at service layer (not just command) — ties to limits/permissions ⏳
- Input validation & injection: SQL is **parameterized** ✅; command/config input ⏳
- DoS: despawn-event flood is handled by the throttled scheduler 🔨 (add a stress test)

**Performance (§65–73)** — 🔨
- JUnit scaling guardrails for the store (100k/1M ops, O(1)) ✅
- Tick-time / allocation / soak / JMH ⛔/⏳ (real-server + JMH, later)

**Compatibility (§40–46)** — ⏳
- Version matrix in CI (1.21.11 build; forward-load on 26.1) ⏳
- Folia (§46): only claim after explicit testing; scheduling seam kept ⏳

**Build/packaging (§47–52)** — ⏳
- JAR-content test: `plugin.yml` present, main class, Kotlin runtime shaded, no test classes ⏳
- Reproducible build / dependency verification ⏳

**Real server (§19–22, §83–87)** — ⏳
- Boot smoke (headless Paper 1.21.11) — in CI ⏳; forward-compat 26.1 ⏳
- In-game acceptance via Mineflayer bot (1.21.11) ⏳
- Regression test per fixed bug (§87): recycle & particle fixes → add pinned regressions ⏳

## Confirmed-bug regression pins (§87)

Every fixed bug gets a permanent test that fails before the fix:
- `/recycle` never rewarded (scoreboard objectives absent) → `RecycleProgress` + `RecycleProgressTest` ✅
- Data-bearing particles threw at spawn → `ParticleData` + `ParticleDataTest` ✅
- Stale static strategy list across reloads → instance-scoped `plugin.strategies` (covered by enable test) ✅
- `RemoveMaterials` IOOBE when target owner had no locations → guarded (targets.isEmpty check) ✅
- (Add each new bug here.)
