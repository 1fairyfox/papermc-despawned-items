package io.fairyfox.papermc.despawneditems.config

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import org.bukkit.Particle
import org.bukkit.configuration.file.FileConfiguration

/**
 * Top-level configuration holder. Owns the effect settings ([fileConfig]) and the
 * storage settings ([storage]). Despawn locations themselves live in the plugin's
 * [io.fairyfox.papermc.despawneditems.location.LocationManager], not here. Call [load] to
 * re-read `config.yml` from disk and rebuild both.
 */
class Config(val plugin: PaperMcDespawnedItems) {
    lateinit var fileConfig: FileConfig
        private set

    lateinit var storage: StorageSettings
        private set

    lateinit var performance: PerformanceSettings
        private set

    lateinit var limits: LimitSettings
        private set

    lateinit var commands: CommandSettings
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
        limits = LimitSettings(plugin.config)
        commands = CommandSettings(plugin.config, plugin.logger)
    }
}

/**
 * Command names and aliases, read from the `commands:` section so `/despi` and
 * `/recycle` can be renamed if they clash with another plugin. Names are validated
 * ([sanitize]); invalid entries fall back to the default with a console warning.
 * Commands register once at startup, so changes need a **server restart** (not just
 * `/despi reload`).
 */
class CommandSettings(c: FileConfiguration, logger: java.util.logging.Logger? = null) {
    /** Name registered for the main management command (default `despi`). */
    val despiName: String = sanitize(c.getString("commands.despi"), DEFAULT_DESPI, logger)

    /** Extra aliases for the main command. */
    val despiAliases: List<String> = sanitizeAliases(c.getStringList("commands.despi-aliases"), logger)

    /** Name registered for the recycle command (default `recycle`). */
    val recycleName: String = sanitize(c.getString("commands.recycle"), DEFAULT_RECYCLE, logger)

    /** Extra aliases for the recycle command. */
    val recycleAliases: List<String> = sanitizeAliases(c.getStringList("commands.recycle-aliases"), logger)

    private fun sanitize(
        raw: String?,
        fallback: String,
        logger: java.util.logging.Logger?,
    ): String {
        val name = raw?.trim()?.lowercase() ?: return fallback
        if (NAME_PATTERN.matches(name)) return name
        logger?.warning("Invalid command name '$raw' in config.yml commands: section; using '$fallback'.")
        return fallback
    }

    private fun sanitizeAliases(
        raw: List<String>,
        logger: java.util.logging.Logger?,
    ): List<String> =
        raw.mapNotNull { alias ->
            val name = alias.trim().lowercase()
            if (NAME_PATTERN.matches(name)) {
                name
            } else {
                logger?.warning("Invalid command alias '$alias' in config.yml commands: section; skipping.")
                null
            }
        }.distinct()

    private companion object {
        const val DEFAULT_DESPI = "despi"
        const val DEFAULT_RECYCLE = "recycle"

        /** Lowercase letters, digits, `_`, `-`, `.` — safe Brigadier literal names. */
        val NAME_PATTERN = Regex("[a-z0-9_.-]+")
    }
}

/**
 * Per-user despawn-location limits, read from the `limits:` section. The default cap
 * applies unless a player has a `despi.limit.<n>` permission (highest wins) or
 * `despi.limit.bypass`. See [io.fairyfox.papermc.despawneditems.limit.DespawnLimits].
 */
class LimitSettings(c: FileConfiguration) {
    /** Cap for a player with no limit permission. */
    val default: Int = c.getInt("limits.default", DEFAULT_LIMIT).coerceAtLeast(0)

    /** When true, no player is capped. */
    val unlimited: Boolean = c.getBoolean("limits.unlimited", false)

    private companion object {
        const val DEFAULT_LIMIT = 10
    }
}

/**
 * Large-server safety limits for the automatic despawn-relocation pipeline, read from
 * the `performance:` section. These bound how much relocation work happens per tick so
 * a flood of despawning items can never storm the server.
 */
class PerformanceSettings(c: FileConfiguration) {
    /** New relocations started per server tick. */
    val maxPerTick: Int = c.getInt("performance.max-per-tick", DEFAULT_MAX_PER_TICK).coerceAtLeast(1)

    /** Maximum relocations processing simultaneously. */
    val maxConcurrent: Int = c.getInt("performance.max-concurrent", DEFAULT_MAX_CONCURRENT).coerceAtLeast(1)

    /** Maximum items waiting to be relocated. */
    val maxQueue: Int = c.getInt("performance.max-queue", DEFAULT_MAX_QUEUE).coerceAtLeast(1)

    /** When the queue is full: true = ignore new items, false = drop the oldest. */
    val dropWhenFull: Boolean = c.getBoolean("performance.drop-when-full", true)

    private companion object {
        const val DEFAULT_MAX_PER_TICK = 20
        const val DEFAULT_MAX_CONCURRENT = 200
        const val DEFAULT_MAX_QUEUE = 10_000
    }
}

/**
 * Storage backend selection and connection details, read from the `storage:` section
 * of `config.yml`. Consumed by
 * [io.fairyfox.papermc.despawneditems.location.StorageFactory].
 */
class StorageSettings(c: FileConfiguration) {
    /** `yaml`, `sqlite`, or `mysql`. */
    val type: String = (c.getString("storage.type") ?: "yaml").lowercase().trim()

    val mysqlHost: String = c.getString("storage.mysql.host") ?: "localhost"
    val mysqlPort: Int = c.getInt("storage.mysql.port", DEFAULT_MYSQL_PORT)
    val mysqlDatabase: String = c.getString("storage.mysql.database") ?: "despawneditems"
    val mysqlUsername: String = c.getString("storage.mysql.username") ?: "root"
    val mysqlPassword: String = c.getString("storage.mysql.password").orEmpty()
    val mysqlProperties: String = c.getString("storage.mysql.properties").orEmpty()

    val poolMaximumSize: Int =
        c.getInt("storage.pool.maximum-pool-size", DEFAULT_POOL_SIZE).coerceIn(1, MAX_POOL_SIZE)

    private companion object {
        const val DEFAULT_MYSQL_PORT = 3306
        const val DEFAULT_POOL_SIZE = 10
        const val MAX_POOL_SIZE = 100
    }
}

/**
 * The main `config.yml` — particle and sound settings for the effect played when
 * an item lands in a despawn container.
 */
class FileConfig(private val plugin: PaperMcDespawnedItems) {
    var particlesEnabled: Boolean = true
    var particleFX: Particle = Particle.HAPPY_VILLAGER
    var particleLengthSeconds: Int = DEFAULT_LENGTH_SECONDS
    var newParticlesEveryNthTick: Int = DEFAULT_NEW_EVERY_TICK
    var particleCountEveryNthTick: Int = DEFAULT_COUNT
    var particleRandomRadius: Float = DEFAULT_RADIUS

    /**
     * Extra data required by some particles (e.g. DUST needs a [Particle.DustOptions]).
     * Null when the configured particle needs no data. Resolved at load time so the
     * effect never calls `spawnParticle` without data a particle requires (which threw).
     */
    var particleData: Any? = null
        private set

    var soundEnabled: Boolean = true

    /** A Minecraft sound key, e.g. `block.fire.extinguish`. */
    var soundKey: String = DEFAULT_SOUND
    var soundVolume: Float = DEFAULT_VOLUME
    var soundPitch: Float = DEFAULT_PITCH

    init {
        load()
    }

    fun load() {
        plugin.saveDefaultConfig()
        val c = plugin.config

        particlesEnabled = c.getBoolean("particles.enabled", true)
        val resolvedParticle =
            ParticleData.resolve(
                c.getString("particles.particle") ?: ParticleData.DEFAULT_PARTICLE_KEY,
                c.getString("particles.color"),
                c.getDouble("particles.dust-size", DEFAULT_DUST_SIZE),
            )
        particleFX = resolvedParticle.particle
        particleData = resolvedParticle.data
        resolvedParticle.warning?.let { plugin.logger.warning(it) }
        particleLengthSeconds = c.getInt("particles.length-seconds", DEFAULT_LENGTH_SECONDS)
        newParticlesEveryNthTick = c.getInt("particles.new-every-nth-tick", DEFAULT_NEW_EVERY_TICK)
        particleCountEveryNthTick = c.getInt("particles.count-every-nth-tick", DEFAULT_COUNT)
        particleRandomRadius = c.getDouble("particles.radius", DEFAULT_RADIUS.toDouble()).toFloat()

        soundEnabled = c.getBoolean("sound.enabled", true)
        soundKey = c.getString("sound.sound") ?: DEFAULT_SOUND
        soundVolume = c.getDouble("sound.volume", DEFAULT_VOLUME.toDouble()).toFloat()
        soundPitch = c.getDouble("sound.pitch", DEFAULT_PITCH.toDouble()).toFloat()
    }

    private companion object {
        const val DEFAULT_LENGTH_SECONDS = 3
        const val DEFAULT_NEW_EVERY_TICK = 2
        const val DEFAULT_COUNT = 15
        const val DEFAULT_RADIUS = 0.5f
        const val DEFAULT_DUST_SIZE = 1.0
        const val DEFAULT_SOUND = "block.fire.extinguish"
        const val DEFAULT_VOLUME = 1.0f
        const val DEFAULT_PITCH = 1.0f
    }
}
