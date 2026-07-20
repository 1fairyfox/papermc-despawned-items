package com.popupmc.despawneditems.config

import org.bukkit.configuration.file.YamlConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Pure config-parsing + boundary tests for [PerformanceSettings]. */
class PerformanceSettingsTest {
    @Test
    fun `defaults when nothing is configured`() {
        val p = PerformanceSettings(YamlConfiguration())
        assertEquals(20, p.maxPerTick)
        assertEquals(200, p.maxConcurrent)
        assertEquals(10_000, p.maxQueue)
        assertTrue(p.dropWhenFull)
    }

    @Test
    fun `reads configured values`() {
        val c =
            YamlConfiguration().apply {
                set("performance.max-per-tick", 5)
                set("performance.max-concurrent", 50)
                set("performance.max-queue", 100)
                set("performance.drop-when-full", false)
            }
        val p = PerformanceSettings(c)
        assertEquals(5, p.maxPerTick)
        assertEquals(50, p.maxConcurrent)
        assertEquals(100, p.maxQueue)
        assertFalse(p.dropWhenFull)
    }

    @Test
    fun `coerces non-positive limits up to 1`() {
        val c =
            YamlConfiguration().apply {
                set("performance.max-per-tick", 0)
                set("performance.max-concurrent", -5)
                set("performance.max-queue", 0)
            }
        val p = PerformanceSettings(c)
        assertEquals(1, p.maxPerTick)
        assertEquals(1, p.maxConcurrent)
        assertEquals(1, p.maxQueue)
    }
}
