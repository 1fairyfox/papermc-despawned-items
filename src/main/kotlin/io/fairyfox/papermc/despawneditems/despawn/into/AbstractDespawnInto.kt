package io.fairyfox.papermc.despawneditems.despawn.into

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.despawn.DespawnProcess
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Container
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

/**
 * A strategy for placing a despawning item into a particular kind of target
 * block (a container, a cooker, empty air, an entity, or the void). Strategies
 * are tried in order by [DespawnProcess].
 */
abstract class AbstractDespawnInto(protected val plugin: PaperMcDespawnedItems) {
    /** Whether this strategy is applicable to [targetBlock]. */
    abstract fun doesApply(targetBlock: Block): Boolean

    /** Attempts to place [DespawnProcess.item] into [targetBlock]. */
    abstract fun despawnInto(
        process: DespawnProcess,
        targetBlock: Block,
    ): DespawnIntoResult

    /** Removes all of [material] from [targetBlock] (used by bulk purges). */
    abstract fun removeFrom(
        material: Material,
        targetBlock: Block,
    )

    /** Removes items matching [material] from [targetBlock] (used by bulk purges). */
    abstract fun removeFrom(
        material: ItemStack,
        targetBlock: Block,
    )

    /** Returns the block's inventory if it has one, else null. */
    protected fun getInventory(block: Block): Inventory? = (block.state as? Container)?.inventory
}
