package io.fairyfox.papermc.despawneditems.ui

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * The **despawn wand** — the item a player holds to click despawn targets on and off.
 *
 * ### Why an item rather than a bare right-click
 *
 * Claiming plain right-click on containers would fight every other plugin and mod on the
 * server: shops, protection plugins, storage mods, backpacks, sorting systems. Requiring a
 * specific held item means the plugin only ever reacts to a gesture the player made
 * *deliberately*, and never intercepts ordinary chest use.
 *
 * ### Why a PDC tag rather than a material check
 *
 * The wand is identified by a **namespaced persistent-data key**, not by its material. A
 * plain blaze rod is still a plain blaze rod; only an item carrying
 * `papermc-despawned-items:wand` is a wand. That makes a false positive against another
 * mod's item essentially impossible — no other plugin writes into this plugin's namespace —
 * and it lets the admin change the wand's material freely without breaking existing wands.
 */
object TargetWand {
    /** PDC key marking an item as a despawn wand. Namespaced to this plugin. */
    fun key(plugin: PaperMcDespawnedItems): NamespacedKey = NamespacedKey(plugin, "wand")

    /** Builds a wand of the configured material, tagged and labelled. */
    fun create(plugin: PaperMcDespawnedItems): ItemStack {
        val settings = plugin.settings.targetUi
        val item = ItemStack(settings.wandMaterial)
        item.editMeta { meta ->
            meta.displayName(
                Component.text("Despawn Wand", NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false),
            )
            meta.lore(
                listOf(
                    Component.text("Right-click a despawn target to open its options.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                    Component.text("Sneak + right-click to toggle it on or off.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                ),
            )
            meta.persistentDataContainer.set(key(plugin), PersistentDataType.BYTE, 1)
        }
        return item
    }

    /** True when [item] carries this plugin's wand tag. */
    fun isWand(
        plugin: PaperMcDespawnedItems,
        item: ItemStack?,
    ): Boolean {
        if (item == null || item.type.isAir) return false
        val meta = item.itemMeta ?: return false
        return meta.persistentDataContainer.has(key(plugin), PersistentDataType.BYTE)
    }
}
