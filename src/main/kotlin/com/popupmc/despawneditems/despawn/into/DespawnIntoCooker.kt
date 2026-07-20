package com.popupmc.despawneditems.despawn.into

import com.popupmc.despawneditems.DespawnedItems
import com.popupmc.despawneditems.despawn.DespawnProcess
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.inventory.BlastingRecipe
import org.bukkit.inventory.FurnaceRecipe
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.SmokingRecipe

/**
 * Places items into furnaces, blast furnaces, and smokers: stacking onto a
 * matching smelt/fuel slot, dropping fuel into the fuel slot, or dropping a
 * cookable item into the input slot when the recipe matches the cooker type.
 */
class DespawnIntoCooker(plugin: DespawnedItems) : AbstractDespawnInto(plugin) {

    override fun doesApply(targetBlock: Block): Boolean = when (targetBlock.type) {
        Material.BLAST_FURNACE, Material.FURNACE, Material.SMOKER -> true
        else -> false
    }

    override fun despawnInto(process: DespawnProcess, targetBlock: Block): DespawnIntoResult {
        val inventory = getInventory(targetBlock) ?: return DespawnIntoResult.NONE
        val current = process.item ?: return DespawnIntoResult.NONE

        val smelt = inventory.getItem(0)
        val fuel = inventory.getItem(1)

        // Stack onto an existing matching slot first.
        if (smelt != null && smelt.type == current.type) {
            return addToStack(process, targetBlock, inventory, smelt, 0)
        } else if (fuel != null && fuel.type == current.type) {
            return addToStack(process, targetBlock, inventory, fuel, 1)
        }

        // Both slots occupied by different items: nothing to do here.
        if (smelt != null && fuel != null) return DespawnIntoResult.NONE

        return when {
            current.type.isFuel && fuel == null -> {
                inventory.setItem(1, current)
                targetBlock.state.update()
                DespawnIntoResult.FULLY
            }
            smelt == null && targetBlock.type == Material.BLAST_FURNACE && isInBlastingRecipe(current) -> {
                inventory.setItem(0, current)
                targetBlock.state.update()
                DespawnIntoResult.FULLY
            }
            smelt == null && targetBlock.type == Material.FURNACE && isInFurnaceRecipe(current) -> {
                inventory.setItem(0, current)
                targetBlock.state.update()
                DespawnIntoResult.FULLY
            }
            smelt == null && targetBlock.type == Material.SMOKER && isInSmokerRecipe(current) -> {
                inventory.setItem(0, current)
                targetBlock.state.update()
                DespawnIntoResult.FULLY
            }
            else -> DespawnIntoResult.NONE
        }
    }

    override fun removeFrom(material: Material, targetBlock: Block) {
        val inventory = getInventory(targetBlock) ?: return
        inventory.remove(material)
        targetBlock.state.update()
    }

    override fun removeFrom(material: ItemStack, targetBlock: Block) {
        val inventory = getInventory(targetBlock) ?: return
        inventory.remove(material)
        targetBlock.state.update()
    }

    /** Adds the despawning stack onto an existing stack, capping at max size. */
    private fun addToStack(
        process: DespawnProcess,
        targetBlock: Block,
        inventory: Inventory,
        toItem: ItemStack,
        slot: Int,
    ): DespawnIntoResult {
        val current = process.item ?: return DespawnIntoResult.NONE

        var newAmount = toItem.amount + current.amount
        val maxStackSize = toItem.type.maxStackSize

        return if (newAmount > maxStackSize) {
            toItem.amount = maxStackSize
            newAmount -= maxStackSize
            current.amount = newAmount
            inventory.setItem(slot, toItem)
            targetBlock.state.update()
            DespawnIntoResult.PARTIALLY
        } else {
            toItem.amount = newAmount
            inventory.setItem(slot, toItem)
            targetBlock.state.update()
            DespawnIntoResult.FULLY
        }
    }

    private fun isInBlastingRecipe(item: ItemStack): Boolean = matchesRecipe(item) { recipe ->
        recipe is BlastingRecipe && recipe.input.type == item.type
    }

    private fun isInFurnaceRecipe(item: ItemStack): Boolean = matchesRecipe(item) { recipe ->
        recipe is FurnaceRecipe && recipe.input.type == item.type
    }

    private fun isInSmokerRecipe(item: ItemStack): Boolean = matchesRecipe(item) { recipe ->
        recipe is SmokingRecipe && recipe.input.type == item.type
    }

    private inline fun matchesRecipe(item: ItemStack, predicate: (org.bukkit.inventory.Recipe) -> Boolean): Boolean {
        val recipes = Bukkit.getServer().recipeIterator()
        while (recipes.hasNext()) {
            if (predicate(recipes.next())) return true
        }
        return false
    }
}
