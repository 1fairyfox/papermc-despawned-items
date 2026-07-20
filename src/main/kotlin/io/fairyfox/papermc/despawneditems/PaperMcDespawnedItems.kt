package io.fairyfox.papermc.despawneditems

import io.fairyfox.papermc.despawneditems.commands.DespiCommand
import io.fairyfox.papermc.despawneditems.config.Config
import io.fairyfox.papermc.despawneditems.despawn.DespawnEffect
import io.fairyfox.papermc.despawneditems.despawn.DespawnProcess
import io.fairyfox.papermc.despawneditems.despawn.DespawnScheduler
import io.fairyfox.papermc.despawneditems.despawn.into.AbstractDespawnInto
import io.fairyfox.papermc.despawneditems.despawn.into.DespawnBlockIntoAir
import io.fairyfox.papermc.despawneditems.despawn.into.DespawnIntoCooker
import io.fairyfox.papermc.despawneditems.despawn.into.DespawnIntoStorage
import io.fairyfox.papermc.despawneditems.despawn.into.DespawnIntoVoid
import io.fairyfox.papermc.despawneditems.despawn.into.DespawnItemIntoEntity
import io.fairyfox.papermc.despawneditems.events.OnItemDespawnEvent
import io.fairyfox.papermc.despawneditems.location.LocationManager
import io.fairyfox.papermc.despawneditems.manage.RemoveMaterials
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

/**
 * PaperMC Despawned Items — a Paper plugin that intercepts items which would normally
 * despawn on the ground and instead relocates them into a registered network of
 * nearby containers, cookers, entities, or empty space.
 *
 * `open` so MockBukkit (ByteBuddy) can subclass it in tests — Kotlin classes are
 * final by default, which MockBukkit's plugin loader cannot proxy.
 */
open class PaperMcDespawnedItems : JavaPlugin() {
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

    /** Bounds the automatic despawn pipeline so large servers stay smooth under load. */
    lateinit var despawnScheduler: DespawnScheduler
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

        // Ordered relocation strategies: contraband → cookers → air → entities → containers.
        strategies =
            listOf(
                DespawnIntoVoid(this),
                DespawnIntoCooker(this),
                DespawnBlockIntoAir(this),
                DespawnItemIntoEntity(this),
                DespawnIntoStorage(this),
            )

        despawnScheduler = DespawnScheduler(this)
        despawnScheduler.start()

        Bukkit.getPluginManager().registerEvents(OnItemDespawnEvent(this), this)

        // Register /despi and /recycle via Paper's Brigadier command API.
        DespiCommand.register(this)

        logger.info("Enabled")
    }

    override fun onDisable() {
        if (this::despawnScheduler.isInitialized) despawnScheduler.stop()
        if (this::locations.isInitialized) locations.shutdown()
        logger.info("Disabled")
    }
}

/** Sends a single-colour message using the Adventure component API. */
fun CommandSender.sendColored(
    text: String,
    color: NamedTextColor,
) {
    sendMessage(Component.text(text, color))
}
