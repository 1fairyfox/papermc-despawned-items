package io.fairyfox.papermc.despawneditems.config

import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Config-parsing tests for the two new sections. These matter more than they look: a bad
 * value in `config.yml` must degrade to a documented default with a warning, never abort
 * the plugin load — a server owner's typo should not take their server down.
 */
class ThrottleAndVoidSettingsTest {
    private fun yaml(vararg entries: Pair<String, Any?>) =
        YamlConfiguration().apply { entries.forEach { (path, value) -> set(path, value) } }

    // --- throttle: -------------------------------------------------------------------

    @Test
    fun `throttle defaults are inert`() {
        val settings = ThrottleSettings(yaml())
        assertFalse(settings.enabled, "throttling must be OFF by default so upgrades change nothing")
    }

    @Test
    fun `throttle values are read from config`() {
        val settings =
            ThrottleSettings(
                yaml(
                    "throttle.enabled" to true,
                    "throttle.strategy" to "rate",
                    "throttle.rate.max-per-window" to 123,
                    "throttle.rate.window-seconds" to 45,
                    "throttle.concurrent.max-per-player" to 9,
                    "throttle.fair-share.default-weight" to 6,
                    "throttle.throttle-unowned" to true,
                ),
            )
        assertTrue(settings.enabled)
        assertEquals(ThrottleStrategy.RATE, settings.strategy)
        assertEquals(123, settings.ratePerWindow)
        assertEquals(45L, settings.windowSeconds)
        assertEquals(9, settings.maxConcurrentPerPlayer)
        assertEquals(6, settings.defaultWeight)
        assertTrue(settings.throttleUnowned)
    }

    @Test
    fun `negative and zero values are coerced into sane ranges`() {
        val settings =
            ThrottleSettings(
                yaml(
                    "throttle.enabled" to true,
                    "throttle.rate.max-per-window" to -50,
                    "throttle.rate.window-seconds" to 0,
                    "throttle.concurrent.max-per-player" to -1,
                    "throttle.fair-share.default-weight" to 0,
                ),
            )
        assertEquals(0, settings.ratePerWindow, "a negative rate clamps to 0 (blocked), never to a negative bucket")
        assertEquals(1L, settings.windowSeconds, "a zero window would divide by zero; it clamps to 1s")
        assertEquals(0, settings.maxConcurrentPerPlayer)
        assertEquals(1, settings.defaultWeight, "weight 0 would starve the actor entirely")
    }

    @Test
    fun `on-limit spellings map to the right decision`() {
        fun decisionFor(value: String) = ThrottleSettings(yaml("throttle.enabled" to true, "throttle.on-limit" to value))

        assertFalse(decisionFor("defer").overLimitToCatchAll)
        assertFalse(decisionFor("drop").overLimitToCatchAll)
        assertTrue(decisionFor("void").overLimitToCatchAll)
        assertFalse(decisionFor("NONSENSE").overLimitToCatchAll, "an unknown value falls back to the lossless default")
    }

    @Test
    fun `every strategy exposes the right capability flags`() {
        assertFalse(ThrottleStrategy.NONE.appliesRate)
        assertTrue(ThrottleStrategy.RATE.appliesRate)
        assertFalse(ThrottleStrategy.RATE.appliesConcurrent)
        assertTrue(ThrottleStrategy.CONCURRENT.appliesConcurrent)
        assertTrue(ThrottleStrategy.FAIR_SHARE.appliesFairShare)
        assertTrue(ThrottleStrategy.COMBINED.appliesRate)
        assertTrue(ThrottleStrategy.COMBINED.appliesConcurrent)
        assertTrue(ThrottleStrategy.COMBINED.appliesFairShare)
    }

    // --- void: -----------------------------------------------------------------------

    @Test
    fun `void defaults are inert`() {
        val settings = VoidSettings(yaml())
        assertEquals(0.0, settings.chance)
        assertTrue(settings.bannedMaterials.isEmpty())
        assertFalse(settings.catchAllEnabled)
        assertFalse(settings.catchAllUsable)
    }

    @Test
    fun `void chance is clamped to a probability`() {
        assertEquals(1.0, VoidSettings(yaml("void.chance" to 7.5)).chance)
        assertEquals(0.0, VoidSettings(yaml("void.chance" to -3.0)).chance)
        assertEquals(0.25, VoidSettings(yaml("void.chance" to 0.25)).chance)
    }

    @Test
    fun `banned materials are resolved and unknown names skipped`() {
        val settings = VoidSettings(yaml("void.banned-materials" to listOf("BEDROCK", "not_a_material", " barrier ")))
        assertTrue(Material.BEDROCK in settings.bannedMaterials)
        assertTrue(Material.BARRIER in settings.bannedMaterials, "names are trimmed and case-insensitive")
        assertEquals(2, settings.bannedMaterials.size, "the junk entry is skipped, not fatal")
    }

    @Test
    fun `catch-all is only usable when enabled AND populated`() {
        assertFalse(VoidSettings(yaml("void.catch-all.enabled" to true)).catchAllUsable)
        assertFalse(
            VoidSettings(yaml("void.catch-all.locations" to listOf("world;0;64;0"))).catchAllUsable,
            "locations without the switch stay off",
        )
        assertTrue(
            VoidSettings(
                yaml(
                    "void.catch-all.enabled" to true,
                    "void.catch-all.locations" to listOf("world;0;64;0"),
                ),
            ).catchAllUsable,
        )
    }

    @Test
    fun `catch-all targets are parsed and malformed entries dropped`() {
        val settings =
            VoidSettings(
                yaml(
                    "void.catch-all.enabled" to true,
                    "void.catch-all.locations" to listOf("world;0;64;0", "broken", "nether;-1;2;-3"),
                ),
            )
        assertEquals(
            listOf(CatchAllTarget("world", 0, 64, 0), CatchAllTarget("nether", -1, 2, -3)),
            settings.catchAllTargets,
        )
    }
}
