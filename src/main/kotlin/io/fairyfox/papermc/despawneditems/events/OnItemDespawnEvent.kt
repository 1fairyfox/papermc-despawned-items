package io.fairyfox.papermc.despawneditems.events

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ItemDespawnEvent

/**
 * Queues a ground item for relocation whenever it is about to despawn. Enqueuing (rather
 * than starting a process immediately) lets the [io.fairyfox.papermc.despawneditems.despawn.DespawnScheduler]
 * bound how much relocation work runs per tick under heavy load.
 */
class OnItemDespawnEvent(private val plugin: PaperMcDespawnedItems) : Listener {
    @EventHandler(ignoreCancelled = true)
    fun onEvent(event: ItemDespawnEvent) {
        // Clone so we don't operate on the soon-to-be-removed item entity.
        val item = event.entity.itemStack.clone()
        // Attribute the item to whoever dropped it, so per-user throttling has an actor.
        // Mob loot, dispensers and block breaks have no thrower — those stay ownerless and
        // are only throttled when `throttle.throttle-unowned` is switched on.
        plugin.despawnScheduler.enqueue(item, event.entity.thrower)
    }
}
