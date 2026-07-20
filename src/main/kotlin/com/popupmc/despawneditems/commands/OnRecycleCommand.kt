package com.popupmc.despawneditems.commands

import com.popupmc.despawneditems.BlacklistedItems
import com.popupmc.despawneditems.DespawnedItems
import com.popupmc.despawneditems.despawn.DespawnProcess
import com.popupmc.despawneditems.sendColored
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scoreboard.Objective
import java.util.Random

/**
 * `/recycle` — despawns the item in the player's main hand and tracks a
 * scoreboard-based reward: every 64 recycled items grants a random "safe"
 * material back to the player.
 */
class OnRecycleCommand(private val plugin: DespawnedItems) : CommandExecutor {

    private val random = Random()

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

        val item = sender.inventory.itemInMainHand

        if (!sender.hasPermission("recycle.use")) {
            sender.sendColored("You don't have permission to use that command", NamedTextColor.RED)
            return false
        }

        if (item.type.isAir || item.amount == 0) {
            sender.sendColored("There's nothing in your hand to recycle.", NamedTextColor.GOLD)
            return false
        }

        DespawnProcess(item.clone(), plugin)
        sender.inventory.setItemInMainHand(null)
        sender.sendColored("Done!", NamedTextColor.GREEN)

        increaseScore(sender)
        return true
    }

    @Suppress("DEPRECATION")
    private fun increaseScore(player: Player) {
        val scoreboard = player.scoreboard

        val partObjective: Objective = scoreboard.getObjective("recycleCountPart") ?: run {
            plugin.logger.warning("Objective recycleCountPart is null")
            return
        }

        var recycleCountPart = partObjective.getScore(player.name).score + 1

        // Below a full stack: just record progress and stop.
        if (recycleCountPart < 64) {
            player.sendColored("${64 - recycleCountPart} left before given random item...", NamedTextColor.GRAY)
            partObjective.getScore(player.name).score = recycleCountPart
            return
        }

        // Reached a full stack: reset the partial counter and continue.
        recycleCountPart = 0
        partObjective.getScore(player.name).score = recycleCountPart

        val countObjective: Objective = scoreboard.getObjective("recycleCount") ?: run {
            plugin.logger.warning("Objective recycleCount is null")
            return
        }

        val recycleCount = countObjective.getScore(player.name).score + 1
        countObjective.getScore(player.name).score = recycleCount

        val paidObjective: Objective = scoreboard.getObjective("recycleCountPaid") ?: run {
            plugin.logger.warning("Objective recycleCountPaid is null")
            return
        }

        val recycleCountPaid = paidObjective.getScore(player.name).score

        // Paid should never exceed unpaid; nothing owed if it does.
        if (recycleCountPaid > recycleCount) return

        val difference = recycleCount - recycleCountPaid
        if (difference <= 0) return

        val reward = ItemStack(BlacklistedItems.itemList[random.nextInt(BlacklistedItems.itemList.size)])
        reward.amount = difference
        player.world.dropItem(player.location, reward)

        paidObjective.getScore(player.name).score = recycleCount
    }
}
