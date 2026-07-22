package io.fairyfox.papermc.despawneditems.throttle

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.config.ThrottleSettings
import org.bukkit.configuration.file.YamlConfiguration
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.entity.PlayerMock
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Permission-resolution matrix for [ThrottleQuotas] — the layer that makes "some users get
 * more despawned items than others" true. Uses real [PlayerMock]s with real permission
 * attachments so the precedence rules are proven against Bukkit's own permission engine,
 * not a stub.
 */
class ThrottleQuotasTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: PaperMcDespawnedItems

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(PaperMcDespawnedItems::class.java)
    }

    @AfterTest
    fun tearDown() = MockBukkit.unmock()

    private fun settings(vararg entries: Pair<String, Any?>): ThrottleSettings {
        val yaml = YamlConfiguration()
        yaml.set("throttle.enabled", true)
        entries.forEach { (path, value) -> yaml.set(path, value) }
        return ThrottleSettings(yaml)
    }

    private fun player(vararg nodes: String): PlayerMock {
        val p = server.addPlayer()
        val attachment = p.addAttachment(plugin)
        nodes.forEach { attachment.setPermission(it, true) }
        p.recalculatePermissions()
        return p
    }

    @Test
    fun `a plain player gets the configured defaults`() {
        val quota =
            ThrottleQuotas.resolve(
                player(),
                settings(
                    "throttle.rate.max-per-window" to 30,
                    "throttle.concurrent.max-per-player" to 5,
                    "throttle.fair-share.default-weight" to 2,
                ),
            )
        assertEquals(30, quota.ratePerWindow)
        assertEquals(5, quota.maxConcurrent)
        assertEquals(2, quota.weight)
        assertFalse(quota.bypass)
    }

    @Test
    fun `throttling disabled in config means everyone is unlimited`() {
        val yaml = YamlConfiguration()
        val quota = ThrottleQuotas.resolve(player(), ThrottleSettings(yaml))
        assertTrue(quota.bypass)
        assertEquals(Int.MAX_VALUE, quota.ratePerWindow)
    }

    @Test
    fun `the bypass permission overrides every quota`() {
        val quota =
            ThrottleQuotas.resolve(
                player(ThrottleQuotas.BYPASS_PERMISSION),
                settings("throttle.rate.max-per-window" to 1),
            )
        assertTrue(quota.bypass)
        assertEquals(Int.MAX_VALUE, quota.maxConcurrent)
    }

    @Test
    fun `permission nodes override the config defaults`() {
        val quota =
            ThrottleQuotas.resolve(
                player("despi.throttle.rate.240", "despi.throttle.concurrent.50", "despi.throttle.weight.4"),
                settings("throttle.rate.max-per-window" to 30),
            )
        assertEquals(240, quota.ratePerWindow)
        assertEquals(50, quota.maxConcurrent)
        assertEquals(4, quota.weight)
    }

    @Test
    fun `the highest granted node wins when several are held`() {
        val quota =
            ThrottleQuotas.resolve(
                player("despi.throttle.rate.60", "despi.throttle.rate.500", "despi.throttle.rate.120"),
                settings(),
            )
        assertEquals(500, quota.ratePerWindow, "stacking groups should not demote a player")
    }

    @Test
    fun `malformed and negative nodes are ignored, not crashed on`() {
        val quota =
            ThrottleQuotas.resolve(
                player("despi.throttle.rate.lots", "despi.throttle.rate.-5", "despi.throttle.weight."),
                settings("throttle.rate.max-per-window" to 42, "throttle.fair-share.default-weight" to 3),
            )
        assertEquals(42, quota.ratePerWindow, "a junk node falls back to the config default")
        assertEquals(3, quota.weight)
    }

    @Test
    fun `an offline actor falls back to the configured defaults`() {
        val quota =
            ThrottleQuotas.resolve(
                UUID.randomUUID(),
                settings("throttle.rate.max-per-window" to 7),
            ) { null }
        assertEquals(7, quota.ratePerWindow)
        assertFalse(quota.bypass)
    }

    @Test
    fun `an online actor resolved by uuid picks up their permissions`() {
        val p = player("despi.throttle.rate.999")
        val quota = ThrottleQuotas.resolve(p.uniqueId, settings()) { p }
        assertEquals(999, quota.ratePerWindow)
    }

    @Test
    fun `ownerless drops bypass unless throttle-unowned is enabled`() {
        assertTrue(ThrottleQuotas.resolve(null, settings()) { null }.bypass)

        val strict = ThrottleQuotas.resolve(null, settings("throttle.throttle-unowned" to true)) { null }
        assertFalse(strict.bypass, "with throttle-unowned the ownerless quota is a real one")
    }
}
