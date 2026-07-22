package io.fairyfox.papermc.despawneditems.despawn

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.despawn.into.DespawnIntoResult
import io.fairyfox.papermc.despawneditems.location.DespawnLocation
import org.bukkit.Location
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable

/**
 * Drives a single item through the despawn pipeline: it repeatedly picks a random,
 * not-yet-tried despawn location, loads that chunk, and offers the item to each
 * despawn strategy ([PaperMcDespawnedItems.strategies]) in priority order until the item is
 * fully placed or the location budget runs out.
 *
 * [item] is nullable because the storage strategies null it out while reconstructing
 * oversized leftover stacks.
 */
class DespawnProcess(
    var item: ItemStack?,
    private val plugin: PaperMcDespawnedItems,
    /**
     * The player this relocation is attributed to, if any. Carried so the per-user
     * throttler can release the actor's concurrency slot when the process ends.
     */
    private val actor: java.util.UUID? = null,
) {
    private var loopsLeft: Int
    private val tried: MutableSet<DespawnLocation> = HashSet()

    var invalid: Boolean = false
        private set

    init {
        loopsLeft = plugin.locations.count
        plugin.despawnProcesses.add(this)

        if (plugin.locations.isEmpty()) {
            selfDestroy()
        } else {
            newLoop()
        }
    }

    private fun newLoop() {
        if (invalid) return
        object : BukkitRunnable() {
            override fun run() {
                val next = nextLocation()
                if (next == null) {
                    selfDestroy()
                    return
                }
                loadWorld(next)
            }
        }.runTaskLater(plugin, 1L)
    }

    /** A random location not yet tried by this process, or null when exhausted. */
    private fun nextLocation(): DespawnLocation? {
        repeat(RANDOM_ATTEMPTS) {
            val candidate = plugin.locations.random() ?: return null
            if (tried.add(candidate)) return candidate
        }
        // Near-exhaustion fallback: pick any untried location deterministically.
        return plugin.locations.all().firstOrNull { it !in tried }?.also { tried.add(it) }
    }

    private fun loadWorld(despawnLocation: DespawnLocation) {
        if (invalid) return
        val location = despawnLocation.toLocation()
        if (location == null) {
            // World isn't loaded right now — count the attempt and move on.
            endLoop()
            return
        }
        location.world.getChunkAtAsync(location).thenRun { worldIsLoaded(location) }
    }

    private fun endLoop() {
        if (invalid) return
        loopsLeft--
        if (loopsLeft <= 0) {
            selfDestroy()
            return
        }
        newLoop()
    }

    fun selfDestroy() {
        plugin.despawnProcesses.remove(this)
        // Release the actor's concurrency slot exactly once, however the process ended.
        if (!invalid) plugin.throttle.onFinish(actor)
        invalid = true
    }

    // The strategy loop legitimately uses continue (strategy doesn't apply / took nothing)
    // and break (a strategy handled it) — that's the core "try each in priority order" flow.
    @Suppress("LoopWithTooManyJumpStatements")
    private fun worldIsLoaded(targetLocation: Location) {
        if (invalid) return

        val targetBlock =
            targetLocation.world
                .getBlockAt(targetLocation.blockX, targetLocation.blockY, targetLocation.blockZ)

        for (strategy in plugin.strategies) {
            if (invalid) return
            if (!strategy.doesApply(targetBlock)) continue

            val result = strategy.despawnInto(this, targetBlock)

            if (result == DespawnIntoResult.PARTIALLY || result == DespawnIntoResult.FULLY) {
                playEffect(targetLocation)
            }

            if (result == DespawnIntoResult.FULLY || result == DespawnIntoResult.CONTRABAND) {
                selfDestroy()
                return
            }

            if (result == DespawnIntoResult.NONE) continue

            break
        }

        endLoop()
    }

    private fun playEffect(location: Location) {
        if (invalid) return
        val cfg = plugin.settings.fileConfig
        if (cfg.soundEnabled || cfg.particlesEnabled) {
            DespawnEffect(location, plugin)
        }
    }

    companion object {
        /** How many random draws to try before falling back to a linear untried scan. */
        private const val RANDOM_ATTEMPTS = 8
    }
}
