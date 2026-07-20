package com.popupmc.despawneditems.commands

import com.popupmc.despawneditems.DespawnedItems
import com.popupmc.despawneditems.despawn.DespawnProcess
import com.popupmc.despawneditems.location.DespawnLocation
import com.popupmc.despawneditems.manage.RemoveMaterials
import com.popupmc.despawneditems.sendColored
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID

/** `/despi despawn ...` — force or inspect despawns (testing). */
class OnDespiCommandDespawn(plugin: DespawnedItems) :
    AbstractDespiCommand(plugin, "despawn", "Manages despawning often for testing") {

    override fun runCommand(sender: CommandSender, args: Array<String>): Boolean {
        if (!canBeElevated(sender)) return false

        val option = getArg(1, args) ?: return sendCount(sender)
        val material = getArg(2, args)
        val amountStr = getArg(3, args)
        var amount = 1

        if (option.equals("create-material", ignoreCase = true) && material == null) {
            error("You must specify a material", sender)
            return false
        }

        if (amountStr != null) {
            amount = amountStr.toIntOrNull() ?: run {
                error("Unable to parse amount", sender)
                return false
            }
        }

        return when {
            option.equals("create-from-hand", ignoreCase = true) -> createFromHand(sender)
            option.equals("create-material", ignoreCase = true) -> createFromMaterial(sender, material!!, amount)
            option.equals("clear-ongoing", ignoreCase = true) -> clear(sender)
            option.equals("count-ongoing", ignoreCase = true) -> sendCount(sender)
            else -> false
        }
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): List<String>? {
        if (!canBeElevated(sender)) return null
        val list = mutableListOf<String>()

        if (args.size == 2) {
            list.addAll(listOf("count-ongoing", "create-from-hand", "create-material", "clear-ongoing"))
        }
        if (args.size == 3 && args[1].equals("create-material", ignoreCase = true)) {
            Material.entries.forEach { list.add(it.name.lowercase()) }
        }
        if (args.size == 4 && args[1].equals("create-material", ignoreCase = true)) {
            runCatching { Material.valueOf(args[2].uppercase()) }
                .onSuccess { list.add(it.maxStackSize.toString()) }
        }
        return list
    }

    override fun displayHelp(sender: CommandSender, args: Array<String>) {
        if (canBeElevated(sender)) {
            sender.sendColored("/despi despawn count-ongoing (Count on-going despawns)", NamedTextColor.GRAY)
            sender.sendColored("/despi despawn create-from-hand (Despawn the item in your hand)", NamedTextColor.GRAY)
            sender.sendColored("/despi despawn create-material <materials> <amt> (Despawn a named material)", NamedTextColor.GRAY)
            sender.sendColored("/despi despawn clear-ongoing (Stop ongoing processes)", NamedTextColor.GRAY)
        } else {
            sender.sendColored("You don't have access to this command", NamedTextColor.GRAY)
        }
    }

    override fun showDescription(sender: CommandSender, args: Array<String>): Boolean = canBeElevated(sender)

    private fun sendCount(sender: CommandSender): Boolean {
        success("Despawn Count: ${plugin.despawnProcesses.size}", sender)
        return true
    }

    private fun clear(sender: CommandSender): Boolean {
        val count = plugin.despawnProcesses.size
        ArrayList(plugin.despawnProcesses).forEach { it.selfDestroy() }
        success("Cleared $count on-going despawns", sender)
        return true
    }

    private fun createFromHand(sender: CommandSender): Boolean {
        val player = isPlayer(sender) ?: return false
        val item = player.inventory.itemInMainHand
        if (item.amount == 0 || item.type.isAir) {
            error("You have no items in your hand", sender)
            return false
        }
        DespawnProcess(item, plugin)
        success("Created forced despawn", sender)
        return true
    }

    private fun createFromMaterial(sender: CommandSender, materialStr: String, amountIn: Int): Boolean {
        val amount = if (amountIn <= 0) 1 else amountIn
        val materials = mutableListOf<Material>()

        for (name in materialStr.split(",")) {
            runCatching { Material.valueOf(name.uppercase()) }
                .onSuccess { materials.add(it) }
                .onFailure { warning("invalid material name $name skipping...", sender) }
        }

        if (materials.isEmpty()) {
            error("No material names usable.", sender)
            return false
        }

        materials.forEach { DespawnProcess(ItemStack(it, amount), plugin) }
        success("Created forced despawn", sender)
        return true
    }
}

/** `/despi exists ...` — query whether a despawn location exists. */
class OnDespiCommandExists(plugin: DespawnedItems) :
    AbstractDespiCommand(plugin, "exists", "Checks despawn location and ownership information") {

    override fun runCommand(sender: CommandSender, args: Array<String>): Boolean {
        val hereOrAnywhere = getArg(1, args)
        val ownedBy = getArg(2, args)
        val playerName = getArg(3, args)

        if (hereOrAnywhere == null ||
            (!hereOrAnywhere.equals("here", ignoreCase = true) && !hereOrAnywhere.equals("anywhere", ignoreCase = true))
        ) return false

        if (hereOrAnywhere.equals("anywhere", ignoreCase = true) &&
            ownedBy.equals("owned-by", ignoreCase = true) && playerName != null
        ) return existsAnyLocationByName(sender, playerName)

        val player = isPlayer(sender, "Only players can check locations") ?: return false

        if (hereOrAnywhere.equals("anywhere", ignoreCase = true) &&
            ownedBy.equals("owned-by-me", ignoreCase = true) && playerName == null
        ) return existsAnyLocationByName(player, player as OfflinePlayer)

        if (hereOrAnywhere.equals("here", ignoreCase = true) &&
            ownedBy.equals("owned-by", ignoreCase = true) && playerName != null
        ) return existsThisLocation(player, playerName)

        if (hereOrAnywhere.equals("here", ignoreCase = true) &&
            ownedBy.equals("owned-by-anyone", ignoreCase = true) && playerName == null
        ) return existsThisLocation(player, null, true)

        if (hereOrAnywhere.equals("here", ignoreCase = true) &&
            ownedBy.equals("owned-by-me", ignoreCase = true) && playerName == null
        ) return existsThisLocation(player, null, false)

        return false
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): List<String> {
        val canElevate = canBeElevated(sender)
        val list = mutableListOf<String>()

        when {
            args.size == 2 -> list.addAll(listOf("here", "anywhere"))
            args.size == 3 -> {
                if (canElevate) list.add("owned-by")
                if (!args[1].equals("anywhere", ignoreCase = true) && canElevate) list.add("owned-by-anyone")
                list.add("owned-by-me")
            }
            args.size == 4 && args[2].equals("owned-by", ignoreCase = true) && canElevate ->
                Bukkit.getOnlinePlayers().forEach { list.add(it.name) }
        }
        return list
    }

    override fun displayHelp(sender: CommandSender, args: Array<String>) {
        sender.sendColored("/despi exists [here|anywhere] owned-by-me (Do I own this or any location)", NamedTextColor.GRAY)
        if (canBeElevated(sender)) {
            sender.sendColored("/despi exists [here|anywhere] owned-by <player> (Does this player own it)", NamedTextColor.GRAY)
            sender.sendColored("/despi exists [here] owned-by-anyone (Does anyone own this location)", NamedTextColor.GRAY)
        }
    }

    override fun showDescription(sender: CommandSender, args: Array<String>): Boolean = true

    private fun existsAnyLocationByName(sender: CommandSender, ownerName: String): Boolean {
        if (!canBeElevated("You don't have permission to check existence for someone else's location", sender)) return false
        return existsAnyLocationByName(sender, getPlayer(ownerName))
    }

    private fun existsAnyLocationByName(sender: CommandSender, owner: OfflinePlayer): Boolean {
        val found = plugin.locations.firstOfOwner(owner.uniqueId)
        if (found != null) {
            success("A location was found!", sender)
            sender.sendColored(found.serialize(), NamedTextColor.GOLD)
        } else {
            warning("No location was found", sender)
        }
        return true
    }

    private fun existsThisLocation(sender: Player, ownerName: String): Boolean {
        if (!canBeElevated("You don't have permission to check existence for someone else's location", sender)) return false
        return existsThisLocation(sender, getPlayer(ownerName), false)
    }

    private fun existsThisLocation(sender: Player, owner: OfflinePlayer?, anyOwner: Boolean): Boolean {
        val actualOwner = owner ?: sender
        val location = getTargetLocation(sender) ?: return false

        val exists = if (anyOwner) {
            plugin.locations.anyAt(location)
        } else {
            plugin.locations.has(location, actualOwner.uniqueId)
        }

        if (exists) success("Location does exist", sender) else warning("Location does not exist", sender)
        return true
    }
}

/** `/despi locations ...` — list/count locations, plus solo-mode testing. */
class OnDespiCommandLocations(plugin: DespawnedItems) :
    AbstractDespiCommand(plugin, "locations", "Obtains all of your locations.") {

    private val backupLocationEntries = mutableListOf<DespawnLocation>()

    override fun runCommand(sender: CommandSender, args: Array<String>): Boolean {
        val playerName = getArg(2, args)
        return when (getArg(1, args)?.lowercase()) {
            null -> false
            "player" -> existsAnyLocationByName(sender, playerName)
            "count" -> totalLocationCount(sender)
            "solo-mode" -> soloLocation(sender)
            "normal-mode" -> undoSolo(sender)
            "here" -> existsAllOwnersByLocation(sender)
            "mine" -> existsAnyLocationByName(sender, null)
            else -> false
        }
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): List<String> {
        val isElevated = canBeElevated(sender)
        val list = mutableListOf<String>()

        if (args.size == 2) {
            if (isElevated) list.addAll(listOf("player", "here", "count", "solo-mode", "normal-mode"))
            list.add("mine")
        }
        if (args.size == 3 && isElevated && args[1].equals("player", ignoreCase = true)) {
            Bukkit.getOnlinePlayers().forEach { list.add(it.name) }
        }
        return list
    }

    override fun displayHelp(sender: CommandSender, args: Array<String>) {
        sender.sendColored("/despi locations mine (All my locations)", NamedTextColor.GRAY)
        if (canBeElevated(sender)) {
            sender.sendColored("/despi locations player <player> (All locations of a player)", NamedTextColor.GRAY)
            sender.sendColored("/despi locations count (All locations by everyone)", NamedTextColor.GRAY)
            sender.sendColored("/despi locations solo-mode (Single despawn target)", NamedTextColor.GRAY)
            sender.sendColored("/despi locations normal-mode (Restore normal despawn targets)", NamedTextColor.GRAY)
            sender.sendColored("/despi locations here (All owners of this location)", NamedTextColor.GRAY)
        }
    }

    override fun showDescription(sender: CommandSender, args: Array<String>): Boolean = true

    private fun totalLocationCount(sender: CommandSender): Boolean {
        if (!canBeElevated("You don't have permission to view location count", sender)) return false
        success("Locations: ${plugin.locations.count}", sender)
        return true
    }

    private fun soloLocation(sender: CommandSender): Boolean {
        if (!canBeElevated("You don't have permission to create a solo location", sender)) return false
        val player = isPlayer(sender) ?: return false
        val location = getTargetLocation(player) ?: return false

        backupLocationEntries.clear()
        backupLocationEntries.addAll(plugin.locations.all())
        plugin.locations.replaceWith(listOf(DespawnLocation.of(location, player.uniqueId)))

        success("Solo'd this location, /despi locations normal-mode to restore", sender)
        return true
    }

    private fun undoSolo(sender: CommandSender): Boolean {
        if (!canBeElevated("You don't have permission to undo solo-mode", sender)) return false

        plugin.locations.replaceWith(backupLocationEntries)
        backupLocationEntries.clear()

        success("Undid solo", sender)
        return true
    }

    private fun existsAnyLocationByName(sender: CommandSender, ownerName: String?): Boolean {
        if (ownerName != null &&
            !canBeElevated("You don't have permission to check existence for someone else's location", sender)
        ) return false

        val player: OfflinePlayer? = if (ownerName == null) isPlayer(sender) else getPlayer(ownerName)
        if (player == null) return false

        val found = plugin.locations.ofOwner(player.uniqueId).toList()
        val label = ownerName ?: "you"

        if (found.isNotEmpty()) {
            success("${found.size} location(s) were found for $label", sender)
            found.forEach { sender.sendColored(it.serialize(), NamedTextColor.GOLD) }
        } else {
            warning("No locations found for $label", sender)
        }
        return true
    }

    @Suppress("DEPRECATION")
    private fun existsAllOwnersByLocation(sender: CommandSender): Boolean {
        if (!canBeElevated("You don't have permission to check existence for someone else's location", sender)) return false
        val player = isPlayer(sender) ?: return false
        val location = getTargetLocation(player) ?: return false

        val found = plugin.locations.atLocation(location)
        if (found.isNotEmpty()) {
            success("${found.size} owner(s) were found for this location", sender)
            found.forEach { sender.sendColored(Bukkit.getOfflinePlayer(it.owner).name ?: it.owner.toString(), NamedTextColor.GOLD) }
        } else {
            warning("No owners found for this location.", sender)
        }
        return true
    }
}

/** `/despi remove ...` — remove a despawn location. */
class OnDespiCommandRemove(plugin: DespawnedItems) :
    AbstractDespiCommand(plugin, "remove", "Removes the location you're pointing at from despawn") {

    override fun runCommand(sender: CommandSender, args: Array<String>): Boolean {
        val hereOrAnywhere = getArg(1, args)
        val ownedBy = getArg(2, args)
        val playerName = getArg(3, args)

        if (hereOrAnywhere == null ||
            (!hereOrAnywhere.equals("here", ignoreCase = true) && !hereOrAnywhere.equals("anywhere", ignoreCase = true))
        ) return false

        if (hereOrAnywhere.equals("anywhere", ignoreCase = true) &&
            ownedBy.equals("owned-by", ignoreCase = true) && playerName != null
        ) return removeAnyLocationByName(sender, playerName)

        val player = isPlayer(sender, "Only players can check locations") ?: return false

        if (hereOrAnywhere.equals("anywhere", ignoreCase = true) &&
            ownedBy.equals("owned-by-me", ignoreCase = true) && playerName == null
        ) return removeAnyLocationByName(player, player as OfflinePlayer)

        if (hereOrAnywhere.equals("here", ignoreCase = true) &&
            ownedBy.equals("owned-by", ignoreCase = true) && playerName != null
        ) return removeThisLocation(player, playerName)

        if (hereOrAnywhere.equals("here", ignoreCase = true) &&
            ownedBy.equals("owned-by-anyone", ignoreCase = true) && playerName == null
        ) return removeThisLocation(player, null, true)

        if (hereOrAnywhere.equals("here", ignoreCase = true) &&
            ownedBy.equals("owned-by-me", ignoreCase = true) && playerName == null
        ) return removeThisLocation(player, null, false)

        return false
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): List<String> {
        val canElevate = canBeElevated(sender)
        val list = mutableListOf<String>()

        when {
            args.size == 2 -> list.addAll(listOf("here", "anywhere"))
            args.size == 3 -> {
                if (canElevate) list.add("owned-by")
                if (!args[1].equals("anywhere", ignoreCase = true) && canElevate) list.add("owned-by-anyone")
                list.add("owned-by-me")
            }
            args.size == 4 && args[2].equals("owned-by", ignoreCase = true) && canElevate ->
                Bukkit.getOnlinePlayers().forEach { list.add(it.name) }
        }
        return list
    }

    override fun displayHelp(sender: CommandSender, args: Array<String>) {
        sender.sendColored("/despi remove [here|anywhere] owned-by-me (Remove your location here or in general)", NamedTextColor.GRAY)
        if (canBeElevated(sender)) {
            sender.sendColored("/despi remove [here|anywhere] owned-by <player> (Remove a player's location)", NamedTextColor.GRAY)
            sender.sendColored("/despi remove [here] owned-by-anyone (Remove any player's location here)", NamedTextColor.GRAY)
        }
    }

    override fun showDescription(sender: CommandSender, args: Array<String>): Boolean = true

    private fun removeAnyLocationByName(sender: CommandSender, ownerName: String): Boolean {
        if (!canBeElevated("You don't have permission to remove someone else's location", sender)) return false
        return removeAnyLocationByName(sender, getPlayer(ownerName))
    }

    private fun removeAnyLocationByName(sender: CommandSender, owner: OfflinePlayer): Boolean {
        val removed = plugin.locations.removeOneOfOwner(owner.uniqueId)
        if (removed) success("A location was removed!", sender)
        else warning("No location was removed (Did the player have locations?)", sender)
        return true
    }

    private fun removeThisLocation(sender: Player, ownerName: String): Boolean {
        if (!canBeElevated("You don't have permission to remove someone else's location", sender)) return false
        return removeThisLocation(sender, getPlayer(ownerName), false)
    }

    private fun removeThisLocation(sender: Player, owner: OfflinePlayer?, anyOwner: Boolean): Boolean {
        val actualOwner = owner ?: sender
        val location = getTargetLocation(sender) ?: return false

        val removed = if (anyOwner) {
            plugin.locations.removeOneAt(location)
        } else {
            plugin.locations.remove(location, actualOwner.uniqueId)
        }

        if (removed) success("Location removed", sender) else warning("Location wasn't removed (Did it ever exist?)", sender)
        return true
    }
}

/** `/despi purge ...` — bulk-remove materials/items from despawn storage. */
class OnDespiCommandPurge(plugin: DespawnedItems) :
    AbstractDespiCommand(plugin, "purge", "Removes a specific item in your hand or all items of a type.") {

    override fun runCommand(sender: CommandSender, args: Array<String>): Boolean {
        val ownedBy = getArg(1, args) ?: return false

        val playerName = if (ownedBy.equals("owned-by-player", ignoreCase = true)) getArg(2, args) else null
        val materialOrHand = if (playerName == null) getArg(2, args) else getArg(3, args)
        val materialNames = if (playerName == null) getArg(3, args) else getArg(4, args)

        if (materialOrHand == null) return false

        return when {
            ownedBy.equals("owned-by-player", ignoreCase = true) && playerName != null ->
                purgeOtherPlayerStuff(sender, playerName, materialOrHand, materialNames)
            ownedBy.equals("owned-by-everyone", ignoreCase = true) ->
                purgeEveryonesStuff(sender, materialOrHand, materialNames)
            ownedBy.equals("owned-by-me", ignoreCase = true) ->
                purgeOwnStuff(sender, materialOrHand, materialNames)
            else -> false
        }
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): List<String> {
        val isElevated = canBeElevated(sender)
        val list = mutableListOf<String>()

        when {
            args.size == 2 -> {
                if (isElevated) list.addAll(listOf("owned-by-player", "owned-by-everyone"))
                list.add("owned-by-me")
            }
            args.size == 3 && isElevated && args[1].equals("owned-by-player", ignoreCase = true) ->
                Bukkit.getOnlinePlayers().forEach { list.add(it.name) }
            args.size == 3 && (args[1].equals("owned-by-everyone", ignoreCase = true) || args[1].equals("owned-by-me", ignoreCase = true)) ->
                list.addAll(listOf("materials", "in-hand"))
            args.size == 4 && isElevated && args[1].equals("owned-by-player", ignoreCase = true) ->
                list.addAll(listOf("materials", "in-hand"))
            args.size == 4 &&
                (args[1].equals("owned-by-everyone", ignoreCase = true) || args[1].equals("owned-by-me", ignoreCase = true)) &&
                args[2].equals("materials", ignoreCase = true) ->
                Material.entries.forEach { list.add(it.name) }
            args.size == 5 && isElevated &&
                args[1].equals("owned-by-player", ignoreCase = true) && args[3].equals("materials", ignoreCase = true) ->
                Material.entries.forEach { list.add(it.name) }
        }
        return list
    }

    override fun displayHelp(sender: CommandSender, args: Array<String>) {
        sender.sendColored("/despi purge owned-by-me materials <name> (Purge materials owned by you)", NamedTextColor.GRAY)
        sender.sendColored("/despi purge owned-by-me in-hand (Purge items like in-hand owned by you)", NamedTextColor.GRAY)
        if (canBeElevated(sender)) {
            sender.sendColored("/despi purge owned-by-player <player> materials <name> (Purge a player's materials)", NamedTextColor.GRAY)
            sender.sendColored("/despi purge owned-by-player <player> in-hand (Purge a player's in-hand item)", NamedTextColor.GRAY)
            sender.sendColored("/despi purge owned-by-everyone materials <name> (Purge everyone's materials)", NamedTextColor.GRAY)
            sender.sendColored("/despi purge owned-by-everyone in-hand (Purge everyone's in-hand item)", NamedTextColor.GRAY)
        }
    }

    override fun showDescription(sender: CommandSender, args: Array<String>): Boolean = true

    private fun purgeOtherPlayerStuff(sender: CommandSender, playerName: String, materialOrHand: String, materials: String?): Boolean {
        @Suppress("DEPRECATION")
        val targetPlayer = Bukkit.getOfflinePlayer(playerName)
        val senderPlayer = isPlayer(sender)

        return when {
            materialOrHand.equals("materials", ignoreCase = true) && materials != null ->
                purgeMaterials(materials, sender, targetPlayer.uniqueId, senderPlayer?.uniqueId)
            materialOrHand.equals("in-hand", ignoreCase = true) -> {
                if (senderPlayer == null) {
                    error("You must be a player to purge an item in your hand", sender)
                    false
                } else {
                    purgeItems(senderPlayer.inventory.itemInMainHand, sender, targetPlayer.uniqueId, senderPlayer.uniqueId)
                }
            }
            else -> false
        }
    }

    private fun purgeEveryonesStuff(sender: CommandSender, materialOrHand: String, materials: String?): Boolean {
        val senderPlayer = isPlayer(sender)
        return when {
            materialOrHand.equals("materials", ignoreCase = true) && materials != null ->
                purgeMaterials(materials, sender, null, senderPlayer?.uniqueId)
            materialOrHand.equals("in-hand", ignoreCase = true) -> {
                if (senderPlayer == null) {
                    error("You must be a player to purge an item in your hand", sender)
                    false
                } else {
                    purgeItems(senderPlayer.inventory.itemInMainHand, sender, null, senderPlayer.uniqueId)
                }
            }
            else -> false
        }
    }

    private fun purgeOwnStuff(sender: CommandSender, materialOrHand: String, materials: String?): Boolean {
        val senderPlayer = isPlayer(sender, "You must be a player to purge your own stuff") ?: return false
        return when {
            materialOrHand.equals("materials", ignoreCase = true) && materials != null ->
                purgeMaterials(materials, sender, senderPlayer.uniqueId, senderPlayer.uniqueId)
            materialOrHand.equals("in-hand", ignoreCase = true) ->
                purgeItems(senderPlayer.inventory.itemInMainHand, sender, senderPlayer.uniqueId, senderPlayer.uniqueId)
            else -> false
        }
    }

    private fun purgeMaterials(materialsStr: String, sender: CommandSender, owner: UUID?, senderID: UUID?): Boolean {
        val materials = mutableListOf<Material>()
        for (name in materialsStr.split(",")) {
            runCatching { Material.valueOf(name.uppercase()) }
                .onSuccess { materials.add(it) }
                .onFailure { warning("Invalid material name $name skipping...", sender) }
        }

        if (materials.isEmpty()) {
            error("No material names usable.", sender)
            return false
        }

        if (senderID != null && plugin.removeMaterialsInst.containsKey(senderID)) {
            error("Removal already in-progress for you", sender)
            return false
        }

        RemoveMaterials(sender, materials, null, plugin, owner, senderID)
        success("Begun removal of materials... This may take a while...", sender)
        return true
    }

    private fun purgeItems(item: ItemStack, sender: CommandSender, owner: UUID?, senderID: UUID?): Boolean {
        if (item.type.isAir || item.amount <= 0) {
            error("Invalid Item", sender)
            return false
        }

        if (senderID != null && plugin.removeMaterialsInst.containsKey(senderID)) {
            error("Removal already in-progress for you", sender)
            return false
        }

        RemoveMaterials(sender, null, item, plugin, owner, senderID)
        success("Begun removal of items... This may take a while...", sender)
        return true
    }
}
