package com.popupmc.despawneditems

import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Acceptance-level smoke test: boot a mocked Paper 1.21 server, load the plugin, and
 * confirm it enables cleanly with its commands registered. This is the harness proof
 * that the whole plugin comes up under MockBukkit.
 */
class PluginEnableTest {

    private lateinit var server: ServerMock
    private lateinit var plugin: DespawnedItems

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(DespawnedItems::class.java)
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `plugin enables cleanly`() {
        assertTrue(plugin.isEnabled, "plugin should be enabled after load")
    }

    @Test
    fun `commands are registered`() {
        assertNotNull(server.getPluginCommand("despi"), "/despi should be registered")
        assertNotNull(server.getPluginCommand("recycle"), "/recycle should be registered")
    }

    @Test
    fun `reward pool is populated and excludes high-value or technical items`() {
        val pool = RewardPool.items
        assertTrue(pool.isNotEmpty(), "reward pool should not be empty")
        assertTrue(pool.none { it.name.contains("NETHERITE") }, "no netherite in rewards")
        assertTrue(pool.none { it.name.contains("DIAMOND") }, "no diamond in rewards")
        assertTrue(pool.none { it.name.contains("COMMAND") }, "no command blocks in rewards")
        assertTrue(pool.none { it.name.endsWith("AIR") }, "no air in rewards")
    }
}
