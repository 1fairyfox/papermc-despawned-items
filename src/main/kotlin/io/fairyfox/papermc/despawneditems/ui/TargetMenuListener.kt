package io.fairyfox.papermc.despawneditems.ui

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.location.TargetOptions
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryOpenEvent

/**
 * Handles clicks inside the [TargetMenu].
 *
 * Identifies its own screens **by holder type** ([TargetMenuHolder]), never by title or
 * contents — so it cannot fire on another plugin's GUI, and another plugin's handler cannot
 * fire on this one. Any click in one of our screens is cancelled outright, which makes the
 * menu read-only: buttons cannot be dragged out, shift-clicked into the player's inventory,
 * or duplicated.
 */
class TargetMenuListener(private val plugin: PaperMcDespawnedItems) : Listener {
    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val holder = event.view.topInventory.holder as? TargetMenuHolder ?: return

        // Ours: nothing in this screen is ever takeable.
        event.isCancelled = true
        if (event.clickedInventory !== event.view.topInventory) return

        val player = event.whoClicked as? Player ?: return
        val target = plugin.locations.targetAt(holder.location, holder.target.owner) ?: return

        val updated =
            when (event.rawSlot) {
                TargetMenuSlots.TOGGLE ->
                    plugin.locations.updateOptions(holder.location, target.owner) {
                        it.copy(enabled = !it.enabled)
                    }

                TargetMenuSlots.PRIORITY ->
                    plugin.locations.updateOptions(holder.location, target.owner) {
                        val step = if (event.isRightClick) -1 else 1
                        val next = it.priority + step
                        // Wrap rather than clamp: a player cycling with one button should
                        // never hit a dead end they cannot get out of.
                        val wrapped =
                            when {
                                next > TargetOptions.MAX_PRIORITY -> TargetOptions.MIN_PRIORITY
                                next < TargetOptions.MIN_PRIORITY -> TargetOptions.MAX_PRIORITY
                                else -> next
                            }
                        it.copy(priority = wrapped)
                    }

                TargetMenuSlots.CONTRABAND ->
                    plugin.locations.updateOptions(holder.location, target.owner) {
                        it.copy(acceptContraband = !it.acceptContraband)
                    }

                else -> null
            } ?: return

        TargetMenu.render(event.view.topInventory, updated.options)
        plugin.modBridge.broadcastTargetChanged(updated)
        plugin.modBridge.sendTargetState(player, updated)
    }

    /** Dragging is a second way to move items; refuse it in our screens too. */
    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        if (event.view.topInventory.holder is TargetMenuHolder) event.isCancelled = true
    }

    /**
     * When a player with a client mod opens a real container, push that block's target
     * state so the mod's in-screen button can render "marked" or "not marked" immediately,
     * with no flicker and no round trip.
     *
     * Costs nothing for everyone else: the send short-circuits for any player who has not
     * registered the channel, which is every vanilla client.
     */
    @EventHandler
    fun onOpen(event: InventoryOpenEvent) {
        val player = event.player as? Player ?: return
        if (!plugin.settings.targetUi.modBridge) return
        if (!player.listeningPluginChannels.contains(plugin.modBridge.channel)) return

        val holder = event.inventory.holder
        if (holder is TargetMenuHolder) return // our own screen, not a world container
        val block = (holder as? org.bukkit.block.Container)?.block ?: return
        plugin.modBridge.sendStateFor(player, block.location)
    }
}
