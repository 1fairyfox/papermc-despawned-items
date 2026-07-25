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

    lateinit var throttle: ThrottleSettings
        private set

    lateinit var voiding: VoidSettings
        private set

    lateinit var targetUi: TargetUiSettings
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
        throttle = ThrottleSettings(plugin.config, plugin.logger)
        voiding = VoidSettings(plugin.config, plugin.logger)
        targetUi = TargetUiSettings(plugin.config)
    }
}

/**
 * The `targets:` section — whether this server will talk to a **client-side mod**.
 *
 * There is deliberately nothing here about in-world items or fake menus. The server-side
 * surface is `/despi`, which works identically on every client and needs no config. This
 * section governs only the optional extra: whether a player running the companion mod gets a
 * real interface, which is a decision that belongs to the server owner.
 */
class TargetUiSettings(c: FileConfiguration) {
    /**
     * Whether client-side mods may drive this plugin at all.
     *
     * Set false and the plugin refuses the handshake, so a conforming mod hides its
     * interface instead of showing controls that silently fail. Nothing else changes:
     * `/despi` is unaffected, and no player loses a capability they had.
     */
    val clientModEnabled: Boolean = c.getBoolean("targets.client-mod.enabled", true)
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
 * Which per-user throttling policies are active. Selected by `throttle.strategy`.
 *
 * Kept as an enum with capability flags rather than a set of booleans so an admin picks
 * one intelligible word, and so [io.fairyfox.papermc.despawneditems.throttle.ThrottleManager]
 * can branch without string comparisons on the hot path.
 */
enum class ThrottleStrategy(
    val appliesRate: Boolean,
    val appliesConcurrent: Boolean,
    val appliesFairShare: Boolean,
) {
    /** No per-user throttling — the global performance budget is the only bound. */
    NONE(false, false, false),

    /** "Max per chunk of time": N relocations per player per window. */
    RATE(true, false, false),

    /** "Max per each one": N relocations in flight per player at once. */
    CONCURRENT(false, true, false),

    /** Weighted round-robin so no one player monopolises the drain. */
    FAIR_SHARE(false, false, true),

    /** All three together — the recommended setting when throttling is switched on. */
    COMBINED(true, true, true),
    ;

    companion object {
        /** Parses a config string (`fair-share`, `FAIR_SHARE`, …); null when unrecognised. */
        fun parse(raw: String?): ThrottleStrategy? {
            val key = raw?.trim()?.uppercase()?.replace('-', '_') ?: return null
            return entries.firstOrNull { it.name == key }
        }
    }
}

/**
 * Per-user throttling of the despawn pipeline, read from the `throttle:` section.
 *
 * **Off by default** — a server upgrading to this version sees no behaviour change until
 * an admin sets `throttle.enabled: true`. See
 * [io.fairyfox.papermc.despawneditems.throttle.ThrottleManager] for the semantics and
 * [io.fairyfox.papermc.despawneditems.throttle.ThrottleQuotas] for the permission nodes
 * that let different ranks get different allowances.
 */
class ThrottleSettings(c: FileConfiguration, logger: java.util.logging.Logger? = null) {
    /** Master switch. When false nothing in this class is consulted on the hot path. */
    val enabled: Boolean = c.getBoolean("throttle.enabled", false)

    /** Which policies apply. Unrecognised values fall back to [ThrottleStrategy.COMBINED] with a warning. */
    val strategy: ThrottleStrategy =
        ThrottleStrategy.parse(c.getString("throttle.strategy"))
            ?: run {
                val raw = c.getString("throttle.strategy")
                if (enabled && raw != null) {
                    logger?.warning("Unknown throttle.strategy '$raw' in config.yml; using 'combined'.")
                }
                ThrottleStrategy.COMBINED
            }

    /** Relocations a player may start per [windowSeconds] before being throttled. */
    val ratePerWindow: Int = c.getInt("throttle.rate.max-per-window", DEFAULT_RATE).coerceAtLeast(0)

    /** Length of the rate window in seconds. */
    val windowSeconds: Long = c.getLong("throttle.rate.window-seconds", DEFAULT_WINDOW).coerceAtLeast(1L)

    /** Relocations a single player may have in flight simultaneously. */
    val maxConcurrentPerPlayer: Int =
        c.getInt("throttle.concurrent.max-per-player", DEFAULT_CONCURRENT).coerceAtLeast(0)

    /** Fair-share weight for players with no `despi.throttle.weight.<n>` permission. */
    val defaultWeight: Int = c.getInt("throttle.fair-share.default-weight", DEFAULT_WEIGHT).coerceAtLeast(1)

    /** Whether ownerless drops (mob loot, dispensers) are throttled too. */
    val throttleUnowned: Boolean = c.getBoolean("throttle.throttle-unowned", false)

    /** `defer` (retry later, default), `drop` (let it despawn), or `void` (hand to the void/catch-all path). */
    val onLimit: String = (c.getString("throttle.on-limit") ?: DEFAULT_ON_LIMIT).lowercase().trim()

    /** [onLimit] resolved to the decision the throttler returns when a quota is exceeded. */
    val onLimitDecision: io.fairyfox.papermc.despawneditems.throttle.ThrottleDecision =
        when (onLimit) {
            "drop", "void" -> io.fairyfox.papermc.despawneditems.throttle.ThrottleDecision.DROP
            else -> io.fairyfox.papermc.despawneditems.throttle.ThrottleDecision.DEFER
        }

    /** True when an over-quota item should be handed to the void/catch-all path rather than simply lost. */
    val overLimitToCatchAll: Boolean = onLimit == "void"

    private companion object {
        const val DEFAULT_RATE = 60
        const val DEFAULT_WINDOW = 60L
        const val DEFAULT_CONCURRENT = 20
        const val DEFAULT_WEIGHT = 1
        const val DEFAULT_ON_LIMIT = "defer"
    }
}

/**
 * A configured catch-all destination: a block position given as `world;x;y;z` in
 * `config.yml`. Admin-owned and config-driven **by design** — catch-alls exist to absorb
 * contraband and voided items, which is an operator concern, not a player one. Keeping
 * them in config (rather than as flagged [io.fairyfox.papermc.despawneditems.location.DespawnLocation]s)
 * also means no storage-schema migration and no way for a player to register one.
 */
data class CatchAllTarget(val world: String, val x: Int, val y: Int, val z: Int) {
    companion object {
        private const val FIELDS = 4
        private const val WORLD_INDEX = 0
        private const val X_INDEX = 1
        private const val Y_INDEX = 2
        private const val Z_INDEX = 3

        /** Parses `world;x;y;z`; null (never throws) when malformed. */
        fun parse(raw: String): CatchAllTarget? {
            val parts = raw.split(';').map { it.trim() }
            if (parts.size != FIELDS) return null
            val world = parts[WORLD_INDEX]
            if (world.isEmpty()) return null
            val x = parts[X_INDEX].toIntOrNull() ?: return null
            val y = parts[Y_INDEX].toIntOrNull() ?: return null
            val z = parts[Z_INDEX].toIntOrNull() ?: return null
            return CatchAllTarget(world, x, y, z)
        }
    }
}

/**
 * The `void:` section — a configurable chance that an item is voided rather than
 * relocated, an admin-extensible banned-material list, and one or more **catch-all**
 * destinations that receive banned and voided items instead of destroying them.
 *
 * **Inert by default**: `chance: 0.0`, no extra banned materials, catch-all disabled.
 */
class VoidSettings(c: FileConfiguration, logger: java.util.logging.Logger? = null) {
    /** Probability in `0.0..1.0` that any given despawning item is voided outright. */
    val chance: Double = c.getDouble("void.chance", 0.0).coerceIn(0.0, 1.0)

    /**
     * Extra material names treated as contraband, on top of the built-in list in
     * [io.fairyfox.papermc.despawneditems.despawn.into.DespawnIntoVoid]. Unknown names are
     * warned about and skipped rather than failing the load.
     */
    val bannedMaterials: Set<org.bukkit.Material> =
        c.getStringList("void.banned-materials")
            .mapNotNull { name ->
                val material = org.bukkit.Material.matchMaterial(name.trim())
                if (material == null) logger?.warning("Unknown material '$name' in config.yml void.banned-materials; skipping.")
                material
            }.toSet()

    /** When true, banned/voided items are delivered to [catchAllTargets] instead of destroyed. */
    val catchAllEnabled: Boolean = c.getBoolean("void.catch-all.enabled", false)

    /** `first` (fill in order) or `random` (spread the load). */
    val catchAllMode: String = (c.getString("void.catch-all.mode") ?: "first").lowercase().trim()

    /** Parsed catch-all destinations; malformed entries are warned about and skipped. */
    val catchAllTargets: List<CatchAllTarget> =
        c.getStringList("void.catch-all.locations")
            .mapNotNull { raw ->
                val parsed = CatchAllTarget.parse(raw)
                if (parsed == null) logger?.warning("Malformed catch-all location '$raw' in config.yml; expected 'world;x;y;z'.")
                parsed
            }

    /** True when a catch-all is both switched on and actually has somewhere to put things. */
    val catchAllUsable: Boolean get() = catchAllEnabled && catchAllTargets.isNotEmpty()

    /** When every catch-all is full: true = destroy the item, false = let it fall through to normal relocation. */
    val destroyWhenCatchAllFull: Boolean = c.getBoolean("void.catch-all.destroy-when-full", true)
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
