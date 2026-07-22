package io.fairyfox.papermc.despawneditems.throttle

import io.fairyfox.papermc.despawneditems.config.ThrottleSettings
import io.fairyfox.papermc.despawneditems.config.ThrottleStrategy
import org.bukkit.configuration.file.YamlConfiguration
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for [ThrottleManager] — each strategy in isolation, then combined.
 *
 * No server is needed: settings are built from a plain [YamlConfiguration], the player
 * lookup is stubbed to "always offline" (which exercises the config-default path), and the
 * clock is driven by hand so rate windows are deterministic.
 */
class ThrottleManagerTest {
    private var now = 0L
    private val alice = UUID.randomUUID()
    private val bob = UUID.randomUUID()

    private fun settings(vararg entries: Pair<String, Any?>): ThrottleSettings {
        val yaml = YamlConfiguration()
        yaml.set("throttle.enabled", true)
        entries.forEach { (path, value) -> yaml.set(path, value) }
        return ThrottleSettings(yaml)
    }

    private fun manager(settings: ThrottleSettings) = ThrottleManager({ settings }, { null }, { now })

    @Test
    fun `disabled throttling always allows`() {
        val yaml = YamlConfiguration()
        val manager = manager(ThrottleSettings(yaml))
        repeat(1_000) { assertSame(ThrottleDecision.ALLOW, manager.evaluate(alice)) }
    }

    @Test
    fun `strategy none always allows even when enabled`() {
        val manager = manager(settings("throttle.strategy" to "none"))
        repeat(500) { assertSame(ThrottleDecision.ALLOW, manager.evaluate(alice)) }
    }

    @Test
    fun `rate strategy defers past the per-window allowance`() {
        val manager =
            manager(
                settings(
                    "throttle.strategy" to "rate",
                    "throttle.rate.max-per-window" to 3,
                    "throttle.rate.window-seconds" to 10,
                ),
            )
        repeat(3) { assertSame(ThrottleDecision.ALLOW, manager.evaluate(alice), "token ${it + 1}") }
        assertSame(ThrottleDecision.DEFER, manager.evaluate(alice), "the 4th in the window is deferred")
    }

    @Test
    fun `rate budget refills as time passes`() {
        val manager =
            manager(
                settings(
                    "throttle.strategy" to "rate",
                    "throttle.rate.max-per-window" to 2,
                    "throttle.rate.window-seconds" to 10,
                ),
            )
        repeat(2) { manager.evaluate(alice) }
        assertSame(ThrottleDecision.DEFER, manager.evaluate(alice))

        now += 5_000L // half the window → one token back
        assertSame(ThrottleDecision.ALLOW, manager.evaluate(alice))
        assertSame(ThrottleDecision.DEFER, manager.evaluate(alice))
    }

    @Test
    fun `each actor gets an independent rate budget`() {
        val manager =
            manager(
                settings(
                    "throttle.strategy" to "rate",
                    "throttle.rate.max-per-window" to 1,
                ),
            )
        assertSame(ThrottleDecision.ALLOW, manager.evaluate(alice))
        assertSame(ThrottleDecision.DEFER, manager.evaluate(alice))
        assertSame(ThrottleDecision.ALLOW, manager.evaluate(bob), "bob's budget is his own")
    }

    @Test
    fun `concurrent strategy blocks once in-flight slots are taken`() {
        val manager =
            manager(
                settings(
                    "throttle.strategy" to "concurrent",
                    "throttle.concurrent.max-per-player" to 2,
                ),
            )
        assertSame(ThrottleDecision.ALLOW, manager.evaluate(alice))
        manager.onStart(alice)
        assertSame(ThrottleDecision.ALLOW, manager.evaluate(alice))
        manager.onStart(alice)

        assertEquals(2, manager.inFlightFor(alice))
        assertSame(ThrottleDecision.DEFER, manager.evaluate(alice), "at the concurrency cap")

        manager.onFinish(alice)
        assertSame(ThrottleDecision.ALLOW, manager.evaluate(alice), "a finished relocation frees a slot")
    }

    @Test
    fun `onFinish never drives the in-flight count negative`() {
        val manager = manager(settings("throttle.strategy" to "concurrent"))
        manager.onFinish(alice)
        manager.onFinish(alice)
        assertEquals(0, manager.inFlightFor(alice))
    }

    @Test
    fun `null actors are unthrottled unless throttle-unowned is set`() {
        val open = manager(settings("throttle.strategy" to "combined", "throttle.rate.max-per-window" to 1))
        repeat(50) { assertSame(ThrottleDecision.ALLOW, open.evaluate(null), "ownerless drops pass by default") }

        val strict =
            manager(
                settings(
                    "throttle.strategy" to "combined",
                    "throttle.rate.max-per-window" to 1,
                    "throttle.throttle-unowned" to true,
                ),
            )
        // Even with throttle-unowned, a null actor has no identity to key a bucket on, so
        // it is allowed through — the setting governs quota *resolution*, documented in
        // ThrottleQuotas. Pinned here so the behaviour cannot drift silently.
        assertSame(ThrottleDecision.ALLOW, strict.evaluate(null))
    }

    @Test
    fun `on-limit drop turns an over-quota item into DROP instead of DEFER`() {
        val manager =
            manager(
                settings(
                    "throttle.strategy" to "rate",
                    "throttle.rate.max-per-window" to 1,
                    "throttle.on-limit" to "drop",
                ),
            )
        assertSame(ThrottleDecision.ALLOW, manager.evaluate(alice))
        assertSame(ThrottleDecision.DROP, manager.evaluate(alice))
    }

    @Test
    fun `on-limit void also drops and flags the catch-all hand-off`() {
        val settings =
            settings(
                "throttle.strategy" to "rate",
                "throttle.rate.max-per-window" to 1,
                "throttle.on-limit" to "void",
            )
        assertTrue(settings.overLimitToCatchAll, "'void' routes over-quota items to the catch-all")
        val manager = manager(settings)
        manager.evaluate(alice)
        assertSame(ThrottleDecision.DROP, manager.evaluate(alice))
    }

    @Test
    fun `fair-share weight comes from config when the actor is offline`() {
        val manager = manager(settings("throttle.strategy" to "fair-share", "throttle.fair-share.default-weight" to 4))
        assertEquals(4, manager.shareFor(alice))
    }

    @Test
    fun `share is unbounded when fair-share is not part of the strategy`() {
        val manager = manager(settings("throttle.strategy" to "rate"))
        assertEquals(Int.MAX_VALUE, manager.shareFor(alice))
    }

    @Test
    fun `combined applies rate and concurrency together`() {
        val manager =
            manager(
                settings(
                    "throttle.strategy" to "combined",
                    "throttle.rate.max-per-window" to 10,
                    "throttle.concurrent.max-per-player" to 1,
                ),
            )
        assertSame(ThrottleDecision.ALLOW, manager.evaluate(alice))
        manager.onStart(alice)
        assertSame(ThrottleDecision.DEFER, manager.evaluate(alice), "concurrency binds before the rate does")
    }

    @Test
    fun `purgeIdle evicts refilled actors but keeps busy ones`() {
        val manager =
            manager(
                settings(
                    "throttle.strategy" to "combined",
                    "throttle.rate.max-per-window" to 2,
                    "throttle.rate.window-seconds" to 1,
                ),
            )
        manager.evaluate(alice)
        manager.evaluate(bob)
        manager.onStart(bob)
        assertTrue(manager.trackedActors >= 2)

        now += 60_000L // everyone's bucket refills
        manager.purgeIdle()

        assertEquals(0, manager.inFlightFor(alice))
        assertEquals(1, manager.inFlightFor(bob), "bob still has work in flight and must not be forgotten")
    }

    @Test
    fun `reset clears all state`() {
        val manager = manager(settings("throttle.strategy" to "combined", "throttle.rate.max-per-window" to 1))
        manager.evaluate(alice)
        manager.onStart(alice)
        manager.reset()
        assertEquals(0, manager.inFlightFor(alice))
        assertSame(ThrottleDecision.ALLOW, manager.evaluate(alice), "a reset bucket is full again")
    }

    @Test
    fun `strategy parsing accepts config spellings and rejects nonsense`() {
        assertEquals(ThrottleStrategy.FAIR_SHARE, ThrottleStrategy.parse("fair-share"))
        assertEquals(ThrottleStrategy.FAIR_SHARE, ThrottleStrategy.parse("FAIR_SHARE"))
        assertEquals(ThrottleStrategy.COMBINED, ThrottleStrategy.parse(" combined "))
        assertEquals(null, ThrottleStrategy.parse("sideways"))
        assertEquals(null, ThrottleStrategy.parse(null))
    }

    @Test
    fun `an unknown strategy falls back to combined rather than failing the load`() {
        val settings = settings("throttle.strategy" to "nonsense")
        assertEquals(ThrottleStrategy.COMBINED, settings.strategy)
    }

    @Test
    fun `decision allowed flag matches the enum`() {
        assertTrue(ThrottleDecision.ALLOW.allowed)
        assertFalse(ThrottleDecision.DEFER.allowed)
        assertFalse(ThrottleDecision.DROP.allowed)
    }
}
