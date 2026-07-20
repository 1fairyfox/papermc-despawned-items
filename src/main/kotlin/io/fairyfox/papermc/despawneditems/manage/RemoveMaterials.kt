package io.fairyfox.papermc.despawneditems.manage

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.location.DespawnLocation
import io.fairyfox.papermc.despawneditems.sendColored
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID

/**
 * A long-running, tick-spread bulk removal: walks the relevant despawn locations one
 * per tick and removes matching materials (or a specific item) from each, reporting
 * progress to [sender]. Only one may run at a time per sender.
 */
class RemoveMaterials(
    private val sender: CommandSender,
    private val materials: List<Material>?,
    private val item: ItemStack?,
    private val plugin: PaperMcDespawnedItems,
    private val owner: UUID?,
    private val senderID: UUID?,
) {
    private var locationIndex = 0
    private var targets: List<DespawnLocation> = emptyList()
    private var invalid = false

    init {
        when {
            plugin.locations.isEmpty() -> forceSelfDestroy()

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

                targets =
                    if (owner != null) {
                        plugin.locations.ofOwner(owner).toList()
                    } else {
                        plugin.locations.all()
                    }

                if (targets.isEmpty()) {
                    forceSelfDestroy()
                } else {
                    newLoop()
                }
            }
        }
    }

    private fun newLoop() {
        if (invalid) return
        object : BukkitRunnable() {
            override fun run() {
                loadWorld(targets[locationIndex])
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
        if (locationIndex >= targets.size) {
            forceSelfDestroy()
        } else {
            newLoop()
        }
    }

    private fun loadWorld(despawnLocation: DespawnLocation) {
        if (invalid) return
        val location = despawnLocation.toLocation()
        if (location == null) {
            loopEnd() // world not loaded; skip this one
            return
        }
        location.world.getChunkAtAsync(location).thenRun { worldIsLoaded(location) }
    }

    // Same strategy-dispatch loop as DespawnProcess: continue = strategy doesn't apply,
    // break = the applicable strategy handled this block.
    @Suppress("LoopWithTooManyJumpStatements")
    private fun worldIsLoaded(location: Location) {
        if (invalid) return

        val block = location.world.getBlockAt(location.blockX, location.blockY, location.blockZ)

        for (strategy in plugin.strategies) {
            if (invalid) return
            if (!strategy.doesApply(block)) continue

            materials?.forEach { material ->
                if (invalid) return
                strategy.removeFrom(material, block)
            }

            item?.let { strategy.removeFrom(it, block) }

            if (locationIndex % PROGRESS_INTERVAL == 0) {
                sender.sendColored(
                    "Still processing... $locationIndex / ${targets.size}",
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

    private companion object {
        /** Emit a progress message every Nth location processed. */
        const val PROGRESS_INTERVAL = 20
    }
}
