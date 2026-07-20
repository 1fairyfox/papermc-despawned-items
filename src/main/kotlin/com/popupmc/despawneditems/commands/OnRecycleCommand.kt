package com.popupmc.despawneditems.commands

import com.popupmc.despawneditems.DespawnedItems
import com.popupmc.despawneditems.RewardPool
import com.popupmc.despawneditems.despawn.DespawnProcess
import com.popupmc.despawneditems.sendColored
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.NamespacedKey
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * `/recycle` — despawns the item in the player's main hand and tracks progress
 * toward a reward: every [ITEMS_PER_REWARD] recycles grants one random "safe"
 * material back to the player.
 *
 * Progress is stored per player in the player's [org.bukkit.persistence.PersistentDataContainer],
 * which persists across sessions and needs no server-side scoreboard setup (the
 * previous scoreboard-objective approach silently did nothing when those objectives
 * were absent).
 */
class OnRecycleCommand(private val plugin: DespawnedItems) : CommandExecutor {

    private val progressKey = NamespacedKey(plugin, "recycle_progress")

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>,
    ): Boolean {
        if (sender !is Player) {
            sender.sendColored("You must be a player to use this command", NamedTextColor.RED)
            return false
        }

        if (!sender.hasPermission("recycle.use")) {
            sender.sendColored("You don't have permission to use that command", NamedTextColor.RED)
            return false
        }

        val item = sender.inventory.itemInMainHand
        if (item.type.isAir || item.amount == 0) {
            sender.sendColored("There's nothing in your hand to recycle.", NamedTextColor.GOLD)
            return false
        }

        DespawnProcess(item.clone(), plugin)
        sender.inventory.setItemInMainHand(null)
        sender.sendColored("Done!", NamedTextColor.GREEN)

        awardProgress(sender)
        return true
    }

    /** Advances the player's recycle progress and pays out a reward at the threshold. */
    private fun awardProgress(player: Player) {
        val pdc = player.persistentDataContainer
        val progress = (pdc.get(progressKey, PersistentDataType.INTEGER) ?: 0) + 1

        if (progress < ITEMS_PER_REWARD) {
            pdc.set(progressKey, PersistentDataType.INTEGER, progress)
            player.sendColored(
                "${ITEMS_PER_REWARD - progress} left before a random item...",
                NamedTextColor.GRAY,
            )
            return
        }

        // Threshold reached: reset progress and drop one random reward.
        pdc.set(progressKey, PersistentDataType.INTEGER, 0)

        val reward = RewardPool.random()
        if (reward == null) {
            plugin.logger.warning("Reward pool is empty — cannot grant a recycle reward")
            return
        }
        player.world.dropItem(player.location, ItemStack(reward))
        player.sendColored("You earned a random item for recycling!", NamedTextColor.GREEN)
    }

    companion object {
        /** How many recycles earn one reward. */
        const val ITEMS_PER_REWARD = 64
    }
}
