package io.fairyfox.papermc.despawneditems.despawn.into

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.despawn.DespawnProcess
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack

/**
 * Always applies. Destroys illegal/technical items outright rather than letting
 * them be placed anywhere.
 */
class DespawnIntoVoid(plugin: PaperMcDespawnedItems) : AbstractDespawnInto(plugin) {
    /** Void destroys contraband; it stores nothing, so bulk removal must skip it. */
    override val supportsRemoval: Boolean = false

    override fun doesApply(targetBlock: Block): Boolean = true

    override fun despawnInto(
        process: DespawnProcess,
        targetBlock: Block,
    ): DespawnIntoResult {
        val item = process.item ?: return DespawnIntoResult.NONE

        if (!isContraband(item.type)) return DespawnIntoResult.NONE

        // Contraband is never *placed*. But if the admin configured a catch-all, hand it a
        // copy first so banned items can be audited instead of silently ceasing to exist.
        if (plugin.settings.voiding.catchAllUsable) {
            plugin.catchAll.deliver(item.clone())
        }

        item.amount = 0
        item.type = Material.AIR
        return DespawnIntoResult.CONTRABAND
    }

    /**
     * True when [material] is contraband — either built into the plugin, or added by the
     * admin through `void.banned-materials`. Read from config on each call so
     * `/despi reload` takes effect without a restart.
     */
    fun isContraband(material: Material): Boolean = material in CONTRABAND || material in plugin.settings.voiding.bannedMaterials

    // Void destroys contraband on placement; there is nothing stored to bulk-remove.
    override fun removeFrom(
        material: Material,
        targetBlock: Block,
    ) = Unit

    override fun removeFrom(
        material: ItemStack,
        targetBlock: Block,
    ) = Unit

    companion object {
        private val CONTRABAND: Set<Material> =
            setOf(
                Material.COMMAND_BLOCK,
                Material.COMMAND_BLOCK_MINECART,
                Material.CHAIN_COMMAND_BLOCK,
                Material.REPEATING_COMMAND_BLOCK,
                Material.DEBUG_STICK,
                Material.JIGSAW,
                Material.STRUCTURE_BLOCK,
                Material.STRUCTURE_VOID,
                Material.NETHERITE_AXE,
                Material.NETHERITE_BLOCK,
                Material.NETHERITE_BOOTS,
                Material.NETHERITE_CHESTPLATE,
                Material.NETHERITE_HELMET,
                Material.NETHERITE_HOE,
                Material.NETHERITE_INGOT,
                Material.NETHERITE_LEGGINGS,
                Material.NETHERITE_PICKAXE,
                Material.NETHERITE_SCRAP,
                Material.NETHERITE_SHOVEL,
                Material.NETHERITE_SWORD,
                Material.ANCIENT_DEBRIS,
            )
    }
}
