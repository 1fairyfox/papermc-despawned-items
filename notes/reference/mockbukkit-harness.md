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

MockBukkit proves code against the mocked API; it cannot prove: MySQL connections
(`StorageFactory.buildMysql`), tile-entity content copies through `BlockStateMeta`
(`copyBlockToLocation` deep branches), real chunk loading, or client-visible behaviour.
Those stay headless-Paper / Mineflayer territory — see `notes/plans/testing.md`
(§19–22, §83–87).
