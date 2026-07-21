# MockBukkit Harness — Deviations & Bridges

Project-specific lore for writing MockBukkit (4.110) tests here. The bridges live in
`src/test/kotlin/io/fairyfox/papermc/despawneditems/TestSupport.kt`. Read this before
writing any test that touches worlds, blocks, or player targeting.

## The three deviations (and their bridges)

1. **`Player.getTargetBlockExact` is unimplemented** (throws
   `UnimplementedOperationException`). Bridge: `TargetingPlayerMock` — a `PlayerMock`
   subclass with a scriptable `target` block; add it via `server.addPlayer(mock)`. All
   `/despi … here/this` flows depend on this.
2. **`World.getChunkAtAsync` is unimplemented.** Bridge: `SyncChunkWorldMock` — a
   `WorldMock` overriding `getChunkAtAsync(x, z, gen, urgent)` to complete immediately.
   Bonus: `DespawnProcess`/`RemoveMaterials` become deterministic — one location
   attempt per `performTicks(1)`.
3. **Container `state.update()` wipes the live inventory.** MockBukkit's
   `ContainerStateMock.update()` restores the (stale, empty) snapshot inventory over
   the live one; the production pattern (mutate `getInventory()`, then
   `block.state.update()`) is fine on real servers but self-destructs under the mock.
   Bridge: `stickyContainer(block)` installs a state whose `update()` is a no-op
   (supported: CHEST, FURNACE, BLAST_FURNACE, SMOKER). Install it right after setting
   `block.type` — **setting the type re-mocks the state**, discarding the sticky one.

## Smaller gotchas

- `PlayerMock.nextMessage()` returns legacy `§`-coded text — assert via the `plain()`
  helper.
- Block states are shared live instances (`block.state === block.state`), not
  snapshots; mutate them directly for fixtures (no `update()` needed).
- `editConfig(plugin, "path" to value, …)` writes config.yml on disk and reloads
  `plugin.settings` — use it instead of poking the cached `FileConfiguration`.
- Brigadier commands registered through Paper's lifecycle events DO dispatch via
  `server.dispatchCommand` — the full command/permission matrix is testable.
- Recipes (`server.addRecipe`), entity spawns (zombie, armor stand, item frame,
  chest/furnace minecarts), PDC, sounds, and particles (null data) all work.
- A `DespawnProcess` constructed while no locations exist self-destroys but keeps its
  `item` — the tests use that as a lightweight carrier for strategy unit tests.

## Where the real-server line is

MockBukkit proves code against the mocked API. The layers beyond it are ALSO
automated now:

- **MySQL/MariaDB** — `MariaDbStorageTest` (Testcontainers) runs against a real
  MariaDB in CI; it disables itself where Docker is unreachable.
- **Real-server boot** — `scripts/server-smoke.sh` boots Paper 1.21.11 + 26.1.2 with
  the built jar in CI (`server-smoke` matrix job).
- **Client layer** — `scripts/ingame-smoke.mjs` joins a real server with a Mineflayer
  bot and asserts plugin replies reach the client (`ingame-smoke` job).

Still mock-unprovable and not otherwise automated: tile-entity content copies through
`BlockStateMeta` (`copyBlockToLocation` deep branches) — manual real-server territory.

## Local Windows + Docker Desktop 29.5 gotcha (2026-07)

Testcontainers 1.21.3's docker-java cannot negotiate with Docker Desktop 29.5 on
Windows — every transport (both npipes, and a socat TCP proxy to the daemon socket)
gets `Status 400` with an empty Info body during strategy probing, while the daemon
itself answers curl fine (API 1.54, min 1.40). Treated as a TC-side incompatibility:
the MariaDB tests skip locally (`disabledWithoutDocker = true`) and execute in CI
(Linux, unix socket). Retest locally on each Testcontainers upgrade.
