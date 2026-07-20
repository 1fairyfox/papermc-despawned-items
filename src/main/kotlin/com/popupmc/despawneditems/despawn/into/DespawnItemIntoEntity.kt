package com.popupmc.despawneditems.despawn.into

import com.popupmc.despawneditems.DespawnedItems
import com.popupmc.despawneditems.despawn.DespawnProcess
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
class DespawnItemIntoEntity(plugin: DespawnedItems) : AbstractDespawnInto(plugin) {
    override fun doesApply(targetBlock: Block): Boolean {
        val isAir = targetBlock.type.isAir
        val entities = targetBlock.location.toCenterLocation().getNearbyEntities(0.5, 0.5, 0.5)
        val hasProperEntity =
            entities.any {
                it is ItemFrame || it is LivingEntity || it is StorageMinecart
            }
        return isAir && hasProperEntity && entities.size == 1
    }

    override fun despawnInto(
        process: DespawnProcess,
        targetBlock: Block,
    ): DespawnIntoResult {
        val item = process.item ?: return DespawnIntoResult.NONE
        val entities = targetBlock.location.toCenterLocation().getNearbyEntities(0.5, 0.5, 0.5)
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
                    // Per the wiki, every fuel item adds 3600 ticks.
                    entity.fuel = entity.fuel + 3600
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

    override fun removeFrom(
        material: ItemStack,
        targetBlock: Block,
    ) {
        val entities = targetBlock.location.toCenterLocation().getNearbyEntities(0.5, 0.5, 0.5)
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
}
