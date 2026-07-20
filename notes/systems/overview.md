# System Map — overview

The machine, end to end. The main systems and how they fit together.

## Despawn relocation pipeline (the core)

**Input:** a ground item reaching its despawn timer (`ItemDespawnEvent`).

1. `OnItemDespawnEvent` clones the item and **enqueues** it into `DespawnScheduler`.
2. `DespawnScheduler` drains the queue once per tick, starting at most
   `performance.max-per-tick` relocations and never exceeding `performance.max-concurrent`
   in flight (queue capped by `performance.max-queue`). This is the large-server throttle.
3. Each relocation is a `DespawnProcess`: it draws random, not-yet-tried despawn
   locations from `LocationManager`, async-loads the chunk, and offers the item to each
   strategy in `plugin.strategies` in order:
   `Void → Cooker → BlockIntoAir → ItemIntoEntity → Storage`.
4. On a partial/full placement, `DespawnEffect` plays the configured sound + particles.

**Output:** the item is placed into a container/cooker/entity/air, destroyed (contraband),
or dropped (no space found within the budget).

## Location store & persistence

- `LocationManager` (`plugin.locations`) is the façade. It holds a `LocationStore`
  (spatial index block→owners, owner index owner→locations, flat bag for O(1) random
  draw) and a `LocationRepository`.
- Mutations mark the owner dirty and schedule a **debounced async flush**; disable and
  `/despi save` flush synchronously.
- `StorageFactory` builds the repository from `storage.type`: `YamlLocationRepository`
  (per-owner files) or `JdbcLocationRepository` (SQLite / MySQL via HikariCP), migrating
  YAML → DB on first switch.

## Limits & permissions

- `DespawnLimits` resolves a player's cap from `limits.unlimited` / `despi.limit.bypass`
  / the highest `despi.limit.<n>` permission / `limits.default`. `/despi add` enforces it
  for self-service adds; admins (`despi.elevated`) adding for others are uncapped.

## Commands

- `/despi` dispatches to registered subcommands (`OnDespiCommand` → `AbstractDespiCommand`
  implementations): add, remove, clear, exists, locations, purge, despawn, effects,
  reload, save.
- `/recycle` despawns the held item and tracks reward progress in the player's PDC.

## Bulk purge

- `RemoveMaterials` walks the relevant locations one per tick, removing matching
  materials/items from each via the same strategies (reverse of placement).
