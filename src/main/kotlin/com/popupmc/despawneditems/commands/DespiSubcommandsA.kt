package com.popupmc.despawneditems.commands

import com.popupmc.despawneditems.DespawnedItems
import com.popupmc.despawneditems.despawn.DespawnEffect
import com.popupmc.despawneditems.limit.DespawnLimits
import com.popupmc.despawneditems.sendColored
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/** `/despi add this [player]` — register the block you're looking at. */
class OnDespiCommandAdd(plugin: DespawnedItems) :
    AbstractDespiCommand(plugin, "add", "Adds a location to receive despawn items") {

    override fun runCommand(sender: CommandSender, args: Array<String>): Boolean {
        val player = isPlayer(sender, "Only players can add locations.") ?: return false
        val thisStr = getArg(1, args)
        val playerName = getArg(2, args)

        if (thisStr == null || !thisStr.equals("this", ignoreCase = true)) return false

        return if (playerName != null) addLocationAsPlayer(player, playerName) else addLocation(player, null)
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): List<String> {
        val list = mutableListOf<String>()
        if (args.size == 2) list.add("this")
        if (args.size == 3 && canBeElevated(sender)) {
            Bukkit.getOnlinePlayers().forEach { list.add(it.name) }
        }
        return list
    }

    override fun displayHelp(sender: CommandSender, args: Array<String>) {
        sender.sendColored("/despi add this (Marks the location you're pointing at as a despawn block owned by you)", NamedTextColor.GRAY)
        if (canBeElevated(sender)) {
            sender.sendColored("/despi add this <player> (Marks the location you're pointing at as owned by someone else)", NamedTextColor.GRAY)
        }
    }

    override fun showDescription(sender: CommandSender, args: Array<String>): Boolean = true

    private fun addLocationAsPlayer(sender: Player, otherPlayerName: String): Boolean {
        if (!canBeElevated("You don't have permission to add ownership of others", sender)) return false
        return addLocation(sender, getPlayer(otherPlayerName))
    }

    private fun addLocation(sender: Player, owner: OfflinePlayer?): Boolean {
        val actualOwner = owner ?: sender
        val location = getTargetLocation(sender) ?: return false

        // Enforce the per-user cap for self-service adds; admins adding for someone else
        // (already gated behind despi.elevated) are not capped.
        if (owner == null) {
            val limits = plugin.settings.limits
            val current = plugin.locations.countOfOwner(sender.uniqueId)
            if (!DespawnLimits.canAddAnother(sender, current, limits)) {
                error("You've reached your limit of ${DespawnLimits.resolve(sender, limits)} despawn location(s).", sender)
                return false
            }
        }

        return if (plugin.locations.add(location, actualOwner.uniqueId)) {
            success("Successfully added location!", sender)
            true
        } else {
            warning("Location already exists!", sender)
            false
        }
    }
}

/** `/despi clear mine|here|<player>` — bulk-remove despawn locations. */
class OnDespiCommandClear(plugin: DespawnedItems) :
    AbstractDespiCommand(plugin, "clear", "Clears all of your despawn locations") {

    override fun runCommand(sender: CommandSender, args: Array<String>): Boolean {
        val arg1 = getArg(1, args) ?: return false
        return when {
            arg1.equals("mine", ignoreCase = true) -> removeAllLocationsByOwner(sender, null)
            arg1.equals("here", ignoreCase = true) -> removeAllOwnersByLocation(sender)
            else -> removeAllLocationsByOwner(sender, arg1)
        }
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): List<String>? {
        if (!canBeElevated(sender)) return null
        val list = mutableListOf<String>()
        if (args.size == 2) {
            list.add("here")
            Bukkit.getOnlinePlayers().forEach { list.add(it.name) }
        }
        return list
    }

    override fun displayHelp(sender: CommandSender, args: Array<String>) {
        sender.sendColored("/despi clear mine (Unmarks all of your despawn blocks)", NamedTextColor.GRAY)
        if (canBeElevated(sender)) {
            sender.sendColored("/despi clear here (Unmarks all despawn block owners of this location)", NamedTextColor.GRAY)
            sender.sendColored("/despi clear <player> (Unmarks all despawn blocks of a player)", NamedTextColor.GRAY)
        }
    }

    override fun showDescription(sender: CommandSender, args: Array<String>): Boolean = true

    private fun removeAllLocationsByOwner(sender: CommandSender, ownerName: String?): Boolean {
        if (ownerName != null &&
            !canBeElevated("You don't have permission to remove all locations of someone else", sender)
        ) return false

        val player: OfflinePlayer? = if (ownerName == null) isPlayer(sender) else getPlayer(ownerName)
        if (player == null) return false

        val removed = plugin.locations.removeAllOfOwner(player.uniqueId)
        val label = ownerName ?: "you"

        if (removed > 0) success("$removed location(s) were removed for $label", sender)
        else warning("No locations found to remove for $label", sender)
        return true
    }

    private fun removeAllOwnersByLocation(sender: CommandSender): Boolean {
        if (!canBeElevated("You don't have permission to remove all owners of this location", sender)) return false
        val player = isPlayer(sender) ?: return false
        val location = getTargetLocation(player) ?: return false

        val removed = plugin.locations.removeAllAt(location)
        if (removed > 0) success("$removed owner(s) were removed for this location", sender)
        else warning("No owners found to remove for this location.", sender)
        return true
    }
}

/** `/despi reload do` — reload config from disk. */
class OnDespiCommandReload(plugin: DespawnedItems) :
    AbstractDespiCommand(plugin, "reload", "Reloads the plugin config") {

    override fun runCommand(sender: CommandSender, args: Array<String>): Boolean {
        if (!canBeElevated(sender)) return false
        if (args.size != 2 || !args[1].equals("do", ignoreCase = true)) return false
        plugin.settings.load()
        plugin.locations.reload()
        success("Config and storage have been reloaded", sender)
        return true
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): List<String>? {
        if (!canBeElevated(sender)) return null
        return if (args.size == 2) mutableListOf("do") else mutableListOf()
    }

    override fun displayHelp(sender: CommandSender, args: Array<String>) {
        if (canBeElevated(sender)) sender.sendColored("/despi reload do", NamedTextColor.GRAY)
        else sender.sendColored("You don't have access to this command", NamedTextColor.GRAY)
    }

    override fun showDescription(sender: CommandSender, args: Array<String>): Boolean = canBeElevated(sender)
}

/** `/despi save do` — force-save despawn locations. */
class OnDespiCommandSave(plugin: DespawnedItems) :
    AbstractDespiCommand(plugin, "save", "Saves locations") {

    override fun runCommand(sender: CommandSender, args: Array<String>): Boolean {
        if (!canBeElevated(sender)) return false
        if (args.size != 2 || !args[1].equals("do", ignoreCase = true)) return false
        plugin.locations.saveNow()
        success("Locations have been saved", sender)
        return true
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): List<String>? {
        if (!canBeElevated(sender)) return null
        return if (args.size == 2) mutableListOf("do") else mutableListOf()
    }

    override fun displayHelp(sender: CommandSender, args: Array<String>) {
        if (canBeElevated(sender)) sender.sendColored("/despi save do", NamedTextColor.GRAY)
        else sender.sendColored("You don't have access to this command", NamedTextColor.GRAY)
    }

    override fun showDescription(sender: CommandSender, args: Array<String>): Boolean = canBeElevated(sender)
}

/** `/despi effects ...` — manage/test the landing effect. */
class OnDespiCommandEffects(plugin: DespawnedItems) :
    AbstractDespiCommand(plugin, "effects", "Manages effects, often for testing") {

    override fun runCommand(sender: CommandSender, args: Array<String>): Boolean {
        if (!canBeElevated(sender)) return false
        return when (getArg(1, args)?.lowercase()) {
            "create-here" -> create(sender)
            "clear-ongoing" -> clear(sender)
            "count-ongoing" -> sendCount(sender)
            else -> false
        }
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): List<String>? {
        if (!canBeElevated(sender)) return null
        return if (args.size == 2) mutableListOf("count-ongoing", "create-here", "clear-ongoing") else mutableListOf()
    }

    override fun displayHelp(sender: CommandSender, args: Array<String>) {
        if (canBeElevated(sender)) {
            sender.sendColored("/despi effects count-ongoing (Count ongoing effects)", NamedTextColor.GRAY)
            sender.sendColored("/despi effects create-here (Make an effect here)", NamedTextColor.GRAY)
            sender.sendColored("/despi effects clear-ongoing (Remove ongoing effects)", NamedTextColor.GRAY)
        } else {
            sender.sendColored("You don't have access to this command", NamedTextColor.GRAY)
        }
    }

    override fun showDescription(sender: CommandSender, args: Array<String>): Boolean = canBeElevated(sender)

    private fun sendCount(sender: CommandSender): Boolean {
        success("Effects Count: ${plugin.effectsPlaying.size}", sender)
        return true
    }

    private fun clear(sender: CommandSender): Boolean {
        val count = plugin.effectsPlaying.size
        ArrayList(plugin.effectsPlaying).forEach { it.forceSelfDestroy() }
        success("Cleared $count on-going effects", sender)
        return true
    }

    private fun create(sender: CommandSender): Boolean {
        val player = isPlayer(sender) ?: return false
        val location = getTargetLocation(player) ?: return false
        DespawnEffect(location, plugin)
        success("Created fake effect at location", sender)
        return true
    }
}
