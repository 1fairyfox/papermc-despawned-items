package com.popupmc.despawneditems.events

import com.popupmc.despawneditems.DespawnedItems
import com.popupmc.despawneditems.despawn.DespawnProcess
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ItemDespawnEvent

/** Starts a [DespawnProcess] whenever a ground item is about to despawn. */
class OnItemDespawnEvent(private val plugin: DespawnedItems) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onEvent(event: ItemDespawnEvent) {
        // Clone so we don't operate on the soon-to-be-removed item entity.
        val item = event.entity.itemStack.clone()
        DespawnProcess(item, plugin)
    }
}
