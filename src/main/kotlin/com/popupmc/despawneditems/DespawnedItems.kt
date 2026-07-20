package com.popupmc.despawneditems

import com.popupmc.despawneditems.commands.OnDespiCommand
import com.popupmc.despawneditems.commands.OnRecycleCommand
import com.popupmc.despawneditems.config.Config
import com.popupmc.despawneditems.despawn.DespawnEffect
import com.popupmc.despawneditems.despawn.DespawnIndexes
import com.popupmc.despawneditems.despawn.DespawnProcess
import com.popupmc.despawneditems.events.OnItemDespawnEvent
import com.popupmc.despawneditems.manage.RemoveMaterials
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

/**
 * DespawnedItems — a Paper plugin that intercepts items which would normally
 * despawn on the ground and instead relocates them into a registered network of
 * nearby containers, cookers, entities, or empty space.
 *
 * This is the modern Kotlin rewrite targeting Paper 26.1.
 */
class DespawnedItems : JavaPlugin() {

    /**
     * Loaded configuration (main config + per-player despawn locations).
     *
     * Named `settings` rather than `config` so it does not collide with
     * [JavaPlugin.getConfig], which returns Bukkit's own [org.bukkit.configuration.file.FileConfiguration].
     */
    lateinit var settings: Config
        private set

    /** Shuffled index used to pick despawn locations without repeats. */
    lateinit var despawnIndexes: DespawnIndexes
        private set

    /** True once [despawnIndexes] exists (it is created after the first config load). */
    val isDespawnIndexesReady: Boolean
        get() = this::despawnIndexes.isInitialized

    /** Effects currently playing — held so they are not garbage collected. */
    val effectsPlaying: MutableList<DespawnEffect> = mutableListOf()

    /** In-flight bulk material removals, keyed by the sender that started them. */
    val removeMaterialsInst: MutableMap<UUID, RemoveMaterials> = HashMap()

    /** In-flight despawn processes — held so they are not garbage collected. */
    val despawnProcesses: MutableList<DespawnProcess> = mutableListOf()

    override fun onEnable() {
        RewardPool.setup()

        settings = Config(this)
        despawnIndexes = DespawnIndexes(this)

        Bukkit.getPluginManager().registerEvents(OnItemDespawnEvent(this), this)

        val despi = getCommand("despi")
        if (despi == null) {
            logger.warning("Command /despi is null — disabling plugin")
            isEnabled = false
            return
        }
        val despiExecutor = OnDespiCommand(this)
        despi.setExecutor(despiExecutor)
        despi.tabCompleter = despiExecutor

        val recycle = getCommand("recycle")
        if (recycle == null) {
            logger.warning("Command /recycle is null — disabling plugin")
            isEnabled = false
            return
        }
        recycle.setExecutor(OnRecycleCommand(this))

        logger.info("DespawnedItems is enabled")
    }

    override fun onDisable() {
        logger.info("DespawnedItems is disabled")
    }
}

/** Sends a single-colour message using the Adventure component API. */
fun CommandSender.sendColored(text: String, color: NamedTextColor) {
    sendMessage(Component.text(text, color))
}
