package com.popupmc.despawneditems.events

import com.popupmc.despawneditems.DespawnedItems
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ItemDespawnEvent

/**
 * Queues a ground item for relocation whenever it is about to despawn. Enqueuing (rather
 * than starting a process immediately) lets the [com.popupmc.despawneditems.despawn.DespawnScheduler]
 * bound how much relocation work runs per tick under heavy load.
 */
class OnItemDespawnEvent(private val plugin: DespawnedItems) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onEvent(event: ItemDespawnEvent) {
        // Clone so we don't operate on the soon-to-be-removed item entity.
        val item = event.entity.itemStack.clone()
        plugin.despawnScheduler.enqueue(item)
    }
}
