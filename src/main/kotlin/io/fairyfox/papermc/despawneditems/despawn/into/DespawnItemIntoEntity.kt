package io.fairyfox.papermc.despawneditems.despawn.into

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.despawn.DespawnProcess
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.minecart.PoweredMinecart
import org.bukkit.entity.minecart.StorageMinecart
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

/**
 * Places items onto a single entity occupying the target space: an item frame,
 * a living entity's equipment slots (armour and hands), a storage minecart's
 * inventory, or a furnace minecart's fuel.
 */
class DespawnItemIntoEntity(plugin: PaperMcDespawnedItems) : AbstractDespawnInto(plugin) {
    override fun doesApply(targetBlock: Block): Boolean {
        val isAir = targetBlock.type.isAir
        val entities = targetBlock.location.toCenterLocation().getNearbyEntities(NEARBY_RADIUS, NEARBY_RADIUS, NEARBY_RADIUS)
        val hasProperEntity =
            entities.any {
                it is ItemFrame || it is LivingEntity || it is StorageMinecart
            }
        return isAir && hasProperEntity && entities.size == 1
    }

    // Exhaustive per-entity-type placement: each branch tries a slot/kind in order. The
    // branches ARE the logic, so the complexity/length/return-count are suppressed here
    // with reason rather than scattered across helpers.
    @Suppress("CyclomaticComplexMethod", "LongMethod", "ReturnCount")
    override fun despawnInto(
        process: DespawnProcess,
        targetBlock: Block,
    ): DespawnIntoResult {
        val item = process.item ?: return DespawnIntoResult.NONE
        val entities = targetBlock.location.toCenterLocation().getNearbyEntities(NEARBY_RADIUS, NEARBY_RADIUS, NEARBY_RADIUS)
        if (entities.size != 1) return DespawnIntoResult.NONE

        val entity = entities.iterator().next()

        when (entity) {
            is ItemFrame -> {
                if (entity.item.type.isAir) {
                    entity.setItem(item.asOne())
                    return addedItem(process)
                }
            }

            is LivingEntity -> {
                val equipment = entity.equipment ?: return DespawnIntoResult.NONE
                val name = item.type.name.lowercase()

                if (isEmpty(equipment.helmet)) {
                    equipment.setHelmet(item.asOne())
                    return addedItem(process)
                } else if (isEmpty(equipment.chestplate) && name.contains("chestplate")) {
                    equipment.setChestplate(item.asOne())
                    return addedItem(process)
                } else if (isEmpty(equipment.leggings) && name.contains("leggings")) {
                    equipment.setLeggings(item.asOne())
                    return addedItem(process)
                } else if (isEmpty(equipment.boots) && name.contains("boots")) {
                    equipment.setBoots(item.asOne())
                    return addedItem(process)
                } else if (equipment.itemInMainHand.type.isAir) {
                    (entity as? ArmorStand)?.setArms(true)
                    equipment.setItemInMainHand(item.asOne())
                    return addedItem(process)
                } else if (equipment.itemInOffHand.type.isAir) {
                    (entity as? ArmorStand)?.setArms(true)
                    equipment.setItemInOffHand(item.asOne())
                    return addedItem(process)
                }
            }

            is InventoryHolder -> {
                val inventory = entity.inventory
                val leftover = inventory.addItem(item.clone())
                targetBlock.state.update()

                if (leftover.isEmpty()) return DespawnIntoResult.FULLY

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

            is PoweredMinecart -> {
                if (item.type.isFuel) {
                    entity.fuel = entity.fuel + FUEL_TICKS_PER_ITEM
                    return addedItem(process)
                }
            }
        }

        return DespawnIntoResult.NONE
    }

    private fun addedItem(process: DespawnProcess): DespawnIntoResult {
        val item = process.item ?: return DespawnIntoResult.NONE
        return if (item.amount == 1) {
            DespawnIntoResult.FULLY
        } else {
            item.amount = item.amount - 1
            DespawnIntoResult.PARTIALLY
        }
    }

    override fun removeFrom(
        material: Material,
        targetBlock: Block,
    ) {
        removeFrom(ItemStack(material), targetBlock)
    }

    // Mirror of despawnInto: exhaustive per-entity-type removal; branches are the logic.
    @Suppress("CyclomaticComplexMethod", "ReturnCount")
    override fun removeFrom(
        material: ItemStack,
        targetBlock: Block,
    ) {
        val entities = targetBlock.location.toCenterLocation().getNearbyEntities(NEARBY_RADIUS, NEARBY_RADIUS, NEARBY_RADIUS)
        if (entities.size != 1) return

        when (val entity = entities.iterator().next()) {
            is ItemFrame -> {
                if (entity.item.isSimilar(material)) {
                    entity.setItem(null)
                    return
                }
            }

            is LivingEntity -> {
                val equipment = entity.equipment ?: return
                when {
                    equipment.helmet?.isSimilar(material) == true -> {
                        equipment.setHelmet(null)
                        return
                    }
                    equipment.chestplate?.isSimilar(material) == true -> {
                        equipment.setChestplate(null)
                        return
                    }
                    equipment.leggings?.isSimilar(material) == true -> {
                        equipment.setLeggings(null)
                        return
                    }
                    equipment.boots?.isSimilar(material) == true -> {
                        equipment.setBoots(null)
                        return
                    }
                    equipment.itemInMainHand.isSimilar(material) -> {
                        equipment.setItemInMainHand(null)
                        return
                    }
                    equipment.itemInOffHand.isSimilar(material) -> {
                        equipment.setItemInOffHand(null)
                        return
                    }
                }
            }

            is InventoryHolder -> entity.inventory.remove(material)

            is PoweredMinecart -> if (material.type.isFuel) entity.fuel = 0
        }
    }

    private fun isEmpty(item: ItemStack?): Boolean = item == null || item.type.isAir

    companion object {
        private const val NEARBY_RADIUS = 0.5

        /** Per the wiki, every fuel item adds 3600 ticks to a furnace minecart. */
        private const val FUEL_TICKS_PER_ITEM = 3600
    }
}
