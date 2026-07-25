package io.fairyfox.papermc.despawneditems.despawn.into

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.config.CatchAllTarget
import io.fairyfox.papermc.despawneditems.despawn.DespawnEffect
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.Container
import org.bukkit.inventory.ItemStack

/**
 * Delivers items to the admin-configured **catch-all** containers.
 *
 * A catch-all is the safety net for the two categories of item that would otherwise simply
 * cease to exist: **banned/contraband** materials, and items selected by the random
 * `void.chance` roll. Instead of destroying them, an admin can route them to one or more
 * chests so they can be audited, recycled, or handed back.
 *
 * Targets are tried in configured order (`mode: first`) or in a shuffled order
 * (`mode: random`, which spreads wear across several chests instead of always filling the
 * first). A target that is missing, unloaded, not a container, or full is skipped; if every
 * target is exhausted the outcome is `void.catch-all.destroy-when-full`.
 *
 * Chunk loading is asynchronous (`getChunkAtAsync`) exactly as the main relocation pipeline
 * does, so delivering to a far-away catch-all never blocks the server thread.
 */
class CatchAllDelivery(private val plugin: PaperMcDespawnedItems) {
    /**
     * Attempts to place [item] into a catch-all container.
     *
     * @param onComplete receives true when the item (or all of it) was accepted, false when
     *   every configured target refused it. Always invoked exactly once, on the main thread.
     */
    fun deliver(
        item: ItemStack,
        onComplete: (Boolean) -> Unit = {},
    ) {
        val settings = plugin.settings.voiding
        if (!settings.catchAllUsable) {
            onComplete(false)
            return
        }
        val targets =
            when (settings.catchAllMode) {
                "random" -> settings.catchAllTargets.shuffled()
                else -> settings.catchAllTargets
            }
        tryNext(item, targets, 0, onComplete)
    }

    private fun tryNext(
        item: ItemStack,
        targets: List<CatchAllTarget>,
        index: Int,
        onComplete: (Boolean) -> Unit,
    ) {
        if (index >= targets.size || item.amount <= 0) {
            onComplete(item.amount <= 0)
            return
        }
        val target = targets[index]
        val world = Bukkit.getWorld(target.world)
        if (world == null) {
            tryNext(item, targets, index + 1, onComplete)
            return
        }
        val location = Location(world, target.x.toDouble(), target.y.toDouble(), target.z.toDouble())
        world.getChunkAtAsync(location).thenRun {
            val block = world.getBlockAt(target.x, target.y, target.z)
            if (insertInto(block, item)) {
                playEffect(location)
                onComplete(true)
            } else {
                tryNext(item, targets, index + 1, onComplete)
            }
        }
    }

    /**
     * Inserts as much of [item] as fits into [block]'s inventory, mutating [item] to the
     * leftover. Returns true only when the whole stack was accepted.
     */
    private fun insertInto(
        block: Block,
        item: ItemStack,
    ): Boolean {
        // Container (a BlockState) rather than the bare InventoryHolder: only the block
        // state carries update(), and without that the insert is never written back.
        val container = block.state as? Container ?: return false
        val leftover = container.inventory.addItem(item.clone())
        container.update()
        val remaining = leftover.values.sumOf { it.amount }
        if (remaining >= item.amount) return false // nothing fitted
        item.amount = remaining
        return remaining == 0
    }

    private fun playEffect(location: Location) {
        val cfg = plugin.settings.fileConfig
        if (cfg.soundEnabled || cfg.particlesEnabled) DespawnEffect(location, plugin)
    }
}
