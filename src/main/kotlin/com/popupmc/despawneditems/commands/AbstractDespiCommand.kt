package com.popupmc.despawneditems.commands

import com.popupmc.despawneditems.DespawnedItems
import com.popupmc.despawneditems.sendColored
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Base class for every `/despi` subcommand. Registers itself into
 * [OnDespiCommand.despiCommands] on construction and provides shared helpers for
 * argument parsing, permission checks, target-block lookup, and coloured
 * feedback messages.
 */
abstract class AbstractDespiCommand(
    protected val plugin: DespawnedItems,
    action: String,
    val description: String,
) {

    val action: String = action.lowercase()

    init {
        OnDespiCommand.despiCommands[this.action] = this
    }

    abstract fun runCommand(sender: CommandSender, args: Array<String>): Boolean
    abstract fun onTabComplete(sender: CommandSender, args: Array<String>): List<String>?
    abstract fun displayHelp(sender: CommandSender, args: Array<String>)
    abstract fun showDescription(sender: CommandSender, args: Array<String>): Boolean

    /** Target block location for a command sender, or null (with an error) if not a player. */
    fun getTargetLocation(sender: CommandSender): Location? {
        val player = isPlayer(sender) ?: return null
        return getTargetLocation(player)
    }

    /** Block the player is looking at within 5 blocks, or null (with an error). */
    fun getTargetLocation(player: Player): Location? {
        val block = player.getTargetBlockExact(5)
        if (block == null || block.type.isAir) {
            error("Unable to find block, are you within 5 blocks of something?", player)
            return null
        }
        return block.location
    }

    fun isPlayer(sender: CommandSender, msg: String? = null): Player? {
        if (sender !is Player) {
            error(msg ?: "Player not found", sender)
            return null
        }
        return sender
    }

    fun getArg(index: Int, args: Array<String>): String? = args.getOrNull(index)

    @Suppress("DEPRECATION")
    fun getPlayer(name: String): OfflinePlayer = Bukkit.getOfflinePlayer(name)

    fun hasPermission(permission: String, sender: CommandSender): Boolean =
        hasPermission(permission, null, sender)

    fun hasPermission(permission: String, msg: String?, sender: CommandSender): Boolean {
        if (!sender.hasPermission(permission)) {
            error(msg ?: "you don't have permission for that", sender)
            return false
        }
        return true
    }

    fun canBeElevated(sender: CommandSender): Boolean =
        hasPermission(ELEVATED_PERMISSION, null, sender)

    fun canBeElevated(msg: String, sender: CommandSender): Boolean =
        hasPermission(ELEVATED_PERMISSION, msg, sender)

    fun error(msg: String, sender: CommandSender) = sender.sendColored("ERROR: $msg", NamedTextColor.RED)

    fun success(msg: String, sender: CommandSender) = sender.sendColored(msg, NamedTextColor.GREEN)

    fun warning(msg: String, sender: CommandSender) = sender.sendColored("WARNING: $msg", NamedTextColor.GOLD)

    companion object {
        const val ELEVATED_PERMISSION = "despi.elevated"
    }
}
