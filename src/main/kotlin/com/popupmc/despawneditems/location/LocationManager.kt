package com.popupmc.despawneditems.location

import com.popupmc.despawneditems.PaperMcDespawnedItems
import org.bukkit.Location
import java.util.UUID

/**
 * Owns the in-memory [LocationStore] and the persistent [LocationRepository], and is the
 * single entry point the rest of the plugin uses to read and mutate despawn locations.
 *
 * Persistence is **incremental and debounced**: a mutation marks its owner dirty and
 * schedules a flush a few seconds later; the flush snapshots the dirty owners on the
 * main thread and writes them off-thread. A synchronous [shutdown]/[saveNow] path exists
 * for plugin disable and `/despi save`, when async tasks can't run.
 */
class LocationManager(private val plugin: PaperMcDespawnedItems) {
    var store: LocationStore = LocationStore()
        private set

    private var repository: LocationRepository = YamlLocationRepository(plugin.dataFolder, plugin.logger)
    private var pendingFlushTaskId: Int = -1

    /** Builds the configured backend and loads all locations into memory. */
    fun load() {
        repository = StorageFactory.create(plugin)
        store = LocationStore()
        store.replaceAll(repository.loadAll())
    }

    /** Flushes, closes the current backend, and reloads (used by `/despi reload`). */
    fun reload() {
        saveNow()
        runCatching { repository.close() }
        load()
    }

    /** Synchronously flushes and closes the backend (used on plugin disable). */
    fun shutdown() {
        cancelScheduledFlush()
        flushBlocking()
        runCatching { repository.close() }
    }

    private fun keyOf(location: Location): BlockKey = BlockKey(location.world.name, location.blockX, location.blockY, location.blockZ)

    // --- mutations (each schedules an incremental flush when it changes state) ---

    fun add(
        location: Location,
        owner: UUID,
    ): Boolean = mutate { store.add(DespawnLocation.of(location, owner)) }

    fun remove(
        location: Location,
        owner: UUID,
    ): Boolean = mutate { store.remove(DespawnLocation.of(location, owner)) }

    /** Removes one (arbitrary) owner's entry at [location]. */
    fun removeOneAt(location: Location): Boolean =
        mutate {
            val victim = store.at(keyOf(location)).firstOrNull() ?: return@mutate false
            store.remove(victim)
        }

    /** Removes one (arbitrary) location owned by [owner]. */
    fun removeOneOfOwner(owner: UUID): Boolean =
        mutate {
            val victim = store.ofOwner(owner).firstOrNull() ?: return@mutate false
            store.remove(victim)
        }

    fun removeAllAt(location: Location): Int = mutateCount { store.removeAt(keyOf(location)) }

    fun removeAllOfOwner(owner: UUID): Int = mutateCount { store.removeOwner(owner) }

    fun clearAll(): Int =
        mutateCount {
            val n = store.size
            store.clear()
            n
        }

    /** Replaces the whole store (used by solo-mode testing); marks everything dirty. */
    fun replaceWith(locations: Collection<DespawnLocation>) {
        store.clear()
        locations.forEach { store.add(it) }
        scheduleFlush()
    }

    private inline fun mutate(block: () -> Boolean): Boolean {
        val changed = block()
        if (changed) scheduleFlush()
        return changed
    }

    private inline fun mutateCount(block: () -> Int): Int {
        val n = block()
        if (n > 0) scheduleFlush()
        return n
    }

    // --- queries ---

    fun has(
        location: Location,
        owner: UUID,
    ): Boolean = store.contains(DespawnLocation.of(location, owner))

    fun anyAt(location: Location): Boolean = store.ownersAt(keyOf(location)).isNotEmpty()

    fun ownersAt(location: Location): Set<UUID> = store.ownersAt(keyOf(location))

    fun atLocation(location: Location): List<DespawnLocation> = store.at(keyOf(location))

    fun ofOwner(owner: UUID): Set<DespawnLocation> = store.ofOwner(owner)

    fun firstOfOwner(owner: UUID): DespawnLocation? = store.ofOwner(owner).firstOrNull()

    val count: Int get() = store.size

    fun countOfOwner(owner: UUID): Int = store.countOfOwner(owner)

    fun isEmpty(): Boolean = store.isEmpty()

    fun all(): List<DespawnLocation> = store.all()

    fun random(): DespawnLocation? = store.randomOrNull()

    // --- persistence ---

    /** Forces a synchronous flush of everything dirty (used by `/despi save`). */
    fun saveNow() {
        cancelScheduledFlush()
        flushBlocking()
    }

    private fun scheduleFlush() {
        if (pendingFlushTaskId != -1) return // a flush is already queued
        if (!plugin.isEnabled) return
        pendingFlushTaskId =
            plugin.server.scheduler.runTaskLater(
                plugin,
                Runnable {
                    pendingFlushTaskId = -1
                    flushDirtyAsync()
                },
                FLUSH_DELAY_TICKS,
            ).taskId
    }

    private fun cancelScheduledFlush() {
        if (pendingFlushTaskId != -1) {
            plugin.server.scheduler.cancelTask(pendingFlushTaskId)
            pendingFlushTaskId = -1
        }
    }

    /** Snapshots dirty owners on the main thread, then writes them off-thread. */
    private fun flushDirtyAsync() {
        val dirty = store.dirtyOwnersSnapshot()
        if (dirty.isEmpty()) return
        val snapshot = dirty.associateWith { store.ofOwner(it).toList() }
        store.clearDirty(dirty)
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable { repository.saveOwners(dirty) { snapshot[it].orEmpty() } },
        )
    }

    /** Synchronous flush (safe during disable, where async tasks won't run). */
    private fun flushBlocking() {
        val dirty = store.dirtyOwnersSnapshot()
        if (dirty.isEmpty()) return
        val snapshot = dirty.associateWith { store.ofOwner(it).toList() }
        repository.saveOwners(dirty) { snapshot[it].orEmpty() }
        store.clearDirty(dirty)
    }

    companion object {
        /** Debounce window before a batch of changes is written (100 ticks = 5s). */
        private const val FLUSH_DELAY_TICKS = 100L
    }
}
