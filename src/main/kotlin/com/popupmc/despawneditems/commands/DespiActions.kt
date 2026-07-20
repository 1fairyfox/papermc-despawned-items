package com.popupmc.despawneditems.commands

import com.popupmc.despawneditems.DespawnedItems
import com.popupmc.despawneditems.despawn.DespawnEffect
import com.popupmc.despawneditems.despawn.DespawnProcess
import com.popupmc.despawneditems.limit.DespawnLimits
import com.popupmc.despawneditems.location.DespawnLocation
import com.popupmc.despawneditems.manage.RemoveMaterials
import com.popupmc.despawneditems.sendColored
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID

/**
 * The behaviour behind every `/despi` subcommand, separated from Brigadier registration
 * ([DespiCommand]) so the logic is straightforward to follow and to exercise. Each method
 * takes an already-resolved sender and arguments and reports its own feedback.
 */
class DespiActions(private val plugin: DespawnedItems) {
    private val fb = CommandFeedback
    private val soloBackup: MutableList<DespawnLocation> = mutableListOf()

    @Suppress("DEPRECATION")
    private fun offline(name: String) = Bukkit.getOfflinePlayer(name)

    // ─── add ────────────────────────────────────────────────────────────────────

    /** `/despi add this [player]`. [ownerName] null = self (limit-checked). */
    fun add(
        player: Player,
        ownerName: String?,
    ) {
        val location = fb.targetBlock(player) ?: return
        val ownerId = if (ownerName == null) player.uniqueId else offline(ownerName).uniqueId

        if (ownerName == null) {
            val limits = plugin.settings.limits
            if (!DespawnLimits.canAddAnother(player, plugin.locations.countOfOwner(player.uniqueId), limits)) {
                fb.error(player, "You've reached your limit of ${DespawnLimits.resolve(player, limits)} despawn location(s).")
                return
            }
        }

        if (plugin.locations.add(location, ownerId)) {
            fb.success(player, "Successfully added location!")
        } else {
            fb.warning(player, "Location already exists!")
        }
    }

    // ─── remove ─────────────────────────────────────────────────────────────────

    fun removeHereOwnedByMe(player: Player) = removeHere(player, player.uniqueId)

    fun removeHereOwnedByPlayer(
        player: Player,
        name: String,
    ) = removeHere(player, offline(name).uniqueId)

    private fun removeHere(
        player: Player,
        owner: UUID,
    ) {
        val location = fb.targetBlock(player) ?: return
        if (plugin.locations.remove(location, owner)) {
            fb.success(player, "Location removed")
        } else {
            fb.warning(player, "Location wasn't removed (Did it ever exist?)")
        }
    }

    fun removeHereOwnedByAnyone(player: Player) {
        val location = fb.targetBlock(player) ?: return
        if (plugin.locations.removeOneAt(location)) {
            fb.success(player, "Location removed")
        } else {
            fb.warning(player, "Location wasn't removed (Did it ever exist?)")
        }
    }

    fun removeAnywhereOwnedByMe(player: Player) = removeAnyOf(player, player.uniqueId)

    fun removeAnywhereOwnedByPlayer(
        sender: CommandSender,
        name: String,
    ) = removeAnyOf(sender, offline(name).uniqueId)

    private fun removeAnyOf(
        sender: CommandSender,
        owner: UUID,
    ) {
        if (plugin.locations.removeOneOfOwner(owner)) {
            fb.success(sender, "A location was removed!")
        } else {
            fb.warning(sender, "No location was removed (Did the player have locations?)")
        }
    }

    // ─── clear ──────────────────────────────────────────────────────────────────

    fun clearMine(player: Player) = clearOwner(player, player.uniqueId, "you")

    fun clearPlayer(
        sender: CommandSender,
        name: String,
    ) = clearOwner(sender, offline(name).uniqueId, name)

    private fun clearOwner(
        sender: CommandSender,
        owner: UUID,
        label: String,
    ) {
        val removed = plugin.locations.removeAllOfOwner(owner)
        if (removed > 0) {
            fb.success(sender, "$removed location(s) were removed for $label")
        } else {
            fb.warning(sender, "No locations found to remove for $label")
        }
    }

    fun clearHere(player: Player) {
        val location = fb.targetBlock(player) ?: return
        val removed = plugin.locations.removeAllAt(location)
        if (removed > 0) {
            fb.success(player, "$removed owner(s) were removed for this location")
        } else {
            fb.warning(player, "No owners found to remove for this location.")
        }
    }

    // ─── exists ─────────────────────────────────────────────────────────────────

    fun existsHereOwnedByMe(player: Player) = existsHere(player, player.uniqueId)

    fun existsHereOwnedByPlayer(
        player: Player,
        name: String,
    ) = existsHere(player, offline(name).uniqueId)

    private fun existsHere(
        player: Player,
        owner: UUID,
    ) {
        val location = fb.targetBlock(player) ?: return
        if (plugin.locations.has(location, owner)) {
            fb.success(player, "Location does exist")
        } else {
            fb.warning(player, "Location does not exist")
        }
    }

    fun existsHereOwnedByAnyone(player: Player) {
        val location = fb.targetBlock(player) ?: return
        if (plugin.locations.anyAt(location)) fb.success(player, "Location does exist") else fb.warning(player, "Location does not exist")
    }

    fun existsAnywhereOwnedByMe(player: Player) = existsAnyOf(player, player.uniqueId)

    fun existsAnywhereOwnedByPlayer(
        sender: CommandSender,
        name: String,
    ) = existsAnyOf(sender, offline(name).uniqueId)

    private fun existsAnyOf(
        sender: CommandSender,
        owner: UUID,
    ) {
        val found = plugin.locations.firstOfOwner(owner)
        if (found != null) {
            fb.success(sender, "A location was found!")
            sender.sendColored(found.serialize(), NamedTextColor.GOLD)
        } else {
            fb.warning(sender, "No location was found")
        }
    }

    // ─── locations ──────────────────────────────────────────────────────────────

    fun locationsMine(player: Player) = listOwner(player, player.uniqueId, "you")

    fun locationsPlayer(
        sender: CommandSender,
        name: String,
    ) = listOwner(sender, offline(name).uniqueId, name)

    private fun listOwner(
        sender: CommandSender,
        owner: UUID,
        label: String,
    ) {
        val found = plugin.locations.ofOwner(owner).toList()
        if (found.isNotEmpty()) {
            fb.success(sender, "${found.size} location(s) were found for $label")
            found.forEach { sender.sendColored(it.serialize(), NamedTextColor.GOLD) }
        } else {
            fb.warning(sender, "No locations found for $label")
        }
    }

    fun locationsCount(sender: CommandSender) = fb.success(sender, "Locations: ${plugin.locations.count}")

    @Suppress("DEPRECATION")
    fun locationsHere(player: Player) {
        val location = fb.targetBlock(player) ?: return
        val found = plugin.locations.atLocation(location)
        if (found.isNotEmpty()) {
            fb.success(player, "${found.size} owner(s) were found for this location")
            found.forEach { player.sendColored(Bukkit.getOfflinePlayer(it.owner).name ?: it.owner.toString(), NamedTextColor.GOLD) }
        } else {
            fb.warning(player, "No owners found for this location.")
        }
    }

    fun soloMode(player: Player) {
        val location = fb.targetBlock(player) ?: return
        soloBackup.clear()
        soloBackup.addAll(plugin.locations.all())
        plugin.locations.replaceWith(listOf(DespawnLocation.of(location, player.uniqueId)))
        fb.success(player, "Solo'd this location, /despi locations normal-mode to restore")
    }

    fun normalMode(sender: CommandSender) {
        plugin.locations.replaceWith(soloBackup)
        soloBackup.clear()
        fb.success(sender, "Undid solo")
    }

    // ─── despawn (admin/testing) ────────────────────────────────────────────────

    fun despawnCount(sender: CommandSender) =
        fb.success(sender, "Despawns: ${plugin.despawnProcesses.size} active, ${plugin.despawnScheduler.queued} queued")

    fun despawnFromHand(player: Player) {
        val item = player.inventory.itemInMainHand
        if (item.amount == 0 || item.type.isAir) {
            fb.error(player, "You have no items in your hand")
            return
        }
        DespawnProcess(item.clone(), plugin)
        fb.success(player, "Created forced despawn")
    }

    fun despawnMaterial(
        sender: CommandSender,
        materialNames: String,
        amount: Int,
    ) {
        val materials = parseMaterials(sender, materialNames)
        if (materials.isEmpty()) {
            fb.error(sender, "No material names usable.")
            return
        }
        val count = amount.coerceAtLeast(1)
        materials.forEach { DespawnProcess(ItemStack(it, count), plugin) }
        fb.success(sender, "Created forced despawn")
    }

    fun despawnClearOngoing(sender: CommandSender) {
        val count = plugin.despawnProcesses.size
        ArrayList(plugin.despawnProcesses).forEach { it.selfDestroy() }
        fb.success(sender, "Cleared $count on-going despawns")
    }

    // ─── effects (admin/testing) ────────────────────────────────────────────────

    fun effectsCount(sender: CommandSender) = fb.success(sender, "Effects Count: ${plugin.effectsPlaying.size}")

    fun effectsCreateHere(player: Player) {
        val location = fb.targetBlock(player) ?: return
        DespawnEffect(location, plugin)
        fb.success(player, "Created fake effect at location")
    }

    fun effectsClearOngoing(sender: CommandSender) {
        val count = plugin.effectsPlaying.size
        ArrayList(plugin.effectsPlaying).forEach { it.forceSelfDestroy() }
        fb.success(sender, "Cleared $count on-going effects")
    }

    // ─── reload / save ──────────────────────────────────────────────────────────

    fun reload(sender: CommandSender) {
        plugin.settings.load()
        plugin.locations.reload()
        fb.success(sender, "Config and storage have been reloaded")
    }

    fun save(sender: CommandSender) {
        plugin.locations.saveNow()
        fb.success(sender, "Locations have been saved")
    }

    // ─── purge ──────────────────────────────────────────────────────────────────

    fun purgeMaterials(
        sender: CommandSender,
        owner: UUID?,
        materialNames: String,
    ) {
        val senderId = (sender as? Player)?.uniqueId
        val materials = parseMaterials(sender, materialNames)
        if (materials.isEmpty()) {
            fb.error(sender, "No material names usable.")
            return
        }
        if (senderId != null && plugin.removeMaterialsInst.containsKey(senderId)) {
            fb.error(sender, "Removal already in-progress for you")
            return
        }
        RemoveMaterials(sender, materials, null, plugin, owner, senderId)
        fb.success(sender, "Begun removal of materials... This may take a while...")
    }

    fun purgeInHand(
        sender: CommandSender,
        owner: UUID?,
    ) {
        val player =
            sender as? Player ?: run {
                fb.error(sender, "You must be a player to purge an item in your hand")
                return
            }
        val item = player.inventory.itemInMainHand
        if (item.type.isAir || item.amount <= 0) {
            fb.error(sender, "Invalid Item")
            return
        }
        if (plugin.removeMaterialsInst.containsKey(player.uniqueId)) {
            fb.error(sender, "Removal already in-progress for you")
            return
        }
        RemoveMaterials(sender, null, item, plugin, owner, player.uniqueId)
        fb.success(sender, "Begun removal of items... This may take a while...")
    }

    fun playerId(name: String): UUID = offline(name).uniqueId

    private fun parseMaterials(
        sender: CommandSender,
        csv: String,
    ): List<Material> =
        csv.split(",").mapNotNull { name ->
            runCatching { Material.valueOf(name.trim().uppercase()) }.getOrElse {
                fb.warning(sender, "Invalid material name $name skipping...")
                null
            }
        }
}
