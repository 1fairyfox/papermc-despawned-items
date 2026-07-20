package io.fairyfox.papermc.despawneditems

import org.bukkit.Material

/**
 * The pool of "safe" materials handed out as `/recycle` rewards. Built once at
 * enable time by scanning every [Material] and excluding air, technical blocks,
 * and high-value materials, so rewards stay ordinary and non-exploitable.
 *
 * (Formerly misnamed `BlacklistedItems` — it is in fact the *allow*-list of
 * reward-eligible materials.)
 */
object RewardPool {
    /** Substrings that disqualify a material from the reward pool. */
    private val EXCLUDED_SUBSTRINGS =
        listOf(
            "command", "spawn", "legacy", "structure",
            "gold", "golden", "iron", "diamond", "emerald", "netherite",
        )

    /** Individual materials disqualified regardless of name. */
    private val EXCLUDED_MATERIALS =
        setOf(
            Material.JIGSAW,
            Material.SPAWNER,
            Material.DEBUG_STICK,
        )

    /** Materials eligible to be given out as a recycle reward. */
    val items: List<Material>
        get() = pool

    private var pool: List<Material> = emptyList()

    /**
     * (Re)builds the reward pool. Safe to call again on reload.
     *
     * Filters purely on each material's stable enum name and identity — deliberately
     * avoiding server-backed calls like `Material.isAir()` (which routes through the
     * block-type registry) so this stays fast and testable off the main thread.
     */
    fun setup() {
        pool =
            Material.entries.filter { type ->
                val name = type.name
                !isAir(name) &&
                    !name.startsWith("LEGACY_") &&
                    type !in EXCLUDED_MATERIALS &&
                    EXCLUDED_SUBSTRINGS.none { it in name.lowercase() }
            }
    }

    /** Name-based air test (AIR, CAVE_AIR, VOID_AIR) that needs no server. */
    private fun isAir(name: String): Boolean = name == "AIR" || name.endsWith("_AIR")

    /** A random reward material, or null if the pool is empty. */
    fun random(): Material? = pool.randomOrNull()
}
