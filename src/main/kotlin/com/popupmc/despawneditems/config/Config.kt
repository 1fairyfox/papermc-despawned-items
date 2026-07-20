package com.popupmc.despawneditems.config

import com.popupmc.despawneditems.DespawnedItems
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.configuration.file.FileConfiguration

/**
 * Top-level configuration holder. Owns the effect settings ([fileConfig]) and the
 * storage settings ([storage]). Despawn locations themselves live in the plugin's
 * [com.popupmc.despawneditems.location.LocationManager], not here. Call [load] to
 * re-read `config.yml` from disk and rebuild both.
 */
class Config(val plugin: DespawnedItems) {

    lateinit var fileConfig: FileConfig
        private set

    lateinit var storage: StorageSettings
        private set

    lateinit var performance: PerformanceSettings
        private set

    init {
        load()
    }

    fun load() {
        plugin.saveDefaultConfig()
        // Re-read the file from disk so `/despi reload` actually picks up edits — the
        // old code reused Bukkit's cached FileConfiguration and silently ignored them.
        plugin.reloadConfig()
        fileConfig = FileConfig(plugin)
        storage = StorageSettings(plugin.config)
        performance = PerformanceSettings(plugin.config)
    }
}

/**
 * Large-server safety limits for the automatic despawn-relocation pipeline, read from
 * the `performance:` section. These bound how much relocation work happens per tick so
 * a flood of despawning items can never storm the server.
 */
class PerformanceSettings(c: FileConfiguration) {
    /** New relocations started per server tick. */
    val maxPerTick: Int = c.getInt("performance.max-per-tick", 20).coerceAtLeast(1)

    /** Maximum relocations processing simultaneously. */
    val maxConcurrent: Int = c.getInt("performance.max-concurrent", 200).coerceAtLeast(1)

    /** Maximum items waiting to be relocated. */
    val maxQueue: Int = c.getInt("performance.max-queue", 10_000).coerceAtLeast(1)

    /** When the queue is full: true = ignore new items, false = drop the oldest. */
    val dropWhenFull: Boolean = c.getBoolean("performance.drop-when-full", true)
}

/**
 * Storage backend selection and connection details, read from the `storage:` section
 * of `config.yml`. Consumed by
 * [com.popupmc.despawneditems.location.StorageFactory].
 */
class StorageSettings(c: FileConfiguration) {
    /** `yaml`, `sqlite`, or `mysql`. */
    val type: String = (c.getString("storage.type") ?: "yaml").lowercase().trim()

    val mysqlHost: String = c.getString("storage.mysql.host") ?: "localhost"
    val mysqlPort: Int = c.getInt("storage.mysql.port", 3306)
    val mysqlDatabase: String = c.getString("storage.mysql.database") ?: "despawneditems"
    val mysqlUsername: String = c.getString("storage.mysql.username") ?: "root"
    val mysqlPassword: String = c.getString("storage.mysql.password") ?: ""
    val mysqlProperties: String = c.getString("storage.mysql.properties") ?: ""

    val poolMaximumSize: Int = c.getInt("storage.pool.maximum-pool-size", 10).coerceIn(1, 100)
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

    /**
     * Extra data required by some particles (e.g. DUST needs a [Particle.DustOptions]).
     * Null when the configured particle needs no data. Resolved at load time so the
     * effect never calls `spawnParticle` without data a particle requires (which threw).
     */
    var particleData: Any? = null
        private set

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
        particleFX = resolveParticle(c.getString("particles.particle") ?: DEFAULT_PARTICLE_KEY)
        particleData = resolveParticleData(c)
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
        return runCatching { Particle.valueOf(key) }.getOrElse {
            plugin.logger.warning("Unknown particle '$name' in config; using $DEFAULT_PARTICLE")
            DEFAULT_PARTICLE
        }
    }

    /**
     * Resolves any extra data the configured [particleFX] requires. Particles needing
     * no data return null; DUST-style particles are built from an optional
     * `particles.color` / `particles.dust-size`. If the particle needs data this
     * plugin doesn't model, it is swapped for [DEFAULT_PARTICLE] with a warning rather
     * than crashing at spawn time — the previous code always passed no data and threw
     * for any data-bearing particle.
     */
    private fun resolveParticleData(c: FileConfiguration): Any? =
        when (particleFX.dataType) {
            Void::class.java -> null
            Particle.DustOptions::class.java -> {
                val color = parseColor(c.getString("particles.color"))
                val size = c.getDouble("particles.dust-size", 1.0).toFloat().coerceIn(0.01f, 4.0f)
                Particle.DustOptions(color, size)
            }
            else -> {
                plugin.logger.warning(
                    "Particle '$particleFX' requires data this plugin can't supply " +
                        "(${particleFX.dataType.simpleName}); using $DEFAULT_PARTICLE instead.",
                )
                particleFX = DEFAULT_PARTICLE
                null
            }
        }

    /** Parses a `#RRGGBB` (or `RRGGBB`) hex colour; falls back to white on any error. */
    private fun parseColor(hex: String?): Color {
        val cleaned = hex?.removePrefix("#")?.trim()
        if (cleaned == null || cleaned.length != 6) return Color.WHITE
        return runCatching { Color.fromRGB(cleaned.toInt(16)) }.getOrDefault(Color.WHITE)
    }

    companion object {
        private val DEFAULT_PARTICLE: Particle = Particle.HAPPY_VILLAGER
        private const val DEFAULT_PARTICLE_KEY = "happy_villager"
    }
}
