package io.fairyfox.papermc.despawneditems.location

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
    /**
     * Per-target settings (on/off, priority, contraband opt-in) — the state behind the
     * in-world toggle button. Defaulted so every existing call site is unchanged and a
     * target that has never been configured costs nothing extra on disk.
     */
    val options: TargetOptions = TargetOptions.DEFAULT,
) {
    /** The position component, without the owner — the [LocationStore] spatial key. */
    val blockKey: BlockKey get() = BlockKey(world, x, y, z)

    /** Shorthand used on the relocation hot path. */
    val enabled: Boolean get() = options.enabled

    /**
     * Serialised form stored in each owner's file. A target with default options is
     * written as `x;y;z;world` — byte-compatible with the original on-disk format — and one
     * with non-default options is prefixed with `@key=value,…|`. See [TargetOptions] for
     * why the options go in front rather than at the end.
     *
     * The owner is implied by which file the entry lives in, so it is not part of the string.
     */
    fun serialize(): String {
        val body = "$x;$y;$z;$world"
        if (options.isDefault) return body
        return "${TargetOptions.MARKER}${options.serializeFields()}${TargetOptions.TERMINATOR}$body"
    }

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
            val (options, body) = stripOptions(serialized) ?: return null

            val parts = body.split(';')
            if (parts.size < MIN_FIELDS) return null
            val (x, y, z) = parseCoordinates(parts) ?: return null
            val world = parts.subList(WORLD_INDEX, parts.size).joinToString(";")
            if (world.isEmpty()) return null
            return DespawnLocation(world, x, y, z, owner, options)
        }

        /** The three leading integer fields, or null when any of them is not an integer. */
        private fun parseCoordinates(parts: List<String>): Triple<Int, Int, Int>? {
            val x = parts[0].toIntOrNull() ?: return null
            val y = parts[1].toIntOrNull() ?: return null
            val z = parts[2].toIntOrNull() ?: return null
            return Triple(x, y, z)
        }

        /**
         * Splits an optional leading `@key=value,…|` block off [serialized], returning the
         * parsed options and the remaining classic `x;y;z;world` body — or null when a
         * marker is present but never terminated.
         *
         * Every pre-existing entry has no marker and takes the identity path, which is what
         * makes the format change invisible to existing data.
         */
        private fun stripOptions(serialized: String): Pair<TargetOptions, String>? {
            if (serialized.isEmpty() || serialized[0] != TargetOptions.MARKER) {
                return TargetOptions.DEFAULT to serialized
            }
            val end = serialized.indexOf(TargetOptions.TERMINATOR)
            if (end < 0) return null
            return TargetOptions.parseFields(serialized.substring(1, end)) to serialized.substring(end + 1)
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
