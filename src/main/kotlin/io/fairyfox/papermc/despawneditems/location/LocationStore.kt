package io.fairyfox.papermc.despawneditems.location

import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

/**
 * In-memory index over every despawn location. Replaces the old flat `ArrayList` that
 * was copied and linearly scanned on every query and rewritten to disk in full on every
 * change. Provides:
 *
 *  - **O(1)** [add], [remove], [contains] and owner/block lookups;
 *  - a **spatial index** ([byBlock]) — block position → owners at that block;
 *  - an **owner index** ([byOwner]) — owner → their locations (for limits & bulk ops);
 *  - a **flat bag** with swap-remove for **O(1) uniform [randomOrNull] draw**;
 *  - **dirty-owner tracking** so persistence writes only the owners that changed.
 *
 * Not thread-safe by design: it is mutated only from the server main thread (events and
 * commands). Async persistence takes a snapshot on the main thread first.
 */
class LocationStore {
    private val byBlock: MutableMap<BlockKey, MutableMap<UUID, DespawnLocation>> = HashMap()
    private val byOwner: MutableMap<UUID, MutableSet<DespawnLocation>> = HashMap()

    /** Flat list backing O(1) random draw; kept in sync with [flatIndex] for swap-remove. */
    private val flat: MutableList<DespawnLocation> = ArrayList()
    private val flatIndex: MutableMap<DespawnLocation, Int> = HashMap()

    private val dirtyOwners: MutableSet<UUID> = HashSet()

    /** Total number of stored locations. */
    val size: Int get() = flat.size

    fun isEmpty(): Boolean = flat.isEmpty()

    fun contains(loc: DespawnLocation): Boolean = byBlock[loc.blockKey]?.containsKey(loc.owner) == true

    /** Adds [loc]. Returns false (no-op) if that owner already has that block. */
    fun add(loc: DespawnLocation): Boolean {
        val owners = byBlock.getOrPut(loc.blockKey) { HashMap() }
        if (owners.containsKey(loc.owner)) return false
        owners[loc.owner] = loc
        byOwner.getOrPut(loc.owner) { HashSet() }.add(loc)
        flat.add(loc)
        flatIndex[loc] = flat.lastIndex
        dirtyOwners.add(loc.owner)
        return true
    }

    /** Removes [loc]. Returns false if it was not present. */
    fun remove(loc: DespawnLocation): Boolean {
        val owners = byBlock[loc.blockKey] ?: return false
        val existing = owners.remove(loc.owner) ?: return false
        if (owners.isEmpty()) byBlock.remove(loc.blockKey)
        byOwner[loc.owner]?.let { set ->
            set.remove(existing)
            if (set.isEmpty()) byOwner.remove(loc.owner)
        }
        removeFromFlat(existing)
        dirtyOwners.add(loc.owner)
        return true
    }

    /** O(1) swap-remove from the flat bag. */
    private fun removeFromFlat(loc: DespawnLocation) {
        val idx = flatIndex.remove(loc) ?: return
        val last = flat.lastIndex
        if (idx != last) {
            val moved = flat[last]
            flat[idx] = moved
            flatIndex[moved] = idx
        }
        flat.removeAt(last)
    }

    // --- queries ---

    /** Owners registered at [key]. */
    fun ownersAt(key: BlockKey): Set<UUID> = byBlock[key]?.keys?.toSet() ?: emptySet()

    /** Locations registered at [key] (one per owner). */
    fun at(key: BlockKey): List<DespawnLocation> = byBlock[key]?.values?.toList() ?: emptyList()

    /** All locations owned by [owner]. */
    fun ofOwner(owner: UUID): Set<DespawnLocation> = byOwner[owner]?.toSet() ?: emptySet()

    /** How many locations [owner] has (used for per-user limits). */
    fun countOfOwner(owner: UUID): Int = byOwner[owner]?.size ?: 0

    /** Immutable snapshot of all locations. */
    fun all(): List<DespawnLocation> = flat.toList()

    /** A uniformly random location (with replacement), or null if empty. */
    fun randomOrNull(): DespawnLocation? = if (flat.isEmpty()) null else flat[ThreadLocalRandom.current().nextInt(flat.size)]

    /** How many locations are switched on (the pipeline ignores the rest). */
    fun enabledCount(): Int = flat.count { it.enabled }

    /**
     * A random **enabled** location, weighted by [TargetOptions.priority].
     *
     * Implemented as weighted rejection sampling over the existing O(1) flat bag rather
     * than a second weighted index: draw uniformly, accept with probability
     * `priority / MAX_PRIORITY`, retry a bounded number of times. That keeps the hot path
     * allocation-free and the data structure single-sourced; the bounded linear fallback
     * only runs when almost everything is disabled or low-priority.
     */
    fun randomEnabledOrNull(): DespawnLocation? {
        if (flat.isEmpty()) return null
        val rng = ThreadLocalRandom.current()
        repeat(SAMPLE_ATTEMPTS) {
            val candidate = flat[rng.nextInt(flat.size)]
            if (!candidate.enabled) return@repeat
            val weight = candidate.options.priority.coerceIn(TargetOptions.MIN_PRIORITY, TargetOptions.MAX_PRIORITY)
            if (weight >= TargetOptions.MAX_PRIORITY || rng.nextInt(TargetOptions.MAX_PRIORITY) < weight) return candidate
        }
        // Fallback: everything drawn was disabled or lost its weight roll. Take any enabled one.
        return flat.firstOrNull { it.enabled }
    }

    /**
     * Replaces the stored entry with the same block+owner as [loc] by [loc] itself (used to
     * change a target's [TargetOptions]). Returns false when there is nothing to update or
     * nothing would change.
     *
     * Goes through [remove] + [add] rather than mutating in place because the flat bag and
     * owner index are keyed by full value equality — swapping the instance is the only way
     * to keep every index consistent.
     */
    fun update(loc: DespawnLocation): Boolean {
        val existing = byBlock[loc.blockKey]?.get(loc.owner) ?: return false
        if (existing == loc) return false
        remove(existing)
        add(loc)
        return true
    }

    // --- bulk removals ---

    /** Removes every location owned by [owner]; returns how many were removed. */
    fun removeOwner(owner: UUID): Int {
        val victims = byOwner[owner]?.toList() ?: return 0
        victims.forEach { remove(it) }
        return victims.size
    }

    /** Removes every location at [key] regardless of owner; returns how many. */
    fun removeAt(key: BlockKey): Int {
        val victims = byBlock[key]?.values?.toList() ?: return 0
        victims.forEach { remove(it) }
        return victims.size
    }

    /** Removes everything. Every previously-known owner is marked dirty so files are rewritten. */
    fun clear() {
        dirtyOwners.addAll(byOwner.keys)
        byBlock.clear()
        byOwner.clear()
        flat.clear()
        flatIndex.clear()
    }

    /** Replaces all contents with [locations] without marking anything dirty (used on load). */
    fun replaceAll(locations: Collection<DespawnLocation>) {
        byBlock.clear()
        byOwner.clear()
        flat.clear()
        flatIndex.clear()
        dirtyOwners.clear()
        locations.forEach { loc ->
            byBlock.getOrPut(loc.blockKey) { HashMap() }[loc.owner] = loc
            byOwner.getOrPut(loc.owner) { HashSet() }.add(loc)
            flat.add(loc)
            flatIndex[loc] = flat.lastIndex
        }
    }

    // --- dirty tracking for incremental persistence ---

    /** Owners whose data changed since the last [clearDirty]. */
    fun dirtyOwnersSnapshot(): Set<UUID> = dirtyOwners.toSet()

    fun clearDirty(owners: Collection<UUID>) = dirtyOwners.removeAll(owners.toSet())

    /** Owners that currently have at least one location (for full flush / file cleanup). */
    fun knownOwners(): Set<UUID> = byOwner.keys.toSet()

    fun markAllDirty() = dirtyOwners.addAll(byOwner.keys)

    private companion object {
        /** Bounded weighted-rejection draws before falling back to a linear scan. */
        const val SAMPLE_ATTEMPTS = 12
    }
}
