package io.fairyfox.papermc.despawneditems.config

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.editConfig
import org.bukkit.Particle
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** `Config.load()` re-reads edited files; settings sections coerce invalid values. */
class ConfigReloadTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: PaperMcDespawnedItems

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(PaperMcDespawnedItems::class.java)
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `defaults load out of the box`() {
        val s = plugin.settings
        assertEquals(10, s.limits.default)
        assertFalse(s.limits.unlimited)
        assertEquals(20, s.performance.maxPerTick)
        assertEquals(200, s.performance.maxConcurrent)
        assertEquals(10_000, s.performance.maxQueue)
        assertTrue(s.performance.dropWhenFull)
        assertEquals("yaml", s.storage.type)
        assertEquals(10, s.storage.poolMaximumSize)
        assertEquals("despi", s.commands.despiName)
        assertEquals(Particle.HAPPY_VILLAGER, s.fileConfig.particleFX)
        assertTrue(s.fileConfig.soundEnabled)
        assertEquals("block.fire.extinguish", s.fileConfig.soundKey)
    }

    @Test
    fun `edited files are picked up by a reload`() {
        editConfig(
            plugin,
            "limits.default" to 42,
            "performance.max-per-tick" to 3,
            "sound.sound" to "block.note_block.pling",
            "particles.enabled" to false,
        )
        val s = plugin.settings
        assertEquals(42, s.limits.default)
        assertEquals(3, s.performance.maxPerTick)
        assertEquals("block.note_block.pling", s.fileConfig.soundKey)
        assertFalse(s.fileConfig.particlesEnabled)
    }

    @Test
    fun `invalid numeric values are coerced to sane bounds`() {
        editConfig(
            plugin,
            "limits.default" to -5,
            "performance.max-per-tick" to 0,
            "performance.max-concurrent" to -1,
            "performance.max-queue" to 0,
            "storage.pool.maximum-pool-size" to 5000,
        )
        val s = plugin.settings
        assertEquals(0, s.limits.default, "negative limit clamps to 0")
        assertEquals(1, s.performance.maxPerTick)
        assertEquals(1, s.performance.maxConcurrent)
        assertEquals(1, s.performance.maxQueue)
        assertEquals(100, s.storage.poolMaximumSize, "pool size caps at 100")
    }

    @Test
    fun `an unknown particle falls back with a warning instead of erroring`() {
        editConfig(plugin, "particles.particle" to "not_a_particle")
        assertEquals(Particle.HAPPY_VILLAGER, plugin.settings.fileConfig.particleFX)
    }

    @Test
    fun `storage settings read the mysql block`() {
        editConfig(
            plugin,
            "storage.mysql.host" to "db.example.org",
            "storage.mysql.port" to 3307,
            "storage.mysql.database" to "mc",
            "storage.mysql.username" to "fox",
            "storage.mysql.password" to "hunter2",
            "storage.mysql.properties" to "useSSL=true",
        )
        val s = plugin.settings.storage
        assertEquals("db.example.org", s.mysqlHost)
        assertEquals(3307, s.mysqlPort)
        assertEquals("mc", s.mysqlDatabase)
        assertEquals("fox", s.mysqlUsername)
        assertEquals("hunter2", s.mysqlPassword)
        assertEquals("useSSL=true", s.mysqlProperties)
    }
}
