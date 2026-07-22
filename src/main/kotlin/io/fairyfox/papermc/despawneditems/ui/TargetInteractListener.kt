package io.fairyfox.papermc.despawneditems.ui

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.sendColored
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

/**
 * Turns a click on a despawn target into a toggle or an options screen.
 *
 * ### The conflict-avoidance contract
 *
 * This listener is written to be a good neighbour on a server full of other plugins and
 * mods. Every rule below exists to stop it stealing an interaction somebody else wanted:
 *
 * 1. **`EventPriority.HIGH` + `ignoreCancelled = true`.** Protection plugins (WorldGuard,
 *    GriefPrevention, claims mods) cancel at LOW/NORMAL. Running after them, and skipping
 *    cancelled events, means a player who may not interact with a block cannot use the wand
 *    on it either — the protection plugin's answer is final.
 * 2. **A tagged wand is required.** Without the wand in hand this listener returns before
 *    touching anything, so ordinary chest, shop and machine interaction is untouched. The
 *    wand is identified by a PDC key in this plugin's namespace, not by material, so it
 *    cannot collide with another mod's item (see [TargetWand]).
 * 3. **Main hand only.** `PlayerInteractEvent` fires once per hand; ignoring the off hand
 *    prevents the double-fire that makes toggles flicker back to their original state.
 * 4. **`useInteractedBlock() == DENY` is respected.** If anything earlier in the chain has
 *    already denied block use, this does nothing even if the event itself is not cancelled.
 * 5. **The event is cancelled only when the click is actually handled.** A wand click on a
 *    block that is not a despawn target falls through untouched, so the wand behaves like an
 *    ordinary item everywhere else — no surprise "nothing happened" for the player and no
 *    swallowed interaction for other plugins.
 */
class TargetInteractListener(private val plugin: PaperMcDespawnedItems) : Listener {
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEvent) {
        if (!isOurGesture(event)) return

        val player = event.player
        val block = event.clickedBlock ?: return
        val location = block.location

        // Rule 5: not ours — leave the event completely alone.
        val target = resolveEditableTarget(player, location) ?: return

        // Deny both halves rather than calling the deprecated setCancelled(): this is the
        // precise, non-deprecated way to say "the wand handled this, don't also open the
        // chest and don't also place the item".
        event.setUseInteractedBlock(Event.Result.DENY)
        event.setUseItemInHand(Event.Result.DENY)

        if (player.isSneaking) {
            toggle(plugin, player, location, target.owner)
        } else {
            TargetMenu.open(player, location, target)
        }
    }

    /**
     * The guard chain: is this the deliberate wand gesture this plugin reacts to, and is
     * everyone else happy for us to have it? Kept separate from the action so the rules read
     * as a list and each one can be tested on its own.
     */
    private fun isOurGesture(event: PlayerInteractEvent): Boolean {
        if (!plugin.settings.targetUi.enabled) return false
        if (event.action != Action.RIGHT_CLICK_BLOCK) return false
        if (event.hand != EquipmentSlot.HAND) return false // rule 3: main hand only
        if (event.useInteractedBlock() == Event.Result.DENY) return false // rule 4
        if (!TargetWand.isWand(plugin, event.item)) return false // rule 2
        return event.player.hasPermission(BUTTON_PERMISSION)
    }

    /**
     * The target this player may edit here: their own registration first, and anyone else's
     * only with the elevated permission. A player must never be able to switch off somebody
     * else's chest.
     */
    private fun resolveEditableTarget(
        player: org.bukkit.entity.Player,
        location: org.bukkit.Location,
    ) = plugin.locations.targetAt(location, player.uniqueId)
        ?: plugin.locations.anyTargetAt(location)?.takeIf { player.hasPermission(ELEVATED_PERMISSION) }

    companion object {
        const val BUTTON_PERMISSION = "despi.button"
        private const val ELEVATED_PERMISSION = "despi.elevated"

        /**
         * Flips a target on or off and tells the player. Shared with the menu so the two
         * entry points can never diverge.
         */
        fun toggle(
            plugin: PaperMcDespawnedItems,
            player: org.bukkit.entity.Player,
            location: org.bukkit.Location,
            owner: java.util.UUID,
        ) {
            val updated =
                plugin.locations.updateOptions(location, owner) { it.copy(enabled = !it.enabled) }
                    ?: return
            plugin.modBridge.broadcastTargetChanged(updated)
            if (updated.options.enabled) {
                player.sendColored("Despawn target switched ON.", NamedTextColor.GREEN)
            } else {
                player.sendColored("Despawn target switched OFF (still registered).", NamedTextColor.YELLOW)
            }
        }
    }
}
