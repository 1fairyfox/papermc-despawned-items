package com.popupmc.despawneditems.manage

import com.popupmc.despawneditems.DespawnedItems
import com.popupmc.despawneditems.config.LocationEntry
import com.popupmc.despawneditems.despawn.DespawnProcess
import com.popupmc.despawneditems.sendColored
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID

/**
 * A long-running, tick-spread bulk removal: walks every relevant despawn
 * location one per tick and removes matching materials (or a specific item) from
 * each, reporting progress to [sender]. Only one may run at a time per sender.
 */
class RemoveMaterials(
    private val sender: CommandSender,
    private val materials: List<Material>?,
    private val item: ItemStack?,
    private val plugin: DespawnedItems,
    private val owner: UUID?,
    private val senderID: UUID?,
) {

    private var locationIndex = 0
    private var senderLocationEntries: MutableList<LocationEntry> = mutableListOf()
    private var invalid = false

    init {
        when {
            plugin.settings.fileLocations.locationEntries.isEmpty() -> forceSelfDestroy()

            materials == null && item == null -> {
                sender.sendColored(
                    "ERROR: Both material and item were null, refusing to start task...",
                    NamedTextColor.RED,
                )
                invalid = true
            }

            else -> {
                if (senderID != null) {
                    plugin.removeMaterialsInst[senderID]?.forceSelfDestroy()
                    plugin.removeMaterialsInst[senderID] = this
                }

                senderLocationEntries = if (owner != null) {
                    plugin.settings.fileLocations.existsAll(owner)
                } else {
                    ArrayList(plugin.settings.fileLocations.locationEntries)
                }

                newLoop()
            }
        }
    }

    private fun newLoop() {
        if (invalid) return
        object : BukkitRunnable() {
            override fun run() {
                loadWorld(senderLocationEntries[locationIndex])
            }
        }.runTaskLater(plugin, 1L)
    }

    fun forceSelfDestroy() {
        senderID?.let { plugin.removeMaterialsInst.remove(it) }
        invalid = true
        sender.sendColored("Completed!", NamedTextColor.GOLD)
    }

    private fun checkSelfDestroy() {
        if (invalid) return
        if (locationIndex >= senderLocationEntries.size) {
            forceSelfDestroy()
        } else {
            newLoop()
        }
    }

    private fun loadWorld(locationEntry: LocationEntry) {
        if (invalid) return
        val location = locationEntry.location
        location.world.getChunkAtAsync(location).thenRun { worldIsLoaded(locationEntry) }
    }

    private fun worldIsLoaded(locationEntry: LocationEntry) {
        if (invalid) return

        val location = locationEntry.location
        val block = location.world.getBlockAt(location.blockX, location.blockY, location.blockZ)

        for (despawnInto in DespawnProcess.despawnIntos) {
            if (invalid) return
            if (!despawnInto.doesApply(block)) continue

            materials?.forEach { material ->
                if (invalid) return
                despawnInto.removeFrom(material, block)
            }

            item?.let { despawnInto.removeFrom(it, block) }

            if (locationIndex % 20 == 0) {
                sender.sendColored(
                    "Still processing... $locationIndex / ${senderLocationEntries.size}",
                    NamedTextColor.YELLOW,
                )
            }

            break
        }

        loopEnd()
    }

    private fun loopEnd() {
        if (invalid) return
        locationIndex += 1
        checkSelfDestroy()
    }
}
