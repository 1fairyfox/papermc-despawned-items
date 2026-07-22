package io.fairyfox.papermc.despawneditems.location

/**
 * Per-target settings for a single [DespawnLocation] — the state behind the in-world
 * toggle button and its options menu.
 *
 * **Serialisation is prefix-based and backward compatible.** The historic on-disk form is
 * `x;y;z;world`, and a world name may itself contain `;` (the parser re-joins the tail),
 * so options cannot simply be appended. Instead a non-default target is written as:
 *
 * ```
 * @enabled=false,priority=3|12;64;-8;my;world
 * ```
 *
 * A coordinate can never start with `@`, so the marker is unambiguous, and a target whose
 * options are all default is written in exactly the old format — meaning existing data
 * loads untouched and a server that never uses the feature never sees a format change.
 *
 * @property enabled whether this target participates in relocation at all. This is what the
 *   button toggles: `false` keeps the registration (and its options) but makes the pipeline
 *   skip it, which is what an admin wants when temporarily taking a chest out of service.
 * @property priority relative draw weight, 1–10. The pipeline picks candidate targets at
 *   random; a priority-3 target is three times as likely to be tried as a priority-1 one.
 * @property acceptContraband whether this target may receive items the void rules would
 *   otherwise destroy. Off by default — opting a chest in is a deliberate act.
 */
data class TargetOptions(
    val enabled: Boolean = true,
    val priority: Int = DEFAULT_PRIORITY,
    val acceptContraband: Boolean = false,
) {
    /** True when every field is at its default, i.e. the classic on-disk form is enough. */
    val isDefault: Boolean get() = this == DEFAULT

    /** The `key=value,…` block, without the `@` marker or the `|` terminator. */
    fun serializeFields(): String =
        buildList {
            if (!enabled) add("enabled=false")
            if (priority != DEFAULT_PRIORITY) add("priority=$priority")
            if (acceptContraband) add("contraband=true")
        }.joinToString(",")

    companion object {
        const val DEFAULT_PRIORITY = 1
        const val MIN_PRIORITY = 1
        const val MAX_PRIORITY = 10

        /** All-defaults instance; shared so the common case allocates nothing. */
        val DEFAULT = TargetOptions()

        /** Marker that introduces an options block. Coordinates can never start with it. */
        const val MARKER = '@'

        /** Separates the options block from the classic `x;y;z;world` payload. */
        const val TERMINATOR = '|'

        /**
         * Parses a `key=value,…` block. Unknown keys and unparsable values are ignored
         * rather than failing the load — a hand-edited file should degrade, not break the
         * server.
         */
        fun parseFields(raw: String): TargetOptions {
            var enabled = true
            var priority = DEFAULT_PRIORITY
            var contraband = false
            for (part in raw.split(',')) {
                val idx = part.indexOf('=')
                if (idx <= 0) continue
                val key = part.substring(0, idx).trim().lowercase()
                val value = part.substring(idx + 1).trim()
                when (key) {
                    "enabled" -> enabled = value.toBooleanStrictOrNull() ?: enabled
                    "priority" -> priority = value.toIntOrNull()?.coerceIn(MIN_PRIORITY, MAX_PRIORITY) ?: priority
                    "contraband" -> contraband = value.toBooleanStrictOrNull() ?: contraband
                }
            }
            return TargetOptions(enabled, priority, contraband)
        }
    }
}
