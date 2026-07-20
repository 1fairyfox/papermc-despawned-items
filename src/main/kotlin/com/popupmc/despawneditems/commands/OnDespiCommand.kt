package com.popupmc.despawneditems.commands

import com.popupmc.despawneditems.DespawnedItems
import com.popupmc.despawneditems.sendColored
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

/**
 * Executor and tab-completer for `/despi`. Lazily registers every subcommand on
 * first use, then dispatches to the matching [AbstractDespiCommand].
 */
class OnDespiCommand(private val plugin: DespawnedItems) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>,
    ): Boolean {
        if (despiCommands.isEmpty()) registerDespiCommands(plugin)

        if (!sender.hasPermission("despi.use")) {
            sender.sendColored("You do not have permission to use this command", NamedTextColor.RED)
            return false
        }

        if (args.isEmpty()) {
            showDescriptions(sender, args)
            return false
        }

        val despiCommand = despiCommands[args[0].lowercase()]
        if (despiCommand == null) {
            showDescriptions(sender, args)
            return true
        }

        if (args.size == 2 && args[1].equals("help", ignoreCase = true) &&
            despiCommand.showDescription(sender, args)
        ) {
            despiCommand.displayHelp(sender, args)
            return true
        }

        val result = despiCommand.runCommand(sender, args)
        if (!result) despiCommand.displayHelp(sender, args)
        return true
    }

    private fun showDescriptions(sender: CommandSender, args: Array<String>) {
        for ((key, cmd) in despiCommands) {
            if (cmd.showDescription(sender, args)) {
                sender.sendMessage(
                    Component.text("/despi $key - ", NamedTextColor.YELLOW)
                        .append(Component.text(cmd.description, NamedTextColor.GOLD)),
                )
            }
        }
    }

    private fun filterTabComplete(results: List<String>, typedSoFar: String): List<String> =
        results.filter { it.lowercase().startsWith(typedSoFar.lowercase()) }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>,
    ): List<String>? {
        if (despiCommands.isEmpty()) registerDespiCommands(plugin)

        if (!sender.hasPermission("despi.use")) return listOf("")
        if (args.isEmpty()) return listOf("")

        if (args.size == 1) {
            val list = mutableListOf("help")
            for ((key, cmd) in despiCommands) {
                if (cmd.showDescription(sender, args)) list.add(key)
            }
            return filterTabComplete(list, args[0])
        }

        val despiCommand = despiCommands[args[0].lowercase()]
        if (despiCommand != null) {
            val list = despiCommand.onTabComplete(sender, args)?.toMutableList()
            if (list.isNullOrEmpty()) return listOf("")
            if (args.size == 2) list.add(0, "help")
            return filterTabComplete(list, args[args.size - 1])
        }

        return listOf("")
    }

    companion object {
        val despiCommands: MutableMap<String, AbstractDespiCommand> = HashMap()

        fun registerDespiCommands(plugin: DespawnedItems) {
            OnDespiCommandAdd(plugin)
            OnDespiCommandRemove(plugin)
            OnDespiCommandExists(plugin)
            OnDespiCommandReload(plugin)
            OnDespiCommandSave(plugin)
            OnDespiCommandLocations(plugin)
            OnDespiCommandClear(plugin)
            OnDespiCommandIndexes(plugin)
            OnDespiCommandEffects(plugin)
            OnDespiCommandDespawn(plugin)
            OnDespiCommandPurge(plugin)
        }
    }
}
