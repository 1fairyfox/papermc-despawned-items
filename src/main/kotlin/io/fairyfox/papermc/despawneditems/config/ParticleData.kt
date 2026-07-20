package io.fairyfox.papermc.despawneditems.config

import org.bukkit.Color
import org.bukkit.Particle

/**
 * Pure resolution of a configured particle into a [Particle] plus any extra data it
 * needs — extracted from [FileConfig] so it can be unit-tested without a server.
 *
 * Regression guard: the effect used to call `spawnParticle` with **no data**, which
 * threw for any data-bearing particle (DUST, BLOCK, ITEM, …). This resolves and
 * validates the data once, up front: data-less particles get null, DUST-style particles
 * are built from a colour + size, and anything needing data this plugin doesn't model
 * (or an unknown key) falls back to [DEFAULT_PARTICLE] with a warning instead of
 * crashing at spawn time.
 */
object ParticleData {
    val DEFAULT_PARTICLE: Particle = Particle.HAPPY_VILLAGER
    const val DEFAULT_PARTICLE_KEY = "happy_villager"

    private const val MIN_DUST_SIZE = 0.01f
    private const val MAX_DUST_SIZE = 4.0f
    private const val HEX_LENGTH = 6
    private const val HEX_RADIX = 16

    /** The resolved particle, its spawn data (or null), and an optional warning to log. */
    data class Resolved(val particle: Particle, val data: Any?, val warning: String?)

    fun resolve(
        key: String,
        colorHex: String?,
        dustSize: Double,
    ): Resolved {
        val particle =
            parseParticle(key)
                ?: return Resolved(DEFAULT_PARTICLE, null, "Unknown particle '$key' in config; using $DEFAULT_PARTICLE")

        return when (particle.dataType) {
            Void::class.java -> Resolved(particle, null, null)
            Particle.DustOptions::class.java -> {
                val size = dustSize.toFloat().coerceIn(MIN_DUST_SIZE, MAX_DUST_SIZE)
                Resolved(particle, Particle.DustOptions(parseColor(colorHex), size), null)
            }
            else ->
                Resolved(
                    DEFAULT_PARTICLE,
                    null,
                    "Particle '$particle' requires data this plugin can't supply " +
                        "(${particle.dataType.simpleName}); using $DEFAULT_PARTICLE instead.",
                )
        }
    }

    /** Resolves a particle from a config key, tolerating namespaces and case; null if unknown. */
    private fun parseParticle(name: String): Particle? {
        val key = name.substringAfter(':').uppercase()
        return runCatching { Particle.valueOf(key) }.getOrNull()
    }

    /** Parses a `#RRGGBB` (or `RRGGBB`) hex colour; falls back to white on any error. */
    fun parseColor(hex: String?): Color {
        val cleaned = hex?.removePrefix("#")?.trim()
        if (cleaned == null || cleaned.length != HEX_LENGTH) return Color.WHITE
        return runCatching { Color.fromRGB(cleaned.toInt(HEX_RADIX)) }.getOrDefault(Color.WHITE)
    }
}
