package io.fairyfox.papermc.despawneditems.commands

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.location.DespawnLocation
import io.fairyfox.papermc.despawneditems.location.TargetOptions
import org.bukkit.entity.Player

/**
 * The `/despi target …` subcommands — per-target settings on the block you are looking at.
 *
 * This is the **primary** surface for a target's options, and deliberately so. It works
 * identically on every client, needs no mod and no special item, and reads like the rest of
 * the command tree. A command is more predictable than an item that pretends to be a tool or
 * a menu that pretends to be part of the game.
 *
 * Kept in its own class rather than piled into [DespiActions], which is already a broad
 * facade — one cohesive group of related subcommands per class.
 */
class TargetActions(private val plugin: PaperMcDespawnedItems) {
    private val fb = CommandFeedback

    /** `/despi target info` — report the looked-at target's settings. */
    fun info(player: Player) {
        val target = editable(player) ?: return
        val o = target.options
        fb.info(
            player,
            "Target ${target.x};${target.y};${target.z} — " +
                "${if (o.enabled) "ON" else "OFF"}, priority ${o.priority}, " +
                "banned items ${if (o.acceptContraband) "accepted" else "refused"}",
        )
    }

    /** `/despi target enable|disable|toggle`. [desired] null means flip it. */
    fun setEnabled(
        player: Player,
        desired: Boolean?,
    ) {
        val updated = update(player) { it.copy(enabled = desired ?: !it.enabled) } ?: return
        if (updated.options.enabled) {
            fb.success(player, "Target switched ON — despawning items may be relocated here.")
        } else {
            fb.success(player, "Target switched OFF — still registered, but skipped.")
        }
    }

    /** `/despi target priority <1-10>`. */
    fun setPriority(
        player: Player,
        priority: Int,
    ) {
        val clamped = priority.coerceIn(TargetOptions.MIN_PRIORITY, TargetOptions.MAX_PRIORITY)
        update(player) { it.copy(priority = clamped) } ?: return
        fb.success(player, "Target priority set to $clamped — drawn $clamped× as often as a priority-1 target.")
    }

    /** `/despi target contraband accept|refuse`. */
    fun setContraband(
        player: Player,
        accept: Boolean,
    ) {
        update(player) { it.copy(acceptContraband = accept) } ?: return
        if (accept) {
            fb.success(player, "This target now accepts banned items instead of them being destroyed.")
        } else {
            fb.success(player, "This target no longer accepts banned items.")
        }
    }

    /** Applies [transform] to the looked-at target and notifies any listening client mods. */
    private fun update(
        player: Player,
        transform: (TargetOptions) -> TargetOptions,
    ): DespawnLocation? {
        val target = editable(player) ?: return null
        val location = target.toLocation() ?: return null
        val updated = plugin.locations.updateOptions(location, target.owner, transform) ?: return null
        plugin.modBridge.broadcastTargetChanged(updated)
        return updated
    }

    /**
     * The target the player is looking at and is allowed to edit: their own first, anyone
     * else's only with `despi.elevated`. Explains itself when there is nothing to edit, so
     * the command never fails silently.
     */
    private fun editable(player: Player): DespawnLocation? {
        val location = fb.targetBlock(player) ?: return null
        plugin.locations.targetAt(location, player.uniqueId)?.let { return it }

        val other = plugin.locations.anyTargetAt(location)
        if (other == null) {
            fb.error(player, "That block is not a despawn target — add it with /despi add this")
            return null
        }
        if (!player.hasPermission(ELEVATED)) {
            fb.error(player, "That despawn target belongs to someone else")
            return null
        }
        return other
    }

    private companion object {
        const val ELEVATED = "despi.elevated"
    }
}
