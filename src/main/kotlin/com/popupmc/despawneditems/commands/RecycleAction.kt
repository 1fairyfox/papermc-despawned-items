package com.popupmc.despawneditems.commands

import com.popupmc.despawneditems.DespawnedItems
import com.popupmc.despawneditems.RecycleProgress
import com.popupmc.despawneditems.RewardPool
import com.popupmc.despawneditems.despawn.DespawnProcess
import com.popupmc.despawneditems.sendColored
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * The `/recycle` behaviour: despawn the held item and track reward progress in the
 * player's [org.bukkit.persistence.PersistentDataContainer]. Permission and player-sender
 * checks are handled by the Brigadier registration in [DespiCommand].
 */
object RecycleAction {

    private const val PROGRESS_KEY = "recycle_progress"

    fun recycle(plugin: DespawnedItems, player: Player) {
        val item = player.inventory.itemInMainHand
        if (item.type.isAir || item.amount == 0) {
            player.sendColored("There's nothing in your hand to recycle.", NamedTextColor.GOLD)
            return
        }

        DespawnProcess(item.clone(), plugin)
        player.inventory.setItemInMainHand(null)
        player.sendColored("Done!", NamedTextColor.GREEN)

        awardProgress(plugin, player)
    }

    private fun awardProgress(plugin: DespawnedItems, player: Player) {
        val key = NamespacedKey(plugin, PROGRESS_KEY)
        val pdc = player.persistentDataContainer
        val current = pdc.get(key, PersistentDataType.INTEGER) ?: 0
        val result = RecycleProgress.advance(current)
        pdc.set(key, PersistentDataType.INTEGER, result.stored)

        if (!result.rewarded) {
            player.sendColored("${result.remaining} left before a random item...", NamedTextColor.GRAY)
            return
        }

        val reward = RewardPool.random()
        if (reward == null) {
            plugin.logger.warning("Reward pool is empty — cannot grant a recycle reward")
            return
        }
        player.world.dropItem(player.location, ItemStack(reward))
        player.sendColored("You earned a random item for recycling!", NamedTextColor.GREEN)
    }
}
