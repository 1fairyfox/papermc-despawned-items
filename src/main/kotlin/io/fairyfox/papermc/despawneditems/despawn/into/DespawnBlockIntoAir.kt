package io.fairyfox.papermc.despawneditems.despawn.into

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.despawn.DespawnProcess
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
class DespawnBlockIntoAir(plugin: PaperMcDespawnedItems) : AbstractDespawnInto(plugin) {
    override fun doesApply(targetBlock: Block): Boolean {
        val entities =
            targetBlock.location.toCenterLocation()
                .getNearbyEntities(NEARBY_RADIUS, NEARBY_RADIUS, NEARBY_RADIUS)
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

    // Placed blocks can't be cleanly retrieved, so bulk-remove is a deliberate no-op here.
    override fun removeFrom(
        material: Material,
        targetBlock: Block,
    ) = Unit

    override fun removeFrom(
        material: ItemStack,
        targetBlock: Block,
    ) = Unit

    /** A block is hazardous to place if it's a known-dangerous type, has gravity, or its
     *  name matches a hazardous family (redstone, buttons, pressure plates, infested). */
    private fun isHazardous(itemType: Material): Boolean {
        if (itemType in HAZARDOUS_MATERIALS || itemType.hasGravity()) return true
        val name = itemType.name.lowercase()
        return HAZARDOUS_NAME_PARTS.any { it in name }
    }

    companion object {
        private const val NEARBY_RADIUS = 0.5

        private val HAZARDOUS_NAME_PARTS = listOf("redstone", "infested", "button", "pressure_plate")

        private val HAZARDOUS_MATERIALS: Set<Material> =
            setOf(
                Material.TNT, Material.SPAWNER, Material.BEE_NEST, Material.BEEHIVE, Material.CACTUS,
                Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.DAMAGED_ANVIL, Material.CHIPPED_ANVIL,
                Material.ANVIL, Material.DAYLIGHT_DETECTOR, Material.LECTERN, Material.TARGET,
                Material.TRIPWIRE, Material.TRIPWIRE_HOOK, Material.OBSERVER, Material.LEVER,
                Material.DETECTOR_RAIL, Material.END_CRYSTAL, Material.FLETCHING_TABLE, Material.ICE,
                Material.SPONGE, Material.WET_SPONGE, Material.GLOWSTONE, Material.SNOW, Material.SNOW_BLOCK,
                Material.PACKED_ICE, Material.BLUE_ICE, Material.FROSTED_ICE, Material.JACK_O_LANTERN,
                Material.LANTERN, Material.TORCH, Material.MAGMA_BLOCK, Material.SEA_LANTERN,
                Material.SHROOMLIGHT, Material.SOUL_LANTERN, Material.TNT_MINECART, Material.TRAPPED_CHEST,
                Material.LAVA, Material.LAVA_BUCKET, Material.WATER, Material.WATER_BUCKET, Material.CONDUIT,
            )

        // An exhaustive tile-entity-state copier: each branch handles one block-state type.
        // It is inherently long and branchy, and splitting it would only scatter closely
        // related logic — hence the scoped suppressions. `baseColor?.` is defensive against
        // the nullable legacy Banner API across versions.
        @Suppress("CyclomaticComplexMethod", "LongMethod", "UnnecessarySafeCall")
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
