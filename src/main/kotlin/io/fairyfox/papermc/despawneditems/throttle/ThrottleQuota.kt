package io.fairyfox.papermc.despawneditems.throttle

import io.fairyfox.papermc.despawneditems.config.ThrottleSettings
import org.bukkit.entity.Player
import java.util.UUID

/**
 * The resolved per-actor allowance. One immutable snapshot of "how much of the despawn
 * pipeline is this player entitled to right now".
 *
 * @property ratePerWindow relocations allowed per [ThrottleSettings.windowSeconds]; 0 means blocked.
 * @property maxConcurrent relocations this actor may have in flight simultaneously.
 * @property weight fair-share weight — a weight-3 player is drained three times as fast
 *   as a weight-1 player when the pipeline is contended. This is the knob that makes
 *   "some users get more despawned items than others" true.
 * @property bypass when true every policy waves the actor through untouched.
 */
data class ThrottleQuota(
    val ratePerWindow: Int,
    val maxConcurrent: Int,
    val weight: Int,
    val bypass: Boolean = false,
) {
    companion object {
        /** The quota handed to anyone who bypasses throttling entirely. */
        val UNLIMITED = ThrottleQuota(Int.MAX_VALUE, Int.MAX_VALUE, 1, bypass = true)
    }
}

/**
 * Resolves a [ThrottleQuota] from permissions, mirroring the precedence
 * [io.fairyfox.papermc.despawneditems.limit.DespawnLimits] already uses for location caps
 * so admins only have to learn one pattern:
 *
 *  1. `throttle.enabled: false` in config, or the `despi.throttle.bypass` permission → unlimited.
 *  2. The highest `despi.throttle.rate.<n>` / `.concurrent.<n>` / `.weight.<n>` node held.
 *  3. The configured defaults.
 *
 * Offline actors (the thrower logged out before their items timed out) cannot be asked
 * for permissions, so they fall back to the configured defaults — documented behaviour,
 * not an accident.
 */
object ThrottleQuotas {
    const val BYPASS_PERMISSION = "despi.throttle.bypass"
    private const val RATE_PREFIX = "despi.throttle.rate."
    private const val CONCURRENT_PREFIX = "despi.throttle.concurrent."
    private const val WEIGHT_PREFIX = "despi.throttle.weight."

    /** Quota for a live [player]. */
    fun resolve(
        player: Player,
        settings: ThrottleSettings,
    ): ThrottleQuota {
        if (!settings.enabled || player.hasPermission(BYPASS_PERMISSION)) return ThrottleQuota.UNLIMITED

        val granted =
            player.effectivePermissions.asSequence()
                .filter { it.value }
                .map { it.permission.lowercase() }
                .toList()

        return ThrottleQuota(
            ratePerWindow = highest(granted, RATE_PREFIX) ?: settings.ratePerWindow,
            maxConcurrent = highest(granted, CONCURRENT_PREFIX) ?: settings.maxConcurrentPerPlayer,
            weight = highest(granted, WEIGHT_PREFIX) ?: settings.defaultWeight,
        )
    }

    /**
     * Quota for an actor identified only by [uuid] — used on the despawn hot path, where
     * the thrower may well be offline. [online] is the player lookup (injected so tests
     * need no server).
     */
    fun resolve(
        uuid: UUID?,
        settings: ThrottleSettings,
        online: (UUID) -> Player?,
    ): ThrottleQuota {
        if (!settings.enabled) return ThrottleQuota.UNLIMITED
        if (uuid == null) {
            // Ownerless drops (mob loot, dispensers, block breaks) are only throttled when
            // the admin explicitly opts in — otherwise a busy mob farm would eat the budget
            // that players are supposed to be sharing.
            return if (settings.throttleUnowned) {
                ThrottleQuota(settings.ratePerWindow, settings.maxConcurrentPerPlayer, settings.defaultWeight)
            } else {
                ThrottleQuota.UNLIMITED
            }
        }
        val player = online(uuid)
        return if (player != null) {
            resolve(player, settings)
        } else {
            ThrottleQuota(settings.ratePerWindow, settings.maxConcurrentPerPlayer, settings.defaultWeight)
        }
    }

    private fun highest(
        granted: List<String>,
        prefix: String,
    ): Int? =
        granted.asSequence()
            .filter { it.startsWith(prefix) }
            .mapNotNull { it.removePrefix(prefix).toIntOrNull() }
            .filter { it >= 0 }
            .maxOrNull()
}
