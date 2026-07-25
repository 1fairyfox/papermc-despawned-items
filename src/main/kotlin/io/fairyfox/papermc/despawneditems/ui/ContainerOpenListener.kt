package io.fairyfox.papermc.despawneditems.ui

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import org.bukkit.block.Container
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryOpenEvent

/**
 * Pushes a block's despawn-target state to a client mod when the player opens that container.
 *
 * This is the whole server-side footprint of the client interface: **one read-only
 * notification**. It cancels nothing, changes nothing, and adds no items — so it cannot
 * conflict with shops, protection plugins, storage mods or anything else that cares about
 * container interaction. If the player's mod has not registered the channel (i.e. almost
 * everyone), the send short-circuits and this listener costs a map lookup.
 *
 * The reason it exists at all is latency: the mod's button should already show the right
 * state the instant the screen appears, rather than flickering while a round trip completes.
 */
class ContainerOpenListener(private val plugin: PaperMcDespawnedItems) : Listener {
    @EventHandler
    fun onOpen(event: InventoryOpenEvent) {
        if (!plugin.settings.targetUi.clientModEnabled) return
        val player = event.player as? Player ?: return
        if (!player.listeningPluginChannels.contains(plugin.modBridge.channel)) return
        if (ClientAccess.denialFor(plugin, player) != null) return

        val block = (event.inventory.holder as? Container)?.block ?: return
        plugin.modBridge.sendStateFor(player, block.location)
    }
}
