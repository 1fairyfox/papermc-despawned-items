package io.fairyfox.papermc.despawneditems.config

import org.bukkit.configuration.file.YamlConfiguration
import java.io.InputStreamReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * `plugin.yml` validity (testing.md §11): the descriptor must agree with the code —
 * right main class, right api-version, every permission the code checks declared.
 */
class PluginYmlValidityTest {
    private fun pluginYml(): YamlConfiguration {
        val stream = checkNotNull(javaClass.classLoader.getResourceAsStream("plugin.yml")) { "plugin.yml missing" }
        return stream.use { YamlConfiguration.loadConfiguration(InputStreamReader(it, Charsets.UTF_8)) }
    }

    @Test
    fun `name follows the papermc-despawned-items naming standard`() {
        assertEquals("papermc-despawned-items", pluginYml().getString("name"))
    }

    @Test
    fun `the main class exists and is the plugin`() {
        val main = assertNotNull(pluginYml().getString("main"))
        val clazz = Class.forName(main)
        assertTrue(org.bukkit.plugin.java.JavaPlugin::class.java.isAssignableFrom(clazz))
    }

    @Test
    fun `api-version matches the Paper 1_21 target`() {
        assertEquals("1.21", pluginYml().getString("api-version"))
    }

    @Test
    fun `the version placeholder is expanded by the build`() {
        val version = assertNotNull(pluginYml().getString("version"))
        assertFalse(version.contains("\${"), "processResources must expand \${version}")
        assertTrue(version.isNotBlank())
    }

    @Test
    fun `every permission the code checks is declared`() {
        // Permission nodes contain dots, so they nest under YAML path separators;
        // deep keys ("permissions.despi.use.description") prove the node is declared.
        val yml = pluginYml()
        for (needed in listOf("despi.use", "despi.elevated", "despi.limit.bypass", "recycle.use")) {
            assertTrue(
                yml.contains("permissions.$needed.description"),
                "$needed must be declared in plugin.yml",
            )
        }
    }

    @Test
    fun `runtime libraries cover the database backends`() {
        val libraries = pluginYml().getStringList("libraries")
        assertTrue(libraries.any { it.startsWith("com.zaxxer:HikariCP") }, "HikariCP pool")
        assertTrue(libraries.any { it.startsWith("org.xerial:sqlite-jdbc") }, "sqlite driver")
        assertTrue(libraries.any { it.startsWith("org.mariadb.jdbc") }, "mariadb driver")
    }
}
