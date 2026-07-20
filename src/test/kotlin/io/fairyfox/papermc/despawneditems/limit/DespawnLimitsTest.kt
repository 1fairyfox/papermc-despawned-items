package io.fairyfox.papermc.despawneditems.limit

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.config.LimitSettings
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The permission matrix for [DespawnLimits]: default, `despi.limit.<n>` tiers (highest
 * wins), `despi.limit.bypass`, and the `limits.unlimited` config override.
 */
class DespawnLimitsTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: PaperMcDespawnedItems
    private val defaults = LimitSettings(YamlConfiguration())

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(PaperMcDespawnedItems::class.java)
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    private fun playerWith(vararg permissions: String): Player {
        val p = server.addPlayer()
        val attachment = p.addAttachment(plugin)
        permissions.forEach { attachment.setPermission(it, true) }
        p.recalculatePermissions()
        return p
    }

    @Test
    fun `default cap when the player has no limit permission`() {
        assertEquals(10, DespawnLimits.resolve(playerWith(), defaults))
    }

    @Test
    fun `a despi_limit_N permission sets the cap`() {
        assertEquals(50, DespawnLimits.resolve(playerWith("despi.limit.50"), defaults))
    }

    @Test
    fun `the highest limit permission wins`() {
        val player = playerWith("despi.limit.50", "despi.limit.100", "despi.limit.20")
        assertEquals(100, DespawnLimits.resolve(player, defaults))
    }

    @Test
    fun `bypass permission means unlimited`() {
        assertEquals(Int.MAX_VALUE, DespawnLimits.resolve(playerWith(DespawnLimits.BYPASS_PERMISSION), defaults))
    }

    @Test
    fun `unlimited config overrides everything`() {
        val unlimited = LimitSettings(YamlConfiguration().apply { set("limits.unlimited", true) })
        assertEquals(Int.MAX_VALUE, DespawnLimits.resolve(playerWith(), unlimited))
    }

    @Test
    fun `canAddAnother respects the resolved cap`() {
        val player = playerWith("despi.limit.3")
        assertTrue(DespawnLimits.canAddAnother(player, currentCount = 2, defaults))
        assertFalse(DespawnLimits.canAddAnother(player, currentCount = 3, defaults))
    }
}
