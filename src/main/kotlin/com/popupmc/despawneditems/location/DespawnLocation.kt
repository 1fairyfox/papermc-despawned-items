package com.popupmc.despawneditems.location

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import java.util.UUID

/**
 * Immutable key identifying a block position within a world (by name). Used as the
 * spatial index key in [LocationStore]; a plain data class so equality/hashing come
 * for free and lookups are O(1).
 */
data class BlockKey(val world: String, val x: Int, val y: Int, val z: Int)

/**
 * A single despawn location: a block position (world name + integer block coords)
 * owned by a player.
 *
 * Deliberately **server-independent and immutable** — it holds a world *name*, not a
 * live [World], so it can be created, compared, indexed, and (de)serialised without a
 * running server. That keeps the hot-path indexes cheap and makes the type fully
 * unit-testable. Resolve to a live [Location] only when actually needed via
 * [toLocation].
 */
data class DespawnLocation(
    val world: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val owner: UUID,
) {
    /** The position component, without the owner — the [LocationStore] spatial key. */
    val blockKey: BlockKey get() = BlockKey(world, x, y, z)

    /**
     * Serialised form stored in each owner's file: `x;y;z;world`. The owner is implied
     * by which file the entry lives in, so it is not part of the string (kept
     * byte-compatible with the original on-disk format).
     */
    fun serialize(): String = "$x;$y;$z;$world"

    /** Resolves to a Bukkit [Location] if the world is currently loaded, else null. */
    fun toLocation(): Location? {
        val loaded: World = Bukkit.getWorld(world) ?: return null
        return Location(loaded, x.toDouble(), y.toDouble(), z.toDouble())
    }

    companion object {
        /** Minimum `x;y;z;world` fields; index 3 begins the (possibly `;`-containing) world. */
        private const val MIN_FIELDS = 4
        private const val WORLD_INDEX = 3

        /**
         * Parses the `x;y;z;world` form for [owner]. Returns null (never throws) if the
         * string is malformed — too few fields, non-integer coordinates, or an empty
         * world name. World names are allowed to contain `;` (the tail is re-joined).
         *
         * `@Suppress("UnreachableCode")`: detekt 1.23.8 (built for Kotlin 2.0) mis-analyses
         * the lines after `?: return null` as unreachable on Kotlin 2.4; the code is reachable
         * and covered by the roundtrip + fuzz tests.
         */
        @Suppress("UnreachableCode")
        fun parse(
            serialized: String,
            owner: UUID,
        ): DespawnLocation? {
            val parts = serialized.split(';')
            if (parts.size < MIN_FIELDS) return null
            val x = parts[0].toIntOrNull() ?: return null
            val y = parts[1].toIntOrNull() ?: return null
            val z = parts[2].toIntOrNull() ?: return null
            val world = parts.subList(WORLD_INDEX, parts.size).joinToString(";")
            if (world.isEmpty()) return null
            return DespawnLocation(world, x, y, z, owner)
        }

        /** Builds a [DespawnLocation] from a live Bukkit [Location] and owner (block coords). */
        fun of(
            location: Location,
            owner: UUID,
        ): DespawnLocation =
            DespawnLocation(
                location.world.name,
                location.blockX,
                location.blockY,
                location.blockZ,
                owner,
            )
    }
}
