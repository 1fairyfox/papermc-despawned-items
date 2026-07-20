package com.popupmc.despawneditems.despawn.into

import com.popupmc.despawneditems.PaperMcDespawnedItems
import com.popupmc.despawneditems.despawn.DespawnProcess
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack

/**
 * Places items into ordinary storage blocks: chests, barrels, hoppers,
 * droppers, dispensers, shulker boxes, and trapped chests. Handles oversized
 * leftover stacks by reconstructing them for the next despawn location.
 */
class DespawnIntoStorage(plugin: PaperMcDespawnedItems) : AbstractDespawnInto(plugin) {
    override fun doesApply(targetBlock: Block): Boolean =
        when (targetBlock.type) {
            Material.BARREL,
            Material.CHEST,
            Material.DISPENSER,
            Material.DROPPER,
            Material.HOPPER,
            Material.SHULKER_BOX,
            Material.TRAPPED_CHEST,
            -> true
            else -> false
        }

    override fun despawnInto(
        process: DespawnProcess,
        targetBlock: Block,
    ): DespawnIntoResult {
        val inventory = getInventory(targetBlock) ?: return DespawnIntoResult.NONE
        val current = process.item ?: return DespawnIntoResult.NONE

        val leftover = inventory.addItem(current.clone())
        targetBlock.state.update()

        if (leftover.isEmpty()) return DespawnIntoResult.FULLY

        // Reconstruct the (possibly oversized) remainder to retry elsewhere.
        process.item = null
        for (leftoverStack in leftover.values) {
            val running = process.item
            if (running == null) {
                process.item = leftoverStack
            } else {
                running.add(leftoverStack.amount)
            }
        }

        return DespawnIntoResult.PARTIALLY
    }

    override fun removeFrom(
        material: Material,
        targetBlock: Block,
    ) {
        val inventory = getInventory(targetBlock) ?: return
        inventory.remove(material)
        targetBlock.state.update()
    }

    override fun removeFrom(
        material: ItemStack,
        targetBlock: Block,
    ) {
        val inventory = getInventory(targetBlock) ?: return
        inventory.remove(material)
        targetBlock.state.update()
    }
}
