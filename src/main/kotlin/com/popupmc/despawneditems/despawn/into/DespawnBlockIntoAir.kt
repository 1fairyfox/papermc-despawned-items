package com.popupmc.despawneditems.despawn.into

import com.popupmc.despawneditems.DespawnedItems
import com.popupmc.despawneditems.despawn.DespawnProcess
import org.bukkit.Material
import org.bukkit.block.Banner
import org.bukkit.block.Beacon
import org.bukkit.block.Beehive
import org.bukkit.block.Block
import org.bukkit.block.BrewingStand
import org.bukkit.block.Chest
import org.bukkit.block.CommandBlock
import org.bukkit.block.CreatureSpawner
import org.bukkit.block.Furnace
import org.bukkit.block.Sign
import org.bukkit.block.Skull
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BannerMeta
import org.bukkit.inventory.meta.BlockDataMeta
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.inventory.meta.SkullMeta

/**
 * Places a block item back into the world at an empty target location, doing its
 * best to carry over the item's stored block data and tile-entity state (banners,
 * skulls, container contents, sign text, spawners, and so on).
 *
 * Faithfully carrying *every* possible block state is effectively impossible via
 * the Bukkit API, so this covers the common cases and refuses items that are
 * hazardous to place (explosives, gravity blocks, redstone, liquids, etc.).
 */
class DespawnBlockIntoAir(plugin: DespawnedItems) : AbstractDespawnInto(plugin) {
    override fun doesApply(targetBlock: Block): Boolean {
        val entities = targetBlock.location.toCenterLocation().getNearbyEntities(0.5, 0.5, 0.5)
        return targetBlock.type.isAir && entities.isEmpty()
    }

    override fun despawnInto(
        process: DespawnProcess,
        targetBlock: Block,
    ): DespawnIntoResult {
        val item = process.item ?: return DespawnIntoResult.NONE
        val itemType = item.type

        if (isHazardous(itemType)) return DespawnIntoResult.NONE
        if (!itemType.isBlock) return DespawnIntoResult.NONE

        copyBlockToLocation(item, targetBlock)

        return if (item.amount > 1) {
            item.amount = item.amount - 1
            DespawnIntoResult.PARTIALLY
        } else {
            DespawnIntoResult.FULLY
        }
    }

    // Placed blocks can't be cleanly retrieved, so bulk-remove is a no-op here.
    override fun removeFrom(
        material: Material,
        targetBlock: Block,
    ) {}

    override fun removeFrom(
        material: ItemStack,
        targetBlock: Block,
    ) {}

    private fun isHazardous(itemType: Material): Boolean {
        val name = itemType.name.lowercase()
        return itemType == Material.TNT ||
            itemType == Material.SPAWNER ||
            itemType == Material.BEE_NEST ||
            itemType == Material.BEEHIVE ||
            itemType == Material.CACTUS ||
            itemType == Material.CAMPFIRE ||
            itemType == Material.SOUL_CAMPFIRE ||
            itemType == Material.DAMAGED_ANVIL ||
            itemType == Material.CHIPPED_ANVIL ||
            itemType == Material.ANVIL ||
            itemType.hasGravity() ||
            name.contains("redstone") ||
            name.contains("infested") ||
            itemType == Material.DAYLIGHT_DETECTOR ||
            itemType == Material.LECTERN ||
            itemType == Material.TARGET ||
            itemType == Material.TRIPWIRE ||
            itemType == Material.TRIPWIRE_HOOK ||
            itemType == Material.OBSERVER ||
            itemType == Material.LEVER ||
            name.contains("button") ||
            name.contains("pressure_plate") ||
            itemType == Material.DETECTOR_RAIL ||
            itemType == Material.END_CRYSTAL ||
            itemType == Material.FLETCHING_TABLE ||
            itemType == Material.ICE ||
            itemType == Material.SPONGE ||
            itemType == Material.WET_SPONGE ||
            itemType == Material.GLOWSTONE ||
            itemType == Material.SNOW ||
            itemType == Material.SNOW_BLOCK ||
            itemType == Material.PACKED_ICE ||
            itemType == Material.BLUE_ICE ||
            itemType == Material.FROSTED_ICE ||
            itemType == Material.JACK_O_LANTERN ||
            itemType == Material.LANTERN ||
            itemType == Material.TORCH ||
            itemType == Material.MAGMA_BLOCK ||
            itemType == Material.SEA_LANTERN ||
            itemType == Material.SHROOMLIGHT ||
            itemType == Material.SOUL_LANTERN ||
            itemType == Material.TNT_MINECART ||
            itemType == Material.TRAPPED_CHEST ||
            itemType == Material.LAVA ||
            itemType == Material.LAVA_BUCKET ||
            itemType == Material.WATER ||
            itemType == Material.WATER_BUCKET ||
            itemType == Material.CONDUIT
    }

    companion object {
        fun copyBlockToLocation(
            sourceBlock: ItemStack,
            targetBlock: Block,
        ) {
            targetBlock.type = sourceBlock.type

            if (!sourceBlock.hasItemMeta()) return
            val meta = sourceBlock.itemMeta ?: return

            if (meta is BlockDataMeta) {
                targetBlock.blockData = meta.getBlockData(sourceBlock.type)
            }

            if (meta is BannerMeta) {
                val targetBanner = targetBlock.state as Banner
                for (pattern in meta.patterns) {
                    targetBanner.addPattern(pattern)
                }
                targetBanner.update()
            }

            if (meta is SkullMeta) {
                val targetSkull = targetBlock.state as Skull
                meta.owningPlayer?.let { targetSkull.setOwningPlayer(it) }
                meta.playerProfile?.let { targetSkull.setPlayerProfile(it) }
                targetSkull.update()
            }

            if (meta !is BlockStateMeta) return
            if (!meta.hasBlockState()) return

            val sourceState = meta.blockState
            val targetState = targetBlock.state

            if (sourceState is InventoryHolder && targetState is InventoryHolder) {
                targetState.inventory.contents = sourceState.inventory.contents
            }

            if (sourceState is Banner && targetState is Banner) {
                sourceState.baseColor?.let { targetState.setBaseColor(it) }
                targetState.setPatterns(sourceState.patterns)
            }

            if (sourceState is BrewingStand && targetState is BrewingStand) {
                targetState.setBrewingTime(sourceState.brewingTime)
            }

            if (sourceState is Beacon && targetState is Beacon) {
                sourceState.primaryEffect?.let { targetState.setPrimaryEffect(it.type) }
                sourceState.secondaryEffect?.let { targetState.setSecondaryEffect(it.type) }
            }

            if (sourceState is Beehive && targetState is Beehive) {
                targetState.setFlower(sourceState.flower)
            }

            if (sourceState is Chest && targetState is Chest) {
                targetState.blockInventory.contents = sourceState.blockInventory.contents
            }

            if (sourceState is CommandBlock && targetState is CommandBlock) {
                targetState.name(sourceState.name())
                targetState.setCommand(sourceState.command)
            }

            if (sourceState is CreatureSpawner && targetState is CreatureSpawner) {
                targetState.setSpawnedType(sourceState.spawnedType)
                targetState.setDelay(sourceState.delay)
            }

            if (sourceState is Furnace && targetState is Furnace) {
                targetState.setBurnTime(sourceState.burnTime)
                targetState.setCookTime(sourceState.cookTime)
            }

            if (sourceState is Sign && targetState is Sign) {
                val lines = sourceState.lines()
                for (i in lines.indices) {
                    targetState.line(i, lines[i])
                }
            }

            if (sourceState is Skull && targetState is Skull) {
                sourceState.owningPlayer?.let { targetState.setOwningPlayer(it) }
            }

            targetState.update()
        }
    }
}
