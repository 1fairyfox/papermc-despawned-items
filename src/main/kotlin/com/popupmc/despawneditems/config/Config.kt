package com.popupmc.despawneditems.config

import com.popupmc.despawneditems.DespawnedItems
import org.bukkit.Particle

/**
 * Top-level configuration holder. Owns both the main settings ([fileConfig]) and
 * the per-player despawn locations ([fileLocations]). Call [load] to reload both.
 */
class Config(val plugin: DespawnedItems) {

    lateinit var fileConfig: FileConfig
        private set

    lateinit var fileLocations: FileLocations
        private set

    init {
        load()
    }

    fun load() {
        fileConfig = FileConfig(plugin)
        fileLocations = FileLocations(plugin)
    }
}

/**
 * The main `config.yml` — particle and sound settings for the effect played when
 * an item lands in a despawn container.
 */
class FileConfig(private val plugin: DespawnedItems) {

    var particlesEnabled: Boolean = true
    var particleFX: Particle = Particle.HAPPY_VILLAGER
    var particleLengthSeconds: Int = 3
    var newParticlesEveryNthTick: Int = 2
    var particleCountEveryNthTick: Int = 15
    var particleRandomRadius: Float = 0.5f

    var soundEnabled: Boolean = true

    /** A Minecraft sound key, e.g. `block.fire.extinguish`. */
    var soundKey: String = "block.fire.extinguish"
    var soundVolume: Float = 1.0f
    var soundPitch: Float = 1.0f

    init {
        load()
    }

    fun load() {
        plugin.saveDefaultConfig()
        val c = plugin.config

        particlesEnabled = c.getBoolean("particles.enabled", true)
        particleFX = resolveParticle(c.getString("particles.particle") ?: "happy_villager")
        particleLengthSeconds = c.getInt("particles.length-seconds", 3)
        newParticlesEveryNthTick = c.getInt("particles.new-every-nth-tick", 2)
        particleCountEveryNthTick = c.getInt("particles.count-every-nth-tick", 15)
        particleRandomRadius = c.getDouble("particles.radius", 0.5).toFloat()

        soundEnabled = c.getBoolean("sound.enabled", true)
        soundKey = c.getString("sound.sound") ?: "block.fire.extinguish"
        soundVolume = c.getDouble("sound.volume", 1.0).toFloat()
        soundPitch = c.getDouble("sound.pitch", 1.0).toFloat()
    }

    /** Resolves a particle from a config key, tolerating namespaces and case. */
    private fun resolveParticle(name: String): Particle {
        val key = name.substringAfter(':').uppercase()
        return runCatching { Particle.valueOf(key) }.getOrDefault(Particle.HAPPY_VILLAGER)
    }
}
