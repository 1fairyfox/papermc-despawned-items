package com.popupmc.despawneditems.location

import java.util.UUID

/**
 * A storage backend for despawn locations. Implementations:
 *
 *  - [YamlLocationRepository] — flat `userdata/<uuid>.yml` files; zero-config default,
 *    byte-compatible with the original plugin's data.
 *  - [JdbcLocationRepository] — SQLite (embedded) or MySQL/MariaDB (networked) over a
 *    HikariCP pool; indexed, transactional, and shareable across servers.
 *
 * All methods are called from the server main thread unless a backend documents
 * otherwise; a backend must be safe to call [saveOwners] repeatedly with small batches.
 */
interface LocationRepository {

    /** Loads every stored location. Malformed entries are skipped, not fatal. */
    fun loadAll(): List<DespawnLocation>

    /**
     * Persists exactly the owners in [owners], sourcing each owner's current locations
     * from [locationsOf]. An owner with no locations is removed from storage. This is
     * the incremental write path — callers pass only the owners that changed.
     */
    fun saveOwners(owners: Collection<UUID>, locationsOf: (UUID) -> Collection<DespawnLocation>)

    /** Releases any resources (e.g. a connection pool). No-op for file backends. */
    fun close() {}
}
