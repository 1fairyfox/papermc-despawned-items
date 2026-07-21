package io.fairyfox.papermc.despawneditems.config

import org.bukkit.configuration.file.YamlConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals

/** Pure unit tests for command renaming/aliases — no server needed. */
class CommandSettingsTest {
    private fun yaml(text: String): YamlConfiguration = YamlConfiguration().also { it.loadFromString(text) }

    @Test
    fun `defaults apply when the section is absent`() {
        val s = CommandSettings(yaml(""))
        assertEquals("despi", s.despiName)
        assertEquals("recycle", s.recycleName)
        assertEquals(emptyList(), s.despiAliases)
        assertEquals(emptyList(), s.recycleAliases)
    }

    @Test
    fun `valid names and aliases are accepted lowercased and trimmed`() {
        val s =
            CommandSettings(
                yaml(
                    """
                    commands:
                      despi: ' Items '
                      despi-aliases: [di, DI, despawned.items]
                      recycle: RECYCLE-2
                      recycle-aliases: [rec_1]
                    """.trimIndent(),
                ),
            )
        assertEquals("items", s.despiName)
        assertEquals(listOf("di", "despawned.items"), s.despiAliases, "aliases dedupe after lowercasing")
        assertEquals("recycle-2", s.recycleName)
        assertEquals(listOf("rec_1"), s.recycleAliases)
    }

    @Test
    fun `invalid names fall back to the defaults`() {
        val s =
            CommandSettings(
                yaml(
                    """
                    commands:
                      despi: 'has spaces'
                      recycle: ''
                      despi-aliases: ['bad alias', ok]
                    """.trimIndent(),
                ),
            )
        assertEquals("despi", s.despiName)
        assertEquals("recycle", s.recycleName)
        assertEquals(listOf("ok"), s.despiAliases, "invalid aliases are skipped, valid kept")
    }
}
