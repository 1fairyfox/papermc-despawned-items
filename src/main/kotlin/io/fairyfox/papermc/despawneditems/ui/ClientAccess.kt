package io.fairyfox.papermc.despawneditems.ui

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import org.bukkit.entity.Player

/**
 * Whether a given player may drive this plugin from a **client-side mod**, and what they are
 * allowed to do once they can.
 *
 * Two independent gates, because they answer different people's questions:
 *
 * 1. **The server owner's gate** — `targets.client-mod.enabled` in `config.yml`. Some
 *    servers do not want client mods at all, or do not want *this* one. Setting it false
 *    makes the plugin refuse the handshake outright, and a conforming mod hides its UI
 *    rather than showing a button that silently does nothing.
 * 2. **The player's gate** — the `despi.client` permission. On a server that does allow
 *    client mods, an admin can still decide which ranks get the in-game interface.
 *
 * Neither gate changes what the *commands* can do: `/despi` remains the universal surface
 * that works on any client, and a server that switches client mods off loses nothing.
 */
object ClientAccess {
    /** Permission controlling whether a player may use the client-mod interface. */
    const val PERMISSION = "despi.client"

    /**
     * Protocol revision. Sent in the handshake so a mod can refuse to talk to a server it
     * does not understand — and so this plugin can evolve the wire format later without
     * breaking older mods.
     */
    const val PROTOCOL_VERSION = 1

    /** Why a client was refused, in a form a mod can branch on and a human can read. */
    enum class Denial(val reason: String) {
        SERVER_DISABLED("client-mod support is switched off on this server"),
        NO_PERMISSION("you do not have permission to use the client interface"),
    }

    /** Null when [player] may use the client interface, else the reason they may not. */
    fun denialFor(
        plugin: PaperMcDespawnedItems,
        player: Player,
    ): Denial? =
        when {
            !plugin.settings.targetUi.clientModEnabled -> Denial.SERVER_DISABLED
            !player.hasPermission(PERMISSION) -> Denial.NO_PERMISSION
            else -> null
        }

    /**
     * The capabilities [player] has, as flags a client mod can use to grey out controls it
     * must not offer. Sent with the handshake so the mod never has to guess — and so a
     * player is never shown a button that the server will refuse.
     */
    fun capabilitiesFor(
        plugin: PaperMcDespawnedItems,
        player: Player,
    ): List<String> =
        buildList {
            if (player.hasPermission("despi.use")) add("manage-own")
            if (player.hasPermission("despi.elevated")) add("manage-others")
            if (player.hasPermission("recycle.use")) add("recycle")
            if (plugin.settings.limits.unlimited || player.hasPermission("despi.limit.bypass")) add("unlimited")
        }
}
