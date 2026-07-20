package com.popupmc.despawneditems

import org.bukkit.Material

/**
 * The pool of "safe" materials handed out as recycle rewards. Built once at
 * enable time by scanning every [Material] and excluding technical, valuable, or
 * otherwise inappropriate blocks.
 */
object BlacklistedItems {

    /** Materials eligible to be given out as a recycle reward. */
    val itemList: MutableList<Material> = mutableListOf()

    fun setup() {
        itemList.clear()
        for (type in Material.entries) {
            if (type.isAir) continue

            if (type == Material.JIGSAW ||
                type == Material.SPAWNER ||
                type == Material.DEBUG_STICK
            ) continue

            val name = type.name.lowercase()

            // Skip technical or high-value materials.
            if (name.contains("command") ||
                name.contains("spawn") ||
                name.contains("legacy") ||
                name.contains("structure") ||
                name.contains("gold") ||
                name.contains("golden") ||
                name.contains("iron") ||
                name.contains("diamond") ||
                name.contains("emerald") ||
                name.contains("netherite")
            ) continue

            itemList.add(type)
        }
    }
}
