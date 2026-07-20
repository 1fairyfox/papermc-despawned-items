package com.popupmc.despawneditems.despawn

import com.popupmc.despawneditems.DespawnedItems
import com.popupmc.despawneditems.config.LocationEntry
import com.popupmc.despawneditems.despawn.into.AbstractDespawnInto
import com.popupmc.despawneditems.despawn.into.DespawnBlockIntoAir
import com.popupmc.despawneditems.despawn.into.DespawnIntoCooker
import com.popupmc.despawneditems.despawn.into.DespawnIntoResult
import com.popupmc.despawneditems.despawn.into.DespawnIntoStorage
import com.popupmc.despawneditems.despawn.into.DespawnIntoVoid
import com.popupmc.despawneditems.despawn.into.DespawnItemIntoEntity
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable

/**
 * Drives a single item through the despawn pipeline: it repeatedly picks a random
 * despawn location, loads the chunk, and offers the item to each [AbstractDespawnInto]
 * strategy in priority order until the item is fully placed or the location budget
 * runs out.
 *
 * [item] is nullable because the storage strategies null it out while
 * reconstructing oversized leftover stacks.
 */
class DespawnProcess(var item: ItemStack?, private val plugin: DespawnedItems) {

    private var loopsLeft: Int
    var invalid: Boolean = false
        private set

    init {
        loopsLeft = plugin.settings.fileLocations.locationEntries.size
        plugin.despawnProcesses.add(this)

        if (plugin.settings.fileLocations.locationEntries.isEmpty()) {
            selfDestroy()
        } else {
            // Build the ordered strategy list once (shared across processes).
            if (despawnIntos.isEmpty()) {
                despawnIntos.add(DespawnIntoVoid(plugin))      // delete contraband first
                despawnIntos.add(DespawnIntoCooker(plugin))    // then furnaces/smokers
                despawnIntos.add(DespawnBlockIntoAir(plugin))  // then place as a block
                despawnIntos.add(DespawnItemIntoEntity(plugin))// then onto entities
                despawnIntos.add(DespawnIntoStorage(plugin))   // finally into containers
            }
            newLoop()
        }
    }

    private fun newLoop() {
        if (invalid) return
        object : BukkitRunnable() {
            override fun run() {
                loadWorld(plugin.despawnIndexes.randomChestCoord())
            }
        }.runTaskLater(plugin, 1L)
    }

    private fun loadWorld(locationEntry: LocationEntry) {
        if (invalid) return
        val location = locationEntry.location
        location.world.getChunkAtAsync(location)
            .thenRun { worldIsLoaded(locationEntry) }
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
        invalid = true
    }

    private fun worldIsLoaded(locationEntry: LocationEntry) {
        if (invalid) return

        val targetLocation = locationEntry.location
        val targetBlock = targetLocation.world
            .getBlockAt(targetLocation.blockX, targetLocation.blockY, targetLocation.blockZ)

        for (despawnInto in despawnIntos) {
            if (invalid) return
            if (!despawnInto.doesApply(targetBlock)) continue

            val result = despawnInto.despawnInto(this, targetBlock)

            if (result == DespawnIntoResult.PARTIALLY || result == DespawnIntoResult.FULLY) {
                playEffect(locationEntry)
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

    private fun playEffect(locationEntry: LocationEntry) {
        if (invalid) return
        val cfg = plugin.settings.fileConfig
        if (cfg.soundEnabled || cfg.particlesEnabled) {
            DespawnEffect(locationEntry, plugin)
        }
    }

    companion object {
        /** Ordered despawn strategies, shared across all processes. */
        val despawnIntos: MutableList<AbstractDespawnInto> = mutableListOf()
    }
}
