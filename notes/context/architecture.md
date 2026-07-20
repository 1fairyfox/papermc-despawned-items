# Architecture

How the repo is put together. *Why* behind choices → `../decisions/architecture.md`;
runtime data flow → `../systems/overview.md`.

## Stack

- **Language:** Kotlin 2.4.x. **Runtime:** Java 21. **Server API:** Paper 1.21.11
  (`compileOnly`; loads on 26.x via forward-compat).
- **Build:** Gradle 9.6.1 (Kotlin DSL) + Shadow (shades the Kotlin stdlib into one jar) +
  Dokka (API docs). JDK 21 auto-provisioned via the foojay toolchain resolver.
- **Storage:** YAML (Bukkit config) by default; SQLite / MySQL / MariaDB via JDBC +
  HikariCP, loaded at runtime through Paper's `libraries:` loader (not shaded).
- **Testing:** JUnit 5 + MockBukkit (`mockbukkit-v1.21`), `@TempDir`, SQLite for DB tests.
  `build` runs the suite; CI runs it on every push.

## Layout of the repo

```
src/main/kotlin/com/popupmc/despawneditems/
  PaperMcDespawnedItems.kt plugin entry point (owns settings, locations, strategies, scheduler)
  RewardPool.kt            allow-list of /recycle reward materials
  config/                  Config + FileConfig/StorageSettings/PerformanceSettings/LimitSettings
  location/                DespawnLocation, BlockKey, LocationStore (indexes),
                           LocationRepository (+Yaml/Jdbc impls), StorageFactory, LocationManager
  limit/                   DespawnLimits (permission-tier cap resolver)
  despawn/                 DespawnScheduler (throttle), DespawnProcess (pipeline), DespawnEffect
    into/                  the 5 relocation strategies + AbstractDespawnInto + DespawnIntoResult
  events/                  OnItemDespawnEvent (enqueues into the scheduler)
  commands/                OnDespiCommand + subcommands (A/B), OnRecycleCommand, AbstractDespiCommand
  manage/                  RemoveMaterials (tick-spread bulk purge)
src/main/resources/        plugin.yml (commands/perms/libraries), config.yml, locations.yml
src/test/kotlin/…          mirrors main; unit / mockbukkit / database / performance tests
build.gradle.kts · settings.gradle.kts · VERSION · docs-theme/ (Dokka css)
```

## Key seams

- **`plugin.settings`** (`Config`) — effect + storage + performance + limit settings,
  re-read from disk on `/despi reload do`.
- **`plugin.locations`** (`LocationManager`) — the single façade over the in-memory
  `LocationStore` (spatial + owner indexes) and the persistent `LocationRepository`.
- **`plugin.strategies`** — instance-scoped ordered relocation strategies (rebuilt each
  enable; never a stale static list).
- **`plugin.despawnScheduler`** — bounds how many relocations start per tick.
