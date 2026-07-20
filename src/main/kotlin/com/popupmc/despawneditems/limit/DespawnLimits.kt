package com.popupmc.despawneditems.limit

import com.popupmc.despawneditems.config.LimitSettings
import org.bukkit.entity.Player

/**
 * Resolves how many despawn locations a player is allowed to own. Precedence:
 *
 *  1. `limits.unlimited: true` in config, or the `despi.limit.bypass` permission → no cap.
 *  2. The highest `despi.limit.<n>` permission the player holds (assign these to
 *     LuckPerms/other groups for per-rank caps).
 *  3. The configured `limits.default`.
 *
 * Kept as a small, permission-reading utility so command code stays declarative.
 */
object DespawnLimits {
    const val BYPASS_PERMISSION = "despi.limit.bypass"
    private const val LIMIT_PREFIX = "despi.limit."

    /** The maximum number of locations [player] may own. [Int.MAX_VALUE] means unlimited. */
    fun resolve(
        player: Player,
        settings: LimitSettings,
    ): Int {
        if (settings.unlimited || player.hasPermission(BYPASS_PERMISSION)) return Int.MAX_VALUE

        val fromPermissions =
            player.effectivePermissions.asSequence()
                .filter { it.value } // only granted permissions
                .map { it.permission.lowercase() }
                .filter { it.startsWith(LIMIT_PREFIX) }
                .mapNotNull { it.removePrefix(LIMIT_PREFIX).toIntOrNull() }
                .maxOrNull()

        return fromPermissions ?: settings.default
    }

    /** Whether [player] is under their cap and may register another location. */
    fun canAddAnother(
        player: Player,
        currentCount: Int,
        settings: LimitSettings,
    ): Boolean = currentCount < resolve(player, settings)
}
