package io.fairyfox.papermc.despawneditems.despawn

import io.fairyfox.papermc.despawneditems.PaperMcDespawnedItems
import io.fairyfox.papermc.despawneditems.editConfig
import org.bukkit.Location
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.world.WorldMock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DespawnEffectTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: PaperMcDespawnedItems
    private lateinit var world: WorldMock

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(PaperMcDespawnedItems::class.java)
        world = server.addSimpleWorld("world")
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    private fun loc() = Location(world, 0.0, 64.0, 0.0)

    @Test
    fun `an effect plays its particle loops then removes itself`() {
        DespawnEffect(loc(), plugin)
        assertEquals(1, plugin.effectsPlaying.size)

        // Defaults: 3s × 20 ticks / every 2 ticks = 30 loops at 2-tick period.
        server.scheduler.performTicks(70)

        assertEquals(0, plugin.effectsPlaying.size, "effect should expire after its loops")
    }

    @Test
    fun `with particles disabled the effect only plays the sound and ends at once`() {
        editConfig(plugin, "particles.enabled" to false)
        DespawnEffect(loc(), plugin)
        assertEquals(0, plugin.effectsPlaying.size, "no particle loop should be scheduled")
    }

    @Test
    fun `with sound disabled particles still play`() {
        editConfig(plugin, "sound.enabled" to false)
        DespawnEffect(loc(), plugin)
        assertEquals(1, plugin.effectsPlaying.size)
        server.scheduler.performTicks(70)
        assertEquals(0, plugin.effectsPlaying.size)
    }

    @Test
    fun `forceSelfDestroy stops a playing effect immediately`() {
        val effect = DespawnEffect(loc(), plugin)
        assertTrue(plugin.effectsPlaying.contains(effect))
        effect.forceSelfDestroy()
        assertEquals(0, plugin.effectsPlaying.size)
        server.scheduler.performTicks(5)
        assertEquals(0, plugin.effectsPlaying.size)
    }
}
