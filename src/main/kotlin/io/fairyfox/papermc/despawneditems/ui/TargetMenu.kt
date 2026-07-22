package io.fairyfox.papermc.despawneditems.ui

import io.fairyfox.papermc.despawneditems.location.DespawnLocation
import io.fairyfox.papermc.despawneditems.location.TargetOptions
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

/**
 * The per-target options screen — the "button" the owner asked for, plus the extra options
 * behind it.
 *
 * Rendered as an ordinary chest inventory, which means it works on **every** client: vanilla,
 * Fabric, NeoForge, Bedrock-via-Geyser, any modpack. No client installation, no protocol
 * version to keep in step with the server, nothing to conflict with.
 *
 * ### Conflict safety
 *
 * The inventory's holder is [TargetMenuHolder], a type private to this plugin. Every click
 * handler in the ecosystem — ours included — identifies its own menus by holder type, so
 * this screen can never be mistaken for another plugin's GUI and vice versa. The holder also
 * carries the exact target being edited, so the click handler never has to guess from titles
 * or slot contents.
 */
class TargetMenuHolder(
    val location: Location,
    val target: DespawnLocation,
) : InventoryHolder {
    private lateinit var backing: Inventory

    override fun getInventory(): Inventory = backing

    internal fun attach(inventory: Inventory) {
        backing = inventory
    }
}

/** Slot layout, kept in one place so the renderer and the click handler cannot drift apart. */
object TargetMenuSlots {
    const val TOGGLE = 11
    const val PRIORITY = 13
    const val CONTRABAND = 15
    const val SIZE = 27
}

/** Builds and opens the options screen for a despawn target. */
object TargetMenu {
    fun open(
        player: Player,
        location: Location,
        target: DespawnLocation,
    ) {
        val holder = TargetMenuHolder(location, target)
        val inventory =
            Bukkit.createInventory(
                holder,
                TargetMenuSlots.SIZE,
                Component.text("Despawn target", NamedTextColor.DARK_AQUA),
            )
        holder.attach(inventory)
        render(inventory, target.options)
        player.openInventory(inventory)
    }

    /** Repaints the buttons for [options] — used on open and after every click. */
    fun render(
        inventory: Inventory,
        options: TargetOptions,
    ) {
        inventory.setItem(TargetMenuSlots.TOGGLE, toggleButton(options.enabled))
        inventory.setItem(TargetMenuSlots.PRIORITY, priorityButton(options.priority))
        inventory.setItem(TargetMenuSlots.CONTRABAND, contrabandButton(options.acceptContraband))
    }

    private fun toggleButton(enabled: Boolean): ItemStack =
        button(
            if (enabled) Material.LIME_DYE else Material.GRAY_DYE,
            if (enabled) "Target is ON" else "Target is OFF",
            if (enabled) NamedTextColor.GREEN else NamedTextColor.RED,
            listOf(
                if (enabled) {
                    "Despawning items may be relocated here."
                } else {
                    "This target is skipped — its registration is kept."
                },
                "Click to turn it ${if (enabled) "off" else "on"}.",
            ),
        )

    private fun priorityButton(priority: Int): ItemStack =
        button(
            Material.COMPARATOR,
            "Priority: $priority",
            NamedTextColor.YELLOW,
            listOf(
                "How often this target is chosen, 1–${TargetOptions.MAX_PRIORITY}.",
                "A priority-3 target is picked three times as often as a priority-1 one.",
                "Click to raise, right-click to lower.",
            ),
        ).also { it.amount = priority.coerceIn(1, TargetOptions.MAX_PRIORITY) }

    private fun contrabandButton(accept: Boolean): ItemStack =
        button(
            if (accept) Material.HOPPER else Material.BARRIER,
            if (accept) "Accepts banned items" else "Refuses banned items",
            if (accept) NamedTextColor.GOLD else NamedTextColor.GRAY,
            listOf(
                if (accept) {
                    "Contraband routed here instead of being destroyed."
                } else {
                    "Banned items are never placed in this target."
                },
                "Click to change.",
            ),
        )

    private fun button(
        material: Material,
        title: String,
        colour: NamedTextColor,
        lore: List<String>,
    ): ItemStack =
        ItemStack(material).also { item ->
            item.editMeta { meta ->
                meta.displayName(Component.text(title, colour).decoration(TextDecoration.ITALIC, false))
                meta.lore(
                    lore.map {
                        Component.text(it, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    },
                )
            }
        }
}
