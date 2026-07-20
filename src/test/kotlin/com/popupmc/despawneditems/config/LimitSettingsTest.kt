package com.popupmc.despawneditems.config

import org.bukkit.configuration.file.YamlConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Pure config-parsing + boundary tests for [LimitSettings]. */
class LimitSettingsTest {
    @Test
    fun `defaults when nothing is configured`() {
        val l = LimitSettings(YamlConfiguration())
        assertEquals(10, l.default)
        assertFalse(l.unlimited)
    }

    @Test
    fun `reads configured values`() {
        val c =
            YamlConfiguration().apply {
                set("limits.default", 25)
                set("limits.unlimited", true)
            }
        val l = LimitSettings(c)
        assertEquals(25, l.default)
        assertTrue(l.unlimited)
    }

    @Test
    fun `negative default is coerced to zero`() {
        val c = YamlConfiguration().apply { set("limits.default", -3) }
        assertEquals(0, LimitSettings(c).default)
    }
}
