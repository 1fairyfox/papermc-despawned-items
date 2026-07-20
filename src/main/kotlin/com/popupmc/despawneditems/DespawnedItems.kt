package com.popupmc.despawneditems

import com.popupmc.despawneditems.commands.OnDespiCommand
import com.popupmc.despawneditems.commands.OnRecycleCommand
import com.popupmc.despawneditems.config.Config
import com.popupmc.despawneditems.despawn.DespawnEffect
import com.popupmc.despawneditems.despawn.DespawnProcess
import com.popupmc.despawneditems.despawn.into.AbstractDespawnInto
import com.popupmc.despawneditems.despawn.into.DespawnBlockIntoAir
import com.popupmc.despawneditems.despawn.into.DespawnIntoCooker
import com.popupmc.despawneditems.despawn.into.DespawnIntoStorage
import com.popupmc.despawneditems.despawn.into.DespawnIntoVoid
import com.popupmc.despawneditems.despawn.into.DespawnItemIntoEntity
import com.popupmc.despawneditems.events.OnItemDespawnEvent
import com.popupmc.despawneditems.location.LocationManager
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
 * `open` so MockBukkit (ByteBuddy) can subclass it in tests — Kotlin classes are
 * final by default, which MockBukkit's plugin loader cannot proxy.
 */
open class DespawnedItems : JavaPlugin() {

    /**
     * Loaded configuration (effect + storage settings).
     *
     * Named `settings` rather than `config` so it does not collide with
     * [JavaPlugin.getConfig], which returns Bukkit's own [org.bukkit.configuration.file.FileConfiguration].
     */
    lateinit var settings: Config
        private set

    /** Owns the despawn-location store and its persistent backend. */
    lateinit var locations: LocationManager
        private set

    /**
     * The ordered despawn strategies, rebuilt each enable so they never capture a stale
     * plugin instance across a reload (the previous static list did).
     */
    lateinit var strategies: List<AbstractDespawnInto>
        private set

    /** Effects currently playing — held so they are not garbage collected. */
    val effectsPlaying: MutableList<DespawnEffect> = mutableListOf()

    /** In-flight bulk material removals, keyed by the sender that started them. */
    val removeMaterialsInst: MutableMap<UUID, RemoveMaterials> = HashMap()

    /** In-flight despawn processes — held so they are not garbage collected. */
    val despawnProcesses: MutableList<DespawnProcess> = mutableListOf()

    override fun onEnable() {
        RewardPool.setup()

        settings = Config(this)

        locations = LocationManager(this)
        locations.load()

        strategies = listOf(
            DespawnIntoVoid(this), // delete contraband first
            DespawnIntoCooker(this), // then furnaces/smokers
            DespawnBlockIntoAir(this), // then place as a block
            DespawnItemIntoEntity(this), // then onto entities
            DespawnIntoStorage(this), // finally into containers
        )

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
        if (this::locations.isInitialized) locations.shutdown()
        logger.info("DespawnedItems is disabled")
    }
}

/** Sends a single-colour message using the Adventure component API. */
fun CommandSender.sendColored(text: String, color: NamedTextColor) {
    sendMessage(Component.text(text, color))
}
