package com.popupmc.despawneditems.commands

import com.popupmc.despawneditems.sendColored
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Small shared helpers for the Brigadier command executors: coloured feedback and the
 * "block you're looking at" lookup. Kept separate so both the command tree and the
 * testable [DespiActions] can use them.
 */
object CommandFeedback {

    fun error(sender: CommandSender, msg: String) = sender.sendColored("ERROR: $msg", NamedTextColor.RED)
    fun success(sender: CommandSender, msg: String) = sender.sendColored(msg, NamedTextColor.GREEN)
    fun warning(sender: CommandSender, msg: String) = sender.sendColored("WARNING: $msg", NamedTextColor.GOLD)
    fun info(sender: CommandSender, msg: String) = sender.sendColored(msg, NamedTextColor.GOLD)

    /** The block [player] is looking at within 5 blocks, or null (with an error) if none. */
    fun targetBlock(player: Player): Location? {
        val block = player.getTargetBlockExact(TARGET_RANGE)
        if (block == null || block.type.isAir) {
            error(player, "Unable to find block, are you within $TARGET_RANGE blocks of something?")
            return null
        }
        return block.location
    }

    private const val TARGET_RANGE = 5
}
